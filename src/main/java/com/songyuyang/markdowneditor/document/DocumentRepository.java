package com.songyuyang.markdowneditor.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    // 查询用户参与的文档
    @Query("select d from Document d join d.members m where m.user.id = :userId")
    List<Document> findAllByMemberUserId(@Param("userId") Long userId);
}
