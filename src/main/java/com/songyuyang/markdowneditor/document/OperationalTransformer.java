package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.document.DocumentOp.OperationType;
import com.songyuyang.markdowneditor.document.dto.OperationRequest;

/**
 * 操作变换（OT，Operational Transformation）核心算法实现。
 *
 * <p>场景：客户端基于版本 V 发出操作 A，但服务端在此期间已应用了并发操作 B（版本 V→V+1）。
 * 此时需要将 A 对 B 做变换，得到等效操作 A'，使得：
 * <pre>apply(apply(doc_V, B), A') == apply(apply(doc_V, A), B)</pre>
 *
 * <p>支持的操作类型：INSERT（插入）与 DELETE（删除）。
 */
public class OperationalTransformer {

    private OperationalTransformer() {
    }

    /**
     * 将操作 {@code op} 对已应用操作 {@code applied} 做变换，返回变换后的操作 op'。
     *
     * @param op      待变换的操作（客户端发来的操作）
     * @param applied 已被服务端优先应用的并发操作
     * @return 变换后的操作，可安全应用于 applied 之后的文档状态
     */
    public static OperationRequest transform(OperationRequest op, DocumentOp applied) {
        String appliedText = applied.getText() == null ? "" : applied.getText();
        int appliedTextLen = appliedText.length();
        if (op.getType() == OperationType.INSERT) {
            return transformInsert(op, applied, appliedTextLen);
        } else {
            return transformDelete(op, applied, appliedTextLen);
        }
    }

    /**
     * 便捷重载：将两个 OperationRequest 互相变换（用于测试收敛性校验）。
     * 将 applied 包装为 DocumentOp 后调用主变换逻辑。
     */
    public static OperationRequest transform(OperationRequest op, OperationRequest applied) {
        DocumentOp docOp = new DocumentOp();
        docOp.setType(applied.getType());
        docOp.setPosition(applied.getPosition());
        docOp.setLength(applied.getLength());
        docOp.setText(applied.getText());
        return transform(op, docOp);
    }

    // INSERT vs DELETE：
    //   若删除区间完全在 op 之前 → op 左移删除长度
    //   若 op 插入位置落在删除区间内 → op 移至删除起点（采用"删除优先"语义，插入文本随删除丢失）
    //   否则无变化
    private static OperationRequest transformInsert(OperationRequest op, DocumentOp applied, int appliedTextLen) {
        int pos = op.getPosition();
        if (applied.getType() == OperationType.INSERT) {
            // INSERT vs INSERT：若 applied 插入位置 <= op 插入位置，op 向右偏移
            // 相同位置时，applied 优先（先应用），op 排在其后
            if (applied.getPosition() <= pos) {
                pos += appliedTextLen;
            }
        } else {
            // INSERT vs DELETE
            int bEnd = applied.getPosition() + applied.getLength();
            if (bEnd <= pos) {
                // 删除区间完全在插入位置之前，op 左移
                pos -= applied.getLength();
            } else if (applied.getPosition() < pos) {
                // 插入位置落在删除区间内 → 删除优先，op 移至删除起点
                pos = applied.getPosition();
            }
            // else: 删除区间在插入位置之后，无变化
        }
        return makeInsert(op.getText(), pos);
    }

    // DELETE vs INSERT：
    //   若插入位置 <= 删除起点 → 删除区间整体右移
    //   若插入位置在删除区间内部 → 不扩展长度（"删除优先"，新插入文本被保留，删除止于新文本前）
    //   若插入在删除区间之后 → 无变化
    private static OperationRequest transformDelete(OperationRequest op, DocumentOp applied, int appliedTextLen) {
        int pos = op.getPosition();
        int len = op.getLength();

        if (applied.getType() == OperationType.INSERT) {
            // DELETE vs INSERT
            if (applied.getPosition() <= pos) {
                // 插入在删除起点之前，删除区间右移
                pos += appliedTextLen;
            }
            // 若插入在删除区间内部或之后，长度不变（不扩展）
        } else {
            // DELETE vs DELETE：各种重叠情况
            int aEnd = pos + len;
            int bPos = applied.getPosition();
            int bEnd = bPos + applied.getLength();

            if (bEnd <= pos) {
                // B 完全在 A 之前：A 起点左移
                pos -= applied.getLength();
            } else if (bPos >= aEnd) {
                // B 完全在 A 之后：无变化
            } else if (bPos <= pos && bEnd >= aEnd) {
                // B 完全覆盖 A：A 所有目标字符已被 B 删除，变为空操作
                pos = bPos;
                len = 0;
            } else if (bPos >= pos && bEnd <= aEnd) {
                // B 在 A 内部：A 需删除字符数减少
                len -= applied.getLength();
            } else if (bPos < pos) {
                // B 从 A 之前开始，与 A 前段重叠：[bPos, pos) 已被 B 删除
                len -= (bEnd - pos);
                pos = bPos;
            } else {
                // B 从 A 内部开始，延伸到 A 之后：A 后段已被 B 删除
                len = bPos - pos;
            }
        }
        return makeDelete(pos, Math.max(0, len));
    }

    private static OperationRequest makeInsert(String text, int pos) {
        OperationRequest r = new OperationRequest();
        r.setType(OperationType.INSERT);
        r.setPosition(Math.max(0, pos));
        r.setLength(0);
        r.setText(text);
        return r;
    }

    private static OperationRequest makeDelete(int pos, int len) {
        OperationRequest r = new OperationRequest();
        r.setType(OperationType.DELETE);
        r.setPosition(Math.max(0, pos));
        r.setLength(len);
        return r;
    }
}
