
package instructor;

import utils.TimeSeries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


public class SequenceDataset {
    public ArrayList<Sequence> dataset;
    public ArrayList<TimeSeries> TimeSeriesDataset;
    public HashMap<Integer, Integer> instsPerClass;
    public boolean isClassHistUpdated;
    public ArrayList<Double> weights;
    public boolean useWeights;
    public int iterativeTimes;
    public int treeId;
    public String trainsetName;


    public SequenceDataset() {
        this.dataset = new ArrayList<Sequence>();
        this.instsPerClass = new HashMap<Integer, Integer>();
        this.isClassHistUpdated = true;
        this.weights = new ArrayList<>();
        this.useWeights = false;
    }

    public SequenceDataset(ArrayList<Sequence> dataset) {
        this.dataset = dataset;
        this.instsPerClass = new HashMap<Integer, Integer>();
        this.updateClassHist();
    }

    public Sequence get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index can not be negative");
        } else if (index > this.dataset.size()) {
            throw new IndexOutOfBoundsException("The index is larger than the TimeSeries objects in dataset");
        } else {
            return this.dataset.get(index);
        }
    }

    public void add(Sequence t) {
        this.dataset.add(t);
        this.isClassHistUpdated = false;
    }

    public void add(ArrayList<Sequence> insts) {
        for (Sequence t : insts) {
            this.add(t);
        }
        this.isClassHistUpdated = false;
    }

    public int size() {
        return this.dataset.size();
    }

    public Double entropy() {
        Double frac = 0D, entropy = 0D;
        int N = this.dataset.size();
        if (!this.isClassHistUpdated) {
            this.updateClassHist();
        }

        for (Integer cls : this.getAllClasses()) {
            if (this.useWeights) {
                frac = 0D;
                for (int i = 0; i < N; i++) {
                    if (this.dataset.get(i).getLabel() == cls) {
                        frac += this.weights.get(i);
                    }
                }
            } else {
                frac = Double.parseDouble(this.instsPerClass.get(cls) + "") / N;
            }
            entropy += (frac > 0.0) ? -1 * (frac) * Math.log(frac) : 0.0;
        }
        return entropy;
    }

    private void updateClassHist() {
        int currCount;
        Integer clsLabel;
        for (Sequence t : this.dataset) {
            clsLabel = t.getLabel();
            currCount = this.instsPerClass.getOrDefault(clsLabel, 0);
            this.instsPerClass.put(t.getLabel(), currCount + 1);
        }
        this.isClassHistUpdated = true;
    }

    public Set<Integer> getAllClasses() {
        return this.instsPerClass.keySet();
    }

    public int getNumOfClasses() {
        return this.instsPerClass.keySet().size();
    }

    public int getInstCount(int instClass) {
        return this.instsPerClass.getOrDefault(instClass, 0);
    }

    public HashMap<Integer, Integer> getClassHist() {
        if (!this.isClassHistUpdated) {
            this.updateClassHist();
        }
        return this.instsPerClass;
    }

    public void setWeights(ArrayList<Double> weights) {
        this.weights = weights;
        this.useWeights = true;
    }

    public Double getWeight(int index) {
        return this.weights.get(index);
    }

    public boolean isUsingWeights() {
        return this.useWeights;
    }

    public ArrayList<Sequence> getDataset() {
        return dataset;
    }

    public void setDataset(ArrayList<Sequence> dataset) {
        this.dataset = dataset;
    }

    public int getIterativeTimes() {
        return iterativeTimes;
    }

    public void setIterativeTimes(int iterativeTimes) {
        this.iterativeTimes = iterativeTimes;
    }

    public int getTreeId() {
        return treeId;
    }

    public void setTreeId(int treeId) {
        this.treeId = treeId;
    }

    public String getTrainsetName() {
        return trainsetName;
    }

    public void setTrainsetName(String trainsetName) {
        this.trainsetName = trainsetName;
    }

    public ArrayList<TimeSeries> getTimeSeriesDataset() {
        return TimeSeriesDataset;
    }

    public void setTimeSeriesDataset(ArrayList<TimeSeries> timeSeriesDataset) {
        TimeSeriesDataset = timeSeriesDataset;
    }
}
