package discriminator;

import evaluator.*;
import instructor.Sequence;
import instructor.SequenceDataset;
import utils.*;

import java.text.DecimalFormat;
import java.util.*;

import static utils.ToolsDataset.exp;
import static utils.ToolsDataset.tree_row_col;

public class PSTree {
    final double threshold = 0.9;
    // 树的根节点
    PSTreeNode rootNode;
    // 数据的属性列名称
    String[] featureNames;
    // 这棵树所包含的数据
    ArrayList<String[]> datas;
    int shapeletId;
    double shapletSimilar;

    // 类标号的值类型
    private final String YES = "Yes";
    private final String NO = "No";

    // 所有属性的类型总数,在这里就是data源数据的列数
    private int attrNum;
    private String filePath;
    // 初始源数据，用一个二维字符数组存放模仿表格数据
    private String[][] data;
    // 数据的属性行的名字
    private String[] attrNames;
    // 每个属性的值所有类型
    private HashMap<String, ArrayList<String>> attrValue;

    public static class Builder {
        // Required params
        private final SequenceDataset trainSet;
        private final int sType;

        // Optional params
        private int minLen;
        private int maxLen;
        private int stepSize;
        private int maxTreeDepth;
        private int leafSize;
        public int iterativeTimes;
        public int treeId;
        public String trainsetName;

        public Builder(SequenceDataset trainSet, int sType) {
            this.trainSet = trainSet;
            this.minLen = 1;
            this.maxLen = trainSet.get(0).size();
            this.stepSize = 1;
            this.sType = sType;
            this.maxTreeDepth = Integer.MAX_VALUE;
            this.leafSize = 1;
            this.iterativeTimes = trainSet.getIterativeTimes();
            this.treeId = trainSet.getTreeId();
            this.trainsetName = trainSet.getTrainsetName();
        }

        public Builder minLen(int minLen) {
            this.minLen = minLen;
            return this;
        }

        public Builder maxLen(int maxLen) {
            this.maxLen = maxLen;
            return this;
        }

        public Builder stepSize(int stepSize) {
            this.stepSize = stepSize;
            return this;
        }

        public Builder treeDepth(int maxDepth) {
            this.maxTreeDepth = maxDepth;
            return this;
        }

        public Builder leafeSize(int leafSize) {
            this.leafSize = leafSize;
            return this;
        }

        public PSTree build() throws Exception {
            return new PSTree(this);
        }
    }

    public BaseShapelets shapeletFinder;
    public static int methodType;

    public int nodeID;
    public PSTree parent;
    public PSTree leftNode;
    public PSTree rightNode;
    public int leafSize;

    public Shapelet shapelet;
    public Shapelet lastShapelet;
    public int nodeLabel;
    public int lastLabel;
    public int maxTreeDepth;
    public int currentTreeDepth;

    public HashMap<Integer, Integer> leafClassHistogram;

    public PSTree(ArrayList<String[]> datas) {
        this.datas = datas;
        this.featureNames = datas.get(0);

        attrValue = new HashMap();
        readData(datas);

        // 通过CART工具类进行决策树的构建，并返回树的根节点
        rootNode = startBuildingTree();
    }

    public PSTree() {
        this.parent = null;
        this.leftNode = null;
        this.rightNode = null;
        this.shapelet = null;
        this.nodeLabel = Integer.MIN_VALUE;
    }

    private PSTree(Builder bldr) throws Exception {
        this();
        methodType = bldr.sType;
        this.nodeID = 1;
        this.currentTreeDepth = 0;
        this.maxTreeDepth = bldr.maxTreeDepth;
        this.leafSize = bldr.leafSize;
        this.createSubTree(bldr.trainSet, bldr.minLen, bldr.maxLen, bldr.stepSize);
        this.printTree("", bldr.trainSet);
        tree_row_col("tree", "tree_temp");
    }

