package discriminator;

import instructor.Sequence;

/**
 * 二级形状体类，用于存储和操作时间序列中的形状体。
 * 该类主要维护了形状体的相似度和收缩因子两个重要属性。
 */
public class SecondaryShapelet {
    // 形状体的相似度，表示该形状体与目标时间序列的相似程度。
    public Double contract;
    // 形状体的收缩因子，用于调整形状体在时间序列中的位置和长度。
    public Double similarity;

    /**
     * 构造函数，初始化一个具有特定相似度和收缩因子的二级形状体。
     *
     * @param shapelet   表示形状体的时间序列。
     * @param similarity 形状体的相似度。
     * @param contract   形状体的收缩因子。
     */
    public SecondaryShapelet(Sequence shapelet, Double similarity, Double contract) {
        this.similarity = similarity;
        this.contract = contract;
    }

    /**
     * 默认构造函数，初始化一个相似度和收缩因子均为0的二级形状体。
     */
    public SecondaryShapelet() {
        this.similarity = 0D;
        this.contract = 0D;
    }

    /**
     * 获取形状体的相似度。
     *
     * @return 形状体的相似度。
     */
    public Double getSimilarity() {
        return this.similarity;
    }

    /**
     * 获取形状体的收缩因子。
     *
     * @return 形状体的收缩因子。
     */
    public Double getContract() {
        return this.contract;
    }

    /**
     * 设置形状体的相似度。
     *
     * @param similarity 新的相似度值。
     */
    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    /**
     * 设置形状体的收缩因子。
     *
     * @param contract 新的收缩因子值。
     */
    public void setContract(Double contract) {
        this.contract = contract;
    }
}
