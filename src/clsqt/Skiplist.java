package clsqt;

import java.util.ArrayList;
import java.util.Iterator;

public class Skiplist<I extends Index<I>, V> {
    //Our skiplist is different from most implementations in two respects: link direction alternates depending on layer and nodes will map to a range of numbers (disallowing overlaps)
    //Does not support duplicate/overlapping indexes, supports duplicate values
    private int currentMaxHeight; //Ideal in terms of convergence speed is 1 : e ratio between each layer... We'll settle for 1:3
    private int listCeiling = 24; //Should be more than enough - if every 3rd node on a layer gets bumped up, we can fit 3^24 nodes, surpassing max int range
    private int[] heightTracker;
    private int size = 0; //Fix
    private Node<I, V> headNode, tailNode;

    public Skiplist(I head, I tail) {
        headNode = new HeadNode(head);
        tailNode = new TailNode(tail);
        currentMaxHeight = 0;
        heightTracker = new int[listCeiling];
        //Link the two nodes that hold everything in between. Apparently initializing an ArrayList with some capacity doesn't actually make it all accessible so we have to start from 0
        for (int h = 0; h < listCeiling; h++) {
            if (h % 2 == 0) {
                headNode.setNext(h, tailNode);
            } else {
                tailNode.setNext(h, headNode);
            }
        }
    }
    //Add constructor for adding a set of indexed values?
    public V get(I i) {
        Node<I, V> targetNode = findPrecursors(i, 0)[0].getNext(0);
        //Perhaps make an protected unsafeGet to avoid the branching?
        if (targetNode.containsIndex(i)) {
            return targetNode.getValue();
        }
        return null;
    }

    //Given a SORTED list of intervals (1 or more indexes), return values of all nodes within the intervals, without duplicates
    //If intervals exceeds skiplist range, we run out of nodes
    //BUG: Subtracts head node from intervals
    public ArrayList<V> intervalsGet(ArrayList<Pair<I, I>> intervals) {
        ArrayList<V> returnList = new ArrayList<V>();
        Node<I, V> currentNode;
        Iterator<Pair<I, I>> it = intervals.iterator();
        Pair<I, I> interval;
        while (it.hasNext()) {
            interval = it.next();
            currentNode = findPrecursors(interval.getL(), 0)[0];
            //If there's no overlap at all, get next node - guaranteed to hit or overshoot
            if (Integer.compareUnsigned(currentNode.getIndex().maxRange(), interval.getL().minRange()) < 0) {
                currentNode = currentNode.getNext();
            }
            //While the interval start is less than currentNode max... But we don't want to advance the interval without having checked it at all
            while (Integer.compareUnsigned(currentNode.getIndex().maxRange(), interval.getL().minRange()) >= 0) {
                //We want to check this for every overlapping node/interval combo... valid as long as our single value position is the same as the node's
                if (currentNode.compareTo(interval.getL()) >= 0 && currentNode.compareTo(interval.getR()) <= 0) {
                    returnList.add(currentNode.getValue());
                }
                //Assumes sorted intervals and nodes so that we're removing intervals monotonically
                //Wish we had operator overloading or unsigned ints... maybe time to refactor for intervals/head&tail nodes
                if (Integer.compareUnsigned(currentNode.getIndex().maxRange(), interval.getR().maxRange()) >= 0) {
                    it.remove();
                    if (it.hasNext()) {
                        interval = it.next();
                        //If we iterate to last and don't continue the while loop, exits here without checking last interval...
                        if (Integer.compareUnsigned(currentNode.getIndex().maxRange(), interval.getL().minRange()) < 0) {
                            currentNode = findPrecursors(interval.getL(), 0)[0].getNext();
                        }
                    }
                    else {
                        //Out of intervals to search, done!
                        break;
                    }
                }
                //Would not reach here if we failed the while loop condition, there has to be at least some overlap
                else {
                    interval.setL((I) new MortonIndex(currentNode.getIndex().maxRange() + 1));
                    //Here our interval no longer intersects with current node; Overlap or overshoot ok - if next node is short, we start seeking again from the very top
                    //As long as we don't search outside the boundaries, this is always valid
                    currentNode = currentNode.getNext();
                }
            }
        }
        return returnList;
    }

    public boolean put(I i, V v) {
        int h = pickNodeHeight();
        Node[] precursorNodes = findPrecursors(i, h);
        if (precursorNodes[0].getNext(0).containsIndex(i)) {
            //Let's hold off on storing multiple values in a node for now - if we collide, replace with new
            precursorNodes[0].getNext(0).setValue(v);
            return true;
        }
        insertNode(precursorNodes, new QTNode<I, V>(i, v, h));
        size++;
        return true;
    }

