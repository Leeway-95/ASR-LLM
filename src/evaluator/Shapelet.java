package evaluator;


import instructor.Sequence;

import java.util.ArrayList;
import java.util.TreeMap;


//s1 Class
public class Shapelet implements Comparable<Shapelet> {

    //s1 Class

    public Double[] content;
    public int seriesId;
    public int startPos;
    public Double splitThreshold;
    public Double splitInfo;
    public Double informationGain;
    public Double separationGap;
    private Double gainRatio;
    public int granularity;
    public int numBins;

    public Sequence s1;
    public Double splitDist;
    public TreeMap<Double, ArrayList<Integer>> dsHist;

    public Sequence s2;
    public int[] split;
    public Double gain;


    public Shapelet(Sequence s1, Sequence s2, int[] split) {
        this.s1 = s1;
        this.s2 = s2;
        this.split = split;
    }

    public Sequence getShapelet2() {
        return s2;
    }

    public void setGain(Double gain) {
        this.gain = gain;
    }

    public Double getGain() {
        return gain;
    }

    public Shapelet() {
        this.s1 = null;
        this.s2 = null;
        split = null;
        this.splitDist = Double.POSITIVE_INFINITY;
        this.dsHist = null;
    }

    public Shapelet(Sequence s1, Double splitDist, TreeMap<Double, ArrayList<Integer>> dsHist) {
        this.s1 = s1;
        this.splitDist = splitDist;
        this.dsHist = dsHist;
    }

    public int[] getLwSplit() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Sequence getShapelet() {
        return this.s1;
    }

    public Double getSplitDist() {
        return this.splitDist;
    }

    public TreeMap<Double, ArrayList<Integer>> getHistMap() {
        return this.dsHist;
    }

    //Constructors
    public Shapelet(Double[] content, int seriesId, int startPos, int granularity, int numBins) {
        this.setContent(content);
        this.setSeriesId(seriesId);
        this.setStartPos(startPos);
        this.setGranularity(granularity);
        this.setnumBins(numBins);

    }

    //Getters and Setters
    public Double[] getContent() {
        return content;
    }

    public void setContent(Double[] content) {
        this.content = content;
    }

