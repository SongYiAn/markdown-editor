package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.auth.AuthService;
import com.songyuyang.markdowneditor.auth.dto.RegisterRequest;
import com.songyuyang.markdowneditor.document.dto.AddMemberRequest;
import com.songyuyang.markdowneditor.document.dto.DocumentCreateRequest;
import com.songyuyang.markdowneditor.document.dto.OperationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
// 文档服务测试
class DocumentServiceTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private DocumentService documentService;

    // 注册辅助方法，返回 [userId, username]，避免测试间用户名冲突
    private Object[] registerUser(String prefix) {
        String username = prefix + "_" + System.nanoTime();
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setPassword("password123");
        r.setDisplayName(prefix);
        Long userId = authService.register(r).getUserId();
        return new Object[]{userId, username};
    }

    @Test
    // 创建文档并应用一次插入操作
    void createAndApplyOperation() {
        Long userId = (Long) registerUser("bob")[0];

        DocumentCreateRequest createRequest = new DocumentCreateRequest();
        createRequest.setTitle("Doc A");
        createRequest.setContent("Hello");
        Long documentId = documentService.createDocument(userId, createRequest).getId();

        OperationRequest op = new OperationRequest();
        op.setType(DocumentOp.OperationType.INSERT);
        op.setPosition(5);
        op.setLength(0);
        op.setText(" World");
        op.setBaseVersion(0);
        String content = documentService.applyOperation(documentId, userId, op).getContent();
        assertThat(content).isEqualTo("Hello World");
    }

    @Test
    // 邀请成员并恢复历史版本
    void addMemberAndRestoreVersion() {
        Object[] ownerInfo = registerUser("owner");
        Long ownerId = (Long) ownerInfo[0];
        Object[] memberInfo = registerUser("member");
        String memberUsername = (String) memberInfo[1];

        DocumentCreateRequest createRequest = new DocumentCreateRequest();
        createRequest.setTitle("Doc B");
        createRequest.setContent("Hello");
        Long documentId = documentService.createDocument(ownerId, createRequest).getId();

        AddMemberRequest addMemberRequest = new AddMemberRequest();
        addMemberRequest.setUsername(memberUsername);
        addMemberRequest.setRole(DocumentRole.EDITOR);
        assertThat(documentService.addMember(documentId, ownerId, addMemberRequest).getUsername())
                .isEqualTo(memberUsername);

        OperationRequest op = new OperationRequest();
        op.setType(DocumentOp.OperationType.INSERT);
        op.setPosition(5);
        op.setLength(0);
        op.setText(" World");
        op.setBaseVersion(0);
        documentService.applyOperation(documentId, ownerId, op);

        // listVersions 按时间倒序，最后一项为最早的初始版本 "Hello"
        java.util.List<com.songyuyang.markdowneditor.document.dto.VersionSummary> versions =
                documentService.listVersions(documentId, ownerId);
        Long versionId = versions.get(versions.size() - 1).getId();
        String restored = documentService.restoreVersion(documentId, versionId, ownerId).getContent();
        assertThat(restored).isEqualTo("Hello");
    }

    @Test
    // OT 变换：两个并发 INSERT 操作应正确合并
    void otTransformConcurrentInserts() {
        Object[] userAInfo = registerUser("ota");
        Long userA = (Long) userAInfo[0];

        DocumentCreateRequest createRequest = new DocumentCreateRequest();
        createRequest.setTitle("OT Doc");
        createRequest.setContent("Hello");
        Long documentId = documentService.createDocument(userA, createRequest).getId();

        // 将 userB 加入文档
        Object[] userBInfo = registerUser("otb");
        Long userBId = (Long) userBInfo[0];
        String userBName = (String) userBInfo[1];
        AddMemberRequest addMember = new AddMemberRequest();
        addMember.setUsername(userBName);
        addMember.setRole(DocumentRole.EDITOR);
        documentService.addMember(documentId, userA, addMember);

        // userA 在版本 0 时插入 " World"（位置 5）
        OperationRequest opA = new OperationRequest();
        opA.setType(DocumentOp.OperationType.INSERT);
        opA.setPosition(5);
        opA.setText(" World");
        opA.setBaseVersion(0);
        documentService.applyOperation(documentId, userA, opA);
        // 文档现在："Hello World"，版本 1

        // userB 也基于版本 0，在位置 0 插入 ">> "
        // OT 应将其变换到正确位置并应用
        OperationRequest opB = new OperationRequest();
        opB.setType(DocumentOp.OperationType.INSERT);
        opB.setPosition(0);
        opB.setText(">> ");
        opB.setBaseVersion(0);
        String result = documentService.applyOperation(documentId, userBId, opB).getContent();
        // 期望：">> Hello World"
        assertThat(result).isEqualTo(">> Hello World");
    }

    @Test
    // OT 变换：并发 DELETE 操作（删除范围无重叠）
    void otTransformConcurrentDeletes() {
        Object[] userAInfo = registerUser("del_a");
        Long userA = (Long) userAInfo[0];

        DocumentCreateRequest createRequest = new DocumentCreateRequest();
        createRequest.setTitle("Delete OT Doc");
        createRequest.setContent("Hello World");
        Long documentId = documentService.createDocument(userA, createRequest).getId();

        Object[] bInfo = registerUser("del_b");
        Long bId = (Long) bInfo[0];
        String bName = (String) bInfo[1];
        AddMemberRequest addMember = new AddMemberRequest();
        addMember.setUsername(bName);
        addMember.setRole(DocumentRole.EDITOR);
        documentService.addMember(documentId, userA, addMember);

        // userA 在版本 0 删除 " World"（位置 5，长度 6）
        OperationRequest opA = new OperationRequest();
        opA.setType(DocumentOp.OperationType.DELETE);
        opA.setPosition(5);
        opA.setLength(6);
        opA.setBaseVersion(0);
        documentService.applyOperation(documentId, userA, opA);
        // 文档："Hello"，版本 1

        // userB 基于版本 0 在位置 0 删除 "Hello"（长度 5）
        OperationRequest opB = new OperationRequest();
        opB.setType(DocumentOp.OperationType.DELETE);
        opB.setPosition(0);
        opB.setLength(5);
        opB.setBaseVersion(0);
        String result = documentService.applyOperation(documentId, bId, opB).getContent();
        // OT 后 opB 删除位置不变（在 opA 之前），文档变为 ""
        assertThat(result).isEqualTo("");
    }
}
