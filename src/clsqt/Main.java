package clsqt;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class Main {
    static private ArrayList<MortonIndex> keys = new ArrayList<>();
    static private Skiplist<MortonIndex, Character> sltest = new Skiplist<>(new MortonIndex(0, 0, 0), new MortonIndex(65536, 65536, 0));
    static private TreeMap<MortonIndex, Character> control = new TreeMap<>();
    static private Random RNG = new Random();
    static private Quadtree<Cartesian> qt = new Quadtree();
    static private QuadtreeGUI gui;

    public static void main(String[] args) throws Exception {
        gui = new QuadtreeGUI(qt);
    }

    static void test() throws Exception {
        for (int i = 0; i < 1594323; i++) {
            keys.add(new MortonIndex(RNG.nextInt(65536), RNG.nextInt(65536), 0));
            add(keys.get(i), (char) (RNG.nextInt(26) + 'A'));
            //add(keys.get(i), (char) (i + '!'));
        }
        //sltest.printlist();
        delete();
        for (MortonIndex k : control.keySet()) {
            if (control.get(k).charValue() != sltest.get(k).charValue()) {
                //Happens because Treemap cannot have duplicate keys and we handle them by just inserting anyways for now
                //Errors expected with large numbers, as TreeMap simply replaces old with new if there is a key collision while we reject the new for now
                throw new Exception("Control vs Skiplist mismatch! Key:" + k.toString() + ", Value: " + control.get(k));
            }
        }
        //sltest.printlist();
        System.out.print("End");
    }

    static void add(Index index, char character) {
        control.put((MortonIndex) index, character);
        sltest.put((MortonIndex) index, character);
    }

    static void delete() {
        MortonIndex randRemoveKey;
        int randRemove;
        for (int i = 0; i < 81; i++) {
            randRemove = RNG.nextInt(keys.size());
            //randRemoveKey = keys.get(8);
            randRemoveKey = keys.get(randRemove);
            keys.remove(randRemove);
            control.remove(randRemoveKey);
            sltest.remove(randRemoveKey);
        }
    }
}