    public PSTree(PSTree parent, int nodeID, SequenceDataset trainSet, int minLen, int maxLen, int stepSize) {
        this();
        this.nodeID = nodeID;
        this.shapeletId = trainSet.getDataset().get(trainSet.getDataset().size() / nodeID).getShapletId();
        this.parent = parent;
        this.currentTreeDepth = parent.currentTreeDepth + 1;
        this.maxTreeDepth = parent.maxTreeDepth;
        this.leafSize = parent.leafSize;
        this.createSubTree(trainSet, minLen, maxLen, stepSize);
    }

    public void createSubTree(SequenceDataset trainSet, int minLen, int maxLen, int stepSize) {
        if (this.currentTreeDepth >= this.maxTreeDepth || trainSet.size() <= this.leafSize || trainSet.entropy() <= 0.1) {
            this.nodeLabel = trainSet.getClassHist().entrySet().stream().max((x, y) -> x.getValue() > y.getValue() ? 1 : -1).get().getKey();
            ArrayList<Sequence> dataset = trainSet.getDataset();
            for (int i = 0; i < dataset.size(); i++) {
                Sequence timeSeries = dataset.get(i);
                if (this.nodeLabel == timeSeries.getLabel()) {
                    this.shapeletId = timeSeries.getShapletId();
                    this.shapletSimilar = timeSeries.getShapletSimilar();
                }
            }
            this.leafClassHistogram = trainSet.getClassHist();
        } else {
            switch (methodType) {
                case 1:
                    shapeletFinder = new LegacyShapelets(trainSet, minLen, maxLen, stepSize);
                    break;
                case 2:
                    shapeletFinder = new RandomizedLegacyShapelets(trainSet, minLen, maxLen, stepSize);
                    break;
                case 3:
                    shapeletFinder = new LogicalShapelets(trainSet, minLen, maxLen, stepSize);
                    break;
                case 4:
                    shapeletFinder = new LabeledShapelets(trainSet, minLen, maxLen, stepSize);

            }
            /**
             *（重要）判断上次搜索的形状集，并仅将其与当前形状集比较一次，
             * 如果大于阈值，则直接填充标签，而不经过分类器
             */
            double similarity;
            if (this.shapelet != null) {
                Double[] s1 = this.shapelet.getContent();
                Double[] s2 = lastShapelet.getContent();
                TimeSeries tsI = new TimeSeries(s1);
                TimeSeries tsJ = new TimeSeries(s2);
                TimeWarpInfo timeWarpInfo = FastDTW.fastDTW(tsI, tsJ, 1, new EuclideanDistance());
                similarity = ToolsDataset.distanceToSimilarity(timeWarpInfo.getDistance());
            } else {
                similarity = 0;
            }
            if (similarity > threshold) {
                this.nodeLabel = lastLabel;
            } else {
                this.shapelet = shapeletFinder.findShapelet();  //key
            }

            SequenceDataset[] splitDataset = null;
            if (methodType == 4) {
                splitDataset = shapeletFinder.splitDataset(this.shapelet.getLwSplit());
            } else {
                splitDataset = shapeletFinder.splitDataset(this.shapelet.getHistMap(), this.shapelet.getSplitDist());
            }

            this.leftNode = new PSTree(this, 2 * this.nodeID, splitDataset[0], minLen, maxLen, stepSize);
            this.rightNode = new PSTree(this, 2 * this.nodeID + 1, splitDataset[1], minLen, maxLen, stepSize);
        }
    }

    public int checkInstance(Sequence testInst) {
        if (this.nodeLabel != Integer.MIN_VALUE) {
            return this.nodeLabel;
        } else {
            if (methodType == 4) {
                ShapeletPS Lw = (ShapeletPS) shapelet;
                LabeledShapelets tmp = (LabeledShapelets) shapeletFinder;
                if (tmp.getBranch(testInst, Lw) == 0) {
                    return this.leftNode.checkInstance(testInst);
                } else {
                    return this.rightNode.checkInstance(testInst);
                }
            } else {
                Double dist = shapeletFinder.getDist(testInst, this.shapelet.getShapelet());
                if (dist < this.shapelet.getSplitDist()) {
                    return this.leftNode.checkInstance(testInst);
                } else {
                    return this.rightNode.checkInstance(testInst);
                }
            }
        }
    }

    public long getTotalCandidates() {
        return shapeletFinder.getTotalCandidates();
    }

