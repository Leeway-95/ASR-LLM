package instructor;

import java.util.*;
import java.util.stream.Collectors;

public class CAE {

    // 计算注意力权重的简化方法
    private static float calculateAttentionWeight(float[] query, float[] key) {
        float sum = 0.0f;
        for (int i = 0; i < query.length; i++) {
            sum += query[i] * key[i]; // 点积
        }
        return (float) Math.exp(sum); // 使用指数函数确保权重为正
    }

    // 应用softmax函数以归一化权重
    private static void applySoftmax(float[] weights) {
        float sum = 0.0f;
        for (float weight : weights) {
            sum += (float) Math.exp(weight);
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (float) Math.exp(weights[i]) / sum;
        }
    }

    // Attention机制的核心方法
    public static List<Float> applyAttention(float[] query, List<float[]> keys, List<float[]> values) {
        List<Float> weightedValues = new ArrayList<>();
        float[] attentionWeights = new float[keys.size()];

        // 计算每个Key相对于Query的注意力权重
        for (int i = 0; i < keys.size(); i++) {
            attentionWeights[i] = calculateAttentionWeight(query, keys.get(i));
        }

        // 应用softmax归一化权重
        applySoftmax(attentionWeights);

        // 使用注意力权重加权并聚合Values
        float sum = 0.0f;
        for (int i = 0; i < values.size(); i++) {
            float weight = attentionWeights[i];
            float[] value = values.get(i);
            for (float v : value) {
                sum += v * weight;
            }
        }

        weightedValues.add(sum); // 这里我们只返回一个聚合值的列表

        return weightedValues;
    }

    // 假设的注意力分数计算函数
    private static float calculateAttentionScore(float[] query, float[] key) {
        // 这里使用简单的点积作为注意力分数的计算方法
        float score = 0.0f;
        for (int i = 0; i < query.length; i++) {
            score += query[i] * key[i];
        }
        return score;
    }

    // 自注意力机制
    public static float[] selfAttention(float[] query, float[][] keys, float[][] values, int attentionHeads) {
        float[] output = new float[keys[0].length];

        // 对每个Attention Head进行操作
        for (int head = 0; head < attentionHeads; head++) {
            // 分离query、key、value以对应不同的头
            float[] q = new float[keys[0].length];
            float[] k = new float[keys[0].length];
            float[] v = new float[values[0].length];

            // 假设我们沿着最后一个维度分割
            for (int i = 0; i < q.length; i++) {
                q[i] = query[i] + head * keys[0].length;
                k[i] = keys[0][i];
                v[i] = values[0][i];
            }

            // 计算注意力分数
            float[] attentionScores = new float[keys.length];
            for (int i = 0; i < keys.length; i++) {
                attentionScores[i] = calculateAttentionScore(q, keys[i]);
            }

            // 应用softmax获取权重
            attentionScores = softmax(attentionScores);

            // 计算加权和
            for (int i = 0; i < v.length; i++) {
                for (int j = 0; j < attentionScores.length; j++) {
                    output[i] += attentionScores[j] * v[i];
                }
            }
        }

        // 假设有多个头，输出需要拼接
        // 这里仅为演示，实际代码需要根据具体实现进行拼接
        return output;
    }

    // Softmax函数
    private static float[] softmax(float[] scores) {
        float sum = 0.0f;
        for (float score : scores) {
            sum += (float) Math.exp(score);
        }
        float[] softmaxScores = new float[scores.length];
        for (int i = 0; i < scores.length; i++) {
            softmaxScores[i] = (float) Math.exp(scores[i]) / sum;
        }
        return softmaxScores;
    }

    public static double[] softmax(double[] z) {
        double sum = 0.0;
        double[] y = new double[z.length];

        // 计算指数和
        for (double aZ : z) {
            sum += Math.exp(aZ);
        }

        // 计算Softmax输出
        for (int i = 0; i < z.length; i++) {
            y[i] = Math.exp(z[i]) / sum;
        }

        return y;
    }

