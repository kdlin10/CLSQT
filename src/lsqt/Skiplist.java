package lsqt;

import java.util.ArrayList;

public class Skiplist<I extends Index<I>, V> {
    //Our skiplist is different from most implementations in two respects: link direction alternates depending on layer and nodes will map to a range of numbers (disallowing overlaps)
    private int currentMaxHeight; //Ideal in terms of convergence speed is 1 : e ratio between each layer... We'll settle for 1:3
    private int listCeiling = 24; //Should be more than enough - if every 3rd node on a layer gets bumped up, we can fit 3^24 nodes, surpassing max int range
    private int[] heightTracker;
    private int size = 0;
    private Node<I, V> headNode, tailNode;

    public Skiplist() {
        headNode = new HeadNode();
        tailNode = new TailNode();
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
        Node<I, V> targetNode = findNode(i);
        return targetNode.getValue();
    }


    public boolean add(I i, V v) {
        int h = pickNodeHeight();
        insertNode(new QTNode<>(i, v, h));
        size++;
        return true;
    }

    public boolean remove(I i) {
        size--;
        return deleteNode(i);
    }

    Node<I, V> findNode(I i) {
        //Check if we've gone past the index - if we haven't, keep going. If we have, check if current node is a hit. If it isn't, we need to drop down a level and keep searching.
        //When there is nothing left to traverse on height 0 and we still haven't found the target, terminate and return null
        //Since the link direction alternates on each level, the comparison is reversed
        int height = currentMaxHeight;
        int alternator = (1 - 2 * (height % 2)); //1 on even heights, -1 on odd
        Node current = (alternator == 1)? headNode : tailNode;
        while (height >= 0) {
            //Loop until we're past i or hit the boundaries
            //This way our head/tail node always returns as desired, instead of potential collision with extreme value
            while (current.getIndex().compareTo(i) * alternator < 0 && current.hasNext(height)) {
                current = current.getNext(height);
            }
            if (current.containsIndex(i)) {
                return current;
            }
            height--;
            alternator = (1 - 2 * (height % 2));
        }
        return null;
    }

    //When would this fail? If we try inserting a node where one already exists? How would we know the available space before we traverse and attempt to add the node?
    //Stuck at max height of 2, starts search at node height instead of skiplist max height
    private boolean insertNode(Node n) {
        int height = Math.max(currentMaxHeight, n.getMaxHeight());
        Index i = n.getIndex();
        int alternator = (1 - 2 * (height % 2));
        Node current = (alternator == 1)? headNode : tailNode;
        Node next;
        while (height >= 0) {
            //Loop until i is greater or hit the boundaries
            while (current.getIndex().compareTo(i) * alternator < 0 && current.hasNext(height)) {
                next = current.getNext(height);
                //The next node is greater than n's index - slide our Node n right between the two
                //If we moved this into the while loop, we'd lose the reference for the previous node that links to n
                if (next.getIndex().compareTo(i) * alternator > 0 && height <= n.getMaxHeight()) {
                    n.setNext(height, next);
                    current.setNext(height, n);
                }
                current = next;
            }
            height--;
            alternator = (1 - 2 * (height % 2));
        }
        insertHeightUpdate(n.getMaxHeight());
        return true;
    }

    private boolean deleteNode(I i) {
        int height = currentMaxHeight;
        int deleteHeight = 0;
        int alternator = (1 - 2 * (height % 2));
        Node current = (alternator == 1)? headNode : tailNode;
        Node next;
        while (height >= 0) {
            //Loop until we're past i or hit the boundaries
            while (current.getIndex().compareTo(i) * alternator < 0 && current.hasNext(height)) {
                next = current.getNext(height);
                //Deletes any node that i fits into
                //When does this get used by quadtree? When we have an empty node to remove, any time else?
                if (next.getIndex().contains(i) == true) {
                    deleteHeight = next.getMaxHeight();
                    current.setNext(height, next.getNext(height));
                }
                current = current.getNext(height);
            }
            height--;
            alternator = (1 - 2 * (height % 2));
        }
        deleteHeightUpdate(deleteHeight);
        //Hmm... we have no condition for returning false
        return true;
    }

    private boolean deleteNode(Node<I, V> n) {
        return deleteNode(n.getIndex());
    }

    private int pickNodeHeight() {
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
        //If we just removed the sole highest node, adjust our max height tracker down
        if (h == currentMaxHeight && heightTracker[h] == 1) {
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

    class HeadNode extends Node {
        //One of the entry/end nodes as we navigate the skiplist. We should probably make all this non-public
        ArrayList<Node> nextNodes; //Started with ArrayList thinking we'd resize/populate as needed, but perhaps should be just an array
        HeadIndex headIndex = new HeadIndex();
        HeadNode() {
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
        public int compareTo(Node n) {
            return -1;
        }
    }

    class TailNode extends Node {
        //The other entry/end node
        ArrayList<Node> nextNodes;
        TailIndex tailIndex = new TailIndex();
        TailNode() {
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
        public int getMaxHeight() {
            return currentMaxHeight;
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
    }

    //The index for our head/tail implements Index<I>, meaning it can be compared to whatever implementation of Index used to construct the skiplist
    class HeadIndex implements Index<I> {
        @Override
        public int compareTo(Index i) {
            return -1;
        }

        @Override
        public boolean contains(Index i) {
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
    }
}
