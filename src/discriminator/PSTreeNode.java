package discriminator;

import java.util.ArrayList;

public class PSTreeNode {

    /* 节点属性名称* */
    private String attrname;

    /* 节点索引标号* */
    private int nodeIndex;

    /* 包含的叶子节点数* */
    private int leafNum;

    /* 节点误差率* */
    private Double alpha;

    /* 父亲分类属性值* */
    private String parentAttrValue;

    /* 孩子节点* */
    private PSTreeNode[] childAttrNode;

    /* 数据记录索引* */
    private ArrayList<String> dataIndex;

    /* 节点类型 */
    private String nodeType;

    public String getAttrname() {
        return attrname;
    }

    public void setAttrname(String attrname) {
        this.attrname = attrname;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public int getLeafNum() {
        return leafNum;
    }

    public void setLeafNum(int leafNum) {
        this.leafNum = leafNum;
    }

    public Double getAlpha() {
        return alpha;
    }

    public void setAlpha(Double alpha) {
        this.alpha = alpha;
    }

    public String getParentAttrValue() {
        return parentAttrValue;
    }

    public void setParentAttrValue(String parentAttrValue) {
        this.parentAttrValue = parentAttrValue;
    }

    public PSTreeNode[] getChildAttrNode() {
        return childAttrNode;
    }

    public void setChildAttrNode(PSTreeNode[] childAttrNode) {
        this.childAttrNode = childAttrNode;
    }

    public ArrayList<String> getDataIndex() {
        return dataIndex;
    }

    public void setDataIndex(ArrayList<String> dataIndex) {
        this.dataIndex = dataIndex;
    }

    enum TYPE {
        PRIMARY_DECISION_NODE,
        SECONDARY_DECISION_NODE,
        CLASSIFIED_LEAF_NODE
    }
}
