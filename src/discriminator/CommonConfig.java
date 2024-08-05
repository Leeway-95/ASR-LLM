package discriminator;

import instructor.Sequence;
import instructor.SequenceDataset;
import utils.ToolsDataset;
import utils.ToolsModel;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * 该函数是 `CommonConfig` 类的构造函数，主要用于根据命令行参数和指定的模式初始化成员变量。
 * 1. **命令行解析**：
 * 2. **验证和帮助显示**：
 * 3. **数据集加载**：
 * - 根据 `mode`（“train” 或 “classify”），根据从命令行参数获得的数据集名称加载适当的数据集（“_TRAIN.arff.csv” 或 “_TEST.arff.csv”），并将其存储在 `dataset` 中。
 * 4. **数据集拆分**：
 * - 对于“分类”模式，手动将虚拟序列添加到“dataset_train”，并将“dataset_test”设置为“dataset”。
 * - 对于“训练”模式，根据标签分布和“args[9]”中指定的训练比率将“dataset”拆分为“dataset_train”和“dataset_test”。
 * 5. **配置和初始化**：
 * - 从默认和特定于数据集的属性文件中读取属性，相应地设置成员变量，如“stepSize”、“leafSize”和“treeDepth”。
 * 6. **命令行构建**:
 */
public class CommonConfig {
    // The following static String variables are the CLI switches
    public static String shapeletMethodSw = "sm";
    public static String shapeletMethodSwL = "method";

    public static String datasetSw = "dn";
    public static String datasetSwL = "dataset-name";

    public static String dataPathSw = "dd";
    public static String dataPathSwL = "data-dir";

    public static String rsltPathSw = "rd";
    public static String rsltPathSwL = "results-dir";

    public static String rsltFNameSw = "rf";
    public static String rsltFNameSwL = "results-file-name";

    public static String paramsPathSw = "pd";
    public static String paramsPathSwL = "params-dir";

    public static String ensembleSizeSw = "es";
    public static String ensembleSizeSwL = "ensemble-size";

    public static String minLenFracSw = "mn";
    public static String minLenFracSwL = "min-len-frac";

    public static String maxLenFracSw = "mx";
    public static String maxLenFracSwL = "max-len-frac";

    public static String saveIterationResults = "itrslts";
    public static String saveIterationResultsL = "save-iter-results";

    public static String trainRadioSw = "tr";
    public static String trainRadioSwL = "dataset-train-radio";

    public String resultsFileName;

    public CommandLine cmdLine;
    public Options options;

    public ArrayList<Sequence> trainSet;
    public ArrayList<Sequence> testSet;

    public int minLen;
    public int maxLen;
    public int stepSize;
    public int leafSize;
    public int treeDepth;

    public boolean detailedResultsEnabled;
    public Double meanSquaredError;
    public Double bias;
    public Double variance;
    public Double irreducibleError;
    public HashMap<Integer, Double> trueVec;
    public HashMap<Integer, Double> predVec;
    public HashMap<Integer, Double> meanVec;