    private static double[] calculateAttentionWeights(Map<String, Integer> labelCounts) {
        // 假设有3个标签，初始化权重数组
        double[] attentionWeights = new double[labelCounts.size()];

        // 计算每个标签的权重
        int index = 0;
        for (String label : labelCounts.keySet()) {
            // 根据连续计数和间断计数的规则计算权重
            // 这里我们简单地使用计数的平方来表示连续计数的权重
            // 间断计数的权重可以设置为一个较低的固定值，或者根据间断的模式来动态计算
            double weight = labelCounts.get(label) * labelCounts.get(label);
            attentionWeights[index++] = weight;
        }

        // 归一化权重
        double sumWeights = Arrays.stream(attentionWeights).sum();
        for (int i = 0; i < attentionWeights.length; i++) {
            attentionWeights[i] /= sumWeights;
        }

        return attentionWeights;
    }

    static Double[] continousAttention(Double[] query, Double[] key, Double[] value) {
        int numKeys = key.length;
        Double[] output = new Double[numKeys];

        // 计算Query和每个Key的点积，然后应用softmax函数来获取权重
        double[] weights = new double[numKeys];
        for (int i = 0; i < numKeys; i++) {
            double sum = 0;
            for (int j = 0; j < query.length; j++) {
                sum += query[j] * key[i];
            }
            weights[i] = Math.exp(sum);
        }
        double sumWeights = Arrays.stream(weights).sum();
        for (int i = 0; i < numKeys; i++) {
            weights[i] /= sumWeights; // softmax归一化
        }

        // 使用权重对Value进行加权求和
        for (int i = 0; i < value.length; i++) {
            output[i] += weights[i] * value[i];
        }

        return output;
    }

    static double[] calculateAttention(Double[] query, Double[][] keys, Double[][] values) {
        int numKeys = keys.length;
        double[] output = new double[numKeys];

        // 计算Query和每个Key的点积，然后应用softmax函数来获取权重
        double[] weights = new double[numKeys];
        for (int i = 0; i < numKeys; i++) {
            double sum = 0;
            for (int j = 0; j < query.length; j++) {
                sum += query[j] * keys[i][j];
            }
            // sum 值多大会引发无穷大
//            weights[i] = Math.exp(sum);
            weights[i] = Math.log10(sum);
        }
        double sumWeights = Arrays.stream(weights).sum();
        if (Double.isInfinite(sumWeights)) {
            sumWeights = 1;
        }
        for (int i = 0; i < numKeys; i++) {
            weights[i] /= sumWeights; // softmax归一化
        }

        // 使用权重对Value进行加权求和
        for (int i = 0; i < numKeys; i++) {
            for (int j = 0; j < values[i].length; j++) {
//                output[j] += BigDecimal.valueOf(weights[i] * values[i][j]).setScale(3, RoundingMode.HALF_UP).doubleValue();
                output[j] += weights[i] * values[i][j];
            }
        }

        return output;
    }