    public long getPrunedCandidates() {
        return shapeletFinder.getPrunedCandidates();
    }

    public void printTree(String spaces, SequenceDataset trainset) throws Exception {
        int iterativeTimes = trainset.getIterativeTimes();
        int treeId = trainset.getTreeId();
        String trainsetName = trainset.getTrainsetName();
        // 按照相似度顺序排序
        DecimalFormat df = new DecimalFormat("#.###"); // 保留三位小数
        Double maxShapletSimilar = 0D;
        int maxshapeletId = 0;
        for (int i = 0; i < trainset.size(); i++) {
            Sequence timeSeries = trainset.get(i);
            Double shapletSimilar = timeSeries.getShapletSimilar();
            if (maxShapletSimilar < shapletSimilar) {
                maxShapletSimilar = shapletSimilar;
                maxshapeletId = timeSeries.getShapletId();
            }
            if (this.shapeletId == timeSeries.getShapletId()) {
                this.shapletSimilar = Double.parseDouble(df.format(timeSeries.getShapletSimilar()));
            }
        }
        if (this.nodeID == 1) {
            this.shapeletId = maxshapeletId;
            this.shapletSimilar = Double.parseDouble(df.format(maxShapletSimilar));
        }

        String s = spaces + " (" + this.nodeID + ") " + this.shapeletId + " [" + this.shapletSimilar + "]";
        if (this.nodeLabel != Integer.MIN_VALUE) {
            s += " Classified as: Class " + this.nodeLabel;
            lastLabel = this.nodeLabel;
            lastShapelet = this.shapelet;
            s += " [" + this.leafClassHistogram + "]";
        }
        System.out.println(s);
        exp("tree_temp", trainsetName + "_" + "tree_" + iterativeTimes + "_" + treeId, this.shapeletId + "_" + this.shapletSimilar + "_" + this.nodeLabel + "\n", true);
        if (this.leftNode != null) {
            this.leftNode.printTree(spaces + "  ", trainset);
        }
        if (this.rightNode != null) {
            this.rightNode.printTree(spaces + "  ", trainset);
        }
    }

    /**
     * 根据指定的数据特征描述进行类别的判断
     *
     * @param features
     */
    public String decideClassType(String features) {
        String classType = "";
        // 查询属性组
        String[] queryFeatures;
        // 在本决策树中对应的查询的属性值描述
        ArrayList<String[]> featureStrs;

        featureStrs = new ArrayList();
        queryFeatures = features.split(",");

        String[] array;
        for (String name : featureNames) {
            for (String featureValue : queryFeatures) {
                array = featureValue.split("=");
                // 将对应的属性值加入到列表中
                if (array[0].equals(name)) {
                    featureStrs.add(array);
                }
            }
        }

        // 开始从根节点往下递归搜索
        classType = recusiveSearchClassType(rootNode, featureStrs);
        return classType;
    }

    /*
     * 递归搜索树，查询属性的分类类别
     *
     * @param node
     *            当前搜索到的节点
     * @param remainFeatures
     *            剩余未判断的属性
     * @return
     */
    private String recusiveSearchClassType(PSTreeNode node, ArrayList<String[]> remainFeatures) {
        String classType = null;

        // 如果节点包含了数据的id索引，说明分类到底了
        if (node.getDataIndex() != null && node.getDataIndex().size() > 0) {
            classType = judgeClassType(node.getDataIndex());

            return classType;
        }

        // 取出剩余属性中的一个匹配属性作为当前的判断属性名称
        String[] currentFeature = null;
        for (String[] featueValue : remainFeatures) {
            if (node.getAttrname().equals(featueValue[0])) {
                currentFeature = featueValue;
                break;
            }
        }

        for (PSTreeNode childNode : node.getChildAttrNode()) {
            // 寻找节点中属于此属性值的分支
            if (currentFeature == null) {
                continue;
            }
            if (childNode.getParentAttrValue().equals(currentFeature[1])) {
                remainFeatures.remove(currentFeature);
                classType = recusiveSearchClassType(childNode, remainFeatures);
                // 如果找到了分类结果，则直接跳出循环
                break;
            } else {
                // 进行第二种情况的判断加上!符号的情况
                String value = childNode.getParentAttrValue();
                if (value != null && value.charAt(0) == '!') {
                    // 去掉第一个"!"字符
                    value = value.substring(1, value.length());


                    if (!value.equals(currentFeature[1])) {
                        remainFeatures.remove(currentFeature);
                        classType = recusiveSearchClassType(childNode, remainFeatures);
                        break;
                    }

                }
            }
        }
        return classType;
    }

