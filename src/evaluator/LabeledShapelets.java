package evaluator;

import discriminator.ShapeletPS;
import instructor.Sequence;
import instructor.SequenceDataset;

import java.util.Random;

/**
 * LabeledShapelets类继承自LegacyShapelets，用于寻找和处理时间序列中的形状let。
 * 它增加了对形状let长度间隔大小、百分比、训练集大小和序列长度的处理。
 */
public class LabeledShapelets extends LegacyShapelets {

    // 形状let长度间隔大小
    private int lengthIntervalSize;
    // 随机选取的形状let的百分比
    public Double percentage;
    // 训练集大小
    private int trainSize;
    // 时间序列长度
    private int seriesLength;
    // 随机数生成器
    public static Random rand;

    /**
     * 构造函数初始化LabeledShapelets对象。
     *
     * @param trainSet 训练集
     * @param minLen 最小形状let长度
     * @param maxLen 最大形状let长度
     * @param stepSize 步长
     */
    public LabeledShapelets(SequenceDataset trainSet, int minLen, int maxLen, int stepSize) {
        super(trainSet, minLen, maxLen, stepSize);
        //+1是因为rand.nextInt不会出现上界的数
        lengthIntervalSize = maxLen - minLen;
        //percentage = Lab.percentage;
        percentage = 0.01;
        trainSize = trainSet.size();
        seriesLength = trainSet.get(0).getS().length;
        rand = new Random();
    }

    // 记录下一个计算的次数
    public Double nextcount = 0D;
    // 记录计算的总次数
    public Double calccount = 0D;

    /**
     * 寻找最佳形状let的方法。
     * 通过计算和比较不同候选形状let的信息增益和间隙，来找到最佳的形状let。
     *
     * @return 最佳形状let
     */
    @Override
    public ShapeletPS findShapelet() {
        Sequence bsfShapelet1 = null;
        Sequence[] currCandidate;
        Sequence bsfShapelet2 = null;
        Sequence t;
        tmpInfo tmpInfo = new tmpInfo();
        int[] bsfSplit = null;
        Double bsfGain = Double.NEGATIVE_INFINITY;
        Double bsfGap = Double.NEGATIVE_INFINITY;
        // 根据序列长度、最小长度和最大长度计算需要检查的形状let数量
        int numToInspect = (int) ((seriesLength * (maxLen - minLen) - (maxLen + minLen) * (maxLen - minLen) / 2) * trainSize * percentage);
        if (numToInspect == 0) numToInspect = 1;

        for (int i = 0; i < numToInspect; i++) {
            Double start = Double.parseDouble(System.currentTimeMillis() + "");
            currCandidate = getNextCandidateLw();
            nextcount += System.currentTimeMillis() - start;
            totalCandidates++;
            info.gain = Double.NEGATIVE_INFINITY;
            start = Double.parseDouble(System.currentTimeMillis() + "");
            Double[] tmp = calcGainAndGap(currCandidate, bsfGain, tmpInfo);
            calccount += System.currentTimeMillis() - start;
            Double currGain, currGap;
            if (tmp[0] != null) {
                currGain = tmp[0];
            } else {
                currGain = 0D;
            }
            if (tmp[1] != null) {
                currGap = tmp[1];
            } else {
                currGap = 0D;
            }
            if (currGain > bsfGain) {
                bsfGain = currGain;
                bsfGap = currGap;
                bsfShapelet1 = currCandidate[0];
                bsfShapelet2 = currCandidate[1];
                bsfSplit = tmpInfo.split;
            } else if (this.nearlyEqual(currGain, bsfGain, 1e-6) && currGap > bsfGap) {
                bsfGain = currGain;
                bsfGap = currGap;
                bsfShapelet1 = currCandidate[0];
                bsfShapelet2 = currCandidate[1];
                bsfSplit = tmpInfo.split;
            }
        }
        ShapeletPS best = new ShapeletPS(bsfShapelet1, bsfShapelet2, bsfSplit);
        best.setGain(bsfGain);
        return best;
    }

    /**
     * 使用熵早期剪枝策略来判断是否应该继续处理当前的形状let。
     *
     * @param nearS1 第一组近邻序列
     * @param nearS2 第二组近邻序列
     * @param index 当前处理的序列索引
     * @param bsfGain 当前最佳信息增益
     * @return 是否应该剪枝
     */
    public boolean entropyEarlyPruning(SequenceDataset nearS1, SequenceDataset nearS2, int index, Double bsfGain) {
        for (Integer cls : trainSet.getAllClasses()) {
            SequenceDataset optimalNearS1Case1 = new SequenceDataset();
            SequenceDataset optimalNearS2Case1 = new SequenceDataset();
            for (int i = 0; i < nearS1.size(); i++)
                optimalNearS1Case1.add(nearS1.get(i));
            for (int i = 0; i < nearS2.size(); i++)
                optimalNearS2Case1.add(nearS2.get(i));
            for (int i = index + 1; i < trainSize; i++) {
                if (trainSet.get(i).getLabel() == cls)
                    optimalNearS1Case1.add(trainSet.get(i));
                else optimalNearS2Case1.add(trainSet.get(i));
            }
            Double start = Double.parseDouble(System.currentTimeMillis() + "");
            Double gain1 = this.calcGain(optimalNearS1Case1, optimalNearS2Case1);
            if (gain1 > bsfGain)
                //不应剪枝
                return false;

            SequenceDataset optimalNearS1Case2 = new SequenceDataset();
            SequenceDataset optimalNearS2Case2 = new SequenceDataset();
            for (int i = 0; i < nearS1.size(); i++)
                optimalNearS1Case2.add(nearS1.get(i));
            for (int i = 0; i < nearS2.size(); i++)
                optimalNearS2Case2.add(nearS2.get(i));
            for (int i = index + 1; i < trainSize; i++) {
                if (trainSet.get(i).getLabel() == cls)
                    optimalNearS2Case2.add(trainSet.get(i));
                else optimalNearS1Case2.add(trainSet.get(i));
            }
            start = Double.parseDouble(System.currentTimeMillis() + "");
            Double gain2 = this.calcGain(optimalNearS1Case2, optimalNearS2Case2);
            if (gain2 > bsfGain)
                //不应剪枝
                return false;
        }
        return true;
    }