    public CommonConfig(String[] args, String mode) {
        this.constructCommandLine(args);
        if (this.cmdLine == null) {
            this.printHelp(true);
            System.exit(1);
        } else {
            resultsFileName = this.getResultsFileName();
            System.out.println("Data set: " + this.getDataSetName() + " - " + resultsFileName);
            List<Sequence> dataset = null;
            if ("train".equals(mode)) {
                dataset = this.loadDataset(this.getDataSetName(), "_TRAIN.arff.csv", mode);
            } else { // "classify".equals(mode)
                dataset = this.loadDataset(this.getDataSetName(), "_TEST.arff.csv", mode);
            }

            ArrayList dataset_train, dataset_test;
            if ("classify".equals(mode)) {
                // 按顺序分入
                dataset_train = new ArrayList<>();
                Double[] ts = new Double[]{0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D};
                dataset_train.add(new Sequence(ts, 0, 0, 0, 0, 0D));
                dataset_test = new ArrayList<>(dataset);
            } else {
                // 按分布分入 "train".equals(mode)
                Double trainRadio = Double.parseDouble(args[9]);
                /**
                 * 1.遍历数据集以计算标签的分布
                 */
                Map<Integer, Integer> labels = new HashMap<>();
                for (Sequence data : dataset) {
                    if (labels.containsKey(data.getLabel())) {
                        labels.put(data.getLabel(), labels.get(data.getLabel()) + 1);
                    } else {
                        labels.put(data.getLabel(), 1);
                    }
                }
                /**
                 * 2.根据分发获得训练集
                 */
                Map<Integer, Integer> labels_train = new HashMap(labels);
                for (Map.Entry<Integer, Integer> entry : labels_train.entrySet()) {
                    entry.setValue(0);
                }
                Map<Integer, Integer> labels_test = new HashMap(labels);
                for (Map.Entry<Integer, Integer> entry : labels_test.entrySet()) {
                    entry.setValue(0);
                }
                dataset_train = new ArrayList<>();
                dataset_test = new ArrayList<>();
                for (int i = 0; i < dataset.size(); i++) {
                    Sequence timeSeries = dataset.get(i);
                    int labels_train_cnt = labels_train.get(timeSeries.getLabel());
                    if (labels_train_cnt < labels.get(timeSeries.getLabel()) * trainRadio) {
                        dataset_train.add(timeSeries);
                        labels_train.put(timeSeries.getLabel(), ++labels_train_cnt);
                    } else {
                        dataset_test.add(timeSeries);
                    }
                }
            }
            this.trainSet = dataset_train;
            this.testSet = dataset_test;

            this.parseCandidateLengthFractions();

            Properties props = new Properties();
            File propsFile;
            Path filePath;
            try {
                filePath = Paths.get(this.getParamsPath(), "default.params");
                propsFile = new File(filePath.toUri());
                if (propsFile.exists()) {
                    props.load(new FileInputStream(propsFile));
                }
                filePath = Paths.get(this.getParamsPath(), this.getDataSetName() + ".params");
                propsFile = new File(filePath.toUri());
                if (propsFile.exists()) {
                    props.load(new FileInputStream(propsFile));
                }
            } catch (Exception e) {
                System.err.println("Error opening properties file: " + e);
            }

            this.stepSize = 1;
            this.leafSize = 1;
            this.treeDepth = Integer.MAX_VALUE;

            if (props.containsKey("stepSize")) {
                this.stepSize = Integer.parseInt(props.getProperty("stepSize"));
            }
            if (props.containsKey("leafSize")) {
                this.leafSize = Integer.parseInt(props.getProperty("leafSize"));
                if (this.leafSize == 0) {
                    HashMap<Integer, Integer> classMap = new HashMap<>();
                    for (Sequence ts : trainSet) {
                        classMap.put(ts.getLabel(), classMap.getOrDefault(ts.getLabel(), 0) + 1);
                    }
                    this.leafSize = classMap.entrySet()
                            .stream()
                            .min((x, y) -> x.getValue() < y.getValue() ? -1 : 1)
                            .get()
                            .getValue();
                }
            }
            if (props.containsKey("treeDepth")) {
                this.treeDepth = Integer.parseInt(props.getProperty("treeDepth"));
            }

            this.meanSquaredError = 0d;
            this.bias = 0d;
            this.variance = 0d;
            this.irreducibleError = 0d;
            this.trueVec = new HashMap<>();
            this.predVec = new HashMap<>();
            this.meanVec = new HashMap<>();
            this.detailedResultsEnabled = this.getDetailedResultsEnabled();
        }
    }

