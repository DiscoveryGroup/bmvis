/*
 * Copyright 2012 University of Helsinki.
 * 
 * This file is part of BMVis.
 *
 * BMVis is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * BMVis is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with BMVis.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package biomine.bmvis;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import javax.swing.Timer;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import prefuse.Display;
import prefuse.controls.Control;
import prefuse.data.tuple.TupleSet;
import prefuse.util.GraphicsLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

import biomine.bmgraph.BMNode;
import biomine.bmgraph.BMEdge;
import biomine.bmgraph.BMEntity;

/**
 * Mouse control. Includes clicking, dragging, zooming, panning,
 * selection and hovering.
 * 
 * @author Kimmo Kulovesi
 */

public class VisualControl implements Control {

    private static final int LEFT_BUTTON = MouseEvent.BUTTON1_MASK;
    private static final int MIDDLE_BUTTON = MouseEvent.BUTTON2_MASK;
    private static final int RIGHT_BUTTON = MouseEvent.BUTTON3_MASK;
    private static final int DOUBLECLICK_DELAY = 250;

    /**
     * Minimum display scale factor (zoom).
     * (Must be positive, not checked). Defaults to 0.03.
     */
    public double MIN_SCALE = 0.03;

    /**
     * Maximum display scale factor (zoom).
     * (Must be greater than MIN_SCALE, not checked.)
     * Defaults to 4.
     */
    public double MAX_SCALE = 4;

    /**
     * Duration of the zoom animation in milliseconds.
     * (Must be non-negative, not checked.)
     * Defaults to 1000.
     */
    public long ZOOM_DURATION = 1000;
    
    /**
     * The currently active node (hover, etc).
     * Changing this externally should be done with great care,
     * e.g. when the mouse cursor is moved over another item in a way
     * such that this control can't know it.
     */
    public NodeItem activeItem = null;

    /**
     * Was activeItem fixed before hovering (which always fixes the item).
     */
    public boolean itemWasFixed = false;

    /**
     * Automatically pin after dragging?
     */
    public boolean autoPin = true;


    private BMVis bmvis = null;
    private boolean isEnabled = true;
    private PrefuseBMGraph graph = null;
    private TupleSet neighborGroup = null;
    private TupleSet selectGroup = null;
    private boolean mayUnselect = false;
    /**
     * Set by mouse events that cause the selection to be changed.
     * If true, bmvis is notified in itemReleased.
     */
    private boolean notifySelectionChange = false;
    private boolean dragged = false;
//    private boolean didUnPin = false;
    private Cursor moveCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    private Cursor zoomCursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
    private Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    private Display display;

    private Point2D point = new Point2D.Double();
    private Rectangle2D rect = new Rectangle2D.Double();
    private double dragStartX = -1.0, dragStartY = -1.0;

    private Timer mouseClickedTimer;

    private static final String NODEINFO_PREFIX = "<html><small style=\"color: #666666; font-weight: normal;\">";
    private static final String NODEINFO_INFIX = "</small><small style=\"color: #006600\"> ";
    private static final String NODEINFO_SUFFIX = "</small></html>";

    private static final LinkedList<String> DISPLAY_EDGE_ATTRIBUTES = new LinkedList();
    private static final LinkedList<String> DISPLAY_NODE_ATTRIBUTES = new LinkedList();
    static {
        DISPLAY_EDGE_ATTRIBUTES.add(PrefuseBMGraph.GOODNESS_KEY);
        DISPLAY_EDGE_ATTRIBUTES.add(PrefuseBMGraph.TTNR_KEY);
        DISPLAY_EDGE_ATTRIBUTES.add(PrefuseBMGraph.KTNR_KEY);
        DISPLAY_EDGE_ATTRIBUTES.add(PrefuseBMGraph.SOURCE_DB_NAME_KEY);
        DISPLAY_EDGE_ATTRIBUTES.add(PrefuseBMGraph.SOURCE_DB_VERSION_KEY);

        DISPLAY_NODE_ATTRIBUTES.add(PrefuseBMGraph.TTNR_KEY);
        DISPLAY_NODE_ATTRIBUTES.add(PrefuseBMGraph.KTNR_KEY);
    }

