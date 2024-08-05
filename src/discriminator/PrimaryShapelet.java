/**
 * Primary Decision Node (PDN)
 * 主要形状体类，用于表示时间序列挖掘中的形状体及其相关信息。
 * 主要形状体包含一个基本的时间序列形状体，以及与之相关的两个次级形状体，
 * 还有形状体之间的相似度信息。
 */
package discriminator;

import instructor.Sequence;
import utils.Tuple2;

public class PrimaryShapelet {
    // 基本形状体，表示一个特定的时间序列片段。
    public Sequence shapelet;
    // 与基本形状体相关的两个次级形状体，用于进一步描述或分析基本形状体。
    public Tuple2<SecondaryShapelet, SecondaryShapelet> secondaryShapelet;
    // 基本形状体与两个次级形状体之间的相似度评分。
    public Double similarity;

    /**
     * 默认构造函数，初始化主要形状体的所有字段为null或0。
     */
    public PrimaryShapelet() {
        this.shapelet = null;
        this.secondaryShapelet = null;
        this.similarity = 0D;
    }

    /**
     * 完全构造函数，初始化主要形状体的所有字段。
     *
     * @param shapelet          基本时间序列形状体。
     * @param secondaryShapelet 与基本形状体相关的两个次级形状体。
     * @param similarity        基本形状体与两个次级形状体之间的相似度评分。
     */
    public PrimaryShapelet(Sequence shapelet, Tuple2<SecondaryShapelet, SecondaryShapelet> secondaryShapelet, Double similarity) {
        this.shapelet = shapelet;
        this.secondaryShapelet = secondaryShapelet;
        this.similarity = similarity;
    }

    /**
     * 获取相似度评分。
     *
     * @return 相似度评分。
     */
    public Double getSimilarity() {
        return similarity;
    }

    /**
     * 获取基本形状体。
     *
     * @return 基本时间序列形状体。
     */
    public Sequence getShapelet() {
        return shapelet;
    }

    /**
     * 获取与基本形状体相关的两个次级形状体。
     *
     * @return 包含两个次级形状体的元组。
     */
    public Tuple2<SecondaryShapelet, SecondaryShapelet> getSecondaryShapelet() {
        return secondaryShapelet;
    }

    /**
     * 设置与基本形状体相关的两个次级形状体。
     *
     * @param secondaryShapelet 包含两个次级形状体的元组。
     */
    public void setSecondaryShapelet(Tuple2<SecondaryShapelet, SecondaryShapelet> secondaryShapelet) {
        this.secondaryShapelet = secondaryShapelet;
    }

    /**
     * 设置基本形状体。
     *
     * @param shapelet 基本时间序列形状体。
     */
    public void setShapelet(Sequence shapelet) {
        this.shapelet = shapelet;
    }

    /**
     * 设置相似度评分。
     *
     * @param similarity 基本形状体与两个次级形状体之间的相似度评分。
     */
    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }
}
