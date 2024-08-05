package utils;


public class Tuple2<T, M> {
    private T first;
    private M second;

    public Tuple2(T first, M second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public M getSecond() {
        return second;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public void setSecond(M second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s)", first, second);
    }

    @Override
    public int hashCode() {
        return this.first.hashCode() + this.second.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean f;
        if (obj instanceof Tuple2) {
            try {
                Tuple2<T, M> x = (Tuple2<T, M>) obj;
                f = x.getFirst().equals(this.first) && x.getSecond().equals(this.second);
            } catch (Exception e) {
                f = false;
            }
        } else {
            f = false;
        }
        return f;
    }
}
