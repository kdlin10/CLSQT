package clsqt;

//Hmm, we're already inherently limiting ourselves to 32 bits since Cartesian returns ints
public class Quadtree<V extends Cartesian> {
    Skiplist<MortonIndex, Cartesian> skiplist;
    int maxDim; //The largest dimension - must be a power of two
    int maxRes; //
    Quadtree() {
        //How do we handle max size/resolution? We can simply set a maximum, truncate extra bits of resolution, or compress the range
        maxRes = 16;
        maxDim = 1024;
        //This means our quadtree actually starts with one point at 0, 0... so when we add the first point, it can't expand to whole range available
        skiplist = new Skiplist<>(new MortonIndex(0, 0, 0), new MortonIndex(maxDim, maxDim, 0));
    }

    public boolean add(Cartesian c) {
        MortonIndex addIndex = new MortonIndex(c, 0);
        int newHeight = skiplist.pickNodeHeight();
        int newRes;
        Node<MortonIndex, Cartesian>[] precursorNodes = skiplist.findPrecursors(addIndex, newHeight);
        Node currentNode = precursorNodes[0];
        MortonIndex currentIndex = (MortonIndex) currentNode.getIndex();
        Node nextNode = currentNode.getNext(0);
        MortonIndex nextIndex = (MortonIndex) nextNode.getIndex(); //Feels like a code smell
        //Our addition should always be in between the precursor and next; it may overlap with either, or it might just fill empty space
        if (currentNode.containsIndex(addIndex)) {
            if (!currentIndex.isDivisible() || currentIndex.compareTo(addIndex) == 0) {
                //Where we're adding can't be subdivided or is exactly occupied - right now, we replace with new... Refector setValue to addValue later?
                currentNode.setValue(c);
                return true;
            }
            //We need to split the existing node to accommodate the new addition
            else {
                newRes = currentIndex.getSplitSize(addIndex);
                currentIndex.setRes(newRes);
            }
        }
        else if (nextNode.containsIndex(addIndex)) {
            if (!nextIndex.isDivisible() || nextIndex.compareTo(addIndex) == 0) {
                nextNode.setValue(c);
                return true;
            }
            else {
                newRes = nextIndex.getSplitSize(addIndex);
                nextIndex.setRes(newRes);
            }
        }
        //Adding new node to empty space
        else {
            //Find the largest resolution that fits between the two nodes without overlap
            //We should delegate testing to the nodes, or else we need a real index for the head/tail
            newRes = MortonIndex.getMaxDiffPowOfTwo(currentIndex.maxRange(), nextIndex.minRange());
            addIndex.setRes(newRes);
            //While there's an overlap, reduce resolution
            while (currentNode.overlapsIndex(addIndex) || nextNode.overlapsIndex(addIndex)) {
                addIndex.setRes(--newRes);
            }
        }
        addIndex.setRes(newRes);
        return skiplist.insertNode(precursorNodes, new QTNode<>(addIndex, c, newHeight));
    }

    //If we remove a node, we want to check for nodes in the same quad and upsize them if there's only one left... meaning we need to find what quad/resolution our Cartesian belongs in first
    //This means we need to look both directions, unless we're at the start or end
    public boolean remove(Cartesian c) {
        MortonIndex cartesianIndex = new MortonIndex(c, 0);
        Node<MortonIndex, Cartesian>[] precursorNodes = skiplist.findPrecursors(cartesianIndex, 0);
        Node<MortonIndex, Cartesian> removeNode = precursorNodes[0].getNext(0);
        MortonIndex removeIndex = (MortonIndex) removeNode.getIndex();
        if (removeNode.containsIndex(cartesianIndex)) {
            skiplist.remove(removeIndex);
            tryExpand(removeIndex);
            return true;
        }
        return false;
    }

    //Looks at a quad specified by i; if there's only one node left, expand it to fill the space, then repeat
    private void tryExpand(MortonIndex i) {
        MortonIndex parentQuadIndex = new MortonIndex(i.getParentStartLoc(), i.getRes() + 1);
        Node<MortonIndex, Cartesian> current = skiplist.findPrecursors(parentQuadIndex, 0)[0].getNext(0);
        int quadCount = 0;
        while (parentQuadIndex.contains(current.getIndex()) && current.hasNext(0)) {
            current = current.getNext(0);
            quadCount++;
        }
        //If we have only one node left, need to prevent it exploding beyond our bounds
        if (quadCount == 1 && 0x0000002 << (current.getIndex().getRes() * 2) <= maxDim) {
            current.getIndex().expand();
            tryExpand(current.getIndex());
        }
    }

    //What does update entail? It means our Cartesian has left its current boundaries and we need to restructure
    //Or should we just have a method to check everything contained in the quadtree and restructure as appropriate?
    /*
    public boolean update(Cartesian c) {

    }

    public V get(Cartesian c) {

    }
     */
/*
    public rectSearch(Cartesian c1, Cartesian c2, Predicate filter) {

    }

    public rectSearch(int x1, int y1, int x2, int y2) {

    }

    public nearestNeighbors(Cartesian c, int k) {

    }
 */
}
