package instructor;

import utils.ToolsDataset;
import utils.ToolsModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 
 * Full Compression Merge（FCM）is a full compression storage model to manage instructions and reduce the capacity of storage in memory.
 * FCM 是一种全压缩存储模型，由缓存实例内存（Cache）和关键实例内存（Memory）组成。 FCM 分为自由模式（FM）和聚合模式（AM），具有可伸缩性。
 * 整个过程，Cache 中的 storagePercentage (SPT) 和 Memory 中的 compressionPercentage (CPT) 两个指标至关重要。
 */
public class FCM {
    // Free Model
    public static final String FM = "FM";
    // ElasticModel.Annotation Model
    public static final String AM = "AM";
    // Storage Size
    public static final Double STORAGE_SIZE = 2000D;
    public static final Double MEMORY_RADIO = 0.5;
    public static final Double CIM_SIZE = STORAGE_SIZE * MEMORY_RADIO;
    public static final Double KIM_SIZE = STORAGE_SIZE * (1 - MEMORY_RADIO);
    // Storage Percentage Threshold 0.1 0.05 0.025
    public static Double STORAGE_PERCENTAGE_THRESHOLD = 0.025;
    // Compression Percentage Threshold 0.1 0.05 0.025
    public static Double COMPRESSION_PERCENTAGE_THRESHOLD = 0.1;
    public static Double storagePercentage, compressionPercentage;
    public static List<String> Memory = new ArrayList<String>();
    public static List<String> Cache = new ArrayList<String>();
    public static List<String> CIMTemp = new ArrayList<String>();
    public static StringBuffer stateData = new StringBuffer();
    public static String state = FM;
    public static Double weightup = 2.0;
    public static Double weightdown = 1.0;
    public static String instruction_temp = "";

    /**
     * 当压缩持续较多时，存在从state到CIM到KIM的管道一直打开，ElasticModel.KCM 会长期处于自由模式。
     * 当相似合并持续较少时，说明有可能标注偏移量比较少，或者达到压缩瓶颈，需要增加内存分配。
     */

