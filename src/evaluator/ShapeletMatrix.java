package evaluator;

import instructor.Sequence;
import instructor.SequenceDataset;
import utils.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class ShapeletMatrix {
    SequenceDataset tsd;
    ArrayList<Sequence> dataset;
    Double[][] matrix;

    public ShapeletMatrix(SequenceDataset tsd) {
        this.tsd = tsd;
        this.dataset = tsd.dataset;
        matrix = new Double[dataset.size()][dataset.size()];
    }

    public Double[][] getMatrix() {
        return matrix;
    }


    public void printDataset() {
        ArrayList<Sequence> dataset = tsd.getDataset();
        for (Sequence ts : dataset) {
            System.out.print(ts.getShapletId() + 1 + " ");
            for (Double t : ts.s) {
                System.out.print(t + " ");
            }
            System.out.println();
        }
    }

    public void printMatrix() {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }

    public Tuple2<Double, Double>[] findSimilarTimeSeries(int treeNodes) {
        Tuple2<Double, Double>[] indexes = new Tuple2[treeNodes];
        int size = dataset.size();
        Random rand = new Random();
        int randomIndex = rand.nextInt(size);
        Tuple2<Double, Double> t = new Tuple2(randomIndex, 0);
        indexes[0] = t;
        Sequence s1 = dataset.get(randomIndex);
        TimeSeries ts1 = new TimeSeries(s1.getS());
        if (treeNodes > size) {
            treeNodes = size;
        }
        double minDistance = 0.0;
        int minDistanceIndex = 0;
        boolean changed;
        for (int j = 1; j < treeNodes; j++) {
            changed = false;
            for (int i = 0; i < size; i++) {
                if (i != randomIndex) {
                    Sequence s2 = dataset.get(i);
                    TimeSeries ts2 = new TimeSeries(s2.getS());
                    TimeWarpInfo timeWarpInfo = FastDTW.fastDTW(ts1, ts2, 1, DistanceFunctionFactory.EUCLIDEAN_DIST_FN);
                    double distance = timeWarpInfo.getDistance();
                    if (minDistance == 0D) {
                        minDistance = distance;
                    } else {
                        if (distance < minDistance) {
                            minDistance = distance;
                            minDistanceIndex = i;
                            dataset.remove(i);
                            size--;
                            changed = true;
                        }
                    }
                }
            }
            if (!changed) {
                break;
            }
            minDistance = Double.parseDouble(String.format("%.3f", minDistance));
            Tuple2<Double, Double> tj = new Tuple2(minDistanceIndex, minDistance);
            indexes[j] = tj;
        }
        System.out.println("MinDistanceIndex: " + Arrays.toString(indexes) + " ,MinDistance: " + minDistance);
        return indexes;
    }

    public boolean evaluate(Shapelet treeClass, Shapelet rootClass, double threshold) {
        TimeSeries ts1 = new TimeSeries(treeClass.content);
        TimeSeries ts2 = new TimeSeries(rootClass.content);
        TimeWarpInfo timeWarpInfo = FastDTW.fastDTW(ts1, ts2, 1, DistanceFunctionFactory.EUCLIDEAN_DIST_FN);
        double similarity = ToolsDataset.distanceToSimilarity(timeWarpInfo.getDistance());
        BigDecimal distance = new BigDecimal(Double.toString(timeWarpInfo.getDistance()));
        similarity = Double.parseDouble(String.format("%.3f", similarity));
        System.out.println("Distance:" + distance.setScale(3, RoundingMode.HALF_UP).doubleValue() + " ,Similarity:" + similarity + ", Threshold:" + threshold);
        if (similarity > threshold) {
            // 距离大 相似度低 不处理
            return false;
        } else {
            return true;
        }
    }

    public static void updateMatrix(Double[][] matrix, Tuple2<Integer, Double>[] indexes) {
        int len = 0;
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] != null) {
                len++;
            }
        }
        if (len > 1) {
            for (int i = 1; i < len; i++) {
                Tuple2<Integer, Double> index = indexes[0];
                int indexi = index.getFirst();
                int indexj = indexes[i].getFirst();
                Double distance = indexes[i].getSecond();
                matrix[indexj][indexi] = distance;
                matrix[indexi][indexj] = Double.valueOf(i);
            }
        }
    }
}
