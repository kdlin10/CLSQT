package clsqt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

//Hmm, we're already inherently limiting ourselves to 32 bits since Cartesian returns ints
public class Quadtree<V extends Cartesian> {
    Skiplist<MortonIndex, V> skiplist;
    int maxDim; //The largest dimension - must be a power of two and all coordinates must be less than this
    int maxRes;
    Quadtree(int powTwo) throws Exception {
        //How do we handle max size/resolution? We can simply set a maximum, truncate extra bits of resolution, or compress the range
        if (powTwo > 0 && powTwo < 17) {
            maxRes = powTwo;
            maxDim = (0x00000001 << powTwo) - 1;
            skiplist = new Skiplist<>(new MortonIndex(0, 0, 0), new MortonIndex(maxDim, maxDim, 0));
        }
        else throw new Exception();
    }

    public boolean add(V c) {
        MortonIndex addIndex = new MortonIndex(c, 0);
        int newHeight = skiplist.pickNodeHeight();
        int newRes;
        Node<MortonIndex, Cartesian>[] precursorNodes = skiplist.findPrecursors(addIndex, newHeight);
        Node currentNode = precursorNodes[0];
        MortonIndex currentIndex = (MortonIndex) currentNode.getIndex();
        Node nextNode = currentNode.getNext(0);
        MortonIndex nextIndex = (MortonIndex) nextNode.getIndex(); //Feels like a code smell
        //Our addition should always be in between the precursor and next; it may be filling space already occupied by either, or it might just fill empty space
        if (currentNode.containsIndex(addIndex)) {
            //Tempting to move this down, but we should retain quadtree logic here
            if (!currentIndex.isDivisible() || currentIndex.compareTo(addIndex) == 0) {
                //Where we're adding can't be subdivided or is exactly occupied - right now, we replace with new... Refector setValue to addValue later?
                currentNode.setValue(c);
                return true;
            }
            //We need to split the existing node to accommodate the new addition
            else {
                newRes = currentIndex.getSplitSize(addIndex);
                currentIndex.setRes(newRes);
                addIndex.setRes(newRes);
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
                addIndex.setRes(newRes);
            }
        }
        //Adding new node to empty space
        else {
            //Find the largest resolution that fits between the two nodes without overlap while aligning with quad structure
            //We should delegate testing to the nodes, or else we need a real index for the head/tail
            newRes = MortonIndex.getMaxDiffPowOfTwo(currentIndex.maxRange(), nextIndex.minRange());
            addIndex.setRes(newRes);
            while (currentNode.overlapsIndex(addIndex) || nextNode.overlapsIndex(addIndex)) {
                addIndex.setRes(--newRes);
            }
        }
        return skiplist.insertNode(precursorNodes, new QTNode<MortonIndex, V>(addIndex, c, newHeight));
    }

    //If we remove a node, we want to check for nodes in the same quad and upsize them if there's only one left... meaning we need to find what quad/resolution our Cartesian belongs in first
    //This means we need to look both directions, unless we're at the start or end
    //Needs to be synchronized with skiplist - avoid repeating action, actually checking contents
    public boolean remove(V c) {
        MortonIndex cartesianIndex = new MortonIndex(c, 0);
        Node<MortonIndex, Cartesian>[] precursorNodes = skiplist.findPrecursors(cartesianIndex, 0);
        Node<MortonIndex, Cartesian> removeNode = precursorNodes[0].getNext();
        MortonIndex removeIndex = (MortonIndex) removeNode.getIndex();
        if (removeNode.containsIndex(cartesianIndex)) {
            skiplist.remove(removeIndex);
            tryExpand(removeIndex);
            return true;
        }
        return false;
    }

