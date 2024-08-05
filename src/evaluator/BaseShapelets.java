/**
 * 抽象基类，用于定义形状let搜索的基础行为。
 * 提供了数据集分割、距离计算和候选项管理等通用功能。
 */
package evaluator;

import instructor.Sequence;
import instructor.SequenceDataset;

import java.util.ArrayList;
import java.util.TreeMap;

public abstract class BaseShapelets {

    // 总候选数
    public static long totalCandidates = 0;
    // 被剪枝的候选数
    public static long prunedCandidates = 0;
    // 训练数据集
    public SequenceDataset trainSet;
    // 最小形状let长度
    public int minLen;
    // 最大形状let长度
    public int maxLen;
    // 步长
    public int stepSize;
    // 信息对象，用于存储搜索过程中的信息
    public Info info;

    /**
     * 构造函数，初始化基类。
     *
     * @param trainSet 训练数据集
     * @param minLen   最小形状let长度
     * @param maxLen   最大形状let长度
     * @param stepSize 步长，用于确定形状let长度的递增
     */
    public BaseShapelets(SequenceDataset trainSet, int minLen, int maxLen, int stepSize) {
        this.trainSet = trainSet;
        this.minLen = minLen;
        this.maxLen = maxLen;
        this.stepSize = stepSize;
        this.info = new Info();
    }

    // 获取总候选数
    public long getTotalCandidates() {
        return totalCandidates;
    }

    // 获取被剪枝的候选数
    public long getPrunedCandidates() {
        return prunedCandidates;
    }

    // 抽象方法，用于寻找形状let
    public abstract Shapelet findShapelet();

    // 抽象方法，计算两个时间序列的距离
    public abstract Double getDist(Sequence t, Sequence s);

    /**
     * 向Map中添加元素，如果键已存在，则将值添加到对应的列表中。
     *
     * @param container 存储键值对的Map
     * @param key       键
     * @param value     值
     */
    public void addToMap(TreeMap<Double, ArrayList<Integer>> container, Double key, Integer value) {
        ArrayList<Integer> values = container.getOrDefault(key, new ArrayList<Integer>());
        values.add(value);
        container.put(key, values);
    }

    /**
     * 根据分割数组将数据集分割为两个子集。
     *
     * @param split 分割数组，指示每个时间序列属于哪个子集
     * @return 分割后的数据集
     */
    public SequenceDataset[] splitDataset(int[] split) {
        SequenceDataset[] splits = new SequenceDataset[2];
        splits[0] = new SequenceDataset();
        splits[1] = new SequenceDataset();
        for (int i = 0; i < trainSet.size(); i++) {
            if (split[i] == 0) {
                splits[0].add(trainSet.get(i));
            } else {
                splits[1].add(trainSet.get(i));
            }
        }
        return splits;
    }

    /**
     * 根据距离值将数据集分割为两个子集。
     *
     * @param obj_hist   对象的历史记录，存储了对象的距离和索引
     * @param split_dist 分割距离值
     * @return 分割后的数据集
     */
    public SequenceDataset[] splitDataset(TreeMap<Double, ArrayList<Integer>> obj_hist, Double split_dist) {
        final int numSplits = 2;
        final boolean usingWeights = this.trainSet.isUsingWeights();

        SequenceDataset[] splits = new SequenceDataset[numSplits];
        Double[] sums = new Double[numSplits];
        ArrayList<ArrayList<Double>> weights = new ArrayList<>();

        for (int i = 0; i < numSplits; i++) {
            splits[i] = new SequenceDataset();
            sums[i] = 0D;
            weights.add(new ArrayList<>());
        }

        for (Double d : obj_hist.keySet()) {
            if (d.doubleValue() < split_dist) {
                for (Integer index : obj_hist.get(d)) {
                    splits[0].add(this.trainSet.get(index));
                    if (usingWeights) {
                        weights.get(0).add(this.trainSet.getWeight(index));
                        sums[0] += this.trainSet.getWeight(index);
                    }
                }
            } else {
                for (Integer index : obj_hist.get(d)) {
                    splits[1].add(this.trainSet.get(index));
                    if (usingWeights) {
                        weights.get(1).add(this.trainSet.getWeight(index));
                        sums[1] += this.trainSet.getWeight(index);
                    }
                }
            }
        }
        if (usingWeights) {
            for (int i = 0; i < numSplits; i++) {
                for (int j = 0; j < weights.get(i).size(); j++) {
                    weights.get(i).set(j, weights.get(i).get(j) / sums[i]);
                }
                splits[i].setWeights(weights.get(i));
            }
        }
        return splits;
    }

    /**
     * 计算需要处理的候选数总数。
     *
     * @return 需要处理的候选数总数
     */
    public long getNumOfCandidatesToProcess() {
        long total = 0L;
        long temp;
        for (int cL = this.minLen; cL <= this.maxLen; cL += this.stepSize) {
            temp = 0L;
            for (int cPiI = 0; (cPiI + cL) <= this.trainSet.get(0).size(); cPiI++) {
                temp++;
            }
            total += temp * this.trainSet.size();
        }
        return total;
    }

    /**
     * 判断两个Double值是否近似相等。
     *
     * @param a       第一个Double值
     * @param b       第二个Double值
     * @param epsilon 允许的误差范围
     * @return 如果两个值近似相等，则返回true；否则返回false
     */
    public boolean nearlyEqual(Double a, Double b, Double epsilon) {
        final Double absA = Math.abs(a);
        final Double absB = Math.abs(b);
        final Double diff = Math.abs(a - b);

        if (a == b) {                       // shortcut, handles infinities
            return true;
        } else if (a == 0 || b == 0 || diff < Double.MIN_NORMAL) {
            // a or b is zero or both are extremely close to it
            // relative error is less meaningful here
            return diff < (epsilon * Double.MIN_NORMAL);
        } else { // use relative error
            return diff / Math.min((absA + absB), Double.MAX_VALUE) < epsilon;
        }
    }

    /**
     * 信息类，用于存储搜索过程中的信息，如信息增益、分割距离和分割间隙。
     */
    public class Info {
        public Double gain;
        public Double splitDist;
        public Double splitGap;
    }
}
