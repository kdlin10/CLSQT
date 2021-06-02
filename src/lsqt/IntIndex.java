package lsqt;

class IntIndex implements Index<IntIndex> {
    //Simple index for testing - just ints
    private int index;

    IntIndex(int i) {
        index = i;
    }

    @Override
    public int compareTo(IntIndex i) {
        //return (index < i.getRaw())? -1 : ((index == i.getRaw()) ? 0 : 1);
        return Integer.compare(index, i.getRaw());
    }

    @Override
    public String toString() {
        return String.valueOf(index);
    }

    private int getRaw() {
        return index;
    }

    @Override
    public boolean contains(IntIndex i) {
        return (i.getRaw() == index);
    }

    /*
    @Override
    public IntIndex minIndex() {
        return new IntIndex(Integer.MIN_VALUE);
    }

    @Override
    public IntIndex maxIndex() {
        return new IntIndex(Integer.MAX_VALUE);
    }
     */
}
