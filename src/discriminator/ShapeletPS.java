package discriminator;

import evaluator.Shapelet;
import instructor.Sequence;

/**
 * ShapeletPS类是Shapelet的子类，用于表示一种特定的形状特征。
 * 它增加了第二个时间序列和分割点的概念，用于更复杂或细化的形状分析。
 */
public class ShapeletPS extends Shapelet {

    /**
     * 第二个时间序列，用于形状特征的比较或组合。
     */
    public Sequence s2;

    /**
     * 分割点数组，定义了如何将时间序列分割以评估形状特征。
     */
    public int[] split;

    /**
     * 信息增益，用于评估特征分裂对数据集划分的改善程度。
     */
    public Double gain;

    /**
     * 默认构造函数，初始化ShapeletPS对象。
     * 形状特征和分割点未初始化。
     */
    public ShapeletPS() {
        this.s1 = null;
        this.s2 = null;
        split = null;
    }

    /**
     * 构造函数，初始化ShapeletPS对象，包括两个时间序列和分割点。
     *
     * @param shapelet  原始时间序列，用于形状分析。
     * @param shapelet2 第二个时间序列，用于比较或组合形状。
     * @param split     分割点数组，定义时间序列的分割方式。
     */
    public ShapeletPS(Sequence shapelet, Sequence shapelet2, int[] split) {
        this.s1 = shapelet;
        this.s2 = shapelet2;
        this.split = split;
    }

    /**
     * 获取第二个时间序列。
     *
     * @return 第二个时间序列。
     */
    public Sequence getShapelet2() {
        return s2;
    }

    /**
     * 设置信息增益值。
     *
     * @param gain 信息增益值。
     */
    public void setGain(Double gain) {
        this.gain = gain;
    }

    /**
     * 获取信息增益值。
     *
     * @return 信息增益值。
     */
    public Double getGain() {
        return gain;
    }

    /**
     * 获取分割点数组。
     *
     * @return 分割点数组。
     */
    @Override
    public int[] getLwSplit() {
        return split;
    }
}