    //Checks for coquads specified by i; if there's only one node left, expand it to fill the space, then repeat
    //Doing this recursively instead of the procedure to size a new quad for empty space because we don't know if there's anything to expand or the neighbors
    private void tryExpand(MortonIndex i) {
        MortonIndex parentQuadIndex = new MortonIndex(i.getParentStartLoc(), i.getRes() + 1 > maxRes? maxRes : i.getRes() + 1);
        Node<MortonIndex, Cartesian> current = skiplist.findPrecursors(parentQuadIndex, 0)[0];
        int quadCount = 0;
        //Very hacky, we should just stop treating it as an actual node with an index...
        while (current.hasNext(0) && /* parentQuadIndex.contains(current.getNext().getIndex()) */ current.getNext().compareTo(new MortonIndex(parentQuadIndex.maxRange(), 0)) <= 0) {
            current = current.getNext();
            quadCount++;
        }
        //If we have only one node left within the quad, expand unless it would go out of bounds
        if (quadCount == 1 && current.getIndex().getRes() + 1 <= maxRes) {
            current.getIndex().expand();
            tryExpand(current.getIndex());
        }
    }

    public ArrayList<V> rectSearch(int x1, int y1, int x2, int y2) {
        return rectSearch(x1, y1, x2, y2, c -> true);
    }

    public ArrayList<V> rectSearch(int x1, int y1, int x2, int y2, Predicate<Cartesian> filter) {
        int xMin, xMax, yMin, yMax;
        xMin = Math.min(x1, x2);
        xMax = Math.max(x1, x2);
        yMin = Math.min(y1, y2);
        yMax = Math.max(y1, y2);
        ArrayList<V> resultList = skiplist.intervalsGet(MortonIndex.decompose(xMin, yMin, xMax, yMax));
        resultList.removeIf(c -> !(c.getX() >= xMin && c.getX() <= xMax && c.getY() >= yMin && c.getY() <= yMax) || filter.test(c) == false);
        return resultList;
    }