    /**
     * 根据得到的数据行分类进行类别的决策
     *
     * @param dataIndex 根据分类的数据索引号
     * @return
     */
    private String judgeClassType(ArrayList<String> dataIndex) {
        // 结果类型值
        String resultClassType = "";
        String classType = "";
        int count = 0;
        int temp = 0;
        Map<String, Integer> type2Num = new HashMap<String, Integer>();

        for (String index : dataIndex) {
            temp = Integer.parseInt(index);
            // 取出最后一列的决策类别数据
            classType = datas.get(temp)[featureNames.length - 1];

            if (type2Num.containsKey(classType)) {
                // 如果类别已经存在，则使计数加1
                count = type2Num.get(classType);
                count++;
            } else {
                count = 1;
            }
            type2Num.put(classType, count);
        }

        // 选出其中类别支持技术最多的一个类别值
        count = -1;
        for (Map.Entry entry : type2Num.entrySet()) {
            int entryValue = Integer.parseInt(entry.getValue().toString());
            if (entryValue > count) {
                count = entryValue;
                resultClassType = (String) entry.getKey();
            }
        }
        return resultClassType;
    }


    public void buildLwDecisionTree(PSTreeNode node, String parentAttrValue, String[][] remainData, ArrayList<String> remainAttr, boolean beLongParentValue) {
        // 属性划分值
        String valueType = "";
        // 划分属性名称
        String spiltAttrName = "";
        Double minGini = Double.parseDouble(Integer.MAX_VALUE + "");
        Double tempGini = 0D;
        // 基尼指数数组，保存了基尼指数和此基尼指数的划分属性值
        String[] giniArray;

        if (beLongParentValue) {
            node.setParentAttrValue(parentAttrValue);
        } else {
            node.setParentAttrValue("!" + parentAttrValue);
        }

        if (remainAttr.size() == 0) {
            if (remainData.length > 1) {
                ArrayList<String> indexArray = new ArrayList();
                for (int i = 1; i < remainData.length; i++) {
                    indexArray.add(remainData[i][0]);
                }
                node.setDataIndex(indexArray);
            }
            return;
        }

        for (String str : remainAttr) {
            giniArray = computeAttrGini(remainData, str);
            tempGini = Double.parseDouble(giniArray[1]);

            if (tempGini < minGini) {
                spiltAttrName = str;
                minGini = tempGini;
                valueType = giniArray[0];
            }
        }
        // 移除划分属性
        remainAttr.remove(spiltAttrName);
        node.setAttrname(spiltAttrName);

        // 孩子节点,分类回归树中，每次二元划分，分出2个孩子节点
        PSTreeNode[] childNode = new PSTreeNode[2];
        String[][] rData;

        boolean[] bArray = new boolean[]{true, false};
        for (int i = 0; i < bArray.length; i++) {
            // 二元划分属于属性值的划分
            rData = removeData(remainData, spiltAttrName, valueType, bArray[i]);

            boolean sameClass = true;
            ArrayList<String> indexArray = new ArrayList();
            for (int k = 1; k < rData.length; k++) {
                indexArray.add(rData[k][0]);
                // 判断是否为同一类的
                if (!rData[k][attrNames.length - 1].equals(rData[1][attrNames.length - 1])) {
                    // 只要有1个不相等，就不是同类型的
                    sameClass = false;
                    break;
                }
            }

            childNode[i] = new PSTreeNode();
            if (!sameClass) {
                // 创建新的对象属性，对象的同个引用会出错
                ArrayList<String> rAttr = new ArrayList();
                for (String str : remainAttr) {
                    rAttr.add(str);
                }
                buildLwDecisionTree(childNode[i], valueType, rData, rAttr, bArray[i]);
            } else {
                String pAtr = (bArray[i] ? valueType : "!" + valueType);
                childNode[i].setParentAttrValue(pAtr);
                childNode[i].setDataIndex(indexArray);
            }
        }

        node.setChildAttrNode(childNode);
    }