    /**
     * Create a new VisualControl.
     * @param bmvis Visualizer instance.
     */
    public VisualControl (BMVis bmvis) {
        assert bmvis != null : "Null BMVis reference";
        this.bmvis = bmvis;
        graph = bmvis.getGraph();
        display = bmvis.getDisplay();
        neighborGroup = bmvis.ensureHasFocusGroup(BMVis.NEIGHBOR_GROUP);
        selectGroup = bmvis.ensureHasFocusGroup(BMVis.SELECT_GROUP);
        mouseClickedTimer = new Timer(DOUBLECLICK_DELAY,
                new ActionListener() {
                    public void actionPerformed (ActionEvent e) {
                        mouseClickedSingle();
                    }
                });
        mouseClickedTimer.stop();
        mouseClickedTimer.setRepeats(false);
    }

    /**
     * Called when a VisualItem is "entered" with the mouse.
     * @param item The item that was entered.
     * @param e The corresponding MouseEvent.
     */
    public void itemEntered(VisualItem item, MouseEvent e) {
        if (!(item instanceof NodeItem))
            return;

        bmvis.disallowLayoutFreezing();
        
        activeItem = (NodeItem)item;
        itemWasFixed = item.isFixed();
        item.setFixed(true);
        
        NodeItem n;
        NodeItem node = (NodeItem)item;
        Iterator<NodeItem> iter = node.neighbors();
        if (node.getBoolean(PrefuseBMGraph.EDGENODE_KEY)) {
            neighborGroup.setTuple(node);
            node.setHighlighted(true);
            n = iter.next();
            neighborGroup.addTuple(n);
            n.setHighlighted(true);
            n = iter.next();
            neighborGroup.addTuple(n);
            n.setHighlighted(true);
        } else {
            neighborGroup.clear();
            while (iter.hasNext()) {
                Iterator<NodeItem> eniter = iter.next().neighbors();
                n = eniter.next();
                n.setHighlighted(true);
                neighborGroup.addTuple(n);
                n = eniter.next();
                n.setHighlighted(true);
                neighborGroup.addTuple(n);
            }
        }
    }
    
    /**
     * Called when a VisualItem is "exited" with the mouse.
     * @param item The item that was exited.
     * @param e The corresponding MouseEvent.
     */
    public void itemExited(VisualItem item, MouseEvent e) {
        if (!(item instanceof NodeItem))
            return;

        bmvis.allowLayoutFreezing();
                
        if (activeItem == item) {
            activeItem = null;
            item.setFixed(itemWasFixed);
        }

        Iterator<VisualItem> iter = neighborGroup.tuples();
        while (iter.hasNext()) {
            iter.next().setHighlighted(false);
        }
        neighborGroup.clear();        
    }

    /**
     * Called when a VisualItem is pressed with the mouse.
     * @param item The item that was pressed.
     * @param e The corresponding MouseEvent.
     */
    public void itemPressed (VisualItem item, MouseEvent e) {        
        if (!(item instanceof NodeItem))
            return;

        display.requestFocus();

        if (checkItemPopupMenu(item, e))
            return;

        if ((e.getModifiers() & LEFT_BUTTON) != LEFT_BUTTON)
            return;       
        display.getAbsoluteCoordinate(e.getPoint(), point);
        dragStartX = point.getX();
        dragStartY = point.getY();
        if (e.isShiftDown()) {
            if (selectGroup.containsTuple(item))
                mayUnselect = true;
            else {
                mayUnselect = false;
                selectGroup.addTuple(item);
                notifySelectionChange = true;
            }
        } else {
            if (selectGroup.containsTuple(item)) {
                if (selectGroup.getTupleCount() == 1)
                    mayUnselect = true;
                else
                    mayUnselect = false;
            } else {
                mayUnselect = false;
                selectGroup.setTuple(item);
                notifySelectionChange = true;
            }
        }
    }

