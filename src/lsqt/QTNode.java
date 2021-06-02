package lsqt;

class QTNode<I extends Index<I>, V> extends Node<I, V> {
    //It's possible for points to overlap and our linear quadtree's resolution is limited, so we have to accommodate multiple points in one quad
    //It might be more correct to call this a skipset?
    private Node<I, V>[] nextNodes; //Nodes will be created and destroyed all the time; probably not worth making an ArrayList
    private V value;
    private I index;


    QTNode(I i, V v, int h) {
        nextNodes = new Node[h+1];
        index = i;
        value = v;
    }
    @Override
    public int compareTo(Node<I, V> n) {
        return index.compareTo(n.getIndex());
    }

    public I getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return index.toString() + "=" + value.toString();
    }

    public int getMaxHeight() {
        return nextNodes.length - 1;
    }

    @Override
    public boolean containsIndex(I i) {
        return index.contains(i);
    }

    public boolean hasNext(int h) {
        return (h < nextNodes.length && nextNodes[h] != null);
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    //If return value isn't used at all, should remove
    public Node setNext(int h, Node n) {
        Node oldNextNode = getNext(h);
        nextNodes[h] = n;
        return oldNextNode;
    }

    public Node getNext(int h) {
        //A low level data structure shouldn't run into this if we check properly at a higher level
        return nextNodes[h];
    }

    public V getValue() {
        return value;
    }
}
