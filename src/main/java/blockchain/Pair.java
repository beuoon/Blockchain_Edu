package blockchain;

public class Pair<T, K> {
    private T k;
    private K v;

    public Pair(T k, K v) {
        this.k = k;
        this.v = v;
    }

    public T getKey() {
        return k;
    }

    public K getValue() {
        return v;
    }
}
