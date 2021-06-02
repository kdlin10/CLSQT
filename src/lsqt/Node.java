package lsqt;

//We don't *need* all our values to be the same class, but it would make life easier for the user, right?
//Could make life harder if some are singles and some are bins... could default to returning an array/collection of V
abstract class Node<I extends Index<I>, V> implements Comparable<Node<I, V>> {
    @Override
    abstract public int compareTo(Node<I, V> n);
    abstract V getValue();
    abstract int getMaxHeight();
    abstract boolean containsIndex(I i);
    abstract boolean hasNext(int h);
    abstract boolean isEmpty();
    abstract Node setNext(int h, Node<I, V> n); //Return the previous next node? Potential shortcut but unused thus far
    abstract Node getNext(int h);
    abstract I getIndex();
    abstract public String toString();
}
