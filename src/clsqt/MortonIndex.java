package clsqt;

import java.util.AbstractMap;

class MortonIndex implements Index<MortonIndex> {
    private int index;
    private int res; //Masks the rightmost 2*res bits of our stored index; limited to 16 for our 32-bit int. 0 = 1x1 quads, 1 = 2x2 quads, 2 = 4x4, 3 = 8x8, etc

    //Limited to 16 bits of precision on x and y axis - 16 bits on the left get thrown away
    //If we have more precision than we can handle, shift to the right in the Quadtree before passing to here?
    MortonIndex(int x, int y, int r) {
        index = encode(x, y);
        res = r;
    }

    MortonIndex(Cartesian c, int r) {
        this(c.getX(), c.getY(), r);
    }

    MortonIndex (int i, int r) {
        index = i;
        res = r;
    }

    public static int encode(int x, int y) {
        return interleaveZeros(x) | interleaveZeros(y) << 1;
    }

    public static int encode(Cartesian c) {
        return interleaveZeros(c.getX()) | interleaveZeros(c.getY()) << 1;
    }

    public static AbstractMap.SimpleEntry<Integer, Integer> decode(MortonIndex i) {
        return MortonIndex.decode(i.index);
    }

    public static AbstractMap.SimpleEntry<Integer, Integer> decode(int encoded) {
        return new AbstractMap.SimpleEntry(deinterleave(encoded), deinterleave(encoded >>> 1));
    }

    @Override
    public int compareTo(MortonIndex i) {
        return Integer.compareUnsigned(index, i.index);
    }

    @Override
    public boolean contains(MortonIndex i) {
        return Integer.compareUnsigned(minRange(), i.minRange()) <= 0 && Integer.compareUnsigned(maxRange(), i.maxRange()) >= 0;
    }

    @Override
    public boolean overlaps(MortonIndex i) {
        //If the compared index is greater than this one and its min is less than this one's max
        //OR if the compared index is less than this one and its max is greater than this one's min
        return (Integer.compareUnsigned(index, i.index) <= 0 && Integer.compareUnsigned(maxRange(), i.minRange()) >= 0) ||
                (Integer.compareUnsigned(index, i.index) >= 0 && Integer.compareUnsigned(minRange(), i.maxRange()) <= 0);
    }

    @Override
    public boolean isDivisible() {
        return !(res == 0);
    }

    public int toInt() {
        return index;
    }

    public int minRange() {
        //Returns index with rightmost 2*res bits to zero
        return index >>> (2 * res) << (2 * res);
    }

    public int maxRange() {
        //Returns index with rightmost 2*res bits to one
        return index | ((0x00000001 << res * 2) - 1);
    }

    public int getRes() {
        return res;
    }

    protected void setRes(int r) {
        res = r;
    }


    //Assumes i is within the bounds of this index
    protected int getSplitSize(MortonIndex i) {
        return getMaxDiffPowOfTwo(index, i.index);
    }

    //How many r*2 bits can we erase on the right before our two indexes overlap? Finds the largest power of 2 that can fit between the two indexes
    public static int getMaxDiffPowOfTwo(int index1, int index2) {
        int r = 0;
        //Conveniently avoids issues with the signum and having to call the method with the indexes in the right order
        while (Math.abs((index1 >>> (2 * (r + 1))) - (index2 >>> (2 * (r + 1)))) > 0) {
            r++;
        }
        return r;
    }

    protected int getQuadrant() {
        return index & (0x00000003 << (2 * res));
    }

    protected int getQuadrant(int r) {
        return index & (0x00000003 << (2 * r));
    }

    protected int getParentStartLoc() {
        return index & (0xFFFFFFFF << ((res + 1) * 2));
    }

    protected boolean isCoQuad(MortonIndex i) {
        return (getParentStartLoc() == i.getParentStartLoc() && res == i.res && getQuadrant() != i.getQuadrant());
    }

    protected void expand() {
        res++;
    }

    //Borrowed from http://asgerhoedt.dk/?p=276
    private static int interleaveZeros(int x) {
        x &= 0x0000ffff;                  	// x = ---- ---- ---- ---- fedc ba98 7654 3210
        x = (x ^ (x << 8)) & 0x00ff00ff;	// x = ---- ---- fedc ba98 ---- ---- 7654 3210 - Each line shifts to left halfway to end and applies xor to original
        x = (x ^ (x << 4)) & 0x0f0f0f0f; 	// x = ---- fedc ---- ba98 ---- 7654 ---- 3210   then bitmasks the middle to 0
        x = (x ^ (x << 2)) & 0x33333333; 	// x = --fe --dc --ba --98 --76 --54 --32 --10
        x = (x ^ (x << 1)) & 0x55555555; 	// x = -f-e -d-c -b-a -9-8 -7-6 -5-4 -3-2 -1-0
        return x;
    }

    private static int deinterleave(int x) {
        x &= 0x55555555;                  // x = -f-e -d-c -b-a -9-8 -7-6 -5-4 -3-2 -1-0
        x = (x ^ (x >>>  1)) & 0x33333333; // x = --fe --dc --ba --98 --76 --54 --32 --10
        x = (x ^ (x >>>  2)) & 0x0f0f0f0f; // x = ---- fedc ---- ba98 ---- 7654 ---- 3210
        x = (x ^ (x >>>  4)) & 0x00ff00ff; // x = ---- ---- fedc ba98 ---- ---- 7654 3210
        x = (x ^ (x >>>  8)) & 0x0000ffff; // x = ---- ---- ---- ---- fedc ba98 7654 3210
        return x;
    }

    private static int floorPowerOfTwoSq(int i) {
        //Adapted from https://graphics.stanford.edu/~seander/bithacks.html - works for 32-bit int
        i |= (i >>> 1); //0010 0000 | 0001 0000 = 0011 0000
        i |= (i >>> 2); //0011 0000 | 0000 1100 = 0011 1100
        i |= (i >>> 4); //0011 1100 | 0000 0011 = 0011 1111 Basically fills everything to the right with 1s
        i |= (i >>> 8);
        i |= (i >>> 16);
        i -= (i >>> 2); //0011 1111 - 0000 1111 = 0011 0000 Gives our two MSB
        i &= 0x55555555; //Bitmask so only the even digit one is left
        return i;
    }

    @Override
    public String toString() {
        return String.valueOf(deinterleave(index)) + ":" + String.valueOf(deinterleave(index >>> 1));
    }
}
