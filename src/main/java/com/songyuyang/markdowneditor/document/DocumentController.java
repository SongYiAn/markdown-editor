package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.common.SecurityUtils;
import com.songyuyang.markdowneditor.document.dto.AddMemberRequest;
import com.songyuyang.markdowneditor.document.dto.DocumentCreateRequest;
import com.songyuyang.markdowneditor.document.dto.DocumentDetail;
import com.songyuyang.markdowneditor.document.dto.DocumentSummary;
import com.songyuyang.markdowneditor.document.dto.MemberSummary;
import com.songyuyang.markdowneditor.document.dto.OperationRequest;
import com.songyuyang.markdowneditor.document.dto.OperationResult;
import com.songyuyang.markdowneditor.document.dto.SnapshotRequest;
import com.songyuyang.markdowneditor.document.dto.VersionDetail;
import com.songyuyang.markdowneditor.document.dto.VersionSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// 文档相关 REST API：创建、查询、成员、版本、操作与加入协作
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final SimpMessagingTemplate messagingTemplate;

    public DocumentController(DocumentService documentService, SimpMessagingTemplate messagingTemplate) {
        this.documentService = documentService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    // 创建文档（默认当前用户为 OWNER）
    public DocumentDetail createDocument(@Valid @RequestBody DocumentCreateRequest request) {
        return documentService.createDocument(SecurityUtils.getCurrentUserId(), request);
    }

    @GetMapping
    // 获取当前用户可访问的文档列表
    public List<DocumentSummary> listDocuments() {
        return documentService.listDocuments(SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/{documentId}")
    // 获取文档详情（需成员权限）
    public DocumentDetail getDocument(@PathVariable Long documentId) {
        return documentService.getDocument(documentId, SecurityUtils.getCurrentUserId());
    }

    @PostMapping("/{documentId}/ops")
    // 应用一次操作并广播给协作成员
    public OperationResult applyOperation(@PathVariable Long documentId,
                                          @Valid @RequestBody OperationRequest request) {
        OperationResult result = documentService.applyOperation(documentId, SecurityUtils.getCurrentUserId(), request);
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/ops", result);
        return result;
    }

    @PostMapping("/{documentId}/versions/snapshot")
    // 保存当前内容快照（支持自定义名称）
    public VersionSummary createSnapshot(@PathVariable Long documentId,
                                         @RequestBody(required = false) SnapshotRequest request) {
        String name = (request != null) ? request.getName() : null;
        return documentService.createSnapshot(documentId, SecurityUtils.getCurrentUserId(), name);
    }

    @GetMapping("/{documentId}/versions")
    // 获取历史版本列表
    public List<VersionSummary> listVersions(@PathVariable Long documentId) {
        return documentService.listVersions(documentId, SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/{documentId}/versions/{versionId}")
    // 获取单个版本详情（含内容，用于预览）
    public VersionDetail getVersionDetail(@PathVariable Long documentId, @PathVariable Long versionId) {
        return documentService.getVersionDetail(documentId, versionId, SecurityUtils.getCurrentUserId());
    }

    @PostMapping("/{documentId}/members")
    // 邀请成员或修改成员权限（OWNER 才允许）
    public MemberSummary addMember(@PathVariable Long documentId,
                                   @Valid @RequestBody AddMemberRequest request) {
        return documentService.addMember(documentId, SecurityUtils.getCurrentUserId(), request);
    }

    @GetMapping("/{documentId}/members")
    // 获取成员列表
    public List<MemberSummary> listMembers(@PathVariable Long documentId) {
        return documentService.listMembers(documentId, SecurityUtils.getCurrentUserId());
    }

    @DeleteMapping("/{documentId}/members/{targetUserId}")
    // 移除指定成员（仅 OWNER 可操作）
    public void removeMember(@PathVariable Long documentId, @PathVariable Long targetUserId) {
        documentService.removeMember(documentId, targetUserId, SecurityUtils.getCurrentUserId());
    }

    @PostMapping("/{documentId}/versions/{versionId}/restore")
    // 恢复到指定历史版本（会生成新版本号）
    public DocumentDetail restoreVersion(@PathVariable Long documentId, @PathVariable Long versionId) {
        return documentService.restoreVersion(documentId, versionId, SecurityUtils.getCurrentUserId());
    }

    @PostMapping("/{documentId}/join")
    // 通过分享链接加入协作（默认 EDITOR）
    public DocumentDetail joinDocument(@PathVariable Long documentId) {
        return documentService.joinByLink(documentId, SecurityUtils.getCurrentUserId());
    }

    @DeleteMapping("/{documentId}")
    // 删除文档（仅 OWNER 可操作）
    public void deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId, SecurityUtils.getCurrentUserId());
    }

    @PostMapping("/{documentId}/title")
    // 更新文档标题（OWNER/EDITOR 可操作）
    public DocumentDetail updateTitle(@PathVariable Long documentId,
                                      @RequestBody java.util.Map<String, String> body) {
        String title = body.get("title");
        return documentService.updateTitle(documentId, SecurityUtils.getCurrentUserId(), title);
    }
}