    public void constructCommandLine(String[] args) {
        this.options = new Options();
        this.options.addOption(Option.builder(shapeletMethodSw)
                        .longOpt(shapeletMethodSwL)
                        .argName("SHAPELET TREE METHOD")
                        .required()
                        .desc("method to build shapelet forest")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(datasetSw)
                        .longOpt(datasetSwL)
                        .argName("FILE")
                        .required()
                        .desc("Data set name to be evaluated")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(dataPathSw)
                        .longOpt(dataPathSwL)
                        .argName("PATH/TO/DATA/DIRECTORY")
                        .desc("Absolute or relative path to the directory with the data set file")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(rsltPathSw)
                        .longOpt(rsltPathSwL)
                        .argName("PATH/TO/RESULTS/DIRECTORY")
                        .desc("Absolute or relative path to the directory where results will be saved"
                                + "\nDefault: 'results' directory in the program directory")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(rsltFNameSw)
                        .longOpt(rsltFNameSwL)
                        .argName("RESULTS FILE NAME")
                        .required()
                        .desc("result file name")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(paramsPathSw)
                        .longOpt(paramsPathSwL)
                        .argName("PATH-TO-PARAMS-FOLDER")
                        .desc("Absolute or relative path to the parameter files directory"
                                + "\nDefault: 'params' directory in the program directory")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(ensembleSizeSw)
                        .longOpt(ensembleSizeSwL)
                        .argName("ENSEMBLE SIZE")
                        .desc("The ensemble size\nDefault: 10 members")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(minLenFracSw)
                        .longOpt(minLenFracSwL)
                        .argName("MIN LEN FRACTION")
                        .desc("Fraction of Time Series Length to use as Minimum Shapelet Length")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(maxLenFracSw)
                        .longOpt(maxLenFracSwL)
                        .argName("MAX LEN FRACTION")
                        .desc("Fraction of Time Series Length to use as Maximum Shapelet Length")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(saveIterationResults)
                        .longOpt(saveIterationResultsL)
                        .argName("Save detailed iteration results")
                        .desc("Enables the creation of results file with detailed results for each iteration")
                        .numberOfArgs(1)
                        .build())
                .addOption(Option.builder(trainRadioSw)
                        .longOpt(trainRadioSwL)
                        .argName("Set the radio of train dataset")
                        .desc("Get a portion of the training set to do the training and a portion to validate the training")
                        .numberOfArgs(1)
                        .build());

        try {
            CommandLineParser cliParser = new DefaultParser();

            this.cmdLine = cliParser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments\nInvalid or no arguments provided");
        }
    }

