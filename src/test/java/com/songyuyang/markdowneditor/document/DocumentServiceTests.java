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

    @Test
    // 创建文档并应用一次插入操作
    void createAndApplyOperation() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("bob");
        register.setPassword("password123");
        register.setDisplayName("Bob");
        Long userId = authService.register(register).getUserId();

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
        RegisterRequest register = new RegisterRequest();
        register.setUsername("owner");
        register.setPassword("password123");
        register.setDisplayName("Owner");
        Long ownerId = authService.register(register).getUserId();

        RegisterRequest memberRegister = new RegisterRequest();
        memberRegister.setUsername("member");
        memberRegister.setPassword("password123");
        memberRegister.setDisplayName("Member");
        authService.register(memberRegister);

        DocumentCreateRequest createRequest = new DocumentCreateRequest();
        createRequest.setTitle("Doc B");
        createRequest.setContent("Hello");
        Long documentId = documentService.createDocument(ownerId, createRequest).getId();

        AddMemberRequest addMemberRequest = new AddMemberRequest();
        addMemberRequest.setUsername("member");
        addMemberRequest.setRole(DocumentRole.EDITOR);
        assertThat(documentService.addMember(documentId, ownerId, addMemberRequest).getUsername())
                .isEqualTo("member");

        OperationRequest op = new OperationRequest();
        op.setType(DocumentOp.OperationType.INSERT);
        op.setPosition(5);
        op.setLength(0);
        op.setText(" World");
        op.setBaseVersion(0);
        documentService.applyOperation(documentId, ownerId, op);

        Long versionId = documentService.listVersions(documentId, ownerId).get(0).getId();
        String restored = documentService.restoreVersion(documentId, versionId, ownerId).getContent();
        assertThat(restored).isEqualTo("Hello");
    }
}