    /**
     * 构造分类回归树，并返回根节点
     *
     * @return
     */
    public PSTreeNode startBuildingTree() {
        initAttrValue();

        ArrayList<String> remainAttr = new ArrayList();
        // 添加属性，除了最后一个类标号属性
        for (int i = 1; i < attrNames.length - 1; i++) {
            remainAttr.add(attrNames[i]);
        }

        PSTreeNode rootNode = new PSTreeNode();
        buildLwDecisionTree(rootNode, "", data, remainAttr, false);
        setIndexAndAlpah(rootNode, 0, false);
        showLwDecisionTree(rootNode, 1);

        return rootNode;
    }

    /**
     * 显示决策树
     *
     * @param node     待显示的节点
     * @param blankNum 行空格符，用于显示树型结构
     */
    private void showLwDecisionTree(PSTreeNode node, int blankNum) {
        System.out.println();
//        for (int i = 0; i < blankNum; i++) {
//            System.out.print("    ");
//        }
        System.out.print("--");
        // 显示分类的属性值
        if (node.getParentAttrValue() != null && node.getParentAttrValue().length() > 0) {
            System.out.print(node.getParentAttrValue());
        } else {
            System.out.print("--");
        }
        System.out.print("--");

        if (node.getDataIndex() != null && node.getDataIndex().size() > 0) {
            String i = node.getDataIndex().get(0);
            System.out.print("【" + node.getNodeIndex() + "】类别:" + data[Integer.parseInt(i)][attrNames.length - 1]);
            System.out.print("[");
            for (String index : node.getDataIndex()) {
                System.out.print(index + ", ");
            }
            System.out.print("]");
        } else {
            // 递归显示子节点
            System.out.print("【" + node.getNodeIndex() + ":" + node.getAttrname() + "】");
            if (node.getChildAttrNode() != null) {
                for (PSTreeNode childNode : node.getChildAttrNode()) {
                    showLwDecisionTree(childNode, 2 * blankNum);
                }
            } else {
                System.out.print("【  Child Null】");
            }
        }
    }

    /**
     * 根据随机选取的样本数据进行初始化
     *
     * @param dataArray 已经读入的样本数据
     */
    public void readData(ArrayList<String[]> dataArray) {
        data = new String[dataArray.size()][];
        dataArray.toArray(data);
        attrNum = data[0].length;
        attrNames = data[0];
    }

    /**
     * 首先初始化每种属性的值的所有类型，用于后面的子类熵的计算时用
     */
    public void initAttrValue() {
        ArrayList<String> tempValues;

        // 按照列的方式，从左往右找
        for (int j = 1; j < attrNum; j++) {
            // 从一列中的上往下开始寻找值
            tempValues = new ArrayList();
            for (int i = 1; i < data.length; i++) {
                if (!tempValues.contains(data[i][j])) {
                    // 如果这个属性的值没有添加过，则添加
                    tempValues.add(data[i][j]);
                }
            }

            // 一列属性的值已经遍历完毕，复制到map属性表中
            attrValue.put(data[0][j], tempValues);
        }
    }

