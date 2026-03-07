package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.document.DocumentOp.OperationType;
import com.songyuyang.markdowneditor.document.dto.OperationRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OperationalTransformer 单元测试
 * 验证 OT 算法在各种并发操作场景下的变换正确性
 */
class OperationalTransformerTests {

    // ── 辅助构造 ──────────────────────────────────────────────────────────────

    private static OperationRequest insert(int pos, String text) {
        OperationRequest op = new OperationRequest();
        op.setType(OperationType.INSERT);
        op.setPosition(pos);
        op.setText(text);
        op.setLength(0);
        return op;
    }

    private static OperationRequest delete(int pos, int len) {
        OperationRequest op = new OperationRequest();
        op.setType(OperationType.DELETE);
        op.setPosition(pos);
        op.setLength(len);
        return op;
    }

    private static DocumentOp appliedInsert(int pos, String text) {
        DocumentOp op = new DocumentOp();
        op.setType(OperationType.INSERT);
        op.setPosition(pos);
        op.setText(text);
        op.setLength(0);
        return op;
    }

    private static DocumentOp appliedDelete(int pos, int len) {
        DocumentOp op = new DocumentOp();
        op.setType(OperationType.DELETE);
        op.setPosition(pos);
        op.setLength(len);
        return op;
    }

    // ── 应用操作到字符串 ──────────────────────────────────────────────────────

    private static String apply(String text, OperationRequest op) {
        if (op.getType() == OperationType.INSERT) {
            int pos = op.getPosition();
            String t = op.getText() == null ? "" : op.getText();
            return text.substring(0, pos) + t + text.substring(pos);
        } else {
            int pos = op.getPosition();
            int len = op.getLength();
            if (len == 0) return text;
            return text.substring(0, pos) + text.substring(pos + len);
        }
    }

    private static String applyDoc(String text, DocumentOp op) {
        if (op.getType() == OperationType.INSERT) {
            int pos = op.getPosition();
            String t = op.getText() == null ? "" : op.getText();
            return text.substring(0, pos) + t + text.substring(pos);
        } else {
            int pos = op.getPosition();
            int len = op.getLength();
            if (len == 0) return text;
            return text.substring(0, pos) + text.substring(pos + len);
        }
    }

    /**
     * OT 收敛性：apply(apply(doc,B), A') == apply(apply(doc,A), B')
     * 其中 A' = transform(A,B)，B' = transform(B,A)
     * 仅用于不发生区间重叠的情况（已证明收敛）
     */
    private void assertConverges(String doc, OperationRequest a, OperationRequest b) {
        OperationRequest aPrime = OperationalTransformer.transform(a, b);
        OperationRequest bPrime = OperationalTransformer.transform(b, a);
        String path1 = apply(apply(doc, b), aPrime);
        String path2 = apply(apply(doc, a), bPrime);
        assertThat(path1)
                .as("OT收敛失败: doc=[%s]", doc)
                .isEqualTo(path2);
    }

    // ── INSERT vs INSERT ──────────────────────────────────────────────────────