    /**
     * Called when a VisualItem is dragged with the mouse.
     * @param item The item that was dragged.
     * @param e The corresponding MouseEvent.
     */
    public void itemDragged (VisualItem item, MouseEvent e) {        
        if (!(item instanceof NodeItem))
            return;
        if ((e.getModifiers() & LEFT_BUTTON) != LEFT_BUTTON)
            return;
        
        bmvis.setLayoutEnabled(bmvis.enableAutomaticLayout);
        
        double x, y;
        double dx, dy;
        double drx, dry;
        NodeItem node;
        display.getAbsoluteCoordinate(e.getPoint(), point);
        dx = point.getX();
        dy = point.getY();
        drx = dx - dragStartX;
        dry = dy - dragStartY;
        dragStartX = dx;
        dragStartY = dy;

        Iterator<VisualItem> iter = selectGroup.tuples();
        while (iter.hasNext()) {
            node = (NodeItem)(iter.next());
            x = node.getX();
            y = node.getY();
            node.setStartX(x);
            node.setStartY(y);
            x += drx;
            y += dry;
            node.setX(x);
            node.setY(y);
            node.setEndX(x);
            node.setEndY(y);

            // If layout isn't enabled, do rudimentary layout for
            // adjacent edges of real nodes
            if (!bmvis.layoutEnabled &&
                    !node.getBoolean(PrefuseBMGraph.EDGENODE_KEY)) {
                Iterator<NodeItem> niter = node.neighbors();
                Iterator<NodeItem> eniter;
                NodeItem ni, eni;
                double nx, ny;
                while (niter.hasNext()) {
                    eni = niter.next();
                    // "eni" is now an edgenode of "node"
                    if (!eni.isFixed() && !selectGroup.containsTuple(eni)) {
                        eniter = eni.neighbors();
                        ni = eniter.next();
                        if (ni == node)
                            ni = eniter.next();
                        // "ni" is now the neighbor connected through "eni"

                        eni.setStartX(eni.getX());
                        eni.setStartY(eni.getY());
                        nx = (x + ni.getX()) / 2.0;
                        ny = (y + ni.getY()) / 2.0;
                        eni.setX(nx);
                        eni.setY(ny);
                        eni.setEndX(nx);
                        eni.setEndY(ny);
                    }
                }
            }
        }
        dragged = true;

        if (autoPin) {
            if (activeItem == item)
                itemWasFixed = true;
            else
                item.setFixed(true);
        }
    }

    /**
     * Called when a VisualItem is released.
     * 
     * If the selection was changed, bmvis will be notified.
     *  
     * @param item The item that was released.
     * @param e The corresponding MouseEvent.
     */
    public void itemReleased (VisualItem item, MouseEvent e) {                
        
        if (!dragged) {
            // no action (but may notify selection change, so do not return yet
        } else if ((e.getModifiers() & LEFT_BUTTON) != LEFT_BUTTON) {
            // no action (but may notify selection change, so do not return yet
        } else {
            bmvis.changed = true;
            dragged = false;
            if (!mayUnselect && selectGroup.containsTuple(item)
                    && selectGroup.getTupleCount() == 1) {
                // Unselect item if it was only selected for dragging
                selectGroup.clear();
                notifySelectionChange = true;
            }            
        }
        if (notifySelectionChange) {
            notifySelectionChange = false;
            bmvis.notifySelectionChanged();
        }

        checkItemPopupMenu(item, e);
    }

