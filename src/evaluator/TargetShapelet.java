/**
 * 目标Shapelet类，用于表示时间序列中的一个潜在形状。
 * 该类封装了一个时间序列和与之关联的相似度值。
 */
package evaluator;

import instructor.Sequence;

public class TargetShapelet {

    /**
     * 存储Shapelet的时间序列。
     */
    public Sequence shapelet;
    /**
     * 存储Shapelet与目标时间序列的相似度值。
     */
    public Double similarity;

    /**
     * 默认构造函数，初始化shapelet为null，similarity为0.0。
     */
    public TargetShapelet() {
        this.shapelet = null;
        this.similarity = 0D;
    }

    /**
     * 带参数的构造函数，用于初始化shapelet和similarity。
     *
     * @param shapelet   时间序列对象，表示一个Shapelet。
     * @param similarity 相似度值，表示Shapelet与目标时间序列的相似程度。
     */
    public TargetShapelet(Sequence shapelet, Double similarity) {
        this.shapelet = shapelet;
        this.similarity = similarity;
    }

    /**
     * 获取Shapelet的时间序列。
     *
     * @return 返回时间序列对象。
     */
    public Sequence getShapelet() {
        return shapelet;
    }

    /**
     * 获取Shapelet的相似度值。
     *
     * @return 返回相似度值。
     */
    public Double getSimilarity() {
        return similarity;
    }

    /**
     * 设置Shapelet的相似度值。
     *
     * @param similarity 新的相似度值。
     */
    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    /**
     * 设置Shapelet的时间序列。
     *
     * @param shapelet 新的时间序列对象。
     */
    public void setShapelet(Sequence shapelet) {
        this.shapelet = shapelet;
    }
}
