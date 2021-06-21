package clsqt;

public class Point implements Cartesian {
    private int x, y;

    Point(int newX, int newY) {
        x = newX;
        y = newY;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}