    private boolean checkItemPopupMenu (final VisualItem item, MouseEvent e) {

        if (!e.isPopupTrigger()) {
            return false;
        }

        final BMEntity bme = graph.getBMEntity((NodeItem)item);
        final boolean isNode = (bme instanceof BMNode);
        JMenuItem menuitem;

            // Popup menu: View in browser
        JPopupMenu menu = new JPopupMenu();
        menuitem = new JMenuItem(new AbstractAction("View in browser") {
            public void actionPerformed (ActionEvent e) {
                bmvis.openURLFor((NodeItem)item);
            }
        });
        menuitem.setEnabled(isNode || (bme != null &&
                                       bme.get(PrefuseBMGraph.URL_KEY) != null));
        menu.add(menuitem);
        menu.addSeparator();

            // Popup menu: Pin or unpin
        boolean wouldUnPin;
        if (activeItem == item) {
            wouldUnPin = (dragged ? false : itemWasFixed);
        } else {
            wouldUnPin = item.isFixed();
        }
        if (wouldUnPin) {
            menu.add(new AbstractAction("Release for layout") {
                public void actionPerformed (ActionEvent e) {
                    bmvis.changed = true;                   
                    if (activeItem == item) {
                        itemWasFixed = false;
                    } else {
                        item.setFixed(false);
                    }                    
                    
                    if (BMVis.REALNODE_PREDICATE.getBoolean(item)) {
                        Iterator<NodeItem> iter;
                        iter = ((NodeItem)item).neighbors();
                        while (iter.hasNext())
                            iter.next().setFixed(false);
                    } else if (!bmvis.layoutEnabled) {
                        bmvis.centerEdgenode((NodeItem)item);
                    }
                    
                    bmvis.setLayoutEnabled(bmvis.enableAutomaticLayout);                    
                }
            });
        } else {
            menu.add(new AbstractAction("Pin position") {
                public void actionPerformed (ActionEvent e) {
                    bmvis.changed = true;
                    if (activeItem == item) {
                        itemWasFixed = true;
                    } else {
                        item.setFixed(true);
                    }
                }
            });
        }

            // Copy node information to clipboard
        menu.addSeparator();
        menuitem = new JMenuItem("Copy to clipboard:");
        menuitem.setEnabled(false);
        menu.add(menuitem);
        if (isNode) {
            List<BMNode> members = graph.getBMGraph().getMembersFor((BMNode)bme);
            StringBuffer buf = null;
            String itemname;
            if (members == null) {
                itemname = ((BMNode)bme).getId();
            } else {
                buf = new StringBuffer();
                for (BMNode member : members) {
                    buf.append(member.getId());
                    buf.append(" \n");
                }
                itemname = "[ids of group members]";
            }
            final String nodestr = (buf == null) ? itemname : buf.toString().trim();
            menuitem = new JMenuItem(new AbstractAction(itemname) {
                public void actionPerformed (ActionEvent e) {
                    bmvis.setClipboard(nodestr);
                }
            });
            menuitem.setText("<html><small>"+itemname+"</small>");
            menu.add(menuitem);

            final String shortname = bme.get("ShortName");
            if (shortname != null) {
                menuitem = new JMenuItem(new AbstractAction(shortname) {
                    public void actionPerformed (ActionEvent e) {
                        bmvis.setClipboard(shortname);
                    }
                });
                menuitem.setText("<html><small>"+shortname+"</small>");
                menu.add(menuitem);
            }

            final String primaryname = bme.get("PrimaryName");
            if (primaryname != null) {
                menuitem = new JMenuItem(new AbstractAction(primaryname) {
                    public void actionPerformed (ActionEvent e) {
                        bmvis.setClipboard(primaryname);
                    }
                });
                menuitem.setText("<html><small>"+primaryname+"</small>");
                menu.add(menuitem);
            }

            menu.addSeparator();

            String s;
            s = bme.get(PrefuseBMGraph.ORGANISM_KEY);
            menuitem = new JMenuItem(new AbstractAction(
                    NODEINFO_PREFIX + "type:" +
                    NODEINFO_INFIX + ((BMNode)bme).getType() +
                    (s == null ? "" : " ("+s+")") +
                    NODEINFO_SUFFIX) {
                        public void actionPerformed (ActionEvent e) {
                            bmvis.setClipboard(((BMNode)bme).getType());
                        }
                    });
            menu.add(menuitem);

            for (String attribute : DISPLAY_NODE_ATTRIBUTES) {
                s = bme.get(attribute);
                if (s != null && s.length() > 0) {
                    final String value = s;
                    menuitem = new JMenuItem(new AbstractAction(
                        NODEINFO_PREFIX +
                        attribute.replace('_', ' ') + ":" +
                        NODEINFO_INFIX + s + NODEINFO_SUFFIX) {
                            public void actionPerformed (ActionEvent e) {
                                bmvis.setClipboard(value);
                            }
                    });
                    menu.add(menuitem);
                }
            }
        } else {
            final BMEdge edge = (BMEdge)bme;
            final String epids = edge.getTo().getId() + "\n" +
                                 edge.getFrom().getId();
            String s;
            // DEBUG: Known bug (or feature?) - group as endpoint is
            // copied by group id, not group member ids
            menuitem = new JMenuItem(new AbstractAction("(endpoint ids)") {
                public void actionPerformed (ActionEvent e) {
                    bmvis.setClipboard(epids);
                }
            });
            menuitem.setText("<html><small>"+epids.replace("\n", " ")+"</small>");
            menu.add(menuitem);
            menu.addSeparator();

            menuitem = new JMenuItem(new AbstractAction(
                NODEINFO_PREFIX + "type:" +
                NODEINFO_INFIX + ((BMEdge)bme).getLinktype() + NODEINFO_SUFFIX) {
                    public void actionPerformed (ActionEvent e) {
                        bmvis.setClipboard(((BMEdge)bme).getLinktype());
                    }
            });
            menu.add(menuitem);

            for (String attribute : DISPLAY_EDGE_ATTRIBUTES) {
                s = bme.get(attribute);
                if (s != null && s.length() > 0) {
                    final String value = s;
                    menuitem = new JMenuItem(new AbstractAction(
                        NODEINFO_PREFIX + attribute.replace('_', ' ') + ":" +
                        NODEINFO_INFIX + s + NODEINFO_SUFFIX) {
                            public void actionPerformed (ActionEvent e) {
                                bmvis.setClipboard(value);
                            }
                    });
                    menu.add(menuitem);
                }
            }
        }

            // Show popup menu
        Point pt = SwingUtilities.convertPoint(e.getComponent(),
                e.getPoint(), display);
        menu.show(display, pt.x, pt.y);
        return true;
    }

