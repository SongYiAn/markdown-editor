package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.auth.UserPrincipal;
import com.songyuyang.markdowneditor.common.ApiException;
import com.songyuyang.markdowneditor.document.dto.OperationRequest;
import com.songyuyang.markdowneditor.document.dto.OperationResult;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
// 协同编辑入口：处理操作并广播
public class DocumentCollabController {
    private final DocumentService documentService;
    private final SimpMessagingTemplate messagingTemplate;

    public DocumentCollabController(DocumentService documentService, SimpMessagingTemplate messagingTemplate) {
        this.documentService = documentService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/document/{documentId}/op")
    // 接收操作，校验身份，应用并广播
    public void handleOperation(@DestinationVariable Long documentId, OperationRequest request, Principal principal) {
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        OperationResult result = documentService.applyOperation(documentId, userPrincipal.getUserId(), request);
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/ops", result);
    }
}
