# Compressed Linear Skip Quadtree (WIP)

## Introduction
I ran into a need to store a collection of objects associated with Cartesian coordinates, with the following requirements:

* Support fast nearest neighbor and varying 2D range searches (Both rectangular and by radius)
* Handle varying access patterns
* Contain both sparse and clustered data
* Deal with frequent adds, deletions, and *moving* points

After delving into the literature, Quadtrees appeared to be a good balance between performance and ease of implementation. In addition to the basic implementation, there are many additional techniques that could be added:

* Sampling to construct a [sparse structure](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.83.7687&rep=rep1&type=pdf) that can be quickly navigated, like a [skiplist](https://en.wikipedia.org/wiki/Skip_list) (log(n) search)
* [Reducing dimensionality](https://www.cs.umd.edu/class/fall2020/cmsc420-0301/files/quad.pdf) with space filling curves (most common being the [Morton curve](https://en.wikipedia.org/wiki/Z-order_curve)) to linearize location with natural ordering but still preserve some locality
* Eliminating empty quads and maximizing the size of existing ones to reduce amount of traversals needed


Not finding any implementations with an ensemble of these techniques, I decided to start one. 

## Description
Below is a sample representation of our skiplist. Each column with an `x` is a node, which contains data, a contiguous range of indicies (simplified as single characters here), and a single pointer to the next node on each layer it occupies. We track changes in nodes on each layer to try and deterministically maintain a ratio of 3:1 nodes between a layer and the one above it. Each layer is bounded at the extremes of our chosen indexing scheme. The direction of the links alternate by layer. 
```
LEVEL 2 |-----------------------------------------> x----------------->|
LEVEL 1 |<------------x <------------x <------------x <------------x <-|
LEVEL 0 |-> x--> x--> x--> x--> x--> x--> x--> x--> x--> x--> x--> x-->|
        ----------------------------------------------------------------
            A    D    E    G    H    L    P    Q    T    U    Y    Z 
```
Given an index to locate, we start at the highest populated level and follow the links. As our nodes are sorted by index, if we find ourselves past the target, we drop down one level and attempt to locate the index there. This is repeated until the node containing our index is located or shown to not exist. 

To insert/delete, we traverse, but connect/disconnect the target node along the way. 

## TODO
- [x] Implement Morton coding
- [ ] Adaptive node resizing/splitting/merging based on Quadtree behavior
- [ ] Debugging/Visualization infrastructure
- [ ] Search query optimization?
- [ ] Front-end? 
- [ ] Filtering? 