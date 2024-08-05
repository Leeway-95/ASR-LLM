package discriminator;

import evaluator.Shapelet;
import evaluator.ShapeletMatrix;
import instructor.Sequence;
import instructor.SequenceDataset;
import utils.ToolsModel;
import utils.Tuple2;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RCSF训练类
 * 用于根据给定参数和数据集训练RCSF模型，通过构建PSTree树集合来实现。
 */
public class RCSFTrain {

    // 存储训练生成的PSTree树的列表
    static ArrayList<PSTree> dtList;

    /**
     * 获取PSTree树列表
     *
     * @return PSTree树的列表
     */
    public static ArrayList<PSTree> getDtList() {
        return dtList;
    }

    /**
     * RCSF训练方法入口
     * 根据传入的参数数组、PSTree列表、子集比例和偏移量进行模型训练。
     *
     * @param params   参数数组，包含训练配置信息
     * @param dtList   PSTree树列表，用于存储训练生成的树
     * @param subRadio 子集比例，用于确定训练和测试集的大小
     * @param offset   偏移量，用于分批训练，控制不同批次的数据集
     */
    public RCSFTrain(String params[], ArrayList<PSTree> dtList, Double subRadio, int offset) {
        this.dtList = dtList;
        try {
            // 初始化配置
            String mode = "train";
            CommonConfig cc = new CommonConfig(params, mode);
            int method = cc.getMethod();

            // 分割数据集
            ArrayList<Sequence> dataTrainSet = cc.getTrainSet();
            ArrayList<Sequence> dataTrainSubset = new ArrayList<>(dataTrainSet.subList((int) (dataTrainSet.size() * subRadio * offset), (int) (dataTrainSet.size() * subRadio * (offset + 1))));
            ArrayList<Sequence> dataTestSet = cc.getTestSet();
            ArrayList<Sequence> dataTestSubset = new ArrayList<>(dataTestSet.subList((int) (dataTestSet.size() * subRadio * offset), (int) (dataTestSet.size() * subRadio * (offset + 1))));

            // 创建数据集对象
            SequenceDataset trainSet = new SequenceDataset(dataTrainSubset),
                    testSet = new SequenceDataset(dataTestSubset),
                    trainSetBagged;

            // 记录训练开始时间
            long start = System.currentTimeMillis();

            // 获取集成大小
            int ensembleSize = cc.getEnsembleSize();
            for (int j = 1; j < ensembleSize + 1; j++) {
                // 打印训练进度
                System.out.println("Iterative times: " + (offset + 1) + ", building tree: " + j);
//                System.out.println();

                // 创建袋装训练集
                trainSetBagged = new SequenceDataset();
                trainSetBagged.setIterativeTimes(offset + 1);
                trainSetBagged.setTreeId(j);
                trainSetBagged.setTrainsetName(cc.getDataSetName());

                // 生成随机索引，进行有放回的抽样
                Random rng = new Random();
                IntStream randIntStream = rng.ints(trainSet.size(), 0, trainSet.size());
                ArrayList<Integer> randIndices = (ArrayList<Integer>) randIntStream.boxed().collect(Collectors.toList());
                for (Integer ind : randIndices) {
                    trainSetBagged.add(trainSet.get(ind));
                }
                ShapeletMatrix matrix = new ShapeletMatrix(trainSetBagged);
                Tuple2[] indexes = matrix.findSimilarTimeSeries(10);
//                matrix.printDataset();
                matrix.updateMatrix(matrix.getMatrix(), indexes);
//                matrix.printMatrix();
                Shapelet s1 = new Shapelet();
                Shapelet s2 = new Shapelet();
                s1.setContent(trainSetBagged.getDataset().get(0).getS());
                s2.setContent(trainSetBagged.getDataset().get(1).getS());
                boolean evaluate = matrix.evaluate(s1, s2, 0.025);
                System.out.print("Evaluate Result: ");
                if (!evaluate) {
                    ToolsModel.printColor(ToolsModel.RED, evaluate + "");
                    System.out.println();
                    continue;
                } else {
                    ToolsModel.printColor(ToolsModel.GREEN, evaluate + "");
                }
                System.out.println();

                // 根据袋装训练集构建PSTree树
                PSTree tree = new PSTree.Builder(trainSetBagged, method)
                        .minLen(cc.getMinLen())
                        .maxLen(cc.getMaxLen())
                        .stepSize(cc.getStepSize())
                        .leafeSize(cc.getLeafSize())
                        .treeDepth(cc.getTreeDepth())
                        .build();
                dtList.add(tree);
            }
            // 记录训练结束时间
            long stop = System.currentTimeMillis();

            // 计算并打印训练耗时
            String trainingTime = ((stop - start) / 1e3) + "";
//                    System.out.println("trainingTime " + trainingTime);

            // 保存训练结果
            cc.saveResults(dtList, trainSet, testSet, trainingTime, mode, offset);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
