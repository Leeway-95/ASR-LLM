
package instructor;


public class Sequence {
    public Double[] s;
    public int sClass;
    public int shapletId;
    public Double shapletSimilar;
    public int sId;
    public int windowId;
    public Double[] sumX;
    public Double[] sumX2;
    public int startPos;

    public Sequence(Double[] timeSeries, int sId, int windowId, int classLabel, int shapletId, Double shapletSimilar) {
        this.sClass = classLabel;
        this.shapletId = shapletId;
        this.shapletSimilar = shapletSimilar;
        this.sId = sId;
        this.windowId = windowId;
        this.s = timeSeries;
        this.sumX = new Double[timeSeries.length];
        this.sumX2 = new Double[timeSeries.length];
        Double val;
        for (int i = 0; i < timeSeries.length; i++) {
            val = timeSeries[i];
            this.s[i] = val;
            if (i == 0) {
                this.sumX[i] = val;
                this.sumX2[i] = val * val;
            } else {
                this.sumX[i] = this.sumX[i - 1] + val;
                this.sumX2[i] = this.sumX2[i - 1] + val * val;
            }
        }
    }

    public Sequence(Double[] timeSeries, int sId, int windowId, int classLabel) {
        this.sClass = classLabel;
        this.sId = sId;
        this.windowId = windowId;
        this.s = new Double[timeSeries.length];
        this.sumX = new Double[timeSeries.length];
        this.sumX2 = new Double[timeSeries.length];
        Double val;
        for (int i = 0; i < timeSeries.length; i++) {
            val = timeSeries[i];
            this.s[i] = val;
            if (i == 0) {
                this.sumX[i] = val;
                this.sumX2[i] = val * val;
            } else {
                this.sumX[i] = this.sumX[i - 1] + val;
                this.sumX2[i] = this.sumX2[i - 1] + val * val;
            }
        }
    }

    public Sequence(Sequence s, int start, int end) {
        this.s = new Double[end - start];
        this.sumX = new Double[end - start];
        this.sumX2 = new Double[end - start];
        Double val;
        for (int indSrc = start, indDest = 0; indSrc < end; indSrc++, indDest++) {
            val = s.get(indSrc);
            this.s[indDest] = val;
            if (indDest == 0) {
                this.sumX[indDest] = val;
                this.sumX2[indDest] = val * val;
            } else {
                this.sumX[indDest] = this.sumX[indDest - 1] + val;
                this.sumX2[indDest] = this.sumX2[indDest - 1] + val * val;
            }
        }
    }

    public void setLabel(int classLabel) {
        this.sClass = classLabel;
    }

    public int getLabel() {
        return this.sClass;
    }

    public Double[] getS() {
        return s;
    }

    public void setS(Double[] s) {
        this.s = s;
    }

    public int getSClass() {
        return sClass;
    }

    public void setSClass(int sClass) {
        this.sClass = sClass;
    }

    public Double getShapletSimilar() {
        return shapletSimilar;
    }

    public void setShapletSimilar(Double shapletSimilar) {
        this.shapletSimilar = shapletSimilar;
    }

    public int getShapletId() {
        return shapletId;
    }

    public void setShapletId(int shapletId) {
        this.shapletId = shapletId;
    }


    public int getTimeSeriesId() {
        return sId;
    }

    public void setTimeSeriesId(int sId) {
        this.sId = sId;
    }

    public int getTimeWindowId() {
        return windowId;
    }

    public void setTimeWindowId(int windowId) {
        this.windowId = windowId;
    }

    public Double[] getSumX() {
        return sumX;
    }

    public void setSumX(Double[] sumX) {
        this.sumX = sumX;
    }

    public Double[] getSumX2() {
        return sumX2;
    }

    public void setSumX2(Double[] sumX2) {
        this.sumX2 = sumX2;
    }

    public Double get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index can not be negative");
        } else if (index > this.s.length) {
            throw new IndexOutOfBoundsException("The index is larger than the TimeSeries length");
        } else {
            return this.s[index];
        }
    }

    public int size() {
        return this.s.length;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (this.shapletId > 0) {
            s.append("ShapletId: " + this.shapletId + "\tClass: " + this.sClass + "\tSequences: ");
        } else {
            s.append("SequenceId: " + this.sId + "\tWindowId: " + this.windowId + "\tClass: " + this.sClass + "\tSequences: ");
        }
        for (int i = 0; i < this.s.length; i++) {
            s.append(String.format("%10.4f ", this.s[i]));
        }
        return s.toString();
    }

//    public Double[] getS() {
//        return this.s;
//    }

    public Double mean(int i, int len) {
        return (s[i] + sumX[i + len - 1] - sumX[i]) / len;
    }

    public Double stdv(int i, int len) {
        Double mu = mean(i, len);
        Double s2 = ((s[i] * s[i] + sumX2[i + len - 1] - sumX2[i]) / len)
                - mu * mu;
        if (s2 <= 0)
            return 0D;
        else
            return Math.sqrt(s2);
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getStartPos() {
        return this.startPos;
    }
}
