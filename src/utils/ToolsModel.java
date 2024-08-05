package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import instructor.Instruction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static utils.ToolsDataset.exp;


public class ToolsModel {
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static void printColor(String color, String message) {
        System.out.print(color + message + RESET);
    }

    public static void printlnColor(String color, String message) {
        System.out.println(color + message + RESET);
    }

    public static String getTime() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        String formattedDate = dateFormat.format(currentDate);
        return formattedDate;
    }

    /**
     * P（Positive）：代表1  N（Negative）：代表0
     * T（True）：代表预测正确  F（False）：代表预测错误
     * TP：预测为1，预测正确，即实际1
     * FP：预测为1，预测错误，即实际0
     * FN：预测为0，预测错误，即实际1
     * TN：预测为0，预测正确，即实际0
     * <p>
     * 1.准确率 Accuracy 模型正确分类的样本占总样本数的比例。准确率代表整体的预测准确程度，既包括正样本，也包括负样本。
     * Accuracy = (TP+TN)/(TP+TN+FP+FN)
     * 2.精确率 Precision 模型判断为正例样本中，真正为正例的样本数比例。精准率代表你认为找的是对的实际上多少是对的。
     * Precision = TP/(TP+FP)
     * 3.召回率（检全率/真正率/灵敏度ROC） Recall 实际为正的样本中被预测为正样本的概率。召回率表示实际上是对的样本中找出来对的的概率。
     * Recall = TP/(TP+FN)
     * 4.F1-score 精确率和和召回率的调和平均值
     * F1-score = 2 * Precision * Recall / （Precision + Recall）
     * 5.AUC 特异度 而 假正率 = 1 - AUC
     * AUC = TN/(FP+TN)
     * 0.5 - 0.7：效果较低，但用于预测股票已经很不错了
     * 0.7 - 0.85：效果一般
     * 0.85 - 0.95：效果很好
     * 0.95 - 1：效果非常好，但一般不太可能
     * <p>
     * 6.Earliness 尽早率 Earliness of ETSC
     * <p>
     * 7.HM 谐波平均律 Harmonic mean of accuracy and earliness
     */

    public static Double getEarliness(List<Integer> stopPoint, int len) {
        /**
         * sum(stopPoint[i]/len)/y
         */
        Double sum = 0D;
        for (int i = 0; i < stopPoint.size(); i++) {
            sum += Double.parseDouble(stopPoint.get(i) + "") / len;
        }
        if (stopPoint.size() == 0) {
            return 0D;
        }
        return sum / stopPoint.size();
    }

    public static Double getHM(Double earliness, Double accuracy) {
        return 2 * (1 - earliness) * accuracy / (1 - earliness + accuracy);
    }

    public static Double getMaxHM(Double[] HMs) {
        Double maxHM = 0D;
        if (HMs.length > 0) {
            for (int i = 0; i < HMs.length; i++) {
                if (HMs[i] > maxHM) {
                    maxHM = HMs[i];
                }
            }
        }
        return maxHM;
    }

    /**
     * @param dataResArray PredictVal_ActualVal(1/0)
     * @return TP_FP_FN_TN
     * TP true positive
     * FP false positive
     */
    public static String getTFPN(List<String> dataResArray) {
        int TP = 0, FP = 0, FN = 0, TN = 0;
        for (int i = 0; i < dataResArray.size(); i++) {
            String predictVal_actualVal = dataResArray.get(i);
            String predictVal = predictVal_actualVal.split("_")[0];
            String actualVal = predictVal_actualVal.split("_")[1];
            /*
             * TP：预测为1，预测正确，即实际1
             * FP：预测为1，预测错误，即实际0
             * FN：预测为0，预测错误，即实际1
             * TN：预测为0，预测正确，即实际0
             */
            if (("1").equals(predictVal) && ("1").equals(actualVal)) {
                TP++;
            } else if (("1").equals(predictVal) && ("0").equals(actualVal)) {
                FP++;
            } else if (("0").equals(predictVal) && ("1").equals(actualVal)) {
                FN++;
            } else if (("0").equals(predictVal) && ("0").equals(actualVal)) {
                TN++;
            }
        }
        return TP + "_" + FP + "_" + FN + "_" + TN;
    }

    /**
     * @param dataResArray PredictVal_ActualVal(1/0)
     * @return Accuracy
     */
    public static Double getAccuracy(List<String> dataResArray) {
        String TFPN = getTFPN(dataResArray);
        String[] TFPNs = TFPN.split("_");
        Double TP = Double.parseDouble(TFPNs[0]);
        Double FP = Double.parseDouble(TFPNs[1]);
        Double FN = Double.parseDouble(TFPNs[2]);
        Double TN = Double.parseDouble(TFPNs[3]);
        return (TP + TN) / (TP + TN + FP + FN);
    }

    /**
     * @param dataResArray PredictVal_ActualVal(1/0)
     * @return Precision
     */
    public static Double getPrecision(List<String> dataResArray) {
        String TFPN = getTFPN(dataResArray);
        String[] TFPNs = TFPN.split("_");
        Double TP = Double.parseDouble(TFPNs[0]);
        Double FP = Double.parseDouble(TFPNs[1]);
        return TP / (TP + FP);
    }

    /**
     * @param dataResArray PredictVal_ActualVal(1/0)
     * @return Accuracy
     */
    public static Double getRecall(List<String> dataResArray) {
        String TFPN = getTFPN(dataResArray);
        String[] TFPNs = TFPN.split("_");
        Double TP = Double.parseDouble(TFPNs[0]);
        Double FN = Double.parseDouble(TFPNs[2]);
        return TP / (TP + FN);
    }

    /**
     * @param dataResArray PredictVal_ActualVal(1/0)
     * @return F1Score
     */
    public static Double getF1Score(List<String> dataResArray) {
        Double precision = getPrecision(dataResArray);
        Double recall = getRecall(dataResArray);
        return 2 * precision * recall / (precision + recall);
    }

    /**
     * @param dataResArray PredictVal_ActualVal(1/0)
     * @return AUC
     */
    public static Double getAUC(List<String> dataResArray) {
        String TFPN = getTFPN(dataResArray);
        String[] TFPNs = TFPN.split("_");
        Double FP = Double.parseDouble(TFPNs[1]);
        Double TN = Double.parseDouble(TFPNs[3]);
        return TN / (FP + TN);
    }

    public static void printAllStatistic(List<String> dataResArray) {
        // 获取指标评估
        final String tfpn = getTFPN(dataResArray);
        final Double accuracy = getAccuracy(dataResArray);
        final Double precision = getPrecision(dataResArray);
        final Double recall = getRecall(dataResArray);
        final Double f1Score = getF1Score(dataResArray);
        final Double auc = getAUC(dataResArray);
        System.out.println("TFPN:" + tfpn);
        System.out.println("Accuracy:" + accuracy + ",Precision:" + precision + ",Recall:" + recall + ",F1Score:" + f1Score + ",AUC:" + auc);
    }

    public static String genJson(String pathname, String mode) throws Exception {
        File file = new File(pathname);
        String jsonStr = "";
        if (file.exists()) {
            List<Instruction> sList = null;
            Map m = null;
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> lm = new ArrayList<>();
            List<Map> reList = new ArrayList();
            Map reMap = new HashMap();
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(
                        pathname));
                String line = reader.readLine();
                while (line != null) {
                    String[] split = line.split(",");
                    sList = new ArrayList<>();
                    m = new HashMap();
                    for (int i = 0; i < split.length; i++) {
                        if (split[i].split("_").length > 1) {
                            String className = split[i].split("_")[0];
                            String timeSeriesId_percent = split[i].split("_")[1];
                            String timeSeriesId = timeSeriesId_percent.split(":")[0];
                            String percent = timeSeriesId_percent.split(":")[1];
//                    System.out.println(className + " " + Integer.parseInt(timeSeriesId) + " " + percent);
                            Instruction s = new Instruction(className, Integer.parseInt(timeSeriesId), Double.parseDouble(percent));
                            sList.add(s);
                        }
                    }
                    m.put("sList", sList);
                    m.put("timeStamp", ToolsModel.getTime());
                    reList.add(m);
                    line = reader.readLine();
                }
                reMap.put("data", reList);

                // choose max
                if (mode.equals("max")) {
                    List reListMax = null;
                    List reList0 = new ArrayList();
                    for (int i = 0; i < reList.size(); i++) {
                        reListMax = new ArrayList();
                        Map m0 = reList.get(i);
                        List sList0 = (List) m0.get("sList");
                        Double percentageMax = 0D;
                        Map<Integer, Tuple2> mapMax = new HashMap<>();
                        for (int j = 0; j < sList0.size(); j++) {
                            Instruction s0 = (Instruction) sList0.get(j);
                            percentageMax = 0D;
                            Double percentage = s0.getPercentage();
                            if (mapMax.get(s0.getTimeSeriesId()) == null) {
                                mapMax.put(s0.getTimeSeriesId(), new Tuple2(s0.getLabel(), s0.getPercentage()));
                            } else {
                                percentageMax = Double.parseDouble(mapMax.get(s0.getTimeSeriesId()).getSecond().toString());
                                if (percentage > percentageMax) {
                                    mapMax.put(s0.getTimeSeriesId(), new Tuple2(s0.getLabel(), percentage));
                                }
                            }
                        }

                        Iterator<Integer> iterator = mapMax.keySet().iterator();
                        while (iterator.hasNext()) {
                            int timeSeriesId = iterator.next();
                            Tuple2 tuple2 = mapMax.get(timeSeriesId);
                            m = new HashMap();
                            m.put("timeSeriesId", timeSeriesId);
                            m.put("label", tuple2.getFirst());
                            m.put("percentage", tuple2.getSecond());
                            reListMax.add(m);
                        }
                        m = new HashMap();
                        m.put("sList", reListMax);
                        m.put("timeStamp", ToolsModel.getTime());
                        reList0.add(m);
                    }
                    reMap.put("data", reList0);
//                System.out.println(reListMax);
                }

                jsonStr = mapper.writeValueAsString(reMap);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return jsonStr.toString();
    }


    public static void main(String[] args) throws Exception {
        exp("ouput", "stateJson.json", genJson("input/stateSet.txt", ""), true);
        exp("ouput", "stateJsonMax.json", genJson("input/stateSet.txt", "max"), true);
    }
}