    public List<Sequence> loadDataset(String fileName, String split, String mode) {
        ArrayList<Sequence> dataset = null;
        Sequence currTS;
        ArrayList<Double> tsList;
        int tsClass = 0, shapletId = 0, tsId = 0, tsTW = 0;

        String currLine, delimiter = ",";
        StringTokenizer st;
//        Path path = Paths.get(this.getDataPath(),
//                fileName,
//                fileName + split);

//        try {
//            FileInputStream fis = new FileInputStream(path.toString());
//            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
//            BufferedReader br = new BufferedReader(isr);
//
//            // 读取文件内容
//            String line;
//            while ((line = br.readLine()) != null) {
//                line = line.replace("\u0000","");
//                System.out.println(line);
//            }
//
//            // 关闭文件
//            br.close();
//            isr.close();
//            fis.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try (BufferedReader br = Files.newBufferedReader(Paths.get(this.getDataPath(),
                fileName,
                fileName + split))) {
            dataset = new ArrayList<Sequence>();
            List<Double[]> smiList = new ArrayList<>();
            int cnt = 0;
            while ((currLine = br.readLine()) != null) {
                if (cnt > 0) {
                    // 第一行标题不处理
                    currLine = currLine.replace("\u0000", "");
                    if (currLine.matches("\\s*")) {
                        continue;
                    } else {
                        currLine = currLine.trim();
                        String[] currLine_split = currLine.split(delimiter);
                        st = new StringTokenizer(currLine, String.valueOf(delimiter));
//                    tsClass = (int) Double.parseDouble(st.nextToken());
                        tsList = new ArrayList<Double>();
                        while (st.hasMoreTokens()) {
                            tsList.add(Double.parseDouble(st.nextToken().replace("\"", "")));
                        }
                        Double[] ts = new Double[tsList.size()];
                        for (int i = 0; i < tsList.size(); i++) {
                            ts[i] = tsList.get(i);
                        }
                        smiList.add(ts);
                        if ("classify".equals(mode)) {
                            tsTW = Integer.parseInt(currLine_split[currLine_split.length - 1].replace("\"", ""));
                            tsId = Integer.parseInt(currLine_split[currLine_split.length - 2].replace("\"", ""));
                            tsClass = Integer.parseInt(currLine_split[currLine_split.length - 3].replace("\"", ""));
                            for (int i = 0; i < 3; i++) {
                                tsList.remove(tsList.size() - 1);
                            }
                            currTS = new Sequence(ts, tsId, tsTW, tsClass);
                        } else {
                            shapletId = Integer.parseInt(currLine_split[currLine_split.length - 1].replace("\"", ""));
                            tsClass = Integer.parseInt(currLine_split[currLine_split.length - 2].replace("\"", ""));
                            for (int i = 0; i < 2; i++) {
                                tsList.remove(tsList.size() - 1);
                            }
                            currTS = new Sequence(ts, tsId, tsTW, tsClass, shapletId, 0D);
                        }
                        dataset.add(currTS);
                    }
                }
                cnt++;
            }
        } catch (IOException e) {
            System.err.println("Error reading the data set file: " + e);
        }
        return ToolsDataset.timeSeriesSimilarity(dataset);
    }

    public String getDataSetName() {
        return this.cmdLine.getOptionValue(datasetSw);
    }

    public String getDataPath() {
        return this.cmdLine.getOptionValue(dataPathSw, "../time_series_data/formats/csv_norm");
    }

    public String getResultsPath() {
        return this.cmdLine.getOptionValue(rsltPathSw, "results");
    }

    public String getResultsFileName() {
        return this.cmdLine.getOptionValue(rsltFNameSw, "RPSF.csv");
    }

    public String getParamsPath() {
        return this.cmdLine.getOptionValue(paramsPathSw, "params");
    }

    public int getEnsembleSize() {
        return Integer.parseInt(this.cmdLine.getOptionValue(ensembleSizeSw, "10"));
    }

    public int getMethod() {
        return Integer.parseInt(this.cmdLine.getOptionValue(shapeletMethodSw, "4"));
    }

    public Double getTrainRadio() {
        return Double.parseDouble(this.cmdLine.getOptionValue(trainRadioSw, "0.5"));
    }

    public ArrayList<Sequence> getTrainSet() {
        return this.trainSet;
    }

    public ArrayList<Sequence> getTestSet() {
        return this.testSet;
    }

    public int getMinLen() {
        return this.minLen;
    }

    public int getMaxLen() {
        return this.maxLen;
    }

    public int getStepSize() {
        return this.stepSize;
    }

    public int getLeafSize() {
        return this.leafSize;
    }

    public int getTreeDepth() {
        return this.treeDepth;
    }

    public boolean getDetailedResultsEnabled() {
        int temp = Integer.parseInt(this.cmdLine.getOptionValue(saveIterationResults, "0"));
        return !(temp == 0);
    }

    /**
     * Print help to provided OutputStream.
     *
     * @param detailed
     */
    public void printHelp(boolean detailed) {
        String cliSyntax = "java ";
        PrintWriter writer = new PrintWriter(System.out);
        HelpFormatter helpFormatter = new HelpFormatter();
        if (detailed) {
            helpFormatter.printHelp(writer, 120, cliSyntax, "", options, 7, 1, "", true);
        } else {
            helpFormatter.printUsage(writer, 120, cliSyntax, options);
        }
        writer.flush();
    }

    public void parseCandidateLengthFractions() {
        Double fLow = Double.parseDouble(cmdLine.getOptionValue(minLenFracSw,
                "0.25"));
        Double fHigh = Double.parseDouble(cmdLine.getOptionValue(maxLenFracSw,
                "0.67"));
        String message;
        if (fLow > fHigh) {
            message = "Maximum length can not be less than Minimum length";
            throw new IllegalArgumentException(message);
        }
        if (fLow <= 0 || fLow > 1) {
            message = "Illegal fraction for 'Minimum candidate length'\n"
                    + "Valid range is (0,1] and min_length < max_length";
            throw new IllegalArgumentException(message);
        }

        if (fHigh <= 0 || fHigh > 1) {
            message = "Illegal fraction for 'Maximum candidate length'\n"
                    + "Valid range is (0,1] and min_length < max_length";
            throw new IllegalArgumentException(message);
        }

        int minLen = Math.max(1, (int) (this.trainSet.get(0).size() * fLow));
        if (minLen < 2) {
            System.err.println("!!! Warning !!!\nMinimum candidate length "
                    + "below 2 units may cause program crashes "
                    + "when using normalization because of 0 "
                    + "variance of the subsequence at length 1");
        }
        this.minLen = minLen;

        int maxLen = Math.min(this.trainSet.get(0).size(),
                (int) (this.trainSet.get(0).size() * fHigh));
        this.maxLen = maxLen;
    }

