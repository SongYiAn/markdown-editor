package com.songyuyang.markdowneditor.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    // 按版本号倒序获取快照
    List<DocumentVersion> findAllByDocumentIdOrderByVersionNumberDesc(Long documentId);
    // 按创建时间倒序获取快照
    List<DocumentVersion> findAllByDocumentIdOrderByCreatedAtDesc(Long documentId);
    // 删除文档的所有版本记录
    void deleteAllByDocumentId(Long documentId);
    // 查询最近一条自动保存版本（用于去重）
    List<DocumentVersion> findTop1ByDocumentIdAndAutoSaveTrueOrderByCreatedAtDesc(Long documentId);
    // 统计自动保存版本数量
    long countByDocumentIdAndAutoSaveTrue(Long documentId);
    // 查询最早的自动保存版本（超出上限时删除）
    List<DocumentVersion> findTop1ByDocumentIdAndAutoSaveTrueOrderByCreatedAtAsc(Long documentId);
}
