package discriminator;

import instructor.SequenceDataset;

import java.util.ArrayList;
import java.util.Map;


public class RCSFClassify {

    public static Map<String, String> classify(String params[], ArrayList<PSTree> dtList, int offset) {
        String mode = "classify", timeSeries = null, labels = null;
        CommonConfig cc = new CommonConfig(params, mode);
        SequenceDataset trainSet = new SequenceDataset(cc.getTrainSet()),
                testSet = new SequenceDataset(cc.getTestSet());
        return cc.saveResults(dtList, trainSet, testSet, "", mode, offset);
    }
}
