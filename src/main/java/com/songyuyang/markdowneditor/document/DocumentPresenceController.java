package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.auth.AuthService;
import com.songyuyang.markdowneditor.auth.UserAccount;
import com.songyuyang.markdowneditor.auth.UserAccountRepository;
import com.songyuyang.markdowneditor.auth.UserPrincipal;
import com.songyuyang.markdowneditor.common.ApiException;
import com.songyuyang.markdowneditor.document.dto.PresenceMessage;
import com.songyuyang.markdowneditor.document.dto.PresenceUser;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
// 协同在线状态：处理进入/离开/光标/输入中，并广播给订阅者
public class DocumentPresenceController {
    private final DocumentMemberRepository documentMemberRepository;
    private final UserAccountRepository userAccountRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthService authService;
    private final Map<Long, Map<Long, PresenceUser>> presence = new ConcurrentHashMap<>();

    public DocumentPresenceController(DocumentMemberRepository documentMemberRepository,
                                      UserAccountRepository userAccountRepository,
                                      SimpMessagingTemplate messagingTemplate,
                                      AuthService authService) {
        this.documentMemberRepository = documentMemberRepository;
        this.userAccountRepository = userAccountRepository;
        this.messagingTemplate = messagingTemplate;
        this.authService = authService;
    }

    @MessageMapping("/document/{documentId}/presence")
    // 处理 presence 消息；优先使用 STOMP 认证，否则回退到 token
    public void handlePresence(@DestinationVariable Long documentId, PresenceMessage message, Principal principal) {
        UserPrincipal userPrincipal = principal instanceof UserPrincipal principalValue ? principalValue : null;
        if (userPrincipal == null && message != null && message.getToken() != null) {
            UserAccount user = authService.authenticate(message.getToken());
            if (user != null) {
                userPrincipal = new UserPrincipal(user.getId(), user.getUsername());
            }
        }
        if (userPrincipal == null) {
            return;
        }
        Long userId = userPrincipal.getUserId();
        documentMemberRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "no access"));

        Map<Long, PresenceUser> docPresence = presence.computeIfAbsent(documentId, id -> new ConcurrentHashMap<>());
        String type = message == null || message.getType() == null ? "" : message.getType();

        if ("leave".equalsIgnoreCase(type)) {
            docPresence.remove(userId);
            broadcastSnapshot(documentId, docPresence);
            return;
        }

        if ("cursor".equalsIgnoreCase(type)) {
            PresenceUser user = docPresence.get(userId);
            if (user == null) {
                user = createPresenceUser(userId, message);
                docPresence.put(userId, user);
            }
            user.setCursor(message.getCursor());
            PresenceMessage cursorMessage = new PresenceMessage();
            cursorMessage.setType("cursor");
            cursorMessage.setUserId(userId);
            cursorMessage.setDisplayName(user.getDisplayName());
            cursorMessage.setCursor(user.getCursor());
            messagingTemplate.convertAndSend("/topic/document/" + documentId + "/presence", cursorMessage);
            return;
        }

        if ("typing".equalsIgnoreCase(type)) {
            PresenceUser user = docPresence.get(userId);
            if (user == null) {
                user = createPresenceUser(userId, message);
                docPresence.put(userId, user);
            }
            PresenceMessage typingMessage = new PresenceMessage();
            typingMessage.setType("typing");
            typingMessage.setUserId(userId);
            typingMessage.setDisplayName(user.getDisplayName());
            messagingTemplate.convertAndSend("/topic/document/" + documentId + "/presence", typingMessage);
            return;
        }

        PresenceUser user = createPresenceUser(userId, message);
        docPresence.put(userId, user);
        broadcastSnapshot(documentId, docPresence);
    }

    // 构造 presence 用户信息（只保存必要字段）
    private PresenceUser createPresenceUser(Long userId, PresenceMessage message) {
        String displayName = userAccountRepository.findById(userId)
                .map(account -> account.getDisplayName())
                .orElse("user-" + userId);
        Integer cursor = message == null ? null : message.getCursor();
        return new PresenceUser(userId, displayName, cursor);
    }

    // 广播当前在线用户快照
    private void broadcastSnapshot(Long documentId, Map<Long, PresenceUser> docPresence) {
        PresenceMessage snapshot = new PresenceMessage();
        snapshot.setType("snapshot");
        snapshot.setUsers(new ArrayList<>(docPresence.values()));
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/presence", snapshot);
    }
}
