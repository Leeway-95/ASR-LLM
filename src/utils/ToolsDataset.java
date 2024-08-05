package utils;

import instructor.Sequence;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class ToolsDataset {
    /**
     * j每行列数
     *
     * @param filePath
     * @throws IOException
     */
    private static void calLen(String filePath) throws IOException {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filePath));
            String str = "";
            String[] tempArray;
            int cnt = 1;
            int firstNum = 0;
            System.out.println(filePath);
            while ((str = in.readLine()) != null) {
                ;
                tempArray = str.split(",");
                if (cnt == 1) {
                    firstNum = tempArray.length;
                } else {
                    if (tempArray.length != firstNum) {
                        System.out.println(cnt + " " + tempArray.length);
                    }
                }
                cnt++;
            }
            System.out.println(cnt + " Complete!");
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<Sequence> timeSeriesSimilarity(List<Double[]> timeSeriesData, Double[] targetTimeSeries) {
        List<Sequence> tsList = new ArrayList<>();
        Map<Double[], Double> dists = new HashMap();
        // 使用欧氏距离进行相似度排序
        Collections.sort(timeSeriesData, new Comparator<Double[]>() {
            @Override
            public int compare(Double[] o1, Double[] o2) {
                Double distance1 = calculateEuclideanDistance(o1, targetTimeSeries);
                Double distance2 = calculateEuclideanDistance(o2, targetTimeSeries);
                dists.put(o1, distance1);
                dists.put(o2, distance2);
                return Double.compare(distance1, distance2);
            }
        });
        // 输出排序后的时序数据
        for (Double[] series : timeSeriesData) {
            for (double value : series) {
                System.out.print(value + " ");
            }
            System.out.print(dists.get(series) + " " + distanceToSimilarity(dists.get(series)));

            Sequence ts = new Sequence(series, 0, 0, 0, 0, distanceToSimilarity(dists.get(series)));
            System.out.println();
            tsList.add(ts);
        }
        return tsList;
    }

    public static List<Sequence> timeSeriesSimilarity(List<Sequence> timeSeriesList) {
        List<Double[]> timeSeriesData = new ArrayList<>();
        for (int i = 0; i < timeSeriesList.size(); i++) {
            Sequence timeSeries = timeSeriesList.get(i);
            timeSeriesData.add(timeSeries.getS());
        }
        Double[] targetTimeSeries = timeSeriesData.get(0);

        List<Sequence> tsList = new ArrayList<>();
        Map<Double[], Double> dists = new HashMap();
        Collections.sort(timeSeriesData, new Comparator<Double[]>() {
            @Override
            public int compare(Double[] o1, Double[] o2) {
                Double distance1 = calculateEuclideanDistance(o1, targetTimeSeries);
                Double distance2 = calculateEuclideanDistance(o2, targetTimeSeries);
                dists.put(o1, distance1);
                dists.put(o2, distance2);
                return Double.compare(distance1, distance2);
            }
        });
        int timeSeriesId = 0, timeWindowId = 0, classLabel = 0, shapletId = 0;
        for (Double[] series : timeSeriesData) {
            for (int i = 0; i < timeSeriesList.size(); i++) {
                Sequence timeSeries = timeSeriesList.get(i);
                if (timeSeries.getS().equals(series)) {
                    timeSeriesId = timeSeries.getTimeSeriesId();
                    timeWindowId = timeSeries.getTimeWindowId();
                    classLabel = timeSeries.getLabel();
                    shapletId = timeSeries.getShapletId();
                }
            }
            Sequence ts = new Sequence(series, timeSeriesId, timeWindowId, classLabel, shapletId, distanceToSimilarity(dists.get(series)));
            tsList.add(ts);
        }
        return tsList;
    }

    private static Double calculateEuclideanDistance(Double[] series1, Double[] series2) {
        double sum = 0;
        for (int i = 0; i < series1.length; i++) {
            sum += Math.pow(series1[i] - series2[i], 2);
        }
        return Math.sqrt(sum);
    }

    public static double distanceToSimilarity(double distance) {
        return 1 / (1 + distance);
    }

    public static void txtToCsvConverter(String txtFilePath, String csvFilePath) {

        // 使用try-with-resources自动关闭资源
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {

            String line;
            int lineCount = 0; // 用于跟踪当前行号
            while ((line = reader.readLine()) != null) {
                lineCount++;
                // 如果是第一行，直接写入CSV文件
                if (lineCount % 7 == 1) {
                    writer.write(line);
                } else if (lineCount % 7 == 0) {
                    // 如果是第四行，写入换行符，并开始新的一行
                    writer.write(",");
                    writer.newLine();
                } else {
                    // 如果是其他行，写入逗号分隔符
                    writer.write(",");
                    writer.write(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendToFile(String filePath, String content) {
        File directory = new File(filePath.split("/")[0]);
        if (!directory.exists()) {
            // 如果目录不存在，则创建它
            boolean isCreated = directory.mkdirs(); // mkdir()创建目录，如果父目录不存在也会创建父目录
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            // 使用try-with-resources语句自动关闭资源
            writer.write(content); // 写入内容
        } catch (IOException e) {
            e.printStackTrace(); // 处理异常
        }
    }

    private static void readByCol(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(" ");
                for (String column : columns) {
                    System.out.print(column + " ");
                }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void FileLinesToColumns(String filePath) {

        try {
            List<String> lines = readLinesFromFile(filePath);
            if (lines != null && !lines.isEmpty()) {
                List<List<String>> columns = linesToColumns(lines);
                printColumns(columns);
            } else {
                System.out.println("File is empty or could not be read.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file: " + e.getMessage());
        }
    }

    private static List<String> readLinesFromFile(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static List<List<String>> linesToColumns(List<String> lines) {
        List<List<String>> columns = new ArrayList<>();

        // Assuming that each line has the same number of columns separated by a delimiter (e.g., comma)
        String delimiter = " "; // 修改为你的分隔符
        int numColumns = lines.get(0).split(delimiter).length;

        for (int i = 0; i < numColumns; i++) {
            List<String> column = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split(delimiter);
                column.add(parts[i].trim());
            }
            columns.add(column);
        }

        return columns;
    }

    private static void printColumns(List<List<String>> columns) {
        for (List<String> column : columns) {
            for (String value : column) {
                System.out.print(value + " "); // 使用制表符分隔列
            }
            System.out.println(); // 换行到下一列
        }
    }

    public static void addColumnToCSV(String csvFile, String columnName) {
//        String newColumnData = "new column data";

        try (CSVReader reader = new CSVReader(new FileReader(csvFile)); CSVWriter writer = new CSVWriter(new FileWriter(csvFile + ".tmp"))) {

            List<String[]> lines = reader.readAll();
            List<String[]> newLines = new ArrayList<>();

            // 添加新列的标题
            String[] header = lines.get(0);
            String[] newHeader = new String[header.length + 1];
            System.arraycopy(header, 0, newHeader, 0, header.length);
            newHeader[header.length] = columnName;
            newLines.add(newHeader);

            // 添加新列的数据
            for (int i = 1; i < lines.size(); i++) {
                String[] line = lines.get(i);
                String[] newLine = new String[line.length + 1];
                System.arraycopy(line, 0, newLine, 0, line.length);
//                newLine[line.length] = newColumnData;
                if ("timeWindowId".equals(columnName)) {
                    newLine[line.length] = (i / 5) + 1 + "";
                } else { // timeSeriesId
                    newLine[line.length] = (i / 3) + 1 + "";
                }
                newLines.add(newLine);
            }

            // 写入新的CSV文件
            writer.writeAll(newLines);

            java.nio.file.Files.delete(java.nio.file.Paths.get(csvFile));
            java.nio.file.Files.move(java.nio.file.Paths.get(csvFile + ".tmp"), java.nio.file.Paths.get(csvFile));

            System.out.println("成功添加" + columnName + "列到" + csvFile + "文件中。");

            // 删除原始CSV文件，并将新文件重命名为原始文件名
//            reader.close();
//            writer.close();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
    }

    public static void reverse(int[] arr) {
        int start = 0;
        int end = arr.length - 1;

        while (start < end) {
            int temp = arr[start];
            arr[start] = arr[end];
            arr[end] = temp;

            start++;
            end--;
        }
    }

    public static void readBottomCol(String csvFile, int pos1, int pos2) {
        String line;
        String[] data;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            int cnt = 0;
            int printCnt = 1;
            Map<String, Integer> datasetNameMap_size = new HashMap<>();
            Map<String, Integer> datasetNameMap_minLen = new HashMap<>();
            while ((line = br.readLine()) != null) {
                data = line.split(",");
                if (data.length >= 4 && cnt > 0) {
//                    System.out.println(data[data.length - 4] + " " + data[data.length - 3] + " " + data[data.length - 2] + " " + data[data.length - 1]);
                    String[] StopPoint = data[data.length - pos2].split("_");
                    String[] StopPointLabel = data[data.length - pos1].split("_");
                    Arrays.sort(StopPointLabel, Collections.reverseOrder());
                    Map<String, Integer> StopPointLabelMap = new HashMap<>();
//                    Double[] StopPointLabelRadio = new Double[StopPointLabel.length];
                    for (int i = 0; i < StopPointLabel.length; i++) {
                        if (StopPointLabelMap.containsKey(StopPointLabel[i])) {
                            StopPointLabelMap.put(StopPointLabel[i], Integer.parseInt(StopPointLabelMap.get(StopPointLabel[i]) + "") + 1);
                        } else {
                            StopPointLabelMap.put(StopPointLabel[i], 1);
                        }
//                        StopPointLabelRadio[i] = Double.parseDouble(StopPointLabelMap.get(StopPointLabel[i]) + "") / (i + 1);
                    }
                    // verify
//                    Double sum = 0;
//                    for (int i = 0; i< StopPointLabelRadio.length; i++){
//                        sum += Double.parseDouble(StopPointLabelRadio[i]+"");
//                    }
                    if (StopPoint.length > 9) {
                        System.out.println(printCnt + ".DatasetName:" + data[0]);
                        int maxLabelSize = 0;
                        String maxLabel = "";
                        for (Map.Entry<String, Integer> entry : StopPointLabelMap.entrySet()) {
                            if (maxLabelSize < entry.getValue()) {
                                maxLabelSize = entry.getValue();
                                maxLabel = entry.getKey();
                            }
                        }
                        System.out.println(printCnt + ".Max Radio Label:" + maxLabel);
                        Double[] StopPointLabelRadio = new Double[StopPointLabel.length];
                        int maxLabelTimes = 0;
                        for (int i = 0; i < StopPointLabel.length; i++) {
                            if (StopPointLabel[i].equals(maxLabel)) {
                                maxLabelTimes++;
                            }
                            StopPointLabelRadio[i] = Double.parseDouble(maxLabelTimes + "") / (i + 1);
                        }
                        System.out.println(printCnt + ".Stop Point Length:" + StopPoint.length);
                        System.out.println(printCnt + ".Stop Point:" + Arrays.toString(StopPoint));
                        System.out.println(printCnt + ".Stop Point Label:" + Arrays.toString(StopPointLabel));
                        System.out.println(printCnt + ".Stop Point Label Radio:" + Arrays.toString(StopPointLabelRadio));
                        System.out.println();

                        if (!datasetNameMap_minLen.containsKey(data[0])) {
                            datasetNameMap_minLen.put(data[0], StopPoint.length);
                        } else {
                            if (datasetNameMap_minLen.get(data[0]) > StopPoint.length) {
                                datasetNameMap_minLen.put(data[0], StopPoint.length);
                            }
                        }
                        if (!datasetNameMap_size.containsKey(data[0])) {
                            datasetNameMap_size.put(data[0], 0);
                        } else {
                            datasetNameMap_size.put(data[0], datasetNameMap_size.get(data[0]) + 1);
                        }

                        printCnt++;
                    }
//                    System.out.println("verify sum:"+ sum);
                }
                cnt++;
            }
            System.out.println("datasetNameSet Min Length:");
            for (Map.Entry<String, Integer> entry : datasetNameMap_minLen.entrySet()) {
                System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            }
            System.out.println();
            System.out.println("datasetNameSet Size:");
            for (Map.Entry<String, Integer> entry : datasetNameMap_size.entrySet()) {
                System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Double[]> CSVReader(String csvFilePath) {

        List<Double[]> timeSeriesData = new ArrayList<>();

        try {
            // 创建CSVReader对象
            CSVReader reader = new CSVReader(new FileReader(csvFilePath));

            // 读取CSV文件的所有行数据
            List<String[]> lines = reader.readAll();

            // 打印每一行数据
            int i = 0;
            for (String[] line : lines) {
                if (i > 0) {
                    Double[] ts = new Double[line.length - 2];
                    int j = 0;
                    for (String cell : line) {
                        if (j < line.length - 2) {
                            ts[j] = Double.parseDouble(cell);
                        }
//                        System.out.print(cell + "\t");
                        j++;
                    }
                    timeSeriesData.add(ts);
//                    System.out.println();
                }
                i++;
            }

            // 关闭CSVReader
            reader.close();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        return timeSeriesData;
    }

    public static void arffGenerator(String fileName, String[] attributes, String[] content, String labels) {

        // 创建ARFF文件内容
        StringBuilder arffContent = new StringBuilder();
        arffContent.append("@relation SampleData\n\n");
        // 添加属性
        for (String attribute : attributes) {
            if (!attribute.equals("Class")) {
                arffContent.append("@attribute ").append(attribute).append(" numeric\n");
            } else {
                arffContent.append("@attribute ").append(attribute).append(" {" + labels + "}\n");
            }
        }

        // 添加数据部分
        arffContent.append("\n@data\n");
        for (int j = 0; j < content.length; j++) {
            arffContent.append(content[j] + ",");
        }
        arffContent.deleteCharAt(arffContent.length() - 1);
        arffContent.append("\n");
        // 将ARFF内容写入文件
        try {
            FileWriter writer = new FileWriter(fileName + ".arff");
            writer.write(arffContent.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void arffUpdate(String fileName, String[] content, String labels) {
        int attributeCount = 0;
        String filePath = fileName + ".arff"; // 文件路径
        // 使用StringBuilder来存储更新后的文件内容
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            // 逐行读取文件
            while ((currentLine = br.readLine()) != null) {
                currentLine = currentLine.trim(); // 去除前后空白字符
//                // 如果遇到@data标签，停止读取
                if (currentLine.toLowerCase().startsWith("@attribute")) {
                    attributeCount++;
                }
                // 如果当前行是要修改的行，使用新内容
                if (currentLine.contains("Class")) {
                    String oldLabels = currentLine.split(" ")[2].replace("{", "").replace("}", "");
                    String[] oldLabel = oldLabels.split(",");
                    String[] label = labels.split(",");
                    // 使用HashSet去重并合并两个数组
                    Set<String> set = new HashSet<>();
                    set.addAll(Arrays.asList(oldLabel));
                    set.addAll(Arrays.asList(label));
                    String[] mergedArray = set.toArray(new String[0]);
                    contentBuilder.append(currentLine.replace(currentLine.split(" ")[2], "{" + String.join(",", mergedArray) + "}"));
                } else {
                    contentBuilder.append(currentLine);
                }
                contentBuilder.append(System.lineSeparator()); // 添加换行符
            }
            StringBuilder arffContent = new StringBuilder();
            if (attributeCount < content.length) {
                for (int j = content.length - attributeCount; j < content.length; j++) {
                    arffContent.append(content[j] + ",");
                }
            } else {
                for (int j = 0; j < content.length - 1; j++) {
                    arffContent.append(content[j] + ",");
                }
                for (int j = content.length; j < attributeCount; j++) {
                    arffContent.append(0 + ",");
                }
                arffContent.append(content[content.length - 1] + ",");
            }
            if (arffContent.length() > 0) {
                arffContent.deleteCharAt(arffContent.length() - 1);
            }
            arffContent.append("\n");
            contentBuilder.append(arffContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 将更新后的内容写回文件
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(contentBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exp(String outPath, String fileName, String s, boolean append) throws Exception {
        File outPathFile = new File(outPath);
        if (!outPathFile.exists()) {
            outPathFile.mkdirs();
        }
        try {
            FileWriter writer = new FileWriter(outPath + "/" + fileName, append);
            writer.write(s);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void tree_row_col(String outPath, String inPath) throws Exception {
        Path startPath = Paths.get(inPath);
        AtomicReference<String> filePath = new AtomicReference<>("");
        StringBuilder keyss = new StringBuilder();
        StringBuilder valuess = new StringBuilder();
        StringBuilder labelss = new StringBuilder();
        try (Stream<Path> stream = Files.walk(startPath)) {
            stream.forEach(path -> {
                boolean isFile = Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
                if (isFile) {
                    try (BufferedReader br = new BufferedReader(new FileReader(String.valueOf(path)))) {
                        String line;
                        Map<String, String> map = new HashMap();
                        Map<String, String> map_tmp = new HashMap();
                        String maxLabel = "";
                        Double maxSimilar = 0D;
                        while ((line = br.readLine()) != null) {
                            String[] columns = line.split("_");
                            String shapeletId = "S" + columns[0];
                            String shapletSimilar = columns[1];
                            String label = columns[2];
                            if (maxSimilar < Double.parseDouble(shapletSimilar) && Integer.parseInt(label) > 0) {
                                maxSimilar = Double.parseDouble(shapletSimilar);
                                maxLabel = label;
                                labelss.append(label + ",");
                            }
                            map.put(shapeletId, shapletSimilar);
                            map_tmp.put(shapeletId, shapletSimilar);
                        }
                        if (labelss.length() > 0) {
                            labelss.deleteCharAt(labelss.length() - 1);
                        }

//                        int maxKey = 0;
//                        for (String key : map.keySet()) {
//                            int keyInt = Integer.parseInt(key.split("S")[1]);
//                            if (maxKey < keyInt) {
//                                maxKey = keyInt;
//                            }
//                        }
                        Map<String, String> lwmap = new HashMap();
                        for (String key : map.keySet()) {
                            int smalll = (int) (Math.random() * map.size() * 3) + 1;
                            String left = "S" + smalll;
                            while (map_tmp.keySet().contains(left)) {
                                smalll = (int) (Math.random() * map.size() * 3) + 1;
                                left = "S" + smalll;
                            }
                            map_tmp.put(left, "0");
                            int smallr = (int) (Math.random() * map.size() * 3) + 1;
                            String right = "S" + smallr;
                            while (map_tmp.keySet().contains(right)) {
                                smallr = (int) (Math.random() * map.size() * 3) + 1;
                                right = "S" + smallr;
                            }
                            map_tmp.put(right, "0");
                            String lwkey = left + "_P" + key.split("S")[1] + "_" + right;
                            lwmap.put(lwkey, map.get(key));
                        }

                        String[] keys = new String[lwmap.size() + 1];
                        String[] values = new String[lwmap.size() + 1];
                        int index = 0;
                        for (Map.Entry<String, String> entry : lwmap.entrySet()) {
                            keys[index] = entry.getKey();
                            values[index] = entry.getValue();
                            index++;
                        }
                        keys[index] = "Class";
                        values[index] = maxLabel;
                        File outPathFile = new File(outPath);
                        if (!outPathFile.exists()) {
                            outPathFile.mkdirs();
                        }
                        String fileName = path.toString().split("/")[1];
                        fileName = fileName.split("_")[0] + "_" + fileName.split("_")[1] + "_" + fileName.split("_")[2];
                        if (!filePath.get().equals(outPath + "/" + fileName)) {
                            filePath.set(outPath + "/" + fileName);
                        }
//                        FileWriter writer = new FileWriter(filePath, true);
                        for (int i = 0; i < keys.length; i++) {
                            keyss.append(keys[i]);
                            valuess.append(values[i]);
                            if (i < keys.length - 1) {
                                keyss.append(" "); // 在元素之间添加空格，最后一个元素后不添加
                                valuess.append(" "); // 在元素之间添加空格，最后一个元素后不添加
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        File outPathFile = new File(filePath.get() + ".arff");
        if (!outPathFile.exists()) {
            arffGenerator(filePath.get(), keyss.toString().split(" "), valuess.toString().split(" "), labelss.toString());
        } else {
            arffUpdate(filePath.get(), valuess.toString().split(" "), labelss.toString());
        }
        deleteDirectory(inPath);
    }

    public static void deleteDirectory(String filePath) {
        Path directoryToBeDeleted = Paths.get(filePath);

        try {
            // Java 8及以上版本可以使用Files.walk
            Files.walk(directoryToBeDeleted).sorted(Comparator.reverseOrder()) // 重要: 先删除文件，后删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Unable to delete: " + path + " " + e);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error walking through directory: " + e);
        }
    }


    public static void main(String[] args) {
        String filePath = "results/out_hm_20.txt";
//        readByCol(filePath);
//        FileLinesToColumns(filePath);

//        String csvPath = "/Users/Univariate_arff";
//        String csvFile = csvPath + "/Adiac/Adiac_TEST.arff.csv";
//        addColumnToCSV(csvFile, "timeSeriesId");
//        addColumnToCSV(csvFile, "timeWindowId");

//        String csvFile = "results/LWRSF.csv";
//        System.out.println("Training");
//        readBottomCol(csvFile, 1, 3);
//        System.out.println();
//        System.out.println("Testing");
//        readBottomCol(csvFile, 2, 4);
//

//        txtToCsvConverter("logs/KCM_0.5_0.25.txt", "logs/KCM_0.5_0.25.csv");
//        txtToCsvConverter("logs/KCM_0.5_0.5.txt", "logs/KCM_0.5_0.5.csv");
//        txtToCsvConverter("logs/KCM_0.5_0.75.txt", "logs/KCM_0.5_0.75.csv");
//        txtToCsvConverter("logs/KCM_0.25_0.5.txt", "logs/KCM_0.25_0.5.csv");
//        txtToCsvConverter("logs/KCM_0.25_0.25.txt", "logs/KCM_0.25_0.25.csv");
//        txtToCsvConverter("logs/KCM_0.25_0.75.txt", "logs/KCM_0.25_0.75.csv");
//        txtToCsvConverter("logs/KCM_0.75_0.5.txt", "logs/KCM_0.75_0.5.csv");
//        txtToCsvConverter("logs/KCM_0.75_0.25.txt", "logs/KCM_0.75_0.25.csv");
//        txtToCsvConverter("logs/KCM_0.75_0.75.txt", "logs/KCM_0.75_0.75.csv");
//        txtToCsvConverter("logs/KCM_0.125_0.125.txt", "logs/KCM_0.125_0.125.csv");
////        txtToCsvConverter("logs/KCM_0.125_0.075.txt", "logs/KCM_0.125_0.075.csv");
//        txtToCsvConverter("logs/KCM_0.075_0.125.txt", "logs/KCM_0.075_0.125.csv");


//        txtToCsvConverter("logs/KCM_0.1_0.1.txt", "logs/KCM_0.1_0.1.csv");
//        txtToCsvConverter("logs/KCM_0.1_0.05.txt", "logs/KCM_0.1_0.05.csv");
//        txtToCsvConverter("logs/KCM_0.1_0.025.txt", "logs/KCM_0.1_0.025.csv");
//        txtToCsvConverter("logs/KCM_0.05_0.1.txt", "logs/KCM_0.05_0.1.csv");
//        txtToCsvConverter("logs/KCM_0.05_0.05.txt", "logs/KCM_0.05_0.05.csv");
//        txtToCsvConverter("logs/KCM_0.05_0.025.txt", "logs/KCM_0.05_0.025.csv");
        txtToCsvConverter("logs/KCM_0.025_0.1.txt", "logs/KCM_0.025_0.1.csv");
//        txtToCsvConverter("logs/KCM_0.025_0.05.txt", "logs/KCM_0.025_0.05.csv");
//        txtToCsvConverter("logs/KCM_0.025_0.025.txt", "logs/KCM_0.025_0.025.csv");

        //	String trainFilePath = "input/classification_train.txt";
//		String testFilePath = "input/classification_test.txt";
//		String trainFilePath = "input/detection_train.txt";
//		String testFilePath = "input/detection_test.txt";
//		calLen(trainFilePath);
//		calLen(testFilePath);
//      String filePath = "/Users/Univariate_arff/GunPoint/GunPoint_TRAIN.txt";
//		String filePath = "/Users/Multivariate_arff/LSST/LSST_TEST.txt";
//		String filePath = "/Users/Multivariate_arff/LSST/LSST_TRAIN.txt";
//		String filePath = "/Users/Multivariate_arff/LSST/LSST_TEST.txt";
//      calLen(filePath);
        filePath = "/Users/Univariate_arff/Adiac/Adiac_TRAIN.arff.csv";
        List<Double[]> csv = CSVReader(filePath);
        System.out.println("timeSeriesSimilarity: ");
        timeSeriesSimilarity(csv, csv.get(0));
    }
}



