package com.songyuyang.markdowneditor.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentOpRepository extends JpaRepository<DocumentOp, Long> {
    // 获取最近 200 条操作记录
    List<DocumentOp> findTop200ByDocumentIdOrderByIdDesc(Long documentId);
    // 删除文档的所有操作记录
    void deleteAllByDocumentId(Long documentId);
}