    //Should we support removing a value without the index? Means we need to traverse the entire skiplist
    //Node designed to hold more than one object... but for now we treat it as single values
    //It might be useful to return the deleted node instead for the quadtree... but not general usage
    public boolean remove(I i) {
        Node[] precursorNodes = findPrecursors(i, currentMaxHeight);
        Node targetNode = precursorNodes[0].getNext(0);
        //Assumes each node stores only one value...
        if (targetNode.containsIndex(i)) {
            size--;
            return deleteNode(precursorNodes, targetNode);
        }
        return false;
    }

    Node[] findPrecursors(Node<I, V> n) {
        return findPrecursors(n.getIndex(), n.getMaxHeight());
    }

    //Return an array of Nodes present up to level h that would connect to a Node with Index i... but we don't always know what height we want.
    //The default should be the height of the node we're looking for... which of course we don't know until we find the node. Might need to just track starting from currentMaxHeight then truncate down
    //Does the snaking traversal the whole way when we only need the previous nodes at height h and below
    //Our sole method of traversal right now, but keeps going even when it encounters our target... ought to split one out
    Node[] findPrecursors(I i, int h) {
        //If we're inserting a node that would be a new max height...
        int height = Math.max(currentMaxHeight, h);
        int alternator = (1 - 2 * (height % 2)); //1 on even heights, -1 on odd
        Node current = (alternator == 1)? headNode : tailNode;
        Node next;
        Node[] precursorNodes = new Node[h+1];
        //Since the link direction alternates on each level, the comparison is reversed
        while (height >= 0) {
            //Guaranteed to terminate when index is in between where current and next nodes are *centered*... but index may fall inside the bounds of current node!
            //Not an issue when limited to one value per node, but for search interval pruning...
            while (current.compareTo(i) * alternator < 0 && current.hasNext(height)) {
                next = current.getNext(height);
                if (next.compareTo(i) * alternator >= 0) {
                    if (height <= h) {
                        precursorNodes[height] = current;
                    }
                    //In the case we do have a Node with Index i, we need to go past it or else we start the next level already past our target
                    //Should never increment past our head/tail, as they never contain anything; in the most extreme case where our last Node contains the index, we increment past it into the head/tail node
                    if (next.containsIndex(i)) {
                        current = next;
                    }
                }
                current = current.getNext(height);
            }
            height--;
            alternator = (1 - 2 * (height % 2));
        }
        return precursorNodes;
    }

    //Is there a point to returning true?
    //This works fine for inserting single nodes, but what if we want to attach a block of them? We could stitch nodes together and use a pseudoblock to connect
    protected boolean insertNode(Node[] attachNodes, Node<I, V> n) {
        for (int i = 0; i < attachNodes.length; i++) {
            n.setNext(i, attachNodes[i].getNext(i));
            attachNodes[i].setNext(i, n);
        }
        insertHeightUpdate(n.getMaxHeight());
        return true;
    }

    //Do we need some sort of sanity check to prevent deleting Nodes that aren't in the skiplist?
    protected boolean deleteNode(Node[] detachNodes, Node<I, V> n) {
        for (int i = 0; i <= n.getMaxHeight(); i++) {
            //Potential for out of bounds if we pass a malformed array
            detachNodes[i].setNext(i, n.getNext(i));
        }
        deleteHeightUpdate(n.getMaxHeight());
        return true;
    }

    protected int pickNodeHeight() {
        //Aims for 1:3 ratio between each layer based on current situation. Picks the highest level that wouldn't violate the 3:1 ratio among lower levels
        //Does nothing for how evenly they are distributed across the range of our index. On the average this is fine, but it could rarely be an issue
        //Making height adjustments of +/-1 is relatively inexpensive... but that would mean variable Node height
        //If we want an even global distribution, we'll also need to track global state... also only works if we add multiple nodes at once to allow trading the different heights around
        int h = 0;
        while(h <= currentMaxHeight && heightTracker[h] >= 2) {
            h++;
        }
        return h;
    }

    private void insertHeightUpdate(int h) {
        if (h > currentMaxHeight) {
            currentMaxHeight = h;
        }
        heightTracker[h]++;
        while (h > 0) {
            h--;
            heightTracker[h] -= 2;
        }
    }

    private void deleteHeightUpdate(int h) {
        if (h == currentMaxHeight && heightTracker[h] == 1 && currentMaxHeight > 0) {
            currentMaxHeight--;
        }
        heightTracker[h]--;
        while (h > 0) {
            h--;
            heightTracker[h] +=2;
        }
    }

    public String toString() {
        Node current = headNode;
        StringBuilder stringBuilder = new StringBuilder();
        while (current.hasNext(0)) {
            stringBuilder.append(current.toString() + ", ");
            current = current.getNext(0);
        }
        stringBuilder.append(current.toString());
        return stringBuilder.toString();
    }