    /**
     * Called when a VisualItem is clicked.
     * @param item The item that was clicked.
     * @param e The corresponding MouseEvent.
     */
    public void itemClicked (VisualItem item, MouseEvent e) {        
        
        if (!(item instanceof NodeItem))
            return;

        if (checkItemPopupMenu(item, e))
            return;

        int modifiers = e.getModifiers();

        if ((modifiers & LEFT_BUTTON) == LEFT_BUTTON) {
//            didUnPin = false;
            switch (e.getClickCount()) {
                case 1: // Select
                    if (selectGroup.getTupleCount() == 0)
                        return;
                    if (mayUnselect) {
                        selectGroup.removeTuple(item);
                        notifySelectionChange = true;
                    } else if (!e.isShiftDown() &&
                               !selectGroup.containsTuple(item)) {
                        selectGroup.setTuple(item);
                        notifySelectionChange = true;
                    }
                    break;
                case 2: // DEBUG: Open URL (unfortunately)
                    if (e.isShiftDown()) {
                        selectGroup.addTuple(item);                        
                        if (selectGroup.getTupleCount() > 1) {
                            zoomToFit(BMVis.SELECT_GROUP);
                            bmvis.notifySelectionChanged();
                            notifySelectionChange = false;
                            return;
                        }
                        else {
                            notifySelectionChange = true;
                        }
                    }
                    if (neighborGroup.getTupleCount() > 0)
                        zoomToFit(BMVis.NEIGHBOR_GROUP);
                default:
                    break;
            }
            
            if (notifySelectionChange) {
                bmvis.notifySelectionChanged();
                 notifySelectionChange = false;
            }
            return;
        }
    }