//    public Double getMinLenFrac() {
//        return Double.parseDouble(cmdLine.getOptionValue(minLenFracSw, "0.25"));
//    }
//
//    public Double getMaxLenFrac() {
//        return Double.parseDouble(cmdLine.getOptionValue(maxLenFracSw, "0.67"));
//    }

    public void saveResults(PSTree dt, SequenceDataset trainSet,
                            SequenceDataset testSet, String trainingTime, String mode, int offset) {
        ArrayList<PSTree> dtList = new ArrayList<>();
        dtList.add(dt);
        this.saveResults(dtList, trainSet, testSet, trainingTime, false, mode, offset);
    }

    public Map<String, String> saveResults(ArrayList<PSTree> dtList,
                                           SequenceDataset trainSet,
                                           SequenceDataset testSet, String trainingTime, String mode, int offset) {
        return this.saveResults(dtList, trainSet, testSet, trainingTime, true, mode, offset);
    }

    public Map<String, String> saveResults(ArrayList<PSTree> dtList,
                                           SequenceDataset trainSet,
                                           SequenceDataset testSet, String trainingTime,
                                           boolean isEnsemble, String mode, int offset) {
        Map<String, String> reMap = new HashMap<>();
        long start;
        String testingTime, trainingAccuracy = "", testingAccuracy, testingEarliness, trainingEarliness = "", testingHM, trainingHM = "", testingStopPoint, trainingStopPoint = "", testingStopPointLabel, trainingStopPointLabel = "", testingTimeSeriesId = "", testingTimeWindowId = "";
        start = System.currentTimeMillis();
        try {
            Files.createDirectories(Paths.get(this.getResultsPath()));
            File resultsFile = new File(Paths.get(this.getResultsPath(),
                            this.resultsFileName)
                    .toString());
            Formatter finalResults = new Formatter();
            if (!resultsFile.exists()) {
                finalResults.format("%s",
                        "Dataset,iterTimes,TrainingTime,TestingTime,"
                                + "TrainingAccuracy,TestingAccuracy,"
                                + "TrainingEarliness,TestingEarliness,"
                                + "TrainingHM,TestingHM,"
                                + "TrainSize,TestSize,TSLen");

                if (isEnsemble) {
                    finalResults.format(",%s", "EnsembleSize");
                }

                finalResults.format("%s", ",TrainingLocalPoints,TestingLocalPoints,TrainingLocalClassifications,TestingLocalClassifications");
                finalResults.format("\n");
            }

            Map<String, String> testReMap = this.getSplitAccuracy(dtList, testSet, mode + "-testSplit");
            Map<String, String> trainReMap = this.getSplitAccuracy(dtList, trainSet, mode + "-trainSplit");

            if (this.detailedResultsEnabled) {
                File iterResultsFile = new File(Paths.get(this.getResultsPath(),
                                this.getDataSetName()
                                        + " - "
                                        + this.resultsFileName)
                        .toString());
                Formatter iterResults = new Formatter();
                iterResults.format("%s\n",
                        "Iteration,TrAccModelI,TsAccModelI,TrAcc,"
                                + "TrMSE,TrBias^2,TrVar,TrIrrErr,TsAcc,"
                                + "TsMSE,TsBias^2,TsVar,TsIrrErr");
                try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(iterResultsFile.getAbsolutePath()),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    ArrayList<PSTree> subList;
                    for (int i = 0; i < dtList.size(); i++) {
                        subList = new ArrayList<PSTree>(dtList.subList(i,
                                i + 1));
                        testingAccuracy = this.getSplitAccuracy(subList, testSet, mode + "-testSplit").get("accuracy");
                        trainingAccuracy = this.getSplitAccuracy(subList, trainSet, mode + "-trainSplit").get("accuracy");
                        iterResults.format("%d,%.3f,%.3f", i + 1,
                                trainingAccuracy, testingAccuracy);
                        subList = new ArrayList<PSTree>(dtList.subList(0,
                                i + 1));
                        testingAccuracy = this.getSplitAccuracy(subList, testSet, mode + "-testSplit").get("accuracy");
                        trainingAccuracy = this.getSplitAccuracy(subList, trainSet, mode + "-trainSplit").get("accuracy");
                        this.calculateBiasVariance(subList, trainSet);
                        iterResults.format(",%.4f,%.4f,%.4f,%.4f,%.4f",
                                trainingAccuracy,
                                this.meanSquaredError, this.bias,
                                this.variance,
                                this.irreducibleError);
                        this.calculateBiasVariance(subList, testSet);
                        iterResults.format(",%.4f,%.4f,%.4f,%.4f,%.4f\n",
                                testingAccuracy,
                                this.meanSquaredError, this.bias,
                                this.variance,
                                this.irreducibleError);
                    }
                    bw.write(iterResults.toString());
                }
                iterResults.close();
            }
            if ("train".equals(mode)) {
                trainingAccuracy = trainReMap.get("accuracy");
                trainingEarliness = trainReMap.get("earliness");
                trainingHM = trainReMap.get("hm");
                trainingStopPoint = trainReMap.get("stopPoint");
                trainingStopPointLabel = trainReMap.get("stopPointLabel");
            }

            // train & classify
            testingAccuracy = testReMap.get("accuracy");
            testingEarliness = testReMap.get("earliness");
            testingHM = testReMap.get("hm");
            testingStopPoint = testReMap.get("stopPoint");
            testingStopPointLabel = testReMap.get("stopPointLabel");
            testingTimeSeriesId = testReMap.get("timeSeriesId");
            testingTimeWindowId = testReMap.get("timeWindowId");

            testingTime = ((System.currentTimeMillis() - start) / 1e3) + "";

            reMap.put("testingHM", testingHM);
            reMap.put("testingStopPoint", testingStopPoint);
            reMap.put("testingStopPointLabel", testingStopPointLabel);
            reMap.put("testingTimeSeriesId", testingTimeSeriesId);
            reMap.put("testingTimeWindowId", testingTimeWindowId);

            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(resultsFile.getAbsolutePath()),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                finalResults.format("%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%d,%d,%d",
                        this.getDataSetName(),
                        offset + 1,
                        trainingTime,
                        testingTime,
                        trainingAccuracy,
                        testingAccuracy,
                        trainingEarliness,
                        testingEarliness,
                        trainingHM,
                        testingHM,
                        this.trainSet.size(),
                        this.testSet.size(),
                        this.trainSet.get(0).size());
                if (isEnsemble) {
                    finalResults.format(",%d", dtList.size());
                }
                finalResults.format(",%s,%s,%s,%s", trainingStopPoint, testingStopPoint, trainingStopPointLabel, testingStopPointLabel);
                finalResults.format("\n");
                bw.write(finalResults.toString());
            }
            System.out.println(finalResults.toString());
            finalResults.close();
        } catch (IOException e) {
            System.err.println("Error saving results: " + e);
        }
        return reMap;
    }

    private Map<String, String> getSplitAccuracy(ArrayList<PSTree> dtList,
                                                 SequenceDataset split, String mode) {
        Map<String, String> reMap = new HashMap();
        List<Integer> stopPoint = new ArrayList<Integer>();
        List<Integer> stopPointLabel = new ArrayList<Integer>();
        List<Integer> timeSeriesId = new ArrayList<Integer>();
        List<Integer> timeWindowId = new ArrayList<Integer>();
        int predClass, correct = 0, majorityVote;
        HashMap<Integer, Integer> predClassCount = new HashMap<>();
        for (int ind = 0; ind < split.size(); ind++) {
            predClassCount.clear();
            for (int j = 0; j < dtList.size(); j++) {
                predClass = dtList.get(j).checkInstance(split.get(ind));
                predClassCount.put(predClass,
                        1 + predClassCount.getOrDefault(predClass,
                                0));
            }
            majorityVote = predClassCount.entrySet().stream()
                    .max((e1, e2) -> ((e1.getValue() > e2.getValue()) ? 1 : -1))
                    .get()
                    .getKey();
            if (majorityVote == split.get(ind).getLabel()) {
                correct++;
                // 记录停止点
                stopPoint.add(ind);
                stopPointLabel.add(majorityVote);
                timeSeriesId.add(split.get(ind).getTimeSeriesId());
                timeWindowId.add(split.get(ind).getTimeWindowId());
            }
        }
        if (mode.contains("classify")) {
            System.out.println("Local Points:" + stopPoint);
            System.out.println("Local Classifications:" + stopPointLabel);
            System.out.println();
        }

        // 计算指标
        Double earliness = ToolsModel.getEarliness(stopPoint, split.size());
        Double accuracy = Double.parseDouble(correct / split.size() + "");
        Double hm = ToolsModel.getHM(earliness, accuracy);
        System.out.println(mode + " Accuracy:" + accuracy + ", Earliness:" + earliness + ", HM:" + hm);

        reMap.put("earliness", earliness + "");
        reMap.put("accuracy", accuracy + "");
        reMap.put("hm", hm + "");
        reMap.put("stopPoint", listToStr(stopPoint));
        reMap.put("stopPointLabel", listToStr(stopPointLabel));
        reMap.put("timeSeriesId", listToStr(timeSeriesId));
        reMap.put("timeWindowId", listToStr(timeWindowId));

        return reMap;
    }

    public String listToStr(List<Integer> stopPoint) {
        if (stopPoint == null) {
            return "";
        }
        String stopPointStr = "";
        for (int i = 0; i < stopPoint.size(); i++) {
            stopPointStr += stopPoint.get(i) + "_";
        }
        if (stopPointStr.length() > 0) {
            stopPointStr = stopPointStr.substring(0, stopPointStr.length() - 1);
        }
        return stopPointStr;
    }

    public void calculateBiasVariance(ArrayList<PSTree> dtList,
                                      SequenceDataset ds) {
        this.meanSquaredError = 0d;
        this.bias = 0d;
        this.variance = 0d;
        Double cummErrorForInstI, errorForInstIWithTreeK;
        int trueLabel, predLabel, bk, ck;
        for (int i = 0; i < ds.size(); i++) {
            trueLabel = ds.get(i).getLabel();
            cummErrorForInstI = 0d;
            for (int k = 0; k < dtList.size(); k++) {
                predLabel = dtList.get(k).checkInstance(ds.get(i));
                errorForInstIWithTreeK = 0d;
                for (Integer key : ds.getAllClasses()) {
                    bk = predLabel == key ? 1 : 0;
                    ck = trueLabel == key ? 1 : 0;
                    errorForInstIWithTreeK += (bk - ck) * (bk - ck);
                }
                cummErrorForInstI += errorForInstIWithTreeK;
            }
            cummErrorForInstI /= dtList.size();
            this.meanSquaredError += cummErrorForInstI;
        }
        this.meanSquaredError /= ds.size();

        for (int i = 0; i < ds.size(); i++) {
            this.trueVec.clear();
            this.predVec.clear();
            this.meanVec.clear();
            this.trueVec.put(ds.get(i).getLabel(), 1d);
            for (int k = 0; k < dtList.size(); k++) {
                predLabel = dtList.get(k).checkInstance(ds.get(i));
                this.predVec.put(predLabel,
                        1 + this.predVec.getOrDefault(predLabel, 0d));
                this.meanVec.put(predLabel,
                        1 + this.meanVec.getOrDefault(predLabel, 0d));
            }
            Double max = this.predVec.entrySet().stream()
                    .max((e1, e2) -> e1.getValue() > e2.getValue() ? 1 : -1)
                    .get().getValue();
            for (Integer key : ds.getAllClasses()) {
                this.predVec.put(key,
                        Math.floor(predVec.getOrDefault(key, 0d) / max));
                this.variance += Math.pow(this.predVec.getOrDefault(key, 0d)
                                - this.meanVec.getOrDefault(key, 0d)
                                / dtList.size(),
                        2)
                        / dtList.size();
                this.bias += Math.pow(this.trueVec.getOrDefault(key, 0d)
                                - this.meanVec.getOrDefault(key, 0d)
                                / dtList.size(),
                        2);
            }
        }
        this.variance /= ds.size();
        this.bias /= ds.size();
        this.irreducibleError = this.meanSquaredError - this.bias
                - this.variance;
    }
}
