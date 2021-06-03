package clsqt;

class MortonIndex implements Index<MortonIndex> {
    private int index;
    private short res; //Masks the rightmost 2*res bits of our stored index; limited to 16 for our 32-bit int

    //Limited to the 16 bits on the right - everything to the left gets thrown away
    MortonIndex(int x, int y) {
        index = encode(x, y);
        res = 1;
    }

    public static int encode(int x, int y) {
        return interleaveZeros(x) | interleaveZeros(y) << 1;
    }

    @Override
    public int compareTo(MortonIndex i) {
        return Integer.compareUnsigned(index, i.index);
    }

    @Override
    public boolean contains(MortonIndex i) {
        return Integer.compareUnsigned(minRange(), i.index) <= 0 && Integer.compareUnsigned(maxRange(), i.index) >= 0;
    }

    public int minRange() {
        //Set the rightmost 2*res bits to zero
        return index >>> (2 * res) << (2 * res);
    }

    public int maxRange() {
        //Set the rightmost 2*res bits to one
        return index |= (1 << res * 2) - 1;
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
        x = (x ^ (x >>  1)) & 0x33333333; // x = --fe --dc --ba --98 --76 --54 --32 --10
        x = (x ^ (x >>  2)) & 0x0f0f0f0f; // x = ---- fedc ---- ba98 ---- 7654 ---- 3210
        x = (x ^ (x >>  4)) & 0x00ff00ff; // x = ---- ---- fedc ba98 ---- ---- 7654 3210
        x = (x ^ (x >>  8)) & 0x0000ffff; // x = ---- ---- ---- ---- fedc ba98 7654 3210
        return x;
    }
}