    // Called by the timer when the mouse is clicked just once
    private void mouseClickedSingle () {        
        selectGroup.clear();
        bmvis.notifySelectionChanged();
        notifySelectionChange = false;
    }

    /**
     * Called when the visualization is clicked with the mouse.
     * @param e The corresponding MouseEvent.
     */
    public void mouseClicked (MouseEvent e) {        
        int modifiers = e.getModifiers();
        if ((modifiers & MIDDLE_BUTTON) == MIDDLE_BUTTON) {
            zoomToFit(BMVis.GRAPH_NODES);
            return;
        }
        // DEBUG: Configure right double-click on white here
        if ((modifiers & RIGHT_BUTTON) == RIGHT_BUTTON &&
                e.getClickCount() > 1) {
            zoomToFit(BMVis.GRAPH_NODES);
            return;
        }

//        didUnPin = false;
        if ((modifiers & LEFT_BUTTON) != LEFT_BUTTON)
            return;

        if (mouseClickedTimer.isRunning() || e.getClickCount() > 1) {
            mouseClickedTimer.stop();
            // DEBUG: Configure left double-click on white here
            zoomToFit(BMVis.GRAPH_NODES);
        } else {
            mouseClickedTimer.restart();
        }
    }

    /**
     * Called when a mouse button is pressed over the display.
     * @param e The corresponding MouseEvent.
     */
    public void mousePressed (MouseEvent e) {
        display.requestFocus();

        dragStartX = e.getX();
        dragStartY = e.getY();
        int modifiers = e.getModifiers();
        if ((modifiers & LEFT_BUTTON) == LEFT_BUTTON) {
            display.setCursor(moveCursor);
        } else if (e.isPopupTrigger() ||
                   (modifiers & RIGHT_BUTTON) == RIGHT_BUTTON) {
            display.getAbsoluteCoordinate(e.getPoint(), point);
            display.setCursor(zoomCursor);
        }
    }

    /**
     * Called when the mouse is dragged over the display.
     * @param e The corresponding MouseEvent.
     */
    public void mouseDragged (MouseEvent e) {
        int modifiers = e.getModifiers();
        if (dragStartX == -1.0 && dragStartY == -1.0) {
            display.setCursor(Cursor.getDefaultCursor());
            return;
        }

        if ((modifiers & LEFT_BUTTON) == LEFT_BUTTON) {
            int x = e.getX();
            int y = e.getY();
            display.pan(x - dragStartX, y - dragStartY);
            dragStartX = x;
            dragStartY = y;
        } else if (e.isPopupTrigger() ||
                   (modifiers & RIGHT_BUTTON) == RIGHT_BUTTON ||
                   display.getCursor() == zoomCursor) {
            double y = e.getY();
            if (zoom(1.0 + ((y - dragStartY) / 125.0)))
                display.setCursor(zoomCursor);
            else
                display.setCursor(waitCursor);
            dragStartY = y;
        }
    }

    /**
     * Called when the mouse is released over the display.
     * @param e The corresponding MouseEvent.
     */
    public void mouseReleased (MouseEvent e) {
        display.setCursor(Cursor.getDefaultCursor());
        dragStartX = -1.0;
        dragStartY = -1.0;
    }

    /**
     * Called when the mouse wheel is moved over the display.
     * @param e The corresponding MouseWheelEvent.
     */
    public void mouseWheelMoved (MouseWheelEvent e) {
        display.requestFocus();
        display.getAbsoluteCoordinate(e.getPoint(), point);
        zoom(1.0 + (0.1 * e.getWheelRotation()));
    }