    void printlist() {
        Node current = headNode;
        StringBuilder stringBuilder = new StringBuilder();
        int h = currentMaxHeight;
        int c = 0;
        //Simple but not very efficient, as we traverse the bottom for each height we want to print instead of doing it all at once
        while (h >= 0) {
            while (current.hasNext(0)) {
                current = current.getNext(0);
                if (current.getMaxHeight() >= h && current.isEmpty() == false) {
                    c++;
                    stringBuilder.append(" " + current.getValue().toString() + "");
                }
                else {
                    stringBuilder.append("  ");
                }
            }
            stringBuilder.append("Sum: " + c + "\n");
            h--;
            c = 0;
            current = headNode;
        }
        System.out.print(stringBuilder.toString());
    }

    class HeadNode extends Node<I, V> {
        //One of the entry/end nodes as we navigate the skiplist. We should probably make all this non-public
        ArrayList<Node> nextNodes; //Started with ArrayList thinking we'd resize/populate as needed, but perhaps should be just an array
        I headIndex;
        HeadNode(I head) {
            headIndex = head;
            nextNodes = new ArrayList<>(listCeiling);
            for (int i = 0; i < listCeiling; i++) {
                nextNodes.add(i, null);
            }
        }
        @Override
        public int getMaxHeight() {
            return currentMaxHeight;
        }

        @Override
        boolean overlapsIndex(I i) {
            return (i.minRange() < headIndex.maxRange());
        }

        @Override
        public boolean containsIndex(Index i) {
            return false;
        }

        @Override
        public boolean hasNext(int h) {
            return h < listCeiling && (h % 2 == 0);
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Node setNext(int h, Node n) {
            Node prevNextNode = getNext(h);
            nextNodes.set(h, n);
            return prevNextNode;
        }

        @Override
        public Node getNext(int h) {
            return nextNodes.get(h);
        }

        @Override
        public Node getNext() {
            return nextNodes.get(0);
        }

        //Would be nice if there was a way to do this without casting
        @Override
        public I getIndex() {
            return (I) headIndex;
        }

        @Override
        public String toString() {
            return "{";
        }

        @Override
        public V getValue() {
            return null;
        }

        @Override
        public V setValue(V v) {
            return null;
        }

        @Override
        public int compareTo(Node n) {
            return -1;
        }

        @Override
        public int compareTo(I i) {
            return -1;
        }
    }

    class TailNode extends Node<I, V> {
        //The other entry/end node
        ArrayList<Node> nextNodes;
        //TailIndex tailIndex = new TailIndex();
        I tailIndex;
        TailNode(I tail) {
            tailIndex = tail;
            nextNodes = new ArrayList<>(listCeiling);
            for (int i = 0; i < listCeiling; i++) {
                nextNodes.add(i, null);
            }
        }

        @Override
        public int compareTo(Node n) {
            return 1;
        }

        @Override
        public int compareTo(I i) {
            return 1;
        }

        @Override
        public int getMaxHeight() {
            return currentMaxHeight;
        }

        @Override
        boolean overlapsIndex(I i) {
            return (i.maxRange() > tailIndex.minRange());
        }

        @Override
        public boolean containsIndex(Index i) {
            return false;
        }

        @Override
        public boolean hasNext(int h) {
            return h < listCeiling && (h % 2 == 1);
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Node setNext(int h, Node n) {
            Node prevNextNode = getNext(h);
            nextNodes.set(h, n);
            return prevNextNode;
        }

        @Override
        public Node getNext(int h) {
            return nextNodes.get(h);
        }

        @Override
        public Node getNext() {
            return nextNodes.get(0);
        }

        @Override
        public I getIndex() {
            return (I) tailIndex;
        }

        @Override
        public String toString() {
            //return Integer.MAX_VALUE + "=TAIL";
            return "}";
        }

        @Override
        public V getValue() {
            return null;
        }

        @Override
        V setValue(V v) {
            return null;
        }
    }

    //The index for our head/tail implements Index<I>, meaning it can be compared to whatever implementation of Index used to construct the skiplist
    //...actually can't be compared to, which we sidestep by having not doing it
    class HeadIndex implements Index<I> {
        @Override
        public int compareTo(Index i) {
            return -1;
        }

        @Override
        public boolean contains(Index i) {
            return false;
        }

        @Override
        public boolean overlaps(Index i) {
            return false;
        }

        @Override
        public int minRange() {
            return 0;
        }

        @Override
        public int maxRange() {
            return 0;
        }

        @Override
        public boolean isDivisible() {
            return false;
        }
    }

    class TailIndex implements Index<I> {
        @Override
        public int compareTo(Index i) {
            return 1;
        }

        @Override
        public boolean contains(Index i) {
            return false;
        }

        @Override
        public boolean overlaps(Index i) {
            return false;
        }

        @Override
        public int minRange() {
            return 0xFFFFFFFF;
        }

        @Override
        public int maxRange() {
            return 0xFFFFFFFF;
        }

        @Override
        public boolean isDivisible() {
            return false;
        }
    }
}