    public static void run(String filePath) throws IOException {
        StringBuilder s = new StringBuilder();
        /**
         * 在自由模式 FM 和 聚合模式 AM 下，时间窗口中的数据流被标注并存储在 Cache 中。
         */
        System.out.print(ToolsModel.getTime() + " ");
        if (FM.equals(state)) {
            ToolsModel.printlnColor(ToolsModel.GREEN, state);
        } else {
            ToolsModel.printlnColor(ToolsModel.RED, state);
        }

        ToolsModel.printColor(ToolsModel.GREEN, "Instructions");
        storagePercentage = getStoragePercentage();
        String instructions_str = stateData.toString().replaceAll(" ", "");
        String[] instructions = instructions_str.split(",");
        System.out.print(": " + Arrays.toString(instructions).replace(" ", "") + ", ");
        String instruction_last = "[]";
        if (!"".equals(instruction_temp)) {
            String[] instruction_temps = instruction_temp.split(",");
            System.out.println("Last instructions: " + Arrays.toString(instruction_temps).replace(" ", ""));
            instruction_last = instruction_temps[instruction_temps.length - 1];
        }
        String instruction_cur = stateData.toString().replaceAll(" ", "").split(",")[0];
        System.out.print("Current instruction: " + instruction_cur + ", last instruction: " + instruction_last + ", ");
        Instruction instruction = new Instruction();
        instruction.setInstructionStr(instruction_cur);
        if (instruction_last.split("_")[0].equals(instruction_cur.split("_")[0])) {
            ToolsModel.printColor(ToolsModel.RED, "WeightUp");
            System.out.print(": " + weightup + " -> ");
            instruction.setPercentage(instruction.getPercentage() * weightup);
        } else {
            ToolsModel.printColor(ToolsModel.GREEN, "WeightDown");
            System.out.print(": " + weightdown + " -> ");
            instruction.setPercentage(instruction.getPercentage() * weightdown);
        }
        System.out.println(instruction);
//                System.out.println(stateData);
        stateData.replace(0, instruction.toString().length() - 1, instruction.toString());
//                System.out.println(stateData);

        instruction_temp = stateData.toString();
        ToolsModel.printColor(ToolsModel.GREEN, "Cache");
        System.out.println(": " + Cache.toString().replaceAll(" ", ""));
        s.append("Cache: " + Cache.toString().replaceAll(" ", "") + "\n");

        ToolsModel.printColor(ToolsModel.RED, "Memory");
        System.out.println(": " + Memory.toString().replaceAll(" ", ""));
        s.append("Memory: " + Memory.toString().replaceAll(" ", "") + "\n");

        if (state.equals(FM)) {
            /**
             * 由于 Cache 的容量是有限的，在自由模式 FM 下，随着 Cache 中数据的积累超过存储比阈值 STORAGE_PERCENTAGE_THRESHOLD，ElasticModel.KCM 进入聚合模式 AM。
             */
            compressionPercentage = 0D;
            storagePercentage = Cache.size() / CIM_SIZE;

            if (storagePercentage > STORAGE_PERCENTAGE_THRESHOLD) {
                state = AM;
            }
        } else if (state.equals(AM) && Cache.size() > 0) {
            /**
             * 在聚合模式 AM 下，进行注意力机制
             * query 输入之后的个数
             * key 输入之前的对齐个数（新增的用0补齐）
             * value 输入之前的对齐权重（新增的用0补齐）
             */
            ToolsModel.printColor(ToolsModel.GREEN, "CIMTemp");
            System.out.println(": " + CIMTemp.toString().replaceAll(" ", ""));
            s.append("CIMTemp: " + CIMTemp.toString().replaceAll(" ", "") + "\n");
            CIMTemp.addAll(Cache);
            Map<String, Double> queryMap = getNum(Cache);
            Map<String, Double> keyMap = getNum(CIMTemp);
//            sortMapByValue(queryMap);
//            sortMapByValue(keyMap);
            System.out.println("queryMap -> " + queryMap);
            System.out.println("keyMap -> " + keyMap);
//            getAlign(queryMap, keyMap);
//            System.out.println("getAlign -> queryMap -> " + queryMap + " keyMap -> " + keyMap);
            Map<String, Double> valueMap = getWeight(CIMTemp);
//            sortMapByValue(valueMap);
            System.out.println("valueMap -> " + valueMap);

            // 使用Apache Commons Collections的toArray方法
            // 将values转换为Object数组，然后进行类型转换
            Double[] q = (Double[]) queryMap.values().toArray(new Double[0]);
            Double[] k = (Double[]) keyMap.values().toArray(new Double[0]);
            Double[] v = (Double[]) valueMap.values().toArray(new Double[0]);
            Arrays.sort(q);
            Arrays.sort(k);
            Arrays.sort(v);
            double k_median = getSortMedian(k);
            double v_median = getSortMedian(v);
            Double[] newArray_k, newArray_v;
            int newSize = q.length * q.length;
            // 创建新数组
            newArray_k = new Double[newSize];
            newArray_v = new Double[newSize];
            if (q.length * q.length > k.length) {
                ToolsModel.printColor(ToolsModel.GREEN, "> k.length ");
                // 复制原始数组到新数组
                System.arraycopy(k, 0, newArray_k, 0, k.length);
                System.arraycopy(v, 0, newArray_v, 0, v.length);
                // 在新数组末尾添加三个0
                for (int i = k.length; i < newSize; i++) {
                    newArray_k[i] = k_median;
                    newArray_v[i] = v_median;
                }
                Arrays.sort(newArray_k);
                Arrays.sort(newArray_v);
            } else if (q.length * q.length < k.length) {
                ToolsModel.printColor(ToolsModel.RED, "< k.length ");
                int m = k.length; // 原始数组长度
                // 计算要复制的开始部分和末尾部分的长度
                int startLength = (m - (m - newSize)) / 2; // 开始部分长度
                int endLength = m - startLength - (m - newSize); // 末尾部分长度
                // 复制开始部分
                System.arraycopy(k, 0, newArray_k, 0, startLength);
                System.arraycopy(v, 0, newArray_v, 0, startLength);
                // 复制末尾部分
                System.arraycopy(k, m - endLength, newArray_k, startLength, endLength);
                System.arraycopy(v, m - endLength, newArray_v, startLength, endLength);
            } // else ==

//            Double[] z = CAE.continousAttention(q, k, v);
            // 计算注意力权重并生成输出
            double[] z = CAE.calculateAttention(q, getTwoDArrayFromOneD(newArray_k), getTwoDArrayFromOneD(newArray_v));
            System.out.println("query -> " + Arrays.toString(q));
            System.out.println("key -> " + Arrays.deepToString(getTwoDArrayFromOneD(newArray_k)));
            System.out.println("value -> " + Arrays.deepToString(getTwoDArrayFromOneD(newArray_v)));
            System.out.println("result -> " + Arrays.toString(z));

            /**
             * 在聚合模式 AM 下，将 Cache 中标注数据拷贝到 Memory，Cache 中的已拷贝数据也将被清除。同时，Memory 会进行压缩。
             * 优化：将 Memory 中压缩部分 放到 Cache 中再合并
             */
            Double bCompressionTotal = Double.parseDouble(Memory.size() + Cache.size() + "");
            for (int i = 0; i < Cache.size(); i++) {
                Memory.add(Cache.get(i));
            }
            Cache.clear();
            String[] merge = mergeStateData().split(",");
            for (int i = 0; i < merge.length; i++) {
                Memory.add(merge[i]);
            }
            Double lenKIM = Double.parseDouble(Memory.size() + "");
            if (lenKIM <= bCompressionTotal) {
                compressionPercentage = (bCompressionTotal - lenKIM) / bCompressionTotal;
            } else {
                compressionPercentage = 0D;
            }
//            System.out.println("Cache transfer to Memory success!");
//            s.append("Cache transfer to Memory success!" + "\n");
            /**
             * 在聚合模式下，随着 Memory 压缩比小于等于压缩比阈值 COMPRESSION_PERCENTAGE_THRESHOLD，ElasticModel.KCM 将再次进入自由模式。
             * 与此同时，Cache 到 Memory 的传输过程也会停止。
             */
//            if (compressionPercentage <= COMPRESSION_PERCENTAGE_THRESHOLD) {
//                state = FM;
//            }
            state = FM;
        }
        if (stateData.toString().contains(",")) {
            String[] split = stateData.toString().split(",");
            for (int i = 0; i < split.length; i++) {
                Cache.add(split[i]);
            }
        } else {
            Cache.add(stateData.toString());
        }
//        System.out.println("Storage Percentage:" + storagePercentage);
//        s.append("Storage Percentage:" + storagePercentage + "\n");
        // 四舍五入到小数点后三位
        s.append("Compression Percentage:" + new BigDecimal(compressionPercentage).setScale(3, RoundingMode.HALF_UP) + "\n");
        System.out.print("Compression Percentage:" + new BigDecimal(compressionPercentage).setScale(3, RoundingMode.HALF_UP) + ", ");
        s.append("Memory Percentage:" + new BigDecimal(Memory.size() / KIM_SIZE).setScale(3, RoundingMode.HALF_UP) + "\n");
        System.out.print("Memory Percentage:" + new BigDecimal(Memory.size() / KIM_SIZE).setScale(3, RoundingMode.HALF_UP) + ", CPT:" + COMPRESSION_PERCENTAGE_THRESHOLD + ", ");
        s.append("Cache Percentage:" + new BigDecimal(Cache.size() / CIM_SIZE).setScale(3, RoundingMode.HALF_UP) + "\n");
        System.out.print("Cache Percentage:" + new BigDecimal(Cache.size() / CIM_SIZE).setScale(3, RoundingMode.HALF_UP) + ", SPT:" + STORAGE_PERCENTAGE_THRESHOLD + ", ");
        s.append("Total Percentage:" + BigDecimal.valueOf(getStoragePercentage()).setScale(3, RoundingMode.HALF_UP) + "\n");
        System.out.println("Total Percentage:" + BigDecimal.valueOf(getStoragePercentage()).setScale(3, RoundingMode.HALF_UP));
        System.out.println();
        s.append("\n");
        ToolsDataset.appendToFile(filePath, s.toString());
    }

