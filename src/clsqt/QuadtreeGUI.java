package clsqt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
        //setSize(qt.maxDim, qt.maxDim);
        setMinimumSize(new Dimension(qt.maxDim, qt.maxDim));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.add(new QTPanel(qt));
    }
}

class QTPanel extends JPanel {
    QTPopup popupMenu;
    Quadtree<Cartesian> quadtree;
    QTPanel(Quadtree<Cartesian> qt) {
        quadtree = qt;
        popupMenu = new QTPopup(qt, this);
        this.setBackground(Color.gray);
        this.setComponentPopupMenu(popupMenu);
        setPreferredSize(new Dimension(qt.maxDim, qt.maxDim));
    }
}

class nodePanel extends JPanel {
    Node<MortonIndex, Cartesian> thisNode;
    nodePanel(Node n) {
        thisNode = n;
        this.setVisible(true);
        this.setBackground(Color.red);
        this.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setInheritsPopupMenu(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        AbstractMap.SimpleEntry<Integer, Integer> quadEntry, pointEntry;
        int s, x, y;
        MortonIndex currentIndex = thisNode.getIndex();
        quadEntry = MortonIndex.decode(currentIndex.minRange());
        pointEntry = MortonIndex.decode(currentIndex);
        s = (int) Math.pow(2, currentIndex.getRes());
        x = quadEntry.getKey();
        y = quadEntry.getValue();

        this.setBounds(x, y, s, s);

        g.setColor(Color.black);
        //Necessary because rendering is relative to the bounds, not the JFrame
        g.drawOval(pointEntry.getKey() - 2 - x, pointEntry.getValue() - 2 - y, 4, 4);
    }
}

class QTPopup extends JPopupMenu {
    Quadtree<Cartesian> quadtree;
    QTPanel parentPanel;
    JMenuItem addPointMenu, removePointMenu;
    QTPopup(Quadtree<Cartesian> qt, QTPanel panel) {
        quadtree = qt;
        parentPanel = panel;
        addPointMenu = new JMenuItem("Add Point: ");
        this.add(addPointMenu);
        removePointMenu = new JMenuItem("Remove Point: ");
        this.add(removePointMenu);
    }

    @Override
    public void show(Component invoker, int x, int y) {
        super.show(invoker, x, y);
        addPointMenu.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assert(y < quadtree.maxDim);
                assert(x < quadtree.maxDim);
                quadtree.add(new Point(x, y));
                parentPanel.add(new nodePanel(quadtree.skiplist.findPrecursors(new MortonIndex(MortonIndex.encode(x, y), 0), 0)[0].getNext(0)));
                parentPanel.validate();
                parentPanel.repaint();
            }
        });
        addPointMenu.setText("Add Point at: " + x + ", " + y); //Apparently the order matters, setting action resets the text

        Component mouseComponent = parentPanel.getComponentAt(x, y);
        if (mouseComponent instanceof nodePanel) {
            removePointMenu.setEnabled(true);
            int pointX = ((nodePanel) mouseComponent).thisNode.getValue().getX();
            int pointY = ((nodePanel) mouseComponent).thisNode.getValue().getY();
            removePointMenu.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    quadtree.remove(new Point(pointX, pointY));
                    Component removeComponent = parentPanel.getComponentAt(pointX, pointY);
                    parentPanel.remove(removeComponent);
                    removeComponent.invalidate();
                    parentPanel.validate();
                    parentPanel.repaint();
                }
            });
            removePointMenu.setText("Remove Point at: " + pointX + ", " + pointY);
        }
        else {
            removePointMenu.setEnabled(false);
            removePointMenu.setText("Remove Point");
        }
    }
}