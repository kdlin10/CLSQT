package clsqt;

public class Pair<L, R> {
    private L left;
    private R right;
    Pair(L l, R r) {
        left = l;
        right = r;
    }

    L getL() {
        return left;
    }

    R getR() {
        return right;
    }

    void setL(L l) {
        left = l;
    }

    void setR(R r) {
        right = r;
    }

    @Override
    public String toString() {
        return left.toString() + ", " + right.toString();
    }
}