    /**
     * 计算机基尼指数
     *
     * @param remainData  剩余数据
     * @param attrName    属性名称
     * @param value       属性值
     * @param beLongValue 分类是否属于此属性值
     * @return
     */
    public Double computeGini(String[][] remainData, String attrName, String value, boolean beLongValue) {
        // 实例总数
        int total = 0;
        // 正实例数
        int posNum = 0;
        // 负实例数
        int negNum = 0;
        // 基尼指数
        Double gini = 0D;

        // 还是按列从左往右遍历属性
        for (int j = 1; j < attrNames.length; j++) {
            // 找到了指定的属性
            if (attrName.equals(attrNames[j])) {
                for (int i = 1; i < remainData.length; i++) {
                    // 统计正负实例按照属于和不属于值类型进行划分
                    if ((beLongValue && remainData[i][j].equals(value)) || (!beLongValue && !remainData[i][j].equals(value))) {
                        if (remainData[i][attrNames.length - 1].equals(YES)) {
                            // 判断此行数据是否为正实例
                            posNum++;
                        } else {
                            negNum++;
                        }
                    }
                }
            }
        }

        total = posNum + negNum;
        Double posProbobly = Double.parseDouble(posNum / total + "");
        Double negProbobly = Double.parseDouble(negNum / total + "");
        gini = 1 - posProbobly * posProbobly - negProbobly * negProbobly;

        // 返回计算基尼指数
        return gini;
    }

    /**
     * 计算属性划分的最小基尼指数，返回最小的属性值划分和最小的基尼指数，保存在一个数组中
     *
     * @param remainData 剩余谁
     * @param attrName   属性名称
     * @return
     */
    public String[] computeAttrGini(String[][] remainData, String attrName) {
        String[] str = new String[2];
        // 最终该属性的划分类型值
        String spiltValue = "";
        // 临时变量
        int tempNum = 0;
        // 保存属性的值划分时的最小的基尼指数
        Double minGiNi = Double.parseDouble(Integer.MAX_VALUE + "");
        ArrayList<String> valueTypes = attrValue.get(attrName);
        // 属于此属性值的实例数
        HashMap<String, Integer> belongNum = new HashMap();

        for (String string : valueTypes) {
            // 重新计数的时候，数字归0
            tempNum = 0;
            // 按列从左往右遍历属性
            for (int i = 1; i < attrNames.length; i++) {
                // 找到了指定的属性
                if (attrName.equals(attrNames[i])) {
                    for (int j = 1; j < remainData.length; j++) {
                        // 统计正负实例按照属于和不属于值类型进行划分
                        if (remainData[j][i].equals(string)) {
                            tempNum++;
                        }
                    }
                }
            }
            belongNum.put(string, tempNum);
        }

        Double tempGini = 0D;
        Double posProbably = 1.0;
        Double negProbably = 1.0;
        for (String string : valueTypes) {
            tempGini = 0D;

            posProbably = 1.0 * belongNum.get(string) / (remainData.length - 1);
            negProbably = 1 - posProbably;

            tempGini += posProbably * computeGini(remainData, attrName, string, true);
            tempGini += negProbably * computeGini(remainData, attrName, string, false);

            if (tempGini < minGiNi) {
                minGiNi = tempGini;
                spiltValue = string;
            }
        }

        str[0] = spiltValue;
        str[1] = minGiNi + "";

        return str;
    }

    /**
     * 属性划分完毕，进行数据的移除
     *
     * @param srcData   源数据
     * @param attrName  划分的属性名称
     * @param valueType 属性的值类型
     * @parame beLongValue
     * 分类是否属于此值类型
     */
    private String[][] removeData(String[][] srcData, String attrName, String valueType, boolean beLongValue) {
        String[][] desDataArray;
        ArrayList<String[]> desData = new ArrayList();
        // 待删除数据
        ArrayList<String[]> selectData = new ArrayList();
        selectData.add(attrNames);

        // 数组数据转化到列表中，方便移除
        for (int i = 0; i < srcData.length; i++) {
            desData.add(srcData[i]);
        }

        // 还是从左往右一列列的查找
        for (int j = 1; j < attrNames.length; j++) {
            if (attrNames[j].equals(attrName)) {
                for (int i = 1; i < desData.size(); i++) {
                    if (desData.get(i)[j].equals(valueType)) {
                        // 如果匹配这个数据，则移除其他的数据
                        selectData.add(desData.get(i));
                    }
                }
            }
        }

        if (beLongValue) {
            desDataArray = new String[selectData.size()][];
            selectData.toArray(desDataArray);
        } else {
            // 属性名称行不移除
            selectData.remove(attrNames);
            // 如果是划分不属于此类型的数据时，进行移除
            desData.removeAll(selectData);
            desDataArray = new String[desData.size()][];
            desData.toArray(desDataArray);
        }

        return desDataArray;
    }

