package evaluator;


public final class OrderLineObj implements Comparable<OrderLineObj> {

    private Double distance;
    private Double classVal;

    /**
     * Constructor to build an orderline object with a given distance and class value
     *
     * @param distance distance from the obj to the shapelet that is being assessed
     * @param classVal the class value of the object that is represented by this OrderLineObj
     */
    public OrderLineObj(Double distance, Double classVal) {
        this.distance = distance;
        this.classVal = classVal;
    }

    public Double getDistance() {
        return this.distance;
    }

    public Double getClassVal() {
        return this.classVal;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public void setClassVal(Double classVal) {
        this.classVal = classVal;
    }

    /**
     * Comparator for two OrderLineObj objects, used when sorting an orderline
     *
     * @param o the comparison OrderLineObj
     * @return the order of this compared to o: -1 if less, 0 if even, and 1 if greater.
     */
    @Override
    public int compareTo(OrderLineObj o) {
        if (o.distance > this.distance) {
            return -1;
        } else if (o.distance == this.distance) {
            return 0;
        }
        return 1;
    }
}

