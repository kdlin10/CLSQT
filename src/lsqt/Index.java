package lsqt;
//Every implementation of Index is comparable to itself and takes itself as a type parameter for compareTo
public interface Index<I extends Index<I>> extends Comparable<I> {
    @Override
    int compareTo(I i);

    @Override
    String toString();

    boolean contains(I i);
}