    /**
     * Called when the mouse wheel is moved over an item.
     * @param e The corresponding MouseWheelEvent.
     */
    public void itemWheelMoved (VisualItem item, MouseWheelEvent e) {
        mouseWheelMoved(e);
    }

    /**
     * Called when a key is typed while the cursor is over an item.
     * @param item The item over which the mouse is.
     * @param e The corresponding KeyEvent.
     */
    public void itemKeyTyped (VisualItem item, KeyEvent e) {
        char c = e.getKeyChar();
        if (c != ' ' && c != '\n')
            return;
        if (item instanceof NodeItem) {
            bmvis.openURLFor((NodeItem)item);
            return;
        }
        keyTyped(e);
    }

    /*
     * Called when a key is typed while the cursor is not over an item.
     * @param e The corresponding KeyEvent.
     */
    public void keyTyped (KeyEvent e) {
        return;
        // bmvis.openURLsFor(selectGroup);
    }

    public void keyPressed (KeyEvent e) { return; }
    public void keyReleased (KeyEvent e) { return; }
    public void mouseMoved (MouseEvent e) { return; }
    public void mouseEntered (MouseEvent e) { return; }
    public void mouseExited (MouseEvent e) { return; }
    public void itemKeyPressed (VisualItem item, KeyEvent e) { return; }
    public void itemKeyReleased (VisualItem item, KeyEvent e) { return; }
    public void itemMoved (VisualItem item, MouseEvent e) { return; }

    /**
     * Zoom the display to fit the boundaries of a given group.
     * @param group The name of the group to fit the Display to.
     */
    public void zoomToFit (String group) {
        display.getVisualization().getBounds(group, rect);
        GraphicsLib.expand(rect, 30 + (int)(1.0 / display.getScale()));

        double x = rect.getCenterX();
        double y = rect.getCenterY();
        Point2D center = new Point2D.Double(x, y);

        x = Math.max(x - rect.getMinX(), rect.getMaxX() - x);
        y = Math.max(y - rect.getMinY(), rect.getMaxY() - y);
        double scale = Math.min(display.getWidth() / (2.0 * x),
                                display.getHeight() / (2.0 * y));
        if (scale > MAX_SCALE)
            scale = MAX_SCALE;
        else if (scale < MIN_SCALE)
            scale = MIN_SCALE;
        scale /= display.getScale();
        display.animatePanAndZoomToAbs(center, scale, ZOOM_DURATION);
    }

    /**
     * Sets the enabled status of this control.
     * @param enabled The new enabled status for this control (true = enable).
     */
    public void setEnabled (boolean enabled) {
        isEnabled = enabled;
    }

    /**
     * Gets the enabled status of this control.
     * @return True iff this control is enabled.
     */
    public boolean isEnabled () { return isEnabled; }

    /**
     * Zoom this display by a given factor.
     * The zooming takes place around the point at which the previous
     * mouse event took place in this control.
     * @param zoom The zoom factor.
     * @return True on success, false when the display is busy.
     */
    protected boolean zoom (double zoom) {
        return zoom(zoom, point);
    }

    /**
     * Zoom this display by a given factor.
     * @param zoom The zoom factor.
     * @param point The point to zoom around (in Display coordinates).
     * @return True on success, false when the Display is busy.
     */
    public boolean zoom (double zoom, Point2D point) {
        if (display.isTranformInProgress())
            return false;

        double scale = display.getScale();
        double newScale = scale * zoom;

        if (newScale < MIN_SCALE)
            zoom = MIN_SCALE / scale;
        else if (newScale > MAX_SCALE)
            zoom = MAX_SCALE / scale;
        
        display.zoomAbs(point, zoom);
        //System.err.println("Zoom level: " + display.getScale());

        return true;
    }
    
}