    /**
     * 为节点设置序列号，并计算每个节点的误差率，用于后面剪枝
     *
     * @param node      开始的时候传入的是根节点
     * @param index     开始的索引号，从1开始
     * @param ifCutNode 是否需要剪枝
     */
    private void setIndexAndAlpah(PSTreeNode node, int index, boolean ifCutNode) {
        PSTreeNode tempNode;
        // 最小误差代价节点，即将被剪枝的节点
        PSTreeNode minAlphaNode = null;
        Double minAlpah = Double.parseDouble(Integer.MAX_VALUE + "");
        Queue<PSTreeNode> nodeQueue = new LinkedList<PSTreeNode>();

        nodeQueue.add(node);
        while (nodeQueue.size() > 0) {
            index++;
            // 从队列头部获取首个节点
            tempNode = nodeQueue.poll();
            tempNode.setNodeIndex(index);
            if (tempNode.getChildAttrNode() != null) {
                for (PSTreeNode childNode : tempNode.getChildAttrNode()) {
                    nodeQueue.add(childNode);
                }
                computeAlpha(tempNode);
                if (tempNode.getAlpha() < minAlpah) {
                    minAlphaNode = tempNode;
                    minAlpah = tempNode.getAlpha();
                } else if (tempNode.getAlpha() == minAlpah) {
                    // 如果误差代价值一样，比较包含的叶子节点个数，剪枝有多叶子节点数的节点
                    if (tempNode.getLeafNum() > minAlphaNode.getLeafNum()) {
                        minAlphaNode = tempNode;
                    }
                }
            }
        }

        if (ifCutNode) {
            // 进行树的剪枝，让其左右孩子节点为null
            minAlphaNode.setChildAttrNode(null);
        }
    }

    /**
     * 为非叶子节点计算误差代价，这里的后剪枝法用的是CCP代价复杂度剪枝
     *
     * @param node 待计算的非叶子节点
     */
    private void computeAlpha(PSTreeNode node) {
        Double rt = 0D;
        Double Rt = 0D;
        Double alpha = 0D;
        // 当前节点的数据总数
        int sumNum = 0;
        // 最少的偏差数
        int minNum = 0;

        ArrayList<String> dataIndex;
        ArrayList<PSTreeNode> leafNodes = new ArrayList();

        addLeafNode(node, leafNodes);
        node.setLeafNum(leafNodes.size());
        for (PSTreeNode attrNode : leafNodes) {
            dataIndex = attrNode.getDataIndex();

            int num = 0;
            sumNum += dataIndex.size();
            for (String s : dataIndex) {
                // 统计分类数据中的正负实例数
                if (data[Integer.parseInt(s)][attrNames.length - 1].equals(YES)) {
                    num++;
                }
            }
            minNum += num;

            // 取小数量的值部分
            if (1.0 * num / dataIndex.size() > 0.5) {
                num = dataIndex.size() - num;
            }

            rt += (1.0 * num / (data.length - 1));
        }

        //同样取出少偏差的那部分
        if (1.0 * minNum / sumNum > 0.5) {
            minNum = sumNum - minNum;
        }

        Rt = 1.0 * minNum / (data.length - 1);
        alpha = 1.0 * (Rt - rt) / (leafNodes.size() - 1);
        node.setAlpha(alpha);
    }

    /**
     * 筛选出节点所包含的叶子节点数
     *
     * @param node     待筛选节点
     * @param leafNode 叶子节点列表容器
     */
    private void addLeafNode(PSTreeNode node, ArrayList<PSTreeNode> leafNode) {
        ArrayList<String> dataIndex;

        if (node.getChildAttrNode() != null) {
            for (PSTreeNode childNode : node.getChildAttrNode()) {
                dataIndex = childNode.getDataIndex();
                if (dataIndex != null && dataIndex.size() > 0) {
                    // 说明此节点为叶子节点
                    leafNode.add(childNode);
                } else {
                    // 如果还是非叶子节点则继续递归调用
                    addLeafNode(childNode, leafNode);
                }
            }
        }
    }
}
