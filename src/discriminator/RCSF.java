package discriminator;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 随机森林算法类
 * 用于构建和处理随机森林模型，包括读取数据、构建决策树和随机森林、进行类别决策等。
 */
public class RCSF {
    // 数据文件路径
    private String filePath;
    // 样本数量占总样本量的比例
    private Double sampleNumRatio;
    // 特征数量占总特征量的比例
    private Double featureNumRatio;
    // 采样后的样本数量
    private int sampleNum;
    // 采样后的特征数量
    private int featureNum;
    // 随机森林中决策树的数量
    private int treeNum;
    // 随机数生成器
    private Random random;
    // 特征名数组
    private String[] featureNames;
    // 所有数据集
    private ArrayList<String[]> totalDatas;
    // 随机森林集合
    private ArrayList<PSTree> decisionForest;

    /**
     * RCSF类的构造函数
     *
     * @param filePath        数据文件路径
     * @param sampleNumRatio  样本数量占总样本量的比例
     * @param featureNumRatio 特征数量占总特征量的比例
     * @throws IOException 如果文件读取失败
     */
    public RCSF(String filePath, Double sampleNumRatio, Double featureNumRatio) throws IOException {
        this.filePath = filePath;
        this.sampleNumRatio = sampleNumRatio;
        this.featureNumRatio = featureNumRatio;
        readDataFile();
    }

    /**
     * 从文件中读取数据
     *
     * @throws IOException 如果文件读取失败
     */
    private void readDataFile() throws IOException {
        File file = new File(filePath);
        ArrayList<String[]> dataArray = new ArrayList<String[]>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str = "";
            String[] tempArray;
            while ((str = in.readLine()) != null) {
                tempArray = str.split(" ");
                dataArray.add(tempArray);
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        totalDatas = dataArray;
        featureNames = totalDatas.get(0);
        sampleNum = (int) ((totalDatas.size() - 1) * sampleNumRatio);
        featureNum = (int) ((featureNames.length - 2) * featureNumRatio);
        treeNum = (totalDatas.size() - 1) / sampleNum;
    }

    /**
     * 生成一棵决策树
     *
     * @return 生成的决策树对象
     */
    private PSTree produceLwDecisionTree() {
        int temp = 0;
        PSTree tree;
        String[] tempData;
        // 采样数据的随机行号集合
        ArrayList<Integer> sampleRandomNum;
        // 采样特征的随机列号集合
        ArrayList<Integer> featureRandomNum;
        ArrayList<String[]> datas;

        sampleRandomNum = new ArrayList<Integer>();
        featureRandomNum = new ArrayList<Integer>();
        datas = new ArrayList<String[]>();

        for (int i = 0; i < sampleNum; ) {
            temp = random.nextInt(totalDatas.size());

            // 跳过第一行的特征名
            if (temp == 0) {
                continue;
            }

            if (!sampleRandomNum.contains(temp)) {
                sampleRandomNum.add(temp);
                i++;
            }
        }

        for (int i = 0; i < featureNum; ) {
            temp = random.nextInt(featureNames.length);

            // 跳过第一列的ID和最后一列的类别
            if (temp == 0 || temp == featureNames.length - 1) {
                continue;
            }

            if (!featureRandomNum.contains(temp)) {
                featureRandomNum.add(temp);
                i++;
            }
        }

        String[] singleRecord;
        String[] headCulumn = null;
        // 遍历随机选中的数据行
        for (int dataIndex : sampleRandomNum) {
            singleRecord = totalDatas.get(dataIndex);

            tempData = new String[featureNum + 2];
            headCulumn = new String[featureNum + 2];

            for (int i = 0, k = 1; i < featureRandomNum.size(); i++, k++) {
                temp = featureRandomNum.get(i);

                headCulumn[k] = featureNames[temp];
                tempData[k] = singleRecord[temp];
            }

            // 添加ID列和类别列
            headCulumn[0] = featureNames[0];
            tempData[featureNum + 1] = singleRecord[featureNames.length - 1];
            headCulumn[featureNum + 1] = featureNames[featureNames.length - 1];

            datas.add(tempData);
        }

        // 添加特征名作为数据集的第一行
        datas.add(0, headCulumn);

        // 重新分配数据行的ID
        temp = 0;
        for (String[] array : datas) {
            if (temp > 0) {
                array[0] = temp + "";
            }
            temp++;
        }

        tree = new PSTree(datas);
        return tree;
    }

    /**
     * 构建随机森林
     */
    public void constructRandomTree() {
        PSTree tree;
        random = new Random();
        decisionForest = new ArrayList<PSTree>();

        System.out.println("下面是随机森林中的决策树：");
        for (int i = 0; i < treeNum; i++) {
            System.out.println("\n决策树" + (i + 1));
            tree = produceLwDecisionTree();
            decisionForest.add(tree);
            System.out.println();
        }
        System.out.println();
    }

    /**
     * 根据给定的特征值判断所属的类别
     *
     * @param features 特征值字符串
     * @return 所属的类别
     */
    public String judgeClassType(String features) {
        // 结果类别及其出现次数的映射
        Map<String, Integer> type2Num = new HashMap<String, Integer>();
        String resultClassType = "";
        String classType = "";
        int count = 0;

        for (PSTree tree : decisionForest) {
            classType = tree.decideClassType(features);
            if (type2Num.containsKey(classType)) {
                count = type2Num.get(classType);
                count++;
            } else {
                count = 1;
            }
            type2Num.put(classType, count);
        }

        // 选择出现次数最多的类别作为结果
        count = -1;
        for (Map.Entry<String, Integer> entry : type2Num.entrySet()) {
            if (entry.getValue() > count) {
                count = entry.getValue();
                resultClassType = entry.getKey();
            }
        }

        return resultClassType;
    }
}