    public static void writeFile(String filePath, String datasetName) throws IOException {
        /**
         * 将CIM中剩余数据转入KIM
         */
//        state = AM;
//        run(filePath);
        /**
         * 写入文件
         */
        if (Memory.size() > 0) {
//            FileWriter writer = new FileWriter("input/stateSet_" + datasetName + ".txt", true);
            String inputTxt = "";
            inputTxt = Memory.toString();
            inputTxt = inputTxt.substring(1, inputTxt.length() - 1) + "\n";
//            writer.write(inputTxt);
//            writer.close();
        }
    }

    public static int getKIMSize() {
        return Memory.size();
    }

    public static int getCIMSize() {
        return Cache.size();
    }

    public static Double getStoragePercentage() {
        return Double.parseDouble(Cache.size() / CIM_SIZE + Memory.size() / KIM_SIZE + "");
    }

    public static Double getCompressionPercentage() {
        return null;
    }

    public static List<String> compressKeyList(List<String> keyList) {
        return keyList;
    }

    public static void addCacheList(String annotation) {
        Cache.add(annotation);
    }

    public static void addKeyList(String annotation) {
        Memory.add(annotation);
    }

    /**
     * label_timeSeriesId:percentage
     * add CAE
     *
     * @param instructions
     */
    public static String getInstructions(List<Instruction> instructions) {
        Map<String, Integer> label_timeSeriesIdMap = new HashMap<String, Integer>();
        Map<String, Integer> timeSeriesIdMap = new HashMap<String, Integer>();
        Set<String> labelSet = new HashSet<String>();
        for (Instruction instruction : instructions) {
            String instruction_str = instruction.toString();
            String label_timeSeriesId = instruction_str.split(":")[0];
            if (!label_timeSeriesIdMap.containsKey(label_timeSeriesId)) {
                label_timeSeriesIdMap.put(label_timeSeriesId, 1);
            } else {
                label_timeSeriesIdMap.put(label_timeSeriesId, label_timeSeriesIdMap.get(label_timeSeriesId) + 1);
            }
            String timeSeriesId = label_timeSeriesId.split("_")[1];
            if (!timeSeriesIdMap.containsKey(timeSeriesId)) {
                timeSeriesIdMap.put(timeSeriesId, 1);
            } else {
                timeSeriesIdMap.put(timeSeriesId, timeSeriesIdMap.get(timeSeriesId) + 1);
            }
            String label = label_timeSeriesId.split("_")[0];
            labelSet.add(label);
        }
//        System.out.println(label_timeSeriesIdMap);
//        System.out.println(timeSeriesIdMap);
//        System.out.println(labelSet);
        StringBuffer buff = new StringBuffer();
        for (String element : labelSet) {
            for (String label_timeSeriesId : label_timeSeriesIdMap.keySet()) {
                String label = label_timeSeriesId.split("_")[0];
                String timeSeriesId = label_timeSeriesId.split("_")[1];
                if (label.equals(element)) {
                    Double percentage = Double.parseDouble(label_timeSeriesIdMap.get(label_timeSeriesId) / timeSeriesIdMap.get(timeSeriesId) + "");
//                    System.out.print(element+ "_" + timeSeriesId + ":" + percentage+",");
                    if (percentage > 0) {
                        buff.append(element + "_" + timeSeriesId + ":" + percentage + ",");
                    }
                }
            }
//            System.out.println();
        }
//        System.out.println(getTime() + " " + buff.toString());
        if (buff.length() > 0) {
            buff.deleteCharAt(buff.length() - 1);
        }
        return buff.toString().replaceAll(" ", "");
    }