    public Optional<V> nearestNeighbor(Node<MortonIndex, V> n) {
        ArrayList<Pair<MortonIndex, MortonIndex>> nearestIntervals = new ArrayList<Pair<MortonIndex, MortonIndex>>();
        boolean hasXNeighbor = true, hasYNeighbor = true;
        Function<MortonIndex, MortonIndex> xPlusGetter, yPlusGetter, xMinusGetter, yMinusGetter;
        MortonIndex nodeIndex = n.getIndex();
        int quadrant = nodeIndex.getQuadrant();
        Pair<Integer, Integer> minXY =  MortonIndex.decode(nodeIndex.minRange());
        Pair<Integer, Integer> maxXY = MortonIndex.decode(nodeIndex.maxRange());
        MortonIndex neighborX, neighborY, neighborXY, neighborXminusY, neighborYminusX;

        if (nodeIndex.getRes() < maxRes) {
            //This covers the three other potential co-quads... I think this is faster than checking for the actual nodes
            nearestIntervals.add(new Pair<>(new MortonIndex(nodeIndex.minRange(nodeIndex.getRes() + 1)), new MortonIndex(nodeIndex.maxRange(nodeIndex.getRes() + 1))));
            //We only need to check two directions for any quadrant, because its existence implies at least two directions in which there is space
            if (minXY.getL() == 0) {
                hasXNeighbor = false;
            }
            else if (maxXY.getL() == maxDim) {
                hasXNeighbor = false;
            }
            if (minXY.getR() == 0) {
                hasYNeighbor = false;
            }
            else if (maxXY.getR() == maxDim) {
                hasYNeighbor = false;
            }
            if (quadrant == 0 || quadrant == 2) {
                //So much boilerplate...
                xPlusGetter = new Function<MortonIndex, MortonIndex>() {
                    @Override
                    public MortonIndex apply(MortonIndex i) {
                        return MortonIndex.getWest(i);
                    }
                };
                xMinusGetter = new Function<MortonIndex, MortonIndex>() {
                    @Override
                    public MortonIndex apply(MortonIndex i) {
                        return MortonIndex.getEast(i);
                    }
                };
            }
            else {
                xPlusGetter = new Function<MortonIndex, MortonIndex>() {
                    @Override
                    public MortonIndex apply(MortonIndex i) {
                        return MortonIndex.getEast(i);
                    }
                };
                xMinusGetter = new Function<MortonIndex, MortonIndex>() {
                    @Override
                    public MortonIndex apply(MortonIndex i) {
                        return MortonIndex.getWest(i);
                    }
                };
            }
            if (quadrant == 0 || quadrant == 1) {
                yPlusGetter = new Function<MortonIndex, MortonIndex>() {
                    @Override
                    public MortonIndex apply(MortonIndex i) {
                        return MortonIndex.getNorth(i);
                    }
                };
                yMinusGetter = new Function<MortonIndex, MortonIndex>() {
                    @Override
                    public MortonIndex apply(MortonIndex i) {
                        return MortonIndex.getSouth(i);
                    }
                };
            }
            else {
                yPlusGetter = new Function<MortonIndex, MortonIndex>() {
                    @Override
                    public MortonIndex apply(MortonIndex i) {
                        return MortonIndex.getSouth(i);
                    }
                };
                yMinusGetter = new Function<MortonIndex, MortonIndex>() {
                    @Override
                    public MortonIndex apply(MortonIndex i) {
                        return MortonIndex.getNorth(i);
                    }
                };
            }
            if (hasXNeighbor && hasYNeighbor) {
                //Refactor Indexes to support intervals?
                neighborX = xPlusGetter.apply(nodeIndex);
                neighborY = yPlusGetter.apply(nodeIndex);
                neighborXY = yPlusGetter.apply(neighborX);
                neighborXminusY = yMinusGetter.apply(neighborX);
                neighborYminusX = xMinusGetter.apply(neighborY);
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborX.minRange()), new MortonIndex(neighborX.maxRange())));
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborY.minRange()), new MortonIndex(neighborY.maxRange())));
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborXY.minRange()), new MortonIndex(neighborXY.maxRange())));
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborXminusY.minRange()), new MortonIndex(neighborXminusY.maxRange())));
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborYminusX.minRange()), new MortonIndex(neighborYminusX.maxRange())));
            }
            else if (hasXNeighbor) {
                neighborX = xPlusGetter.apply(nodeIndex);
                neighborXminusY = yMinusGetter.apply(neighborX);
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborX.minRange()), new MortonIndex(neighborX.maxRange())));
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborXminusY.minRange()), new MortonIndex(neighborXminusY.maxRange())));
            }
            else if (hasYNeighbor) {
                neighborY = yPlusGetter.apply(nodeIndex);
                neighborYminusX = xMinusGetter.apply(neighborY);
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborY.minRange()), new MortonIndex(neighborY.maxRange())));
                nearestIntervals.add(new Pair<>(new MortonIndex(neighborYminusX.minRange()), new MortonIndex(neighborYminusX.maxRange())));
            }
        }
        else {
            nearestIntervals.add(new Pair<MortonIndex, MortonIndex>(new MortonIndex(nodeIndex.minRange()), new MortonIndex(nodeIndex.maxRange())));
        }
        nearestIntervals.sort(new Comparator<Pair<MortonIndex, MortonIndex>>() {
            @Override
            public int compare(Pair<MortonIndex, MortonIndex> o1, Pair<MortonIndex, MortonIndex> o2) {
                return Integer.compareUnsigned(o1.getL().minRange(), o2.getL().minRange());
            }
        });
        double lowestDistance = Double.MAX_VALUE;
        int nodeX = n.getValue().getX();
        int nodeY = n.getValue().getY();
        double distance;
        Optional<V> returnValue = Optional.empty();
        for (V c : skiplist.intervalsGet(nearestIntervals)) {
            distance = Math.sqrt(Math.pow(nodeX - c.getX(), 2) + Math.pow(nodeY - c.getY(), 2));
            if (distance < lowestDistance && c.equals(n.getValue()) == false) {
                lowestDistance = distance;
                returnValue = Optional.of(c);
            }
        }
        return returnValue;
    }


    //What does update entail? It means our Cartesian changed coordinates and we need to check and restructure if necessary... but of course, if the coordinate is different, we can't reuse it as index
    //Or should we just have a method to check everything contained in the quadtree and restructure as appropriate?
    /*
    public boolean update(Cartesian c) {

    }

    public V get(Cartesian c) {

    }
    */
}
