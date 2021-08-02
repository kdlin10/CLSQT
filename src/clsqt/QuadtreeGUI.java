package clsqt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

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
        setMinimumSize(new Dimension(qt.maxDim, qt.maxDim));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.add(new QTPanel(qt));
    }
}

class QTPanel extends JPanel {
    QTPopup popupMenu;
    Quadtree<Cartesian> quadtree;
    SearchPanel searchPanel;
    QTPanel(Quadtree<Cartesian> qt) {
        quadtree = qt;
        popupMenu = new QTPopup(qt, this);
        searchPanel = new SearchPanel();
        setComponentZOrder(searchPanel, 0);
        this.setBackground(Color.gray);
        this.setComponentPopupMenu(popupMenu);
        setPreferredSize(new Dimension(qt.maxDim, qt.maxDim));
    }

    class SearchPanel extends JPanel {
        Collection<Cartesian> latestResults;
        int x1, x2, y1, y2;
        SearchPanel() {
            this.setBorder(BorderFactory.createLineBorder(Color.blue));
            this.setVisible(false);
            this.setBackground(new Color(255, 255, 255, 0));
        }

        protected void updateSearchRange(int xInit, int yInit, int xFinal, int yFinal) {
            x1 = Math.min(xInit, xFinal);
            y1 = Math.min(yInit, yFinal);
            x2 = Math.max(xInit, xFinal);
            y2 = Math.max(yInit, yFinal);
        }

        protected void updateSearchResults(Collection<Cartesian> results) {
            latestResults = results;
            this.setVisible(true);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            this.setBounds(x1, y1, Math.abs(x1 - x2), Math.abs(y1 - y2));
            if (this.isVisible()) {
                g.setColor(Color.green);
                for (Cartesian c : latestResults) {
                    g.fillOval(c.getX() - 2 - x1, c.getY() - 2 - y1, 4, 4);
                }
            }
        }
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
        Pair<Integer, Integer> quadEntry, pointEntry;
        int s, x, y;
        MortonIndex currentIndex = thisNode.getIndex();
        quadEntry = MortonIndex.decode(currentIndex.minRange());
        pointEntry = MortonIndex.decode(currentIndex);
        s = (int) Math.pow(2, currentIndex.getRes());
        x = quadEntry.getL();
        y = quadEntry.getR();

        this.setBounds(x, y, s, s);

        g.setColor(Color.black);
        //Necessary because rendering is relative to the bounds, not the JFrame
        g.drawOval(pointEntry.getL() - 2 - x, pointEntry.getR() - 2 - y, 4, 4);
    }
}

class QTPopup extends JPopupMenu {
    private Quadtree<Cartesian> quadtree;
    private QTPanel parentPanel;
    private JMenuItem addPointMenu, removePointMenu, rectSearchMenu;
    private boolean searchStart = false;
    private int initX, initY;

    QTPopup(Quadtree<Cartesian> qt, QTPanel panel) {
        quadtree = qt;
        parentPanel = panel;
        addPointMenu = new JMenuItem("Add Point: ");
        this.add(addPointMenu);
        removePointMenu = new JMenuItem("Remove Point: ");
        this.add(removePointMenu);
        rectSearchMenu = new JMenuItem("Rectangular search: ");
        this.add(rectSearchMenu);
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

        if (searchStart == false) {
            rectSearchMenu.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    initX = x;
                    initY = y;
                    searchStart = true;
                    parentPanel.searchPanel.setVisible(false);
                }
            });
            rectSearchMenu.setText("Set search point at: " + x + ", " + y);
        }
        else {
            rectSearchMenu.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    searchStart = false;
                    ArrayList<Cartesian> searchResult = quadtree.rectSearch(initX, initY, x, y);
                    for (Cartesian c : searchResult) {
                        System.out.println(c.getX() + ", " + c.getY());
                    }
                    System.out.println("Search yielded " + searchResult.size() + " results");
                    parentPanel.searchPanel.updateSearchRange(initX, initY, x, y);
                    parentPanel.searchPanel.updateSearchResults(searchResult);
                }
            });
            rectSearchMenu.setText("Search from " + initX + ", " + initY + " to " + x + ", " + y);
        }
    }
}