package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.auth.UserAccount;
import com.songyuyang.markdowneditor.auth.UserAccountRepository;
import com.songyuyang.markdowneditor.common.ApiException;
import com.songyuyang.markdowneditor.document.dto.AddMemberRequest;
import com.songyuyang.markdowneditor.document.dto.DocumentCreateRequest;
import com.songyuyang.markdowneditor.document.dto.DocumentDetail;
import com.songyuyang.markdowneditor.document.dto.DocumentSummary;
import com.songyuyang.markdowneditor.document.dto.MemberSummary;
import com.songyuyang.markdowneditor.document.dto.OperationRequest;
import com.songyuyang.markdowneditor.document.dto.OperationResult;
import com.songyuyang.markdowneditor.document.dto.VersionDetail;
import com.songyuyang.markdowneditor.document.dto.VersionSummary;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 文档核心业务：权限校验、内容变更、版本快照与成员管理
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentMemberRepository documentMemberRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentOpRepository documentOpRepository;
    private final UserAccountRepository userAccountRepository;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentMemberRepository documentMemberRepository,
                           DocumentVersionRepository documentVersionRepository,
                           DocumentOpRepository documentOpRepository,
                           UserAccountRepository userAccountRepository) {
        this.documentRepository = documentRepository;
        this.documentMemberRepository = documentMemberRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentOpRepository = documentOpRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    // 创建文档并初始化拥有者与首个版本快照
    public DocumentDetail createDocument(Long userId, DocumentCreateRequest request) {
        UserAccount owner = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user not found"));
        Document document = new Document();
        document.setTitle(request.getTitle().trim());
        if (request.getContent() != null) {
            document.setContent(request.getContent());
        }
        document.addOwner(owner);
        document.setUpdatedAt(Instant.now());
        Document saved = documentRepository.save(document);
        createVersionSnapshot(saved, owner, "初始版本", false);
        return new DocumentDetail(saved.getId(), saved.getTitle(), saved.getContent(), saved.getVersion(),
                DocumentRole.OWNER, saved.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    // 获取当前用户可访问的文档列表
    public List<DocumentSummary> listDocuments(Long userId) {
        return documentMemberRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(DocumentMember::getId).reversed())
                .map(member -> new DocumentSummary(member.getDocument().getId(), member.getDocument().getTitle(),
                        member.getRole(), member.getDocument().getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    // 获取单个文档详情（需成员权限）
    public DocumentDetail getDocument(Long documentId, Long userId) {
        DocumentMember member = requireMember(documentId, userId);
        Document document = member.getDocument();
        return new DocumentDetail(document.getId(), document.getTitle(), document.getContent(), document.getVersion(),
                member.getRole(), document.getUpdatedAt());
    }

    @Transactional
    // 由拥有者邀请/设置成员权限
    public MemberSummary addMember(Long documentId, Long userId, AddMemberRequest request) {
        DocumentMember owner = requireOwner(documentId, userId);
        UserAccount user = userAccountRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user not found"));
        if (Objects.equals(user.getId(), owner.getUser().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "cannot add self");
        }
        DocumentMember member = documentMemberRepository.findByDocumentIdAndUserId(documentId, user.getId())
                .orElseGet(() -> {
                    DocumentMember created = new DocumentMember();
                    created.setDocument(owner.getDocument());
                    created.setUser(user);
                    return created;
                });
        member.setRole(request.getRole());
        documentMemberRepository.save(member);
        return new MemberSummary(user.getId(), user.getUsername(), user.getDisplayName(), member.getRole());
    }

    @Transactional(readOnly = true)
    // 列出文档成员
    public List<MemberSummary> listMembers(Long documentId, Long userId) {
        requireMember(documentId, userId);
        return documentMemberRepository.findAllByDocumentId(documentId).stream()
                .map(member -> new MemberSummary(member.getUser().getId(), member.getUser().getUsername(),
                        member.getUser().getDisplayName(), member.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional
    // 移除成员（仅 OWNER 可操作，且不能移除自己）
    public void removeMember(Long documentId, Long targetUserId, Long operatorUserId) {
        requireOwner(documentId, operatorUserId);
        if (Objects.equals(targetUserId, operatorUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "cannot remove self");
        }
        DocumentMember member = documentMemberRepository.findByDocumentIdAndUserId(documentId, targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "member not found"));
        if (member.getRole() == DocumentRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "cannot remove owner");
        }
        documentMemberRepository.delete(member);
    }

    @Transactional
    // 恢复到历史版本（会生成新的版本号）
    public DocumentDetail restoreVersion(Long documentId, Long versionId, Long userId) {
        DocumentMember owner = requireOwner(documentId, userId);
        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "version not found"));
        if (!Objects.equals(version.getDocument().getId(), documentId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "version does not belong to document");
        }
        Document document = owner.getDocument();
        document.setContent(version.getContent());
        document.setVersion(document.getVersion() + 1);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        createVersionSnapshot(document, owner.getUser(), "恢复至版本 " + version.getVersionNumber(), true);
        return new DocumentDetail(document.getId(), document.getTitle(), document.getContent(), document.getVersion(),
                owner.getRole(), document.getUpdatedAt());
    }

    @Transactional
    // 应用一次操作（支持 OT 变换：客户端版本落后时自动变换而非拒绝）
    public OperationResult applyOperation(Long documentId, Long userId, OperationRequest request) {
        DocumentMember member = requireMember(documentId, userId);
        if (member.getRole() == DocumentRole.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "read only");
        }
        Document document = member.getDocument();
        long serverVersion = document.getVersion();
        if (request.getBaseVersion() > serverVersion) {
            throw new ApiException(HttpStatus.CONFLICT, "version conflict");
        }
        // OT 变换：若客户端版本落后，将操作逐一对每条并发操作做变换
        OperationRequest effectiveOp = request;
        if (request.getBaseVersion() < serverVersion) {
            List<DocumentOp> concurrentOps = documentOpRepository
                    .findAllByDocumentIdAndAppliedVersionGreaterThanOrderByAppliedVersionAsc(
                            documentId, request.getBaseVersion());
            for (DocumentOp concurrent : concurrentOps) {
                effectiveOp = OperationalTransformer.transform(effectiveOp, concurrent);
            }
        }
        String content = document.getContent();
        String newContent;
        if (effectiveOp.getType() == DocumentOp.OperationType.INSERT) {
            String text = effectiveOp.getText() == null ? "" : effectiveOp.getText();
            int pos = effectiveOp.getPosition();
            if (pos < 0 || pos > content.length()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid position");
            }
            newContent = content.substring(0, pos) + text + content.substring(pos);
        } else {
            int pos = effectiveOp.getPosition();
            int len = effectiveOp.getLength();
            if (pos < 0 || pos + len > content.length()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid range");
            }
            newContent = content.substring(0, pos) + content.substring(pos + len);
        }
        document.setContent(newContent);
        document.setVersion(serverVersion + 1);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        DocumentOp op = new DocumentOp();
        op.setAuthor(member.getUser());
        op.setDocument(document);
        op.setBaseVersion(request.getBaseVersion());
        op.setAppliedVersion(document.getVersion());
        op.setType(effectiveOp.getType());
        op.setPosition(effectiveOp.getPosition());
        op.setLength(effectiveOp.getLength());
        op.setText(effectiveOp.getText());
        documentOpRepository.save(op);
        // 每次操作成功后自动创建编辑记录
        createVersionSnapshot(document, member.getUser(), null, true);
        return new OperationResult(document.getVersion(), effectiveOp.getType(), effectiveOp.getPosition(),
                effectiveOp.getLength(), effectiveOp.getText(), newContent, userId);
    }

    @Transactional
    // 创建手动快照
    public VersionSummary createSnapshot(Long documentId, Long userId, String name) {
        DocumentMember member = requireMember(documentId, userId);
        if (member.getRole() == DocumentRole.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "read only");
        }
        UserAccount currentUser = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user not found"));
        Document document = member.getDocument();
        document.setVersion(document.getVersion() + 1);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        String snapshotName = (name != null && !name.trim().isEmpty()) ? name.trim() : "快照 v" + document.getVersion();
        DocumentVersion version = createVersionSnapshot(document, currentUser, snapshotName, false);
        return new VersionSummary(version.getId(), version.getVersionNumber(), version.getName(),
                version.isAutoSave(), version.getCreatedAt(), currentUser.getDisplayName());
    }

    @Transactional(readOnly = true)
    // 列出历史版本
    public List<VersionSummary> listVersions(Long documentId, Long userId) {
        requireMember(documentId, userId);
        return documentVersionRepository.findAllByDocumentIdOrderByCreatedAtDesc(documentId).stream()
                .map(version -> new VersionSummary(version.getId(), version.getVersionNumber(),
                        version.getName(), version.isAutoSave(), version.getCreatedAt(),
                        version.getCreatedBy().getDisplayName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    // 获取单个版本详情（包含快照内容，用于预览）
    public VersionDetail getVersionDetail(Long documentId, Long versionId, Long userId) {
        requireMember(documentId, userId);
        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "version not found"));
        if (!version.getDocument().getId().equals(documentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "no access");
        }
        return new VersionDetail(version.getId(), version.getVersionNumber(),
                version.getName(), version.getContent(), version.getCreatedAt(),
                version.getCreatedBy().getDisplayName());
    }

    @Transactional
    // 通过分享链接加入协作，默认编辑权限
    public DocumentDetail joinByLink(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "document not found"));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user not found"));
        DocumentMember member = documentMemberRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseGet(() -> {
                    DocumentMember created = new DocumentMember();
                    created.setDocument(document);
                    created.setUser(user);
                    created.setRole(DocumentRole.EDITOR);
                    return created;
                });
        documentMemberRepository.save(member);
        return new DocumentDetail(document.getId(), document.getTitle(), document.getContent(), document.getVersion(),
                member.getRole(), document.getUpdatedAt());
    }

    @Transactional
    // 删除文档（仅 OWNER 可操作），同时删除成员、版本、操作记录
    public void deleteDocument(Long documentId, Long userId) {
        requireOwner(documentId, userId);
        documentOpRepository.deleteAllByDocumentId(documentId);
        documentVersionRepository.deleteAllByDocumentId(documentId);
        documentMemberRepository.deleteAllByDocumentId(documentId);
        documentRepository.deleteById(documentId);
    }

    @Transactional
    // 更新文档标题（OWNER/EDITOR 可操作）
    public DocumentDetail updateTitle(Long documentId, Long userId, String title) {
        DocumentMember member = requireMember(documentId, userId);
        if (member.getRole() == DocumentRole.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "read only");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "title is required");
        }
        Document document = member.getDocument();
        document.setTitle(title.trim());
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        return new DocumentDetail(document.getId(), document.getTitle(), document.getContent(),
                document.getVersion(), member.getRole(), document.getUpdatedAt());
    }

    private DocumentMember requireMember(Long documentId, Long userId) {
        return documentMemberRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "no access"));
    }

    private DocumentMember requireOwner(Long documentId, Long userId) {
        DocumentMember member = requireMember(documentId, userId);
        if (member.getRole() != DocumentRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "owner only");
        }
        return member;
    }

    // 自动保存版本最大数量
    private static final int AUTO_SAVE_LIMIT = 100;

    private DocumentVersion createVersionSnapshot(Document document, UserAccount createdBy, String name, boolean autoSave) {
        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(document.getVersion());
        version.setName(name);
        version.setAutoSave(autoSave);
        version.setContent(document.getContent());
        version.setCreatedBy(createdBy);
        DocumentVersion saved = documentVersionRepository.save(version);
        // 自动保存版本超出上限时，删除最早的一条
        if (autoSave) {
            long count = documentVersionRepository.countByDocumentIdAndAutoSaveTrue(document.getId());
            if (count > AUTO_SAVE_LIMIT) {
                List<DocumentVersion> oldest = documentVersionRepository
                        .findTop1ByDocumentIdAndAutoSaveTrueOrderByCreatedAtAsc(document.getId());
                if (!oldest.isEmpty()) {
                    documentVersionRepository.delete(oldest.get(0));
                }
            }
        }
        return saved;
    }
}

