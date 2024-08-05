package instructor;

/**
 * 
 * The type of aggregated classification is List<Instruction>
 */
public class Instruction {
    //    private long timeStamp;
    private int timeSeriesId;
    private Double percentage;
    private String label;

    public Instruction() {

    }

    public Instruction(String label, int timeSeriesId, Double percentage) {
//        this.timeStamp = Calendar.getInstance().getTimeInMillis();
        this.label = label;
        this.timeSeriesId = timeSeriesId;
        this.percentage = percentage;
    }

//    public long getTimeStamp() {
//        return timeStamp;
//    }

    public void setInstructionStr(String s) {
        String[] s1 = s.split("_");
        this.label = s1[0];
        this.timeSeriesId = Integer.parseInt(s1[1].split(":")[0]);
        this.percentage = Double.parseDouble(s1[1].split(":")[1]);
    }

    public int getTimeSeriesId() {
        return timeSeriesId;
    }

    public void setTimeSeriesId(int timeSeriesId) {
        this.timeSeriesId = timeSeriesId;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

//    @Override
//    public String toString() {
//        return "ElasticModel.State{" +
//                "timeSeriesId=" + timeSeriesId +
//                ", percentage=" + percentage +
//                ", label='" + label + '\'' +
//                '}';
//    }

    @Override
    public String toString() {
        return label + "_" + timeSeriesId + ":" + percentage;
    }
}
