package evaluator;

import instructor.Sequence;
import instructor.SequenceDataset;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

/**
 * 继承自BaseShapelets，用于寻找时间序列数据中最具代表性的形状let。
 * 该类实现了从给定的时间序列数据集中提取形状let的过程，包括各种优化策略，如熵剪枝、距离剪枝和长度调整。
 */
public class LegacyShapelets extends BaseShapelets {

    // 控制是否还有更多候选项
    public boolean hasMoreCandidates;
    // 控制是否启用熵剪枝
    public boolean entropyPruningEnabled;
    // 控制是否启用距离剪枝
    public boolean distancePruningEnabled;
    // 控制候选项的长度是否递减
    public boolean decreasingLengthOrder;
    // 控制是否启用归一化
    public boolean normalizationEnabled;
    // 控制是否启用长度归一化
    public boolean lengthNormalizationEnabled;
    // 当前处理的实例索引
    public int currInst;
    // 当前处理的实例中位置索引
    public int currPosInInst;
    // 当前处理的形状let长度
    public int currLen;

    // 用于内部计数目的
    int count;

    /**
     * 构造函数，初始化LegacyShapelets。
     *
     * @param trainSet 训练数据集
     * @param minLen   最小形状let长度
     * @param maxLen   最大形状let长度
     * @param stepSize 步长，用于控制形状let长度的变化
     */
    public LegacyShapelets(SequenceDataset trainSet, int minLen, int maxLen, int stepSize) {
        super(trainSet, minLen, maxLen, stepSize);
        try {
            // 从属性文件中加载配置参数
            Properties props = new Properties();
            File propsFile = new File(System.getProperty("ls-props", "ls.properties"));
            if (propsFile.exists()) {
                props.load(new FileInputStream(propsFile));
            }
            this.normalizationEnabled = Boolean.parseBoolean(props.getProperty("normalize", "true"));
            this.entropyPruningEnabled = Boolean.parseBoolean(props.getProperty("entropy_pruning", "true"));
            this.distancePruningEnabled = Boolean.parseBoolean(props.getProperty("distance_pruning", "true"));
            this.decreasingLengthOrder = Boolean.parseBoolean(props.getProperty("decreasing_candidate_length", "true"));
            this.lengthNormalizationEnabled = Boolean.parseBoolean(props.getProperty("length_normalization", "false"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.hasMoreCandidates = true;
        this.currInst = 0;
        this.currPosInInst = 0;
        this.currLen = this.decreasingLengthOrder ? this.maxLen : this.minLen;
    }

    /**
     * 寻找最佳形状let。
     * 该方法通过遍历所有可能的形状let，并计算其信息增益，来找到最具代表性的形状let。
     *
     * @return 最佳形状let对象
     */
    @Override
    public Shapelet findShapelet() {
        Sequence bsfShapelet = null;
        Sequence currCandidate;
        Sequence t;
        Double bsfGain = Double.NEGATIVE_INFINITY;
        Double bsfSplit = Double.NEGATIVE_INFINITY;
        Double bsfGap = Double.NEGATIVE_INFINITY;
        Double currDist;
        TreeMap<Double, ArrayList<Integer>> bsfOrderLine = null;
        TreeMap<Double, ArrayList<Integer>> orderLine = null;
        boolean pruned;
        int count = 0;
        while ((currCandidate = this.getNextCandidate()) != null) {
            count++;
            totalCandidates++;
            pruned = false;
            info.gain = Double.NEGATIVE_INFINITY;
            info.splitDist = Double.NEGATIVE_INFINITY;
            // 初始化或清空orderLine以用于当前形状let
            if (orderLine == bsfOrderLine) {
                orderLine = new TreeMap<>();
            } else {
                orderLine.clear();
            }
            for (int ind = 0; ind < this.trainSet.size(); ind++) {
                t = this.trainSet.get(ind);
                currDist = subseqDist(t, currCandidate);
                this.addToMap(orderLine, currDist, ind);
                // 如果启用熵剪枝，检查是否可以提前终止
                if (this.entropyPruningEnabled && entropyEarlyPruning(orderLine, ind, bsfGain)) {
                    pruned = true;
                    prunedCandidates++;
                    break;
                }
            }
            // 计算分割间隙，用于比较形状let的优劣
            Double start = Double.parseDouble(System.currentTimeMillis() + "");
            if (!pruned) {
                this.calcInfoGain_SplitDist(orderLine);
                this.calcSplitGap(orderLine, info.splitDist, currCandidate.size());
                if (info.gain > bsfGain) {
                    bsfGain = info.gain;
                    bsfSplit = info.splitDist;
                    bsfGap = info.splitGap;
                    bsfShapelet = currCandidate;
                    bsfOrderLine = orderLine;
                } else if (this.nearlyEqual(info.gain, bsfGain, 1e-6) && info.splitGap > bsfGap) {
                    bsfGain = info.gain;
                    bsfSplit = info.splitDist;
                    bsfGap = info.splitGap;
                    bsfShapelet = currCandidate;
                    bsfOrderLine = orderLine;
                }
            }
        }
        System.out.println(count);
        // 创建并返回最佳形状let对象
        Shapelet bestFound = new Shapelet(bsfShapelet, bsfSplit, bsfOrderLine);
        return bestFound;
    }

    /**
     * 计算分割间隙。
     * 分割间隙是衡量一个形状let在给定分割点处分割数据集后，左右两部分差异性的度量。
     *
     * @param orderLine 以距离排序的点集合
     * @param splitDist 分割点的距离
     * @param len       形状let的长度
     */
    private void calcSplitGap(TreeMap<Double, ArrayList<Integer>> orderLine, Double splitDist, int len) {
        Double meanLeft = 0D, meanRight = 0D;
        Double[] distances = orderLine.keySet().toArray(new Double[orderLine.keySet().size()]);
        int j = 0;
        for (int i = 0; distances[i] < splitDist; i++) {
            meanLeft += distances[i];
            j++;
        }
        meanLeft /= j;
        for (int i = j; i < distances.length; i++) {
            meanRight += distances[i];
        }
        meanRight /= (distances.length - j);
        info.splitGap = (meanRight - meanLeft) / Math.sqrt(len);
    }

    /**
     * 获取下一个候选项。
     * 该方法根据当前配置和状态生成下一个形状let候选项。
     *
     * @return 下一个形状let候选项，如果没有更多候选项则返回null
     */
    public Sequence getNextCandidate() {
        Sequence candidate = null;
        if (this.hasMoreCandidates) {
            Sequence currTS = this.trainSet.get(this.currInst);
            candidate = new Sequence(currTS, this.currPosInInst, this.currPosInInst + this.currLen);
            this.incrementCandidatePosition();
        }
        return candidate;
    }

    /**
     * 更新当前形状let的候选项位置。
     * 该方法用于在生成下一个候选项后更新当前状态，包括当前实例索引、位置索引和形状let长度。
     */
    public void incrementCandidatePosition() {
        count++;
        this.currPosInInst++;
        if (this.currPosInInst + this.currLen > this.trainSet.get(this.currInst).size()) {
            this.currPosInInst = 0;
            this.currInst++;
            if (this.currInst > (this.trainSet.size() - 1)) {
                this.currInst = 0;
                int changeFactor = this.stepSize * (this.decreasingLengthOrder ? -1 : 1);
                this.currLen += changeFactor;
                if ((this.decreasingLengthOrder && (this.currLen < this.minLen))
                        || (!this.decreasingLengthOrder && (this.currLen > this.maxLen))) {
                    System.out.println("total:" + count);
                    count = 0;
                    this.hasMoreCandidates = false;
                }
            }
        }
    }

    /**
     * 计算子序列距离。
     * 该方法用于计算两个时间序列之间的距离，是形状let搜索过程中的核心计算之一。
     *
     * @param t 第一个时间序列
     * @param s 第二个时间序列
     * @return 两个时间序列之间的距离
     */
    //s是子序列
    public Double subseqDist(Sequence t, Sequence s) {
        Double minDist = Double.POSITIVE_INFINITY;
        boolean stopped;
        Double currDist;
        Double ti,
                tMu,
                tSigma,
                si,
                sMu = s.mean(0, s.size()),
                sSigma = s.stdv(0, s.size());
        Double lengthNormalizer = this.lengthNormalizationEnabled ? 1.0 / s.size()
                : 1.0;
        for (int tInd = 0; tInd < t.size() - s.size() + 1; tInd++) {
            stopped = false;
            currDist = 0D;
            tMu = t.mean(tInd, s.size());
            tSigma = t.stdv(tInd, s.size());
            for (int sInd = 0; sInd < s.size(); sInd++) {
                ti = t.get(tInd + sInd);
                si = s.get(sInd);
                if (this.normalizationEnabled) {
                    ti = (ti - tMu) / tSigma;
                    si = (si - sMu) / sSigma;
                }
                currDist += Math.pow((ti - si), 2) * lengthNormalizer;
                if (this.distancePruningEnabled && currDist >= minDist) {
                    stopped = true;
                    break;
                }
            }
            if (this.distancePruningEnabled && !stopped) {
                minDist = currDist;
            } else if (currDist < minDist) {
                minDist = currDist;
            }
        }
        return minDist;
    }

    /**
     * 判断是否可以进行熵剪枝。
     * 该方法用于在计算信息增益前判断是否可以通过熵剪枝提前终止形状let的搜索。
     *
     * @param orderLine 以距离排序的点集合
     * @param ind       当前处理的实例索引
     * @param bsfGain   当前最佳信息增益
     * @return 如果可以进行熵剪枝则返回true，否则返回false
     */
    public boolean entropyEarlyPruning(TreeMap<Double, ArrayList<Integer>> orderLine, int ind, Double bsfGain) {
        Double minEnd = 0D;
        Double maxEnd = 1 + orderLine.lastKey();
        TreeMap<Double, ArrayList<Integer>> optimisticOrderLine = new TreeMap<>();
        for (Integer cls : this.trainSet.getAllClasses()) {
            Double start = Double.parseDouble(System.currentTimeMillis() + "");
            createOptimisticOrderLine(optimisticOrderLine, orderLine, ind, cls.intValue(), minEnd, maxEnd);
            this.calcInfoGain_SplitDist(optimisticOrderLine);
            if (info.gain > bsfGain) {
                return false;
            }
            start = Double.parseDouble(System.currentTimeMillis() + "");
            createOptimisticOrderLine(optimisticOrderLine, orderLine, ind, cls.intValue(), maxEnd, minEnd);
            this.calcInfoGain_SplitDist(optimisticOrderLine);
            if (info.gain > bsfGain) {
                return false;
            }
        }
        return true;
    }

    private void createOptimisticOrderLine(TreeMap<Double, ArrayList<Integer>> optimisticOrderLine,
                                           TreeMap<Double, ArrayList<Integer>> orderLine, int ind, int cls, Double end1,
                                           Double end2) {
        optimisticOrderLine.clear();
        for (Entry<Double, ArrayList<Integer>> entry : orderLine.entrySet()) {
            optimisticOrderLine.put(entry.getKey(), new ArrayList<Integer>(entry.getValue()));
        }
        for (int i = ind + 1; i < this.trainSet.size(); i++) {
            if (this.trainSet.get(i).getLabel() == cls) {
                this.addToMap(optimisticOrderLine, end1, i);
            } else {
                this.addToMap(optimisticOrderLine, end2, i);
            }
        }
    }

    public void calcInfoGain_SplitDist(TreeMap<Double, ArrayList<Integer>> orderLine) {
        int size = orderLine.keySet().size();
        Double[] keys = orderLine.keySet().toArray(new Double[size]);
        Double meanSplit;
        Double currGain;
        Double bestGain = Double.NEGATIVE_INFINITY;
        Double bestSplit = 0D;
        for (int i = 0; i < size - 1; i++) {
            meanSplit = (keys[i] + keys[i + 1]) / 2;
            currGain = this.calcGain(orderLine, meanSplit);
            if (currGain > bestGain) {
                bestGain = currGain;
                bestSplit = meanSplit;
            }
        }
        info.gain = bestGain;
        info.splitDist = bestSplit;
    }

    public Double calcGain(TreeMap<Double, ArrayList<Integer>> orderLine, Double splitDist) {
        SequenceDataset[] d = splitDataset(orderLine, splitDist);
        return trainSet.entropy() - (d[0].entropy() * d[0].size() + d[1].entropy() * d[1].size()) / trainSet.size();
    }

    @Override
    public Double getDist(Sequence t, Sequence s) {
        return subseqDist(t, s);
    }

    public void toggleEntropyPruning(boolean newState) {
        this.entropyPruningEnabled = newState;
    }

    public void setInversedSearch() {
        this.decreasingLengthOrder = false;
        this.currLen = this.minLen;
    }

    public void disableNormalization() {
        this.normalizationEnabled = false;
    }
}
