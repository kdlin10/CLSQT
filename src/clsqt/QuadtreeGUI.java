package clsqt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.AbstractMap;

public class QuadtreeGUI {
    QTFrame qtFrame;
    QuadtreeGUI(Quadtree<Cartesian> qt) {
        SwingUtilities.invokeLater(() -> {
            qtFrame = new QTFrame(qt);
            qtFrame.setVisible(true);
        });
    }
}

class QTFrame extends JFrame {
    QTFrame(Quadtree<Cartesian> qt) {
        setTitle("Quadtree Graphical Representation");
        setSize(qt.maxDim, qt.maxDim);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.add(new QTPanel(qt));
    }
}

class QTPanel extends JPanel {
    Quadtree<Cartesian> quadtree;
    QTPanel(Quadtree<Cartesian> qt) {
        quadtree = qt;
        QTPopup popupMenu = new QTPopup(qt);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        AbstractMap.SimpleEntry<Integer, Integer> quadEntry, pointEntry;
        int s, x, y;
        Node<MortonIndex, Cartesian> currentNode = quadtree.skiplist.findPrecursors(new MortonIndex(0, 0), 0)[0].getNext(0);
        MortonIndex currentIndex;
        while (currentNode.hasNext(0)) {
            currentIndex = currentNode.getIndex();
            quadEntry = MortonIndex.decode(currentIndex.minRange());
            pointEntry = MortonIndex.decode(currentIndex);
            s = (int) Math.pow(2, currentIndex.getRes());
            x = quadEntry.getKey();
            y = quadEntry.getValue();
            g.setColor(new Color(255, 0, 0));
            g.fillRect(x, y, s, s);
            g.setColor((new Color(0, 0, 255)));
            g.drawRect(x, y, s, s);
            g.setColor(new Color(0, 0, 0));
            g.drawOval(pointEntry.getKey() - 2, pointEntry.getValue() - 2, 4, 4);
            currentNode = currentNode.getNext(0);
        }
    }
}

class QTPopup extends JPopupMenu {
    Quadtree<Cartesian> quadtree;
    JMenuItem addPointMenu;
    int addX, addY;
    QTPopup(Quadtree<Cartesian> qt) {
        quadtree = qt;
        addPointMenu = new JMenuItem("Add Point");
        addPointMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assert(quadtree.maxDim < 1024);
                assert(quadtree.maxDim < 1024);
                quadtree.add(new Point(addX, addY));
            }
        });
        this.add(addPointMenu);
    }

    @Override
    public void show(Component invoker, int x, int y) {
        super.show(invoker, x, y);
        addX = x;
        addY = y;
        addPointMenu.setText("Add Point at: " + addX + ", " + addY);
    }
}