    public static void appendStateData(String newStateData) {
        if (stateData.length() > 0) {
            stateData.append("," + newStateData);
        } else {
            stateData.append(newStateData);
        }
    }

    public static void clearStateData() {
        stateData.setLength(0);
    }

    public static void clear() {
        Cache.clear();
        Memory.clear();
    }

    public static String mergeStateData() {
//        System.out.println("mergeStateData start");

//        System.out.println(Cache.toString());

        Set<String> timeSeriesIdSet = new HashSet<String>();

        StringBuffer buff = new StringBuffer();
        Map<String, Double> label_timeSeriesIdMap = new HashMap<String, Double>();
        for (String label_timeSeriesId_percentageList : Memory) {

            for (String label_timeSeriesId_percentage : label_timeSeriesId_percentageList.split(",")) {
                if (label_timeSeriesId_percentage.split(":").length > 1) {
                    String label_timeSeriesId = label_timeSeriesId_percentage.split(":")[0];
                    String percentage = label_timeSeriesId_percentage.split(":")[1];
                    timeSeriesIdSet.add(label_timeSeriesId.split("_")[1]);
                    if (!label_timeSeriesIdMap.containsKey(label_timeSeriesId)) {
                        label_timeSeriesIdMap.put(label_timeSeriesId, Double.parseDouble(percentage));
                    } else {
                        label_timeSeriesIdMap.put(label_timeSeriesId, (label_timeSeriesIdMap.get(label_timeSeriesId) + Double.parseDouble(percentage)) / 2);
                    }
                }
            }
        }
        Memory.clear();
        /**
         * 优化：重占比，同一个 timeSeriesId label 占比总和为 1
         */
        Map<String, Double> timeSeriesIdMap = new HashMap<>();
        for (String timeSeriesId : timeSeriesIdSet) {
            timeSeriesIdMap.put(timeSeriesId, 0.0);
        }
        for (String label_timeSeriesId : label_timeSeriesIdMap.keySet()) {
            String timeSeriesId = label_timeSeriesId.split("_")[1];
            timeSeriesIdMap.put(timeSeriesId, timeSeriesIdMap.get(timeSeriesId) + label_timeSeriesIdMap.get(label_timeSeriesId));
        }

        for (String label_timeSeriesId : label_timeSeriesIdMap.keySet()) {
            String timeSeriesId = label_timeSeriesId.split("_")[1];
            // 四舍五入到小数点后三位
            buff.append(label_timeSeriesId).append(":").append(BigDecimal.valueOf(label_timeSeriesIdMap.get(label_timeSeriesId) / timeSeriesIdMap.get(timeSeriesId)).setScale(3, RoundingMode.HALF_UP)).append(",");
        }

        if (buff.length() > 0) {
            buff.deleteCharAt(buff.length() - 1);
        }
//        System.out.println(labelSet);
//        System.out.println("mergeStateData end");
        return buff.toString();
    }

