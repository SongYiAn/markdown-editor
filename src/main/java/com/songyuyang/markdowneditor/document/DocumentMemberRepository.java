package com.songyuyang.markdowneditor.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentMemberRepository extends JpaRepository<DocumentMember, Long> {
    // 根据文档与用户查询成员关系
    Optional<DocumentMember> findByDocumentIdAndUserId(Long documentId, Long userId);

    // 获取用户参与的所有文档
    List<DocumentMember> findAllByUserId(Long userId);

    // 获取文档的所有成员
    List<DocumentMember> findAllByDocumentId(Long documentId);
    // 删除文档的所有成员
    void deleteAllByDocumentId(Long documentId);
}