    @Test
    // INSERT vs INSERT：applied 在 op 之前，op 位置右移
    void insertVsInsertBefore() {
        OperationRequest op = insert(5, " World");
        DocumentOp applied = appliedInsert(0, ">> ");
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getPosition()).isEqualTo(8); // 5 + 3
        assertThat(t.getText()).isEqualTo(" World");
        assertConverges("Hello", insert(5, " World"), insert(0, ">> "));
    }

    @Test
    // INSERT vs INSERT：applied 在 op 之后，op 位置不变
    void insertVsInsertAfter() {
        OperationRequest op = insert(0, ">> ");
        DocumentOp applied = appliedInsert(5, " World");
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getPosition()).isEqualTo(0);
        assertConverges("Hello", insert(0, ">> "), insert(5, " World"));
    }

    @Test
    // INSERT vs INSERT：相同位置，applied 优先（服务端已先应用，op 右移至其后）
    // 注意：相同位置插入的双向收敛依赖外部客户端 ID 排序，此处仅验证服务端变换结果
    void insertVsInsertSamePosition() {
        OperationRequest op = insert(3, "AAA");
        DocumentOp applied = appliedInsert(3, "BBB");
        OperationRequest t = OperationalTransformer.transform(op, applied);
        // 服务端已应用 BBB，故 AAA 应排在 BBB 之后：位置 = 3 + len("BBB") = 6
        assertThat(t.getPosition()).isEqualTo(6);
        // 验证应用结果：先 BBB 后 AAA
        String doc = "Hello";
        String afterApplied = "Hel" + "BBB" + "lo"; // "HelBBBlo"
        String merged = apply(afterApplied, t);      // insert "AAA" at 6
        assertThat(merged).isEqualTo("HelBBBAAAlo");
    }

    // ── INSERT vs DELETE ──────────────────────────────────────────────────────

    @Test
    // INSERT vs DELETE：delete 完全在 op 之前，op 位置左移
    void insertVsDeleteBefore() {
        OperationRequest op = insert(5, "!");
        DocumentOp applied = appliedDelete(0, 3);
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getPosition()).isEqualTo(2); // 5 - 3
        assertConverges("Hello", insert(5, "!"), delete(0, 3));
    }

    @Test
    // INSERT vs DELETE：delete 完全在 op 之后，op 位置不变
    void insertVsDeleteAfter() {
        OperationRequest op = insert(2, "X");
        DocumentOp applied = appliedDelete(4, 3);
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getPosition()).isEqualTo(2);
        assertConverges("Hello World", insert(2, "X"), delete(4, 3));
    }

    @Test
    // INSERT vs DELETE：op 位置在 delete 区间内，op 移至删除起点（删除优先）
    void insertVsDeleteInsideRange() {
        OperationRequest op = insert(3, "X");
        DocumentOp applied = appliedDelete(1, 5); // 删 [1,6)
        OperationRequest t = OperationalTransformer.transform(op, applied);
        // 插入位置 3 落在 [1,6) 内，移至删除起点 1
        assertThat(t.getPosition()).isEqualTo(1);
    }

    // ── DELETE vs INSERT ──────────────────────────────────────────────────────

    @Test
    // DELETE vs INSERT：insert 在 delete 之前，delete 位置右移
    void deleteVsInsertBefore() {
        OperationRequest op = delete(5, 6); // 删 " World"
        DocumentOp applied = appliedInsert(0, ">> "); // 前插 ">> "
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getPosition()).isEqualTo(8); // 5 + 3
        assertThat(t.getLength()).isEqualTo(6);
        assertConverges("Hello World", delete(5, 6), insert(0, ">> "));
    }

    @Test
    // DELETE vs INSERT：insert 在 delete 之后，无变化
    void deleteVsInsertAfter() {
        OperationRequest op = delete(0, 5);
        DocumentOp applied = appliedInsert(6, "XY");
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getPosition()).isEqualTo(0);
        assertThat(t.getLength()).isEqualTo(5);
        assertConverges("Hello World", delete(0, 5), insert(6, "XY"));
    }

    @Test
    // DELETE vs INSERT：insert 在 delete 内部，长度不变（删除优先，不扩展）
    void deleteVsInsertInsideRange() {
        OperationRequest op = delete(0, 5); // 删 "Hello"
        DocumentOp applied = appliedInsert(3, "XY"); // 在 l 后插入
        OperationRequest t = OperationalTransformer.transform(op, applied);
        // 删除优先：不扩展长度
        assertThat(t.getPosition()).isEqualTo(0);
        assertThat(t.getLength()).isEqualTo(5);
    }

    // ── DELETE vs DELETE ──────────────────────────────────────────────────────

    @Test
    // DELETE vs DELETE：B 完全在 A 之前，A 左移
    void deleteVsDeleteBefore() {
        OperationRequest op = delete(5, 6); // 删 " World"
        DocumentOp applied = appliedDelete(0, 3); // 删 "Hel"
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getPosition()).isEqualTo(2); // 5 - 3
        assertThat(t.getLength()).isEqualTo(6);
        assertConverges("Hello World", delete(5, 6), delete(0, 3));
    }

    @Test
    // DELETE vs DELETE：B 完全在 A 之后，无变化
    void deleteVsDeleteAfter() {
        OperationRequest op = delete(0, 3);
        DocumentOp applied = appliedDelete(5, 6);
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getPosition()).isEqualTo(0);
        assertThat(t.getLength()).isEqualTo(3);
        assertConverges("Hello World", delete(0, 3), delete(5, 6));
    }

    @Test
    // DELETE vs DELETE：B 完全覆盖 A，变为空操作
    void deleteVsDeleteCovered() {
        OperationRequest op = delete(2, 3); // 删 [2,5)
        DocumentOp applied = appliedDelete(0, 8); // 删 [0,8)，覆盖 A
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getLength()).isEqualTo(0);
    }

    @Test
    // DELETE vs DELETE：A 完全包含 B，A 长度减少
    void deleteVsDeleteContains() {
        OperationRequest op = delete(0, 8); // 删前 8 个字符 "Hello Wo"
        DocumentOp applied = appliedDelete(2, 3); // 删 "llo"
        OperationRequest t = OperationalTransformer.transform(op, applied);
        assertThat(t.getLength()).isEqualTo(5); // 8 - 3
        assertConverges("Hello World", delete(0, 8), delete(2, 3));
    }

    @Test
    // 端到端：通过 DocumentService 验证并发 INSERT 的 OT 合并结果
    void mergedStateAfterConcurrentInserts() {
        String doc = "Hello";
        // A 在位置 5 插入 " World"，先被应用
        DocumentOp applied = appliedInsert(5, " World");
        String afterB = applyDoc(doc, applied); // "Hello World"

        // B 在位置 0 插入 ">> "，基于旧版本，需变换
        OperationRequest b = insert(0, ">> ");
        OperationRequest bPrime = OperationalTransformer.transform(b, applied);
        String merged = apply(afterB, bPrime);

        assertThat(merged).isEqualTo(">> Hello World");
    }
}