    public static void main(String[] args) {
        // 假设的query, keys, values和attention heads数量
//        float[] query = {0.3f, 0.4f, 0.5f};
//        float[][] keys = {{0.1f, 0.2f, 0.3f}, {0.4f, 0.5f, 0.6f}};
//        float[][] values = {{0.2f, 0.3f, 0.4f}, {0.5f, 0.6f, 0.7f}};
//        int attentionHeads = 2;
        // 运行自注意力机制
//        float[] result = selfAttention(query, keys, values, attentionHeads);
        // 输出结果
//        System.out.println("Self Attention Result: " + Arrays.toString(result));

//         示例数据：Query, Key, Value
//        double[] query1 = {0.2, 0.3, 0.4};
//        double[][] keys1 = {{0.4, 0.5, 0.6}, {0.7, 0.8, 0.9}, {0.1, 0.2, 0.3}};
//        double[][] values1 = {{1.0, 1.1, 1.2}, {1.3, 1.4, 1.5}, {1.6, 1.7, 1.8}};

        Double[] query1 = {1.0, 1.0, 1.0, 1.0, 3.0, 3.0, 3.0, 3.0, 3.0, 4.0, 5.0};
        Double[][] keys1 = {{1.0, 1.0, 2.0, 2.0, 4.0, 5.0, 5.0, 5.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0}, {7.0, 7.0, 7.0, 9.0, 10.0, 11.0, 11.0, 11.0, 21.0, 23.0, 80.0}};
        Double[][] values1 = {{1.0, 1.0, 2.0, 3.0, 5.0, 5.0, 5.0, 5.0, 6.0, 6.0, 6.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0, 7.0}, {7.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 24.0, 24.0, 113.0}};

        // 计算注意力权重并生成输出
        double[] output = calculateAttention(query1, keys1, values1);
        System.out.println("Attention Output: " + Arrays.toString(output));

//        // 连续计数权重表示
//        // 示例标签序列，表示为键值对，键为标签，值为出现次数
//        Map<String, Integer> labelCounts = new HashMap<>();
//        labelCounts.put("A", 3); // 标签A连续出现3次
//        labelCounts.put("B", 1); // 标签B间断出现1次
//        labelCounts.put("C", 2); // 标签C连续出现2次
//
//        // 根据计数密度计算注意力权重
//        double[] attentionWeights = calculateAttentionWeights(labelCounts);
//
//        // 输出注意力权重
//        System.out.println("Attention Weights: " + Arrays.toString(attentionWeights));
//
//        double[] z = {1.0, 2.0, 3.0}; // 输入向量
//        double[] softmaxOutput = softmax(z); // 应用Softmax函数
//
//        System.out.println("Softmax Output: ");
//        for (double value : softmaxOutput) {
//            System.out.printf("%.4f ", value);
//        }
//
        // 示例Query、Keys和Values
//        float[] query = {1.0f, 2.0f, 3.0f};
//        List<float[]> keys = new ArrayList<>();
//        keys.add(new float[]{0.1f, 0.2f, 0.3f});
//        keys.add(new float[]{0.4f, 0.5f, 0.6f});
//        keys.add(new float[]{0.7f, 0.8f, 0.9f});
//        List<float[]> values = new ArrayList<>();
//        values.add(new float[]{1.0f, 1.0f, 1.0f});
//        values.add(new float[]{2.0f, 2.0f, 2.0f});
//        values.add(new float[]{3.0f, 3.0f, 3.0f});
//
//        // 应用Attention机制
//        List<Float> result = applyAttention(query, keys, values);
//
//        // 输出加权聚合后的值
//        System.out.println("Weighted Sum of Values: " + result);

        /**
         * 2024-05-20 01:50:59 FM
         * Cache: [12_29:1.0,15_35:1.0,12_31:1.0,4_29:1.0,2_32:1.0,15_36:1.0,10_37:1.0,2_32:1.0,2_34:1.0,2_37:1.0,4_38:1.0,12_45:1.0,6_40:1.0,6_41:1.0,10_46:1.0,2_45:1.0,4_48:1.0,12_49:1.0,2_48:1.0,2_52:1.0,4_55:1.0,2_54:1.0,2_56:1.0,10_59:1.0,2_61:1.0,2_66:1.0]
         * Memory: [25_32:0.5,21_72:1.0,21_75:1.0,14_18:1.0,11_69:1.0,36_49:1.0,11_23:0.5,15_27:1.0,14_5:0.25,11_114:1.0,14_4:0.5,11_73:1.0,36_98:1.0,11_32:0.5,4_57:1.0,36_12:0.5,12_86:1.0,25_42:1.0,28_34:1.0,26_99:1.0,18_115:1.0,25_5:0.25,10_29:1.0,21_8:0.5,12_11:0.5,17_21:0.5,36_47:1.0,4_25:0.5,21_35:1.0,1_102:1.0,21_37:0.5,21_39:1.0,9_105:1.0,23_71:1.0,26_122:1.0,28_44:0.5,23_76:1.0,2_17:0.5,12_19:1.0,36_1:1.0,23_70:1.0,25_91:1.0,25_92:1.0,4_30:1.0,14_111:1.0,2_11:0.5,2_12:0.5,14_44:0.5,2_13:1.0,9_85:1.0,9_82:1.0,9_87:1.0,25_20:1.0,9_126:1.0,25_62:1.0,23_84:1.0,26_37:0.5,25_66:1.0,2_3:1.0,2_4:0.5,4_2:1.0,12_24:1.0,26_7:1.0,12_25:0.25,2_8:0.5,6_5:0.5,14_53:1.0,12_77:1.0,2_21:0.5,36_67:1.0,2_23:0.5,36_25:0.25,25_17:0.5,36_121:1.0,1_123:1.0,26_28:1.0,36_124:1.0,21_59:1.0,36_61:1.0]
         * Compression Percentage:0.0
         * Memory Percentage:0.079
         * Cache Percentage:0.026
         * Total Percentage:0.105
         *
         * 2024-05-20 01:50:59 AM
         * Cache: []
         * Memory: [25_32:0.25,21_72:1.0,21_75:1.0,14_18:1.0,36_49:0.5,11_23:0.5,10_59:0.5,14_5:0.25,11_114:1.0,14_4:0.5,11_32:0.25,12_86:1.0,25_42:1.0,28_34:0.5,26_99:1.0,18_115:1.0,4_29:0.3333333333333333,25_5:0.25,21_8:0.5,12_11:0.5,17_21:0.5,36_47:1.0,4_25:0.5,1_102:1.0,6_40:1.0,6_41:1.0,23_71:1.0,23_76:1.0,4_38:1.0,2_17:0.5,36_1:1.0,10_37:0.3333333333333333,23_70:1.0,25_91:1.0,25_92:1.0,4_30:1.0,14_111:1.0,2_11:0.5,2_12:0.5,2_13:1.0,25_20:1.0,9_126:1.0,23_84:1.0,2_3:1.0,2_4:0.5,2_8:0.5,10_46:1.0,6_5:0.5,12_77:1.0,2_21:0.5,2_23:0.5,4_48:0.5,36_25:0.25,25_17:0.5,36_121:1.0,36_124:1.0,21_59:0.5,11_69:1.0,2_37:0.3333333333333333,15_27:1.0,11_73:1.0,15_35:0.5,15_36:1.0,4_55:1.0,36_98:1.0,4_57:1.0,2_32:0.5,36_12:0.5,12_45:0.5,2_34:0.5,12_49:0.5,2_48:0.5,10_29:0.3333333333333333,2_45:0.5,21_35:0.5,21_37:0.16666666666666666,21_39:1.0,9_105:1.0,26_122:1.0,28_44:0.5,12_19:1.0,2_52:1.0,2_54:1.0,2_56:1.0,14_44:0.5,9_85:1.0,9_82:1.0,9_87:1.0,25_62:1.0,26_37:0.16666666666666666,25_66:0.5,4_2:1.0,12_24:1.0,26_7:1.0,12_25:0.25,12_29:0.3333333333333333,2_61:0.5,14_53:1.0,2_64:1.0,36_67:1.0,2_66:0.5,12_31:1.0,1_123:1.0,26_28:1.0,36_61:0.5]
         * Compression Percentage:0.009433962264150943
         * Memory Percentage:0.105
         * Cache Percentage:0.0
         * Total Percentage:0.105
         */
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 5);
        map.put("banana", 3);
        map.put("cherry", 7);
        map.put("date", 1);
        map.put("elderberry", 4);