    public static String keyListToString() {
        return Memory.toString();
    }

    public static String cacheListToString() {
        return Cache.toString();
    }

    public static void runWithTimes(String filePath, String datasetName, int times) throws IOException {
        while (times > 0) {
            List<Instruction> testList = new ArrayList<Instruction>();
            // 模拟随机
            testList.add(new Instruction("class1" + times, times + 4, 0D));
            appendStateData(getInstructions(testList));
            run(filePath);
            clear();
            times--;
        }
    }

    public static Map<String, Double> getNum(List<String> list) {
        Map<String, Double> labelMap = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            Instruction instru = new Instruction();
            instru.setInstructionStr(list.get(i));
            String label = instru.getLabel();
            if (labelMap.containsKey(label)) {
                labelMap.put(label, labelMap.get(label) + 1.0);
            } else {
                labelMap.put(label, 1.0);
            }
        }
        return labelMap;
    }


    public static Map<String, Double> getWeight(List<String> list) {
        Map<String, Double> labelMap = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            Instruction instru = new Instruction();
            instru.setInstructionStr(list.get(i));
            String label = instru.getLabel();
            Double percentage = instru.getPercentage();
            if (labelMap.containsKey(label)) {
                labelMap.put(label, BigDecimal.valueOf(labelMap.get(label) + percentage).setScale(3, RoundingMode.HALF_UP).doubleValue());
            } else {
                labelMap.put(label, percentage);
            }
        }
        return labelMap;
    }

    public static void getAlign(Map<String, Double> map1, Map<String, Double> map2) {
        // 获取map1的键集合
        Set<String> keys1 = map1.keySet();
        // 获取map2的键集合
        Set<String> keys2 = map2.keySet();

        // 找出map1中独有的键
        keys1.stream()
                .filter(key -> !keys2.contains(key))
                .forEach(key -> map2.put(key, 0.0));

        // 找出map2中独有的键
        keys2.stream()
                .filter(key -> !keys1.contains(key))
                .forEach(key -> map1.put(key, 0.0));
    }

    public static void sortMapByValue(Map<String, Double> map) {
        // 使用LinkedHashMap来保持排序
        Map<String, Double> sortedMap = map.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing, // 这个合并函数实际上不会被调用
                        LinkedHashMap::new // 使用LinkedHashMap来保持元素的插入顺序
                ));

        // 打印排序后的Map
        sortedMap.forEach((key, value) -> System.out.println(key + " : " + value));
    }

    public static double getSortMedian(Double[] array) {
        // 计算中位数
        double median;
        int middle = array.length / 2;
        if (array.length % 2 == 1) {
            // 如果数组长度是奇数，直接取中间的元素
            median = array[middle];
        } else {
            // 如果数组长度是偶数，取中间两个元素的平均值
            median = (array[middle - 1] + array[middle]) / 2.0;
        }
        // 输出中位数
        return median;
    }

    public static Double[][] getTwoDArrayFromOneD(Double[] oneDArray) {
        // 计算行数或列数，这里我们取最接近的平方根
        int size = (int) Math.ceil(Math.sqrt(oneDArray.length));

        // 初始化二维数组
        Double[][] twoDArray = new Double[size][];

        // 填充二维数组
        int index = 0;
        for (int i = 0; i < size; i++) {
            // 创建一个新的一维数组作为二维数组的一行
            twoDArray[i] = new Double[oneDArray.length / size + (oneDArray.length % size > i ? 1 : 0)];
            for (int j = 0; j < twoDArray[i].length; j++) {
                if (index < oneDArray.length) {
                    twoDArray[i][j] = oneDArray[index++];
                } else {
                    twoDArray[i][j] = 0.0; // 如果一维数组元素不足，填充0
                }
            }
        }
        return twoDArray;
    }


}
