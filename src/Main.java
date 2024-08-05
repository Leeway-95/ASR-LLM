import discriminator.PSTree;
import discriminator.RCSFClassify;
import discriminator.RCSFTrain;
import instructor.FCM;
import instructor.Instruction;
import utils.ToolsDataset;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        String dirs[] = new String[]{
                "Adiac",
                "WordSynonyms",
                "SwedishLeaf",
                "FaceAll",
                "CricketX",
                "CricketY",
                "CricketZ",
                "Fish",
                "Lightning7"
        };

        String filePath = "logs";
        File outPathFile = new File(filePath);
        if (!outPathFile.exists()) {
            outPathFile.mkdirs();
        }
        filePath += "/KCM_" + FCM.STORAGE_PERCENTAGE_THRESHOLD + "_" + FCM.COMPRESSION_PERCENTAGE_THRESHOLD + ".txt";
        StringBuilder inits = new StringBuilder();
        inits.append("Cache: []\n");
        inits.append("Memory: []\n");
        inits.append("Compression Percentage:0.0\n");
        inits.append("Memory Percentage:0.0\n");
        inits.append("Cache Percentage:0.0\n");
        inits.append("Total Percentage:0.0\n");
        inits.append("\n");
        ToolsDataset.appendToFile(filePath, inits.toString());

        for (String s : dirs) {
            /**
             * add timeSeriesID and timeWindowID column to time series dataset (UCR 2018) CSV
             */

            String params[] = new String[]{
                    // data path
                    "-dd", "/Users/Univariate_arff",
//                            "-dd", "/Users/Multivariate_arff",
                    // dataset name
                    "-dn", s,
                    // method, 1:LRSF ,2: gRSF, 3: RSF, 4: LWRSF
                    "-sm", "4",
                    // result file name, written in project_root/results
                    "-rf", "LWRSF.csv",
                    "-tr", "0.7" // train and update mode reference train and verify-train radio, classify mode set 1
            };

            Double subRadio = 0.1;
            String timeSeriesId = "", timeWindowId = "", stopPointLabel = "";
            Double maxTestingHM = 0D;

            /**
             * 1. Training /  / train initial LWRSF model
             */
            System.out.println("1. Train / input time series train dataset (UCR 2018) - " + s + " / train initial LWRSF model");
            ArrayList<PSTree> lwrsf = new ArrayList<>();
            new RCSFTrain(params, lwrsf, subRadio, 0);
            System.out.println("Complete training, Start to verify!");
            Map<String, String> classify = RCSFClassify.classify(params, RCSFTrain.getDtList(), 0);
            if (maxTestingHM <= Double.parseDouble(classify.get("testingHM"))) {
                timeSeriesId = classify.get("testingTimeSeriesId");
                timeWindowId = classify.get("testingTimeWindowId");
                stopPointLabel = classify.get("testingStopPointLabel");
                maxTestingHM = Double.parseDouble(classify.get("testingHM"));
            }

            /**
             *  2. Annotating  / use LWRSF model classify / update decision tree for LWRSF
             */
            System.out.println("2. Annotating - " + s + " / use LWRSF model classify / update decision tree for LWRSF");
            for (int i = 1; i < 1; i++) {
                System.out.println("Iterative times " + i + " / use LWRSF model classify / update decision tree for LWRSF");
                new RCSFTrain(params, RCSFTrain.getDtList(), subRadio, i);
                classify = RCSFClassify.classify(params, RCSFTrain.getDtList(), i);

                if (maxTestingHM <= Double.parseDouble(classify.get("testingHM"))) {
                    timeSeriesId = classify.get("testingTimeSeriesId");
                    timeWindowId = classify.get("testingTimeWindowId");
                    stopPointLabel = classify.get("testingStopPointLabel");
                    maxTestingHM = Double.parseDouble(classify.get("testingHM"));
                }
            }

            System.out.println("Max Testing HM:" + maxTestingHM);
            System.out.println("Time Series Points:" + timeSeriesId);
            System.out.println("Time Window Points:" + timeWindowId);
            System.out.println("Local Classifications:" + stopPointLabel);
            System.out.println();

            /**
             * 3. KCM compress / switch state dataset
             */
            System.out.println("3. KCM (Key Cache Memory) compress / switch state dataset");
            String[] timeWindowIds = timeWindowId.split("_");
            String[] timeSeriesIds = timeSeriesId.split("_");
            String[] labels = stopPointLabel.split("_");
            String timeWindowIdTemp = "";
            List<Instruction> instructions = new ArrayList<Instruction>();

            for (int i = 0; i < timeWindowIds.length; i++) {
                String timeWindowId0 = timeWindowIds[i];
                if (!timeWindowId0.equals(timeWindowIdTemp)) {
                    if (i > 0) {
                        {
                            String stateData = FCM.getInstructions(instructions);
                            if (!"".equals(stateData)) {
                                FCM.appendStateData(stateData);
                                FCM.run(filePath);
                                FCM.clearStateData();
                                instructions = new ArrayList<Instruction>();
                            }
                        }
                    }
                    timeWindowIdTemp = timeWindowId0;
                }
                instructions.add(new Instruction(labels[i], Integer.parseInt(timeSeriesIds[i]), 0D));
            }
            FCM.writeFile(filePath, s);
        }
    }
}
