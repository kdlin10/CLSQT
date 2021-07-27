# Compressed Linear Skip Quadtree (WIP)

## Introduction
I ran into a need to store a collection of objects associated with Cartesian coordinates, with the following requirements:

* Support fast nearest neighbor and varying 2D range searches (Both rectangular and by radius)
* Handle varying access patterns
* Contain both sparse and clustered data
* Deal with frequent adds, deletions, and *moving* points

Based on the literature, Quadtrees appeared to be a good balance between performance and ease of implementation. In addition to the most basic implementation, there are many additional techniques that could be added:

* Constructing a [multi-level, sparsely populated structure](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.83.7687&rep=rep1&type=pdf) that can be quickly navigated, analogous to a [skiplist](https://en.wikipedia.org/wiki/Skip_list) (log(n) search)
* [Reducing dimensionality](https://www.cs.umd.edu/class/fall2020/cmsc420-0301/files/quad.pdf) with space filling curves (most common being the [Morton curve](https://en.wikipedia.org/wiki/Z-order_curve)) to linearize location with natural ordering but while preserving some locality
* Eliminating empty quads and maximizing the size of existing ones to reduce number of traversals needed
 
## Description
Below is a sample representation of our skiplist. Each column with an `x` is a node, which contains data, a contiguous range of indicies (represented as single characters here), and a pointer to the next node on each layer it occupies. We track changes in nodes on each layer to try and deterministically maintain a ratio of 3:1 nodes between a layer and the one above it. Each layer is bounded at the extremes of our chosen indexing scheme. The direction of the links alternate by layer. 
```
LEVEL 2 |-----------------------------------------> x----------------->|
LEVEL 1 |<------------x <------------x <------------x <------------x <-|
LEVEL 0 |-> x--> x--> x--> x--> x--> x--> x--> x--> x--> x--> x--> x-->|
        ----------------------------------------------------------------
            A    D    E    G    H    L    P    Q    T    U    Y    Z 
```
Given an index to locate, we start at the highest populated level and follow the links. As our nodes are sorted by index, if we find ourselves past the target, we drop down one level and attempt to locate the index there. This is repeated until the node containing our index is located or shown to not exist. Insertions and deletions follow a similar pattern. 

Our 2D Cartesian coordinates are transformed to conform to the 1D Morton curve (also known as the Z-order, for the shape it traces), which is defined recursively:

```
┌────┬────┐
│ 0  | 1  │
├────┼────┤ 
│ 2  │ 3  │
└────┴────┘

┌────┬────┬────┬────┐
│ 0  | 1  │ 4  | 5  |
├────┼────┼────┼────┤
│ 2  │ 3  │ 6  │ 7  │
├────┼────┼────┼────┤
│ 8  │ 9  │ 12 │ 13 │
├────┼────┼────┼────┤
│ 10 │ 11 │ 14 │ 15 │
└────┴────┴────┴────┘
```
Our encoding uses two bits for each level of resolution and interleaves x and y at each bit: 

```
    0    1
  ┌────┬────┐
0 │ 00 | 01 │
  ├────┼────┤ 
1 │ 10 │ 11 │
  └────┴────┘
```
With this scheme, given a Morton index, we can determine which quadrant at the highest level of resolution (first subdivision of the coordinate space) is being specified by examining the two most significant bits. The next two bits will reveal which quadrant is being specified at the next level of resolution, and so forth. As our skiplist nodes are assigned a Morton index, one implication is that we can mask various bits to expand/reduce the space specified by the node while preserving detailed information. Thus, our nodes are dynamically sized to take up the maximum amount of space available to them. This allows for faster searches, as a node that occupies the search space but does not yield the target is proof it does not exist within that range. Given a node, we can also formulate the bounds for a nearest neighbor search, as its size implies the existence of at least one adjacent node of equal size. Maximizing the space occupied by nodes also minimizes updates necessary when the position of our coordinates changes. 

## TODO
- [x] Implement Morton coding
- [x] Adaptive node resizing/splitting/merging based on Quadtree behavior
- [ ] Debugging/Visualization infrastructure
- [ ] Search query optimization?
- [ ] Front-end? 
- [ ] Filtering? 