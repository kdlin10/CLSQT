package clsqt;
//Every implementation of Index is comparable to itself and takes itself as a type parameter for compareTo
public interface Index<I extends Index<I>> extends Comparable<I> {
    @Override
    int compareTo(I i);

    @Override
    String toString();

    int minRange(); //Not needed for a skiplist that doesn't use an range-based index... But I couldn't think of a good alternative

    int maxRange();

    boolean contains(I i);

    boolean overlaps(I i);

    boolean isDivisible();
}