    public Double d1cost = 0D, d2cost = 0D;

    /**
     * 计算当前候选形状let的信息增益和间隙。
     *
     * @param currCandidate 当前候选形状let
     * @param bsfGain 当前最佳信息增益
     * @param tmpInfo 临时信息存储对象
     * @return 一个包含信息增益和间隙的数组
     */
    private Double[] calcGainAndGap(Sequence[] currCandidate, Double bsfGain, tmpInfo tmpInfo) {
        boolean pruned = false;
        Double gap = 0D;
        Sequence t;
        Double distance1, distance2;

        int[] split = new int[trainSize];
        SequenceDataset nearS1 = new SequenceDataset();
        SequenceDataset nearS2 = new SequenceDataset();
        for (int i = 0; i < trainSize; i++) {
            t = trainSet.get(i);
            Double start = Double.parseDouble(System.currentTimeMillis() + "");
            distance1 = subseqDist(t, currCandidate[0]);
            d1cost += (System.currentTimeMillis() - start);
            start = Double.parseDouble(System.currentTimeMillis() + "");
            distance2 = subseqDist(t, currCandidate[1]);
            d2cost += (System.currentTimeMillis() - start);
            if (distance1 < distance2) {
                gap += (distance2 - distance1);
                nearS1.add(t);
            } else {
                gap += (distance1 - distance2);
                nearS2.add(t);
                split[i] = 1;
            }
            start = Double.parseDouble(System.currentTimeMillis() + "");
            if (entropyPruningEnabled && entropyEarlyPruning(nearS1, nearS2, i, bsfGain)) {
                pruned = true;
                prunedCandidates++;
                break;
            }
        }
        Double[] rst = new Double[2];
        Double start = Double.parseDouble(System.currentTimeMillis() + "");
        if (!pruned) {
            tmpInfo.split = split;
            rst[0] = calcGain(nearS1, nearS2);
            rst[1] = gap / Math.sqrt(currCandidate[0].size());
        }
        return rst;
    }

    /**
     * 计算给定两组序列的信息增益。
     *
     * @param nearS1 第一组序列
     * @param nearS2 第二组序列
     * @return 信息增益
     */
    private Double calcGain(SequenceDataset nearS1, SequenceDataset nearS2) {
        return trainSet.entropy() - (nearS1.entropy() * nearS1.size() + nearS2.entropy() * nearS2.size()) / trainSize;
    }

    /**
     * 获取下一个候选形状let的方法。
     *
     * @return 下一个候选形状let
     */
    public Sequence[] getNextCandidateLw() {
        int length1 = rand.nextInt(lengthIntervalSize) + minLen;
        int length2 = rand.nextInt(lengthIntervalSize) + minLen;
        int startPos1 = rand.nextInt(seriesLength - length1);
        int startPos2 = rand.nextInt(seriesLength - length2);

        int seriesId1 = rand.nextInt(trainSize);
        int seriesId2 = rand.nextInt(trainSize);
        while (trainSet.get(seriesId1).getLabel() == trainSet.get(seriesId2).getLabel())
            seriesId2 = rand.nextInt(trainSize);
        Sequence whole1 = trainSet.get(seriesId1);
        Sequence whole2 = trainSet.get(seriesId2);

        Sequence[] ts = new Sequence[2];
        ts[0] = new Sequence(whole1, startPos1, startPos1 + length1);
        ts[0].setLabel(whole1.getLabel());
        ts[0].setStartPos(startPos1);
        ts[1] = new Sequence(whole2, startPos2, startPos2 + length2);
        ts[1].setLabel(whole2.getLabel());
        ts[1].setStartPos(startPos2);

        return ts;

    }

    /**
     * 根据测试实例和形状let决定分支。
     *
     * @param testInst 测试实例
     * @param Lw 形状let
     * @return 分支编号
     */
    public int getBranch(Sequence testInst, ShapeletPS Lw) {
        Double distance1 = subseqDist(testInst, Lw.getShapelet());
        Double distance2 = subseqDist(testInst, Lw.getShapelet2());
        if (distance1 < distance2)
            return 0;
        else
            return 1;
    }
}

class tmpInfo {
    public int[] split;
}