        // 使用LinkedHashMap来保持排序
        Map<String, Integer> sortedMap = map.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing, // 这个合并函数实际上不会被调用
                        LinkedHashMap::new // 使用LinkedHashMap来保持元素的插入顺序
                ));

        // 打印排序后的Map
        sortedMap.forEach((key, value) -> System.out.println(key + " : " + value));


        int[] oneDArray = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        // 计算行数或列数，这里我们取最接近的平方根
        int size = (int) Math.ceil(Math.sqrt(oneDArray.length));

        // 初始化二维数组
        int[][] twoDArray = new int[size][];

        // 填充二维数组
        int index = 0;
        for (int i = 0; i < size; i++) {
            // 创建一个新的一维数组作为二维数组的一行
            twoDArray[i] = new int[oneDArray.length / size + (oneDArray.length % size > i ? 1 : 0)];
            for (int j = 0; j < twoDArray[i].length; j++) {
                if (index < oneDArray.length) {
                    twoDArray[i][j] = oneDArray[index++];
                } else {
                    twoDArray[i][j] = 0; // 如果一维数组元素不足，填充0
                }
            }
        }

        // 打印二维数组
        for (int[] row : twoDArray) {
            for (int value : row) {
                System.out.print(value + " ");
            }
            System.out.println();
        }

        double[] originalArray = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        int n = 3; // 跳过的元素数量

        // 计算跳过n个元素后的数组长度
        int newLength = originalArray.length - n;

        // 创建新数组
        double[] newArray = new double[newLength];

        // 复制原始数组的元素到新数组，跳过中间的n个元素
        System.arraycopy(originalArray, 0, newArray, 0, n); // 复制前n个元素
        System.arraycopy(originalArray, n + n, newArray, n, originalArray.length - n - n);

        // 打印新数组
        for (double value : newArray) {
            System.out.print(value + " ");
        }
    }
}