    public int getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(int seriesId) {
        this.seriesId = seriesId;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public Double getSplitThreshold() {
        return splitThreshold;
    }

    public void setSplitThreshold(Double splitThreshold) {
        this.splitThreshold = splitThreshold;
    }

    public Double getInformationGain() {
        return informationGain;
    }

    public void setInformationGain(Double informationGain) {
        this.informationGain = informationGain;
    }

    public Double getSeparationGap() {
        return separationGap;
    }

    public void setSeparationGap(Double separationGap) {
        this.separationGap = separationGap;
    }


    public Double getGainRatio() {
        return this.gainRatio;

    }

    private void setGainRatio(Double bsfGainR) {
        this.gainRatio = bsfGainR;

    }

    public int getLength() {
        return content.length;
    }

    private void setGranularity(int granularity) {
        this.granularity = granularity;
    }

    public int getGranularity() {
        return this.granularity;
    }

    public Double getSplitInfo() {
        return this.splitInfo;
    }

    public void setSplitInfo(Double splitInfo) {
        this.splitInfo = splitInfo;
    }

    public void setnumBins(int numBins) {
        this.numBins = numBins;

    }

    public int getNumBins() {
        return this.numBins;
    }

    public Double getGap() {
        return this.separationGap;
    }

    /*
     * Compute Gain Ratio: Information Gain / Split Info
     * 1 - For each threshold (starting between 0 and 1 and ending between end-1 and end
     * 2 - Compute the information gain (Parent Entropy - EntropyAfterSplit)
     * 3 - EntropyAfterSplit = EntropyLeft + EntropyRight
     */
    public void calcGainRatioAndThreshold(
            ArrayList<OrderLineObj> orderline,
            TreeMap<Double, Integer> classDistribution) {


        Double lastDist = orderline.get(0).getDistance();
        Double thisDist = -1D;
        Double bsfGainR = -1D;
        Double bsfGain = -1D;
        Double threshold = -1D;
        Double Infogain = 0D;
        Double splitInfoValue = -1.0;
        for (int i = 1; i < orderline.size(); i++) {
            thisDist = orderline.get(i).getDistance();
            if (i == 1 || thisDist != lastDist) { // check that threshold has moved

                // count class instances below and above threshold
                TreeMap<Double, Integer> lessClasses = new TreeMap<Double, Integer>();
                TreeMap<Double, Integer> greaterClasses = new TreeMap<Double, Integer>();

                for (Double j : classDistribution.keySet()) {
                    lessClasses.put(j, 0);
                    greaterClasses.put(j, 0);
                }

                int sumOfLessClasses = 0;
                int sumOfGreaterClasses = 0;

                // visit those below threshold
                for (int j = 0; j < i; j++) {
                    Double thisClassVal = orderline.get(j).getClassVal();
                    int storedTotal = lessClasses.get(thisClassVal);
                    storedTotal++;
                    lessClasses.put(thisClassVal, storedTotal);
                    sumOfLessClasses++;
                }

                // visit those above threshold
                for (int j = i; j < orderline.size(); j++) {
                    Double thisClassVal = orderline.get(j).getClassVal();
                    int storedTotal = greaterClasses.get(thisClassVal);
                    storedTotal++;
                    greaterClasses.put(thisClassVal, storedTotal);
                    sumOfGreaterClasses++;
                }

                int sumOfAllClasses = sumOfLessClasses
                        + sumOfGreaterClasses;

                Double parentEntropy = entropy(classDistribution);
                // calculate the info gain below the threshold
                Double lessFrac = Double.parseDouble(sumOfLessClasses
                        / sumOfAllClasses + "");
                Double entropyLess = entropy(lessClasses);


                // calculate the info gain above the threshold
                Double greaterFrac = Double.parseDouble(sumOfGreaterClasses
                        / sumOfAllClasses + "");
                Double entropyGreater = entropy(greaterClasses);

                Infogain = parentEntropy - lessFrac * entropyLess
                        - greaterFrac * entropyGreater;


                splitInfoValue = splitInfo(orderline);

                gainRatio = Infogain / splitInfoValue;


                if (gainRatio > bsfGainR) {
                    bsfGainR = gainRatio;
                    threshold = (thisDist - lastDist) / 2 + lastDist;
                }

                if (Infogain > bsfGain) {
                    bsfGain = Infogain;
                }
            }
            lastDist = thisDist;
        }
        if (bsfGainR >= 0) {
            this.setGainRatio(bsfGainR);
            this.setSplitThreshold(threshold);
            this.setSplitInfo(splitInfoValue);
        }
        if (bsfGain >= 0) {
            this.setInformationGain(bsfGain);
        }
    }


    //Compute SplitInfo
    public Double splitInfo(ArrayList<OrderLineObj> orderline) {
        ArrayList<Double> splitInfoParts = new ArrayList<Double>();
        Double toAdd;
        Double thisPart = 0D;
        Double splitInfo = 0D;
        Double totalElements = Double.parseDouble(orderline.size() + "");
        Double sum = 0D;
        Double distToCompare = orderline.get(0).getDistance();

        for (int x = 0; x < orderline.size(); x++) {
            if (orderline.get(x).getDistance() == distToCompare) {
                sum++;
            } else {
                thisPart = (sum / totalElements);
                toAdd = -thisPart * Math.log10(thisPart) / Math.log10(2);
                if (Double.isNaN(toAdd))
                    toAdd = 0D;
                splitInfoParts.add(toAdd);
                sum = 1D;
                toAdd = 0D;
            }

            distToCompare = orderline.get(x).getDistance();
        }

        if (sum > 0) {
            thisPart = (sum / totalElements);
            toAdd = -thisPart * Math.log10(thisPart) / Math.log10(2);
            if (Double.isNaN(toAdd))
                toAdd = 0D;
            splitInfoParts.add(toAdd);
            toAdd = 0D;
        }


        for (int i = 0; i < splitInfoParts.size(); i++) {
            splitInfo += splitInfoParts.get(i);
        }

        return splitInfo;

    }


    //Compute Entropy
    private static Double entropy(TreeMap<Double, Integer> classDistributions) {
        if (classDistributions.size() == 1) {
            return 0D;
        }

        Double thisPart;
        Double toAdd;
        int total = 0;
        for (Double d : classDistributions.keySet()) {
            total += classDistributions.get(d);
        }
        // to avoid NaN calculations, the individual parts of the entropy are
        // calculated and summed.
        // i.e. if there is 0 of a class, then that part would calculate as NaN,
        // but this can be caught and
        // set to 0.
        ArrayList<Double> entropyParts = new ArrayList<Double>();
        for (Double d : classDistributions.keySet()) {
            thisPart = Double.parseDouble(classDistributions.get(d) / total + "");
            toAdd = -thisPart * Math.log10(thisPart) / Math.log10(2);
            if (Double.isNaN(toAdd))
                toAdd = 0D;
            entropyParts.add(toAdd);
        }

        Double entropy = 0D;
        for (int i = 0; i < entropyParts.size(); i++) {
            entropy += entropyParts.get(i);
        }
        return entropy;
    }


    public void calcInfoGainAndThreshold(ArrayList<OrderLineObj> orderline, TreeMap<Double, Integer> classDistribution) {
        // for each split point, starting between 0 and 1, ending between end-1 and end
        // addition: track the last threshold that was used, don't bother if it's the same as the last one
        Double lastDist = orderline.get(0).getDistance(); // must be initialised as not visited(no point breaking before any data!)
        Double thisDist = -1D;

        Double bsfGain = -1D;
        Double threshold = -1D;

        for (int i = 1; i < orderline.size(); i++) {
            thisDist = orderline.get(i).getDistance();
            if (i == 1 || thisDist != lastDist) { // check that threshold has moved(no point in sampling identical thresholds)- special case - if 0 and 1 are the same dist

                // count class instances below and above threshold
                TreeMap<Double, Integer> lessClasses = new TreeMap<Double, Integer>();
                TreeMap<Double, Integer> greaterClasses = new TreeMap<Double, Integer>();

                for (Double j : classDistribution.keySet()) {
                    lessClasses.put(j, 0);
                    greaterClasses.put(j, 0);
                }

                int sumOfLessClasses = 0;
                int sumOfGreaterClasses = 0;

                //visit those below threshold
                for (int j = 0; j < i; j++) {
                    Double thisClassVal = orderline.get(j).getClassVal();
                    int storedTotal = lessClasses.get(thisClassVal);
                    storedTotal++;
                    lessClasses.put(thisClassVal, storedTotal);
                    sumOfLessClasses++;
                }

                //visit those above threshold
                for (int j = i; j < orderline.size(); j++) {
                    Double thisClassVal = orderline.get(j).getClassVal();
                    int storedTotal = greaterClasses.get(thisClassVal);
                    storedTotal++;
                    greaterClasses.put(thisClassVal, storedTotal);
                    sumOfGreaterClasses++;
                }

                int sumOfAllClasses = sumOfLessClasses + sumOfGreaterClasses;

                Double parentEntropy = entropy(classDistribution);

                // calculate the info gain below the threshold
                Double lessFrac = Double.parseDouble(sumOfLessClasses / sumOfAllClasses + "");
                Double entropyLess = entropy(lessClasses);
                // calculate the info gain above the threshold
                Double greaterFrac = Double.parseDouble(sumOfGreaterClasses / sumOfAllClasses + "");
                Double entropyGreater = entropy(greaterClasses);

                Double gain = parentEntropy - lessFrac * entropyLess - greaterFrac * entropyGreater;

                if (gain > bsfGain) {
                    bsfGain = gain;
                    threshold = (thisDist - lastDist) / 2 + lastDist;
                }
            }
            lastDist = thisDist;
        }
        if (bsfGain >= 0) {
            this.informationGain = bsfGain;
            this.splitThreshold = threshold;
            this.separationGap = calculateSeparationGap(orderline, threshold);
        }
    }


    private Double calculateSeparationGap(ArrayList<OrderLineObj> orderline, Double distanceThreshold) {

        Double sumLeft = 0D;
        Double leftSize = 0D;
        Double sumRight = 0D;
        Double rightSize = 0D;

        for (int i = 0; i < orderline.size(); i++) {
            if (orderline.get(i).getDistance() < distanceThreshold) {
                sumLeft += orderline.get(i).getDistance();
                leftSize++;
            } else {
                sumRight += orderline.get(i).getDistance();
                rightSize++;
            }
        }

        Double thisSeparationGap = 1 / rightSize * sumRight - 1 / leftSize * sumLeft; //!!!! they don't divide by 1 in orderLine::minGap(int j)

        if (rightSize == 0 || leftSize == 0) {
            return -1D; // obviously there was no seperation, which is likely to be very rare but i still caused it!
        }                //e.g if all data starts with 0, first s1 length =1, there will be no seperation as all time series are same dist
        // equally true if all data contains the s1 candidate, which is a more realistic example

        return thisSeparationGap;
    }


    // comparison to determine order of shapelets in terms of gain ration then shortness
    // comparison 1: to determine order of shapelets in terms of info gain, then separation gap, then shortness
    public int compareTo(Shapelet s1) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;


        if (this.informationGain != s1.getInformationGain()) {
            if (this.informationGain > s1.getInformationGain()) {
                return BEFORE;
            } else {
                return AFTER;
            }
        } else {// if this.informationGain == s1.informationGain
       /* if(this.separationGap != s1.getGap()){
            if(this.separationGap > s1.getGap()){
                return BEFORE;
            }else{
                return AFTER;
            }
        } else if(this.content.length != s1.getLength()){
            if(this.content.length < s1.getLength()){
                return BEFORE;
            }else{
                return AFTER;
            }
        } else{*/
            return EQUAL;
        }
    }


}
