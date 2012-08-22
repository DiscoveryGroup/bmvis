package biomine.bmvis;

import biomine.bmgraph.BMGraph;
import biomine.bmgraph.BMNode;
import biomine.bmgraph.DatabaseLinks;
import biomine.bmgraph.read.BMGraphReader;
import biomine.bmgraph.write.BMGraphWriter;
import biomine.bmvis.action.EdgeAttributeColorAction;
import biomine.bmvis.action.NodeAttributeColorAction;
import biomine.bmvis.action.NodeStatusColorAction;
import biomine.bmvis.initial.GraphLayout;
import biomine.bmvis.render.ArrowEdgeRenderer;
import biomine.bmvis.render.BMNodeLabelRenderer;
import biomine.bmvis.render.NodeLabelRenderer;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.action.assignment.SizeAction;
import prefuse.action.layout.SpecifiedLayout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.util.force.ForceSimulator;
import prefuse.util.ui.JForcePanel;
import prefuse.util.ui.UILib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.applet.AppletContext;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

/**
 * Biomine graph visualization.
 * 
 * Note: this has been under some plugin-related changes recently by Lauri.
 * The aim was to keep this class as intact as possible and do everything
 * in a plugin-aware subclass. Unfortunately, it turned out some changes
 * were required here also. This is unfortunate, as BMVis is currently under
 * heavy refactoring and merging the versions could be painful.
 * 
 * The aim is to proceed so that once Kimmo's refactoring of this class
 * is finished, Kimmo and Lauri shall coordinatedly check how the changes made here
 * shall be migrated to the new implementation. For a list of performed changes, 
 * see (sub)class biomine.bmvispluginsBMVIS_lauri.
 * 
 * @author Kimmo Kulovesi
 */

public class BMVis extends JApplet {

    private static final String BIOMINE_URL = "http://www.cs.helsinki.fi/group/biomine/";

    private static final String DEFAULT_EDGE_LABEL = PrefuseBMGraph.TYPE_KEY;
    private static final String DEFAULT_NODE_SUBLABEL = PrefuseBMGraph.TYPE_KEY;

    private static final HashMap<String, String> ALLOWED_NODE_LABELS = new HashMap<String, String>();
    private static final HashMap<String, String> ALLOWED_EDGE_LABELS = new HashMap<String, String>();

    static {
        ALLOWED_NODE_LABELS.put(PrefuseBMGraph.TYPE_KEY, "Type");
        ALLOWED_NODE_LABELS.put(PrefuseBMGraph.DBID_KEY, "Source database ID");
        ALLOWED_NODE_LABELS.put("ShortName", "Short name");
        ALLOWED_NODE_LABELS.put("PrimaryName", "Long name");
        /*
        ALLOWED_NODE_LABELS.put(PrefuseBMGraph.KTNR_KEY, "K-Terminal Network Reliability");
        ALLOWED_NODE_LABELS.put(PrefuseBMGraph.TTNR_KEY, "Two-Terminal Network Reliability");
        */

        ALLOWED_EDGE_LABELS.put(PrefuseBMGraph.TYPE_KEY, "Type");
        ALLOWED_EDGE_LABELS.put(PrefuseBMGraph.GOODNESS_KEY, "Edge weight");
        ALLOWED_EDGE_LABELS.put(PrefuseBMGraph.SOURCE_DB_NAME_KEY, "Source database");
    }

    /**
     * Name of the graph in the Visualization.
     */
    public static final String GRAPH = "graph";
    
    /**
     * Name of the graph nodes in the Visualization.
     */
    public static final String GRAPH_NODES = GRAPH+".nodes";

    /**
     * Name of the graph edges in the Visualization.
     */
    public static final String GRAPH_EDGES = GRAPH+".edges";

    /**
     * Name of the "specified layout" action.
     */
    public static final String SPECIFIED_LAYOUT = "specified_layout";

    /**
     * Name of the drawing action.
     */
    public static final String DRAW_ACTION = "draw";

    /**
     * Name of the animating action.
     */
    public static final String ANIMATE_ACTION = "animate";

    /**
     * Name of the layout action.
     */
    public static final String LAYOUT_ACTION = "layout";

    /**
     * Name of the quick layout action.
     */
    public static final String QUICK_LAYOUT_ACTION = "quicklayout";

    /**
     * Focus group name for the currently highlighted node's neighbors.
     */
    public static final String NEIGHBOR_GROUP = "neighbors";

    /**
     * Focus group name for the currently selected nodes.
     */
    public static final String SELECT_GROUP = "selected";

    /**
     * Predicate for "edge nodes" (nodes representing edge labels).
     */
    public static final Predicate EDGENODE_PREDICATE =
        ExpressionParser.predicate("["+PrefuseBMGraph.EDGENODE_KEY+"]");

    /**
     * Predicate for "real nodes" (not edge nodes).
     */
    public static final Predicate REALNODE_PREDICATE =
        ExpressionParser.predicate("["+PrefuseBMGraph.EDGENODE_KEY+"] = false");

    /**
     * Predicate for groupnodes.
     */
    public static final Predicate GROUPNODE_PREDICATE =
        ExpressionParser.predicate("["+PrefuseBMGraph.GROUPNODE_KEY+"]");

    /**
     * Predicate for "arrow edges" (the side of the edge with the arrow).
     */
    public static final Predicate ARROW_EDGE_PREDICATE =
        ExpressionParser.predicate("["+PrefuseBMGraph.ARROW_KEY+"]");

    /**
     * Predicate for hovering.
     */
    public static final Predicate HOVER_PREDICATE =
                                  ExpressionParser.predicate(VisualItem.HOVER);

    /**
     * Predicate for pinned items.
     */
    public static final Predicate PINNED_PREDICATE =
        ExpressionParser.predicate("["+PrefuseBMGraph.PINNED_KEY+"]");

    /**
     * Is the layout algorithm currently enabled. Note that changing
     * this value does NOT change the status of the layout algorithm,
     * but must be toggled by running or canceling the LAYOUT_ACTION
     * in the Visualization.
     */
    public boolean layoutEnabled;

    /**
     * Should we enable automatic layout.
     */
    public boolean enableAutomaticLayout;
    
    /**
     * Should we pin stationary (that is, converged) nodes automatically.
     */
    protected boolean freezeLayout; 
    
    protected class BMGraphWindow extends JFrame {
        public BMGraphWindow (String title) {
            super(title);
            // DEBUG: Ask before quitting?
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(1000, 700);
        }
    }

    private class ReaderCallback implements BMGraphReader.ErrorCallback {
        private String messages;
        private int messageCount;

        public ReaderCallback () {
            messages = "";
            messageCount = 0;
        }

        public void readerError (String message, String file, int line,
                                 int column) {
            ++messageCount;
            message = "Error:"+line+":"+column+":"+message;
            System.err.println("BMGraph "+message);
            messages = messages+"\n"+message;
        }

        public void readerWarning (String message, String file, int line,
                                   int column) {
            message = "Warning:"+line+":"+column+":"+message;
            System.err.println("BMGraph "+message);
            messages = messages+"\n"+message;
            ++messageCount;
        }

        public String getMessages () { return messages; }
        public int getMessageCount () { return messageCount; }
    }

    private PrefuseBMGraph prefuseBMGraph;
    private BMGraph bmgraph;
    private Graph graph;
    private Visualization vis;
    private Display display;
    protected Container graphWindow;
    protected File graphLocation;
    private DefaultRendererFactory rendererFactory;
    protected BMNodeLabelRenderer nodeRenderer;
    private FreezingForceDirectedLayout layoutEngine;
    private String title;
    private JFileChooser fileChooser;
    protected JMenuBar menubar;
    private AppletContext appletContext;
    private URL graphURL;
    private VisualControl control;
    private ActionList layoutAction;
    private ArrowEdgeRenderer arrowRenderer;
    private String groupviewURL;
    protected boolean developerMode;
    protected boolean hideNodeTypes;

    /**
     * Has the graph been changed since last load/save?
     */
    public boolean changed = false;

    /**
     * Pin (fix) all VisualItems returned by a given iterator.
     * @param iterator Iterator for the items to pin.
     */

    public void pinAll (Iterator<VisualItem> iterator) {
        changed = true;        
        while (iterator.hasNext()) {
            VisualItem elem = iterator.next();
            elem.setFixed(false);
            elem.setFixed(true);
        }
        setLayoutEnabled(enableAutomaticLayout);
    }

    /**
     * Place an edgenode to the center position between connected nodes.
     * @param item Edgenode item.
     */

    public void centerEdgenode (NodeItem item) {
        if (!item.getBoolean(PrefuseBMGraph.EDGENODE_KEY))
            return;

        Iterator<NodeItem> iter = item.neighbors();
        NodeItem ni = iter.next();
        double nx = ni.getX();
        double ny = ni.getY();

        item.setStartX(item.getX());
        item.setStartY(item.getY());

        ni = iter.next();
        nx = (nx + ni.getX()) / 2.0;
        ny = (ny + ni.getY()) / 2.0;
        item.setX(nx);
        item.setY(ny);
        item.setEndX(nx);
        item.setEndY(ny);
    }

    /**
     * Unpin (release from fix) all VisualItems returned by a given iterator.
     * @param iterator Iterator for the items to unpin.
     */

    public void unPinAll (Iterator<VisualItem> iterator) {
        changed = true;
        while (iterator.hasNext()) {
            VisualItem elem = iterator.next();
            elem.setFixed(true);
            elem.setFixed(false);
            if (!layoutEnabled && elem instanceof NodeItem)
                centerEdgenode((NodeItem)elem);
        }
        setLayoutEnabled(enableAutomaticLayout);
    }

    /**
     * Get the Visualization used by this applet/application.
     * @return The Visualization in use.
     */

    public Visualization getVisualization () {
        return vis;
    }

    /**
     * Get the PrefuseBMGraph used by this applet/application.
     * @return The PrefuseBMGRaph in use.
     */

    public PrefuseBMGraph getGraph () {
        return prefuseBMGraph;
    }
    
    public BMGraph getBMGraph () {
        return bmgraph;
    }

    /**
     * Set the enabled status of the layout algorithm.
     * @param status The new status of the layout algorithm.
     */
    public void setLayoutEnabled (boolean status) {
        if (layoutAction != null) {
        	layoutEnabled = status;
        	layoutAction.setEnabled(layoutEnabled);
        }
    }

    public void allowLayoutFreezing() {
    	if (freezeLayout) layoutEngine.setFreezeStationary(true);
    }
    
    public void disallowLayoutFreezing() {
    	layoutEngine.setFreezeStationary(false);
    }
    
    /**
     * Get the Display used by this applet/application.
     * @return The Display in use.
     */

    public Display getDisplay () {
        return display;
    }

    /**
     * Ensure that the current visualization has a specified focus group.
     * @param group The focus group name. It will be added if it doesn't exist.
     * @return The specified focus group from the Visualization.
     */

    public TupleSet ensureHasFocusGroup (String group) {
        TupleSet fg = vis.getFocusGroup(group);
        if (fg == null) {
            vis.addFocusGroup(group);
            fg = vis.getFocusGroup(group);
            assert fg != null : "Failed to add focus group";
        }
        return fg;
    }

    public void unloadGraph () {
        if (graphWindow != null) {
            try {
                if (display != null)
                    graphWindow.remove(display);
                if (title != null)
                    setWindowTitle(title);
            } catch (Exception e) {
                // Ignore exceptions while unloading, may already be in
                // error state
            }
        }
        
        // Why had the menubar has to be removed, as was done below? (Lauri)
        // Modified this so that menubar is preserved when loading a new graph,
        // and the old menubar must not be disposed of, as is done here...
//        if (menubar != null) {
//            while (menubar.getMenuCount() > 1)
//                menubar.remove(menubar.getMenu(1));
//        }
        display = null;
        vis = null;
        bmgraph = null;
        graph = null;
        prefuseBMGraph = null;
        rendererFactory = null;
        nodeRenderer = null;
        layoutEngine = null;
        System.gc();
    }

    public List<BMNode> getSelectedNodes() {
        LinkedList<BMNode> result = new LinkedList();
        Iterator<VisualItem> iter = vis.items(SELECT_GROUP,
                                              REALNODE_PREDICATE);        
        VisualItem item;
        while (iter.hasNext()) {
            item = iter.next();
            BMNode bmnode = prefuseBMGraph.getBMNode((NodeItem)item);
            if (bmnode != null) {
                result.add(bmnode);
            }
        }
        return result;
    }
    
    private VisualItem getVisualItem (String nodeID) throws ParseException {
    	BMNode bmn = getGraph().getBMGraph().getNode(nodeID);
    	return getVisualization().getVisualItem(BMVis.GRAPH_NODES, prefuseBMGraph.getPrefuseNode(bmn));
    }
    
//    public void setNodeHighlight (String nodeID, boolean highlighted) throws ParseException {
//    	getVisualItem(nodeID).setHighlighted(highlighted);
//    }
    
    /** This is to be used from JavaScript to "highlight" nodes. */
    public void setSelectedNodes (String[] selectedIDs) {
    	TupleSet select = vis.getFocusGroup(SELECT_GROUP);
    	select.clear();
    	
    	for (String s: selectedIDs) {
    		try {
    			select.addTuple(getVisualItem(s));
    		} catch (ParseException pe) {
    			System.err.println(pe.toString() + " while trying to resolve \"" + s + "\".");
    		}
    	}    	
    }
    
    public void zoomToSelected () {
    	control.zoomToFit(SELECT_GROUP);
    }
    
    public void zoomToNodes (String[] nodeIDs) throws ParseException {
    	String groupName = nodeIDs.toString();
    	try {
    		vis.addFocusGroup(groupName);
    	} catch (Exception e) {}
    	
    	TupleSet temp = vis.getFocusGroup(groupName);
    	temp.clear();
    	
    	for (String s: nodeIDs) {
    		try {
    			temp.addTuple(getVisualItem(s));
    		} catch (ParseException pe) {
    			System.err.println(pe.toString() + " while trying to resolve \"" + s + "\".");
    		}
    	}
    	control.zoomToFit(groupName);
    }
    
    
    public boolean loadChosenGraph () {
        File file = fileChooser.getSelectedFile();
        if (file == null)
            return false;
        return loadGraph(file);
    }
    
    public boolean loadGraph (File file) {
                
        do {
            unloadGraph();
            graphLocation = file;
            if (loadGraph(false))
                return true;
            if (fileChooser.showOpenDialog(graphWindow) == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
            } else {
                JOptionPane.showMessageDialog(graphWindow,
                                              "No valid graph loaded, quitting...",
                                              "Error: No graph loaded", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } while (true);
    }

    private String getGroupNodeURL (Node node) {
        if (node == null)
            return null;
        BMNode bmnode = prefuseBMGraph.getBMNode(node);
        if (bmnode == null)
            return null;
        List<BMNode> members = bmgraph.getMembersFor(bmnode);
        return DatabaseLinks.getGroupURL(bmnode, members, groupviewURL);
    }

    /**
     * Open URL for the given node.
     * @param node The Prefuse Node to open URL for.
     */
    public void openURLFor (Node node) {
        if (node == null)
            return;

        String url = node.getString(PrefuseBMGraph.URL_KEY);

        if (url != null || node.getBoolean(PrefuseBMGraph.EDGENODE_KEY)) {
            // Only allow custom URL for edges
        } else if (node.getBoolean(PrefuseBMGraph.GROUPNODE_KEY)) {
            url = getGroupNodeURL(node);
        } else {
            url = DatabaseLinks.getURL(node.getString(PrefuseBMGraph.TYPE_KEY),
                                       node.getString(PrefuseBMGraph.DB_KEY),
                                       node.getString(PrefuseBMGraph.ID_KEY));
            if (url == null && groupviewURL != null) {
                // Display URL-less nodes with the group viewer
                BMNode bmnode = prefuseBMGraph.getBMNode(node);
                if (bmnode != null) {
                    url = DatabaseLinks.getGroupURL(bmnode, (List<BMNode>)null,
                                                    groupviewURL);
                }
            }
        }
        if (url != null)
            openURL(url);
    }

    /**
     * Open URLs for the given set of nodes.
     * @param group The TupleSet of nodes for which to open URLs.
     */
    public void openURLsFor (TupleSet group) {
        if (group == null)
            return;
        Iterator<VisualItem> iter = group.tuples();
        TreeSet<BMNode> nodes = new TreeSet<BMNode>();
        while (iter.hasNext()) {
            VisualItem item = iter.next();
            if (item instanceof NodeItem) {
                BMNode bmnode = prefuseBMGraph.getBMNode((NodeItem)item);
                if (bmnode == null) {
                    openURLFor((NodeItem)item);
                    continue;
                }
                if (item.getBoolean(PrefuseBMGraph.GROUPNODE_KEY)) {
                    String url = getGroupNodeURL((NodeItem)item);
                    if (url != null)
                        openURL(url);
                } else {
                    nodes.add(bmnode);
                }
            }
        }
        if (!nodes.isEmpty()) {
            LinkedList<String> urls = DatabaseLinks.getURLs(nodes.iterator(),
                                                            groupviewURL);
            if (urls != null) {
                for (String url : urls)
                    openURL(url);
            }
        }
    }

    private void openURL (String url) {
        if (url == null)
            return;
        if (appletContext != null) {
            try {
                appletContext.showDocument(new URL(url), "_blank");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(graphWindow,
                    "Failed to open URL: "+url+"\n"+e.getMessage(),
                    "Error opening URL", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        String os = System.getProperty("os.name");
        if (os.startsWith("Mac OS")) {
            try {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                    new Class[] {String.class});
                if (openURL != null)
                    openURL.invoke(null, new Object[] {url});
            } catch (Exception e) {
                System.err.println("Error opening URL:" + e.getMessage());
            }
            return;
        }
        if (os.startsWith("Windows")) {
            try {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler "
                                          + url);
            } catch (Exception e) {
                System.err.println("Error opening URL:" + e.getMessage());
            }
            return;
        }
        // DEBUG: Assuming Firefox for everything not OS X or Windows
        try {
            Runtime.getRuntime().exec(new String[] {
                "firefox", "-remote", "openurl("+url+", new-tab)" });
        } catch (Exception e) {
            System.err.println("Error opening URL:" + e.getMessage());
        }
    }
    
    
    private abstract class LabelChangeAction extends AbstractAction {
        protected String label;
        public LabelChangeAction (String l) { super(l); label = l; }
    }

    private abstract class DirectionChangeAction extends AbstractAction {
        protected NodeLabelRenderer.EdgeDirection direction;
        public DirectionChangeAction (NodeLabelRenderer.EdgeDirection dir) {
            super(dir.getName()); direction = dir;
        }
    }

    /**
     * Set the system clipboard contents.
     * @param str The String to set the system clipboard to.
     */
    public void setClipboard (String str) {
        assert str != null : "Null str";

        Clipboard clipboard;
        StringSelection data = new StringSelection(str);

        try {
            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard != null) {
                clipboard.setContents(data, null);
            }
        } catch (java.security.AccessControlException e) {
            if (appletContext != null) {
                try {
                    str = str.replace('\'', '"');
                    appletContext.showDocument(new URL(
                        "javascript:copyToClipboard(encodeURIComponent('"+str+"'))"));
                } catch (Exception e2) {
                    JOptionPane.showMessageDialog(graphWindow, e2.getMessage(),
                        "Copying to clipboard failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(graphWindow, e.getMessage(),
                "Copying to clipboard failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean loadGraph (boolean createMenus) {
        assert graphLocation != null : "Graph location not set";

        vis = null;
        bmgraph = null;
        graph = null;
        prefuseBMGraph = null;

        ReaderCallback results = new ReaderCallback();
        BMGraphReader reader = new BMGraphReader(results);
        InputStream input;
        try {
            if (graphURL == null)
                input = new FileInputStream(graphLocation);
            else
                input = graphURL.openStream();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(graphWindow, e.getMessage(),
                "Failed to open graph", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!reader.parseStream(input, graphLocation.getName())) {
            JOptionPane.showMessageDialog(graphWindow, results.getMessages(),
                "Failed to load graph", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (results.getMessageCount() > 0) {
            JOptionPane.showMessageDialog(graphWindow, results.getMessages(),
                "Warnings while loading BMGraph", JOptionPane.WARNING_MESSAGE);
        }
        bmgraph = reader.getGraph();
        
        //If initial positions are not set
        //they are solved used initial.GraphLayout.solvePositions
        //(Aleksi)
        boolean isPosSet = false;
        for (BMNode node : bmgraph.getNodes()) {
            if (node.get(PrefuseBMGraph.POS_KEY) != null) {
                isPosSet = true;
                break;
            }
        }
        if (!isPosSet) {
            GraphLayout.solvePositions(bmgraph);
            isPosSet = true;
            for (BMNode node : bmgraph.getSpecialNodes()) {
                node.put(PrefuseBMGraph.PINNED_KEY, "1");
            }
        }
		
        try {
            prefuseBMGraph = new PrefuseBMGraph(bmgraph);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(graphWindow,
                "Failed to obtain visualizable graph from loaded BMGraph!",
                "Error in graph conversion", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        {
            String[] db = bmgraph.getDatabaseArray();
            if (db[3] != null)
                groupviewURL = db[3] + DatabaseLinks.GROUPVIEW_PARAMS;
            else
                groupviewURL = DatabaseLinks.GROUPVIEW_URL;
            if (db[0] != null) {
                if (/*"biomine".equals(db[0]) &&*/ db[1] != null)
                    groupviewURL += "&version="+db[1];
            }
        }
        graph = prefuseBMGraph.getPrefuseGraph();
        System.gc();

        // Visualization and renderers

        vis = new Visualization();
        vis.add(GRAPH, graph);
        vis.setInteractive(GRAPH_EDGES, null, false);
        vis.addFocusGroup(NEIGHBOR_GROUP);
        vis.addFocusGroup(SELECT_GROUP);

        nodeRenderer = new BMNodeLabelRenderer(this,
                    hideNodeTypes ? null : DEFAULT_NODE_SUBLABEL, 
                    DEFAULT_EDGE_LABEL);
        arrowRenderer = new ArrowEdgeRenderer();
        arrowRenderer.setEdgeDirection(NodeLabelRenderer.EdgeDirection.CANONICAL);
        nodeRenderer.setEdgeDirection(NodeLabelRenderer.EdgeDirection.CANONICAL);
        rendererFactory = new DefaultRendererFactory(nodeRenderer, arrowRenderer);
        vis.setRendererFactory(rendererFactory);

        display = new Display(vis);
        display.setHighQuality(false);
        display.setSize(graphWindow.getSize());

        // Layout and controls

        SpecifiedLayout specifiedLayoutEngine;
        specifiedLayoutEngine = new SpecifiedLayout(GRAPH,
                                        PrefuseBMGraph.POS_X_KEY,
                                        PrefuseBMGraph.POS_Y_KEY);
        ActionList specifiedLayout = new ActionList();
        specifiedLayout.add(specifiedLayoutEngine);
        specifiedLayout.add(new RepaintAction());
        vis.putAction(SPECIFIED_LAYOUT, specifiedLayout);
        vis.run(SPECIFIED_LAYOUT);
        pinAll(vis.items(GRAPH_NODES, PINNED_PREDICATE));

        layoutEngine = new FreezingForceDirectedLayout
        					(GRAPH, false, 2000, this);
        ForceSimulator fsim = layoutEngine.getForceSimulator();
        try {
        	fsim.getForces()[2].setParameter(0, 0.0001f);
            fsim.getForces()[3].setParameter(0, 0.0000496f);
            fsim.getForces()[3].setParameter(1, 55f);
            fsim.getForces()[0].setParameter(0, -10f);
            fsim.getForces()[0].setParameter(1, -1f);
            fsim.getForces()[0].setParameter(2, 0.5f);
            fsim.getForces()[1].setParameter(0, 0.012f); // DEBUG: 0.004?
        } catch (Exception e) {
            System.err.println("Error: Disturbance in the force. (getForces)");
        }
        ForceDirectedLayout quickLayoutEngine;
        quickLayoutEngine = new ForceDirectedLayout(GRAPH, fsim, false, true);
        quickLayoutEngine.setIterations(50);
        
        control = new VisualControl(this);
        display.addControlListener(control);

        // Constant drawing (doesn't change after loading)

        ActionList draw = new ActionList();
        draw.add(new ColorAction(GRAPH_NODES, VisualItem.TEXTCOLOR,
                                    ColorPalette.NODE_TEXT));
        draw.add(new ColorAction(GRAPH_NODES, VisualItem.STROKECOLOR, 0));
        EdgeAttributeColorAction edgeStroke = new EdgeAttributeColorAction(
                GRAPH_EDGES, VisualItem.STROKECOLOR,
                PrefuseBMGraph.TYPE_KEY,
                PrefuseBMGraph.COMPUTED_STROKE_COLOR_KEY,
                ColorPalette.EDGE_COLORS,
                ColorPalette.DEFAULT_EDGE_STROKE,
                PrefuseBMGraph.RGB_STROKE_COLOR_KEY);
        draw.add(edgeStroke);
        edgeStroke = new EdgeAttributeColorAction(
                GRAPH_EDGES, VisualItem.FILLCOLOR,
                PrefuseBMGraph.TYPE_KEY,
                PrefuseBMGraph.COMPUTED_STROKE_COLOR_KEY,
                ColorPalette.EDGE_COLORS,
                ColorPalette.DEFAULT_EDGE_STROKE,
                PrefuseBMGraph.RGB_STROKE_COLOR_KEY);
        draw.add(edgeStroke);
        /*draw.add(new ColorAction(GRAPH_EDGES, VisualItem.FILLCOLOR,
                                    ColorPalette.NODE_TEXT));*/
        /*draw.add(new DataColorAction(GRAPH_EDGES, PrefuseBMGraph.GOODNESS_KEY,
                                               prefuse.Constants.NUMERICAL,
                                               VisualItem.STROKECOLOR));
                                               */
        NodeAttributeColorAction stroke = new NodeAttributeColorAction(
                GRAPH_NODES, VisualItem.STROKECOLOR,
                PrefuseBMGraph.QUERYSET_KEY, null,
                ColorPalette.QUERYSET_COLORS, 0, 0);
        stroke.add(GROUPNODE_PREDICATE, ColorPalette.GROUPNODE_STROKE);
        draw.add(stroke);
        StrokeAction strokeAction = new StrokeAction(GRAPH_NODES,
                                        new BasicStroke(2f,
                                            BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_BEVEL));
        strokeAction.add(GROUPNODE_PREDICATE, new BasicStroke(1.0f,
                                            BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_BEVEL));
        draw.add(strokeAction);
        strokeAction = new StrokeAction(GRAPH_EDGES,
                                        new BasicStroke(0.75f,
                                            BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_BEVEL));
        draw.add(strokeAction);
        SizeAction sizeAction = new SizeAction(GRAPH_NODES);
        sizeAction.add(GROUPNODE_PREDICATE, 1.5f);
        draw.add(sizeAction);

        // Animated drawing (changes due to user actions)

        ActionList animate = new ActionList(Activity.INFINITY, 49);
        animate.add(new NodeStatusColorAction(GRAPH_NODES, this));
        animate.add(new RepaintAction());

        // Layout actions

        layoutAction = new ActionList(Activity.INFINITY, 31);
        layoutAction.add(layoutEngine);

        ActionList quickLayoutAction = new ActionList();
        quickLayoutAction.add(quickLayoutEngine);
        quickLayoutAction.add(new RepaintAction());

        // Activate actions

        vis.putAction(DRAW_ACTION, draw);
        vis.putAction(ANIMATE_ACTION, animate);
        vis.putAction(LAYOUT_ACTION, layoutAction);
        vis.putAction(QUICK_LAYOUT_ACTION, quickLayoutAction);
        vis.run(DRAW_ACTION);
        vis.run(ANIMATE_ACTION);
        vis.run(LAYOUT_ACTION);
        layoutEnabled = true;

        // create menus (only the first time!)
        
        if (createMenus) {
            JMenu menu = new JMenu("Layout");
            menubar.add(menu);
            menu.add(new JMenuItem(new AbstractAction("Pin: Selected") {
                public void actionPerformed (ActionEvent e) {
                    pinAll(vis.items(SELECT_GROUP));
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Pin: Nodes") {
                public void actionPerformed (ActionEvent e) {
                    pinAll(vis.items(GRAPH_NODES, REALNODE_PREDICATE));
                }
            }));
            menu.addSeparator();
            menu.add(new JMenuItem(new AbstractAction("Release: Selected") {
                public void actionPerformed (ActionEvent e) {
                    unPinAll(vis.items(SELECT_GROUP));
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Release: Edges") {
                public void actionPerformed (ActionEvent e) {
                    unPinAll(vis.items(GRAPH_NODES, EDGENODE_PREDICATE));
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Release: All") {
                public void actionPerformed (ActionEvent e) {
                    unPinAll(vis.items());
                }
            }));
    
            if (developerMode) {
                menu.addSeparator();
                menu.add(new JMenuItem(new AbstractAction("Re-layout all") {
                    public void actionPerformed (ActionEvent e) {
                        boolean wasEnabled = enableAutomaticLayout;
                        setLayoutEnabled(false);
                        unPinAll(vis.items(GRAPH_NODES, PINNED_PREDICATE));
                        vis.run(QUICK_LAYOUT_ACTION);
                        setLayoutEnabled(wasEnabled);
                    }
                }));
                menu.add(new JMenuItem(new
                            AbstractAction("Revert to saved layout") {
                    public void actionPerformed (ActionEvent e) {
                        pinAll(vis.items(GRAPH_NODES, PINNED_PREDICATE));
                        vis.run(SPECIFIED_LAYOUT);
                    }
                }));
        
                menu.addSeparator();
                menu.add(new JMenuItem(new AbstractAction("Force-simulator setup...")
                {
                    public void actionPerformed (ActionEvent e) {                   	
                        JDialog jf = new JDialog();
                        jf.setTitle("BMVis Layout Forces");
                        jf.add(new JForcePanel(layoutEngine.getForceSimulator()),
                        		BorderLayout.NORTH);                      
                        jf.pack();
                        jf.setVisible(true);
                        jf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    }
                }));
            }
            
            menu.addSeparator();
            
            JCheckBoxMenuItem checkbox = new JCheckBoxMenuItem(
                new AbstractAction("Auto-pin after drag") {
                    public void actionPerformed (ActionEvent e) {
                        control.autoPin = !(control.autoPin);
                    }
                });
            menu.add(checkbox);
            checkbox.setSelected(control.autoPin);
            
			checkbox = new JCheckBoxMenuItem(new AbstractAction(
					"Enable automatic layout") {
				public void actionPerformed(ActionEvent e) {
					enableAutomaticLayout = !enableAutomaticLayout;
					setLayoutEnabled(enableAutomaticLayout);
				}
			});
            menu.add(checkbox);
            enableAutomaticLayout = true;
            setLayoutEnabled(enableAutomaticLayout);
            checkbox.setSelected(enableAutomaticLayout);
            
            checkbox = new JCheckBoxMenuItem(
            		new AbstractAction("Freeze stationary layout") {
            			public void actionPerformed(ActionEvent e) {
            				freezeLayout = !freezeLayout;
            				layoutEngine.setFreezeStationary(freezeLayout);
            				if (freezeLayout == false && enableAutomaticLayout) {
            					setLayoutEnabled(true);
            				}
            			}
            		});
            menu.add(checkbox);
            freezeLayout = true;
            checkbox.setSelected(freezeLayout);
           
            menu = new JMenu("Labels");
            menubar.add(menu);
            JMenu submenu = new JMenu("Node labels");
            menu.add(submenu);
            checkbox = new JCheckBoxMenuItem(
                new AbstractAction("(default label)") {
                    public void actionPerformed (ActionEvent e) {
                        nodeRenderer.setNodeLabelDisplay(
                            !nodeRenderer.getNodeLabelDisplay());
                    }
                });
            checkbox.setSelected(nodeRenderer.getNodeLabelDisplay());
            submenu.add(checkbox);
            Map<String, Class> attributes;
            attributes = prefuseBMGraph.getViewableNodeAttributes();
            for (String attribute : attributes.keySet()) {
                String attributeName = attribute;
                if (!developerMode) {
                    attributeName = ALLOWED_NODE_LABELS.get(attribute);
                    if (attributeName == null)
                        continue;
                }
                if (attributes.get(attribute) != boolean.class) {
                    checkbox = new JCheckBoxMenuItem(
                        new LabelChangeAction(attribute) {
                            public void actionPerformed (ActionEvent e) {
                                int i = nodeRenderer.nodeLabels.indexOf(label);
                                if (i < 0) {
                                    nodeRenderer.addNodeLabel(label);
                                } else {
                                    nodeRenderer.nodeLabels.remove(i);
                                    nodeRenderer.resetNodeLabels();
                                }
                            }
                        });
                    checkbox.setSelected(nodeRenderer.nodeLabels.contains(attribute));
                    checkbox.setText(attributeName);
                    submenu.add(checkbox);
                }
            }        
            
            submenu = new JMenu("Edge labels");
            menu.add(submenu);
            attributes = prefuseBMGraph.getViewableEdgeAttributes();
            for (String attribute : attributes.keySet()) {
                String attributeName = attribute;
                if (!developerMode) {
                    attributeName = ALLOWED_EDGE_LABELS.get(attribute);
                    if (attributeName == null)
                        continue;
                }
                if (attributes.get(attribute) != boolean.class) {
                    checkbox = new JCheckBoxMenuItem(
                        new LabelChangeAction(attribute) {
                            public void actionPerformed (ActionEvent e) {
                                int i = nodeRenderer.edgeLabels.indexOf(label);
                                if (i < 0) {
                                    nodeRenderer.addEdgeLabel(label);
                                } else {
                                    nodeRenderer.edgeLabels.remove(i);
                                    nodeRenderer.resetEdgeLabels();
                                }
                            }
                        });
                    checkbox.setSelected(nodeRenderer.edgeLabels.contains(attribute));
                    checkbox.setText(attributeName);
                    submenu.add(checkbox);
                }
            }

            if (developerMode) {
                submenu = new JMenu("Edge direction");
                menu.add(submenu);
                JRadioButtonMenuItem radioitem;
                ButtonGroup bg = new ButtonGroup();
                for (NodeLabelRenderer.EdgeDirection dir :
                     NodeLabelRenderer.EdgeDirection.values()) {
                    radioitem = new JRadioButtonMenuItem(new DirectionChangeAction(dir) {
                        public void actionPerformed (ActionEvent e) {
                            nodeRenderer.setEdgeDirection(direction);
                            arrowRenderer.setEdgeDirection(direction);
                        }
                    });
                    submenu.add(radioitem);
                    if (dir == nodeRenderer.getEdgeDirection())
                        radioitem.setSelected(true);
                    bg.add(radioitem);
                }
            }

            menu = new JMenu("Selection");
            menubar.add(menu);
            menu.add(new JMenuItem(new AbstractAction("View selected nodes in browser") {
                public void actionPerformed (ActionEvent e) {
                    openURLsFor(vis.getFocusGroup(SELECT_GROUP));
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Copy selected nodes to clipboard") {
                public void actionPerformed (ActionEvent e) {
                    StringBuffer nodestr = new StringBuffer();
                    List<BMNode> members;
                    for (BMNode node : getSelectedNodes()) {
                        members = bmgraph.getMembersFor(node);
                        if (members == null) {
                            nodestr.append(node.getId());
                            nodestr.append(" \n");
                        } else {
                            for (BMNode member : members) {
                                nodestr.append(member.getId());
                                nodestr.append(" \n");
                            }
                        }
                    }
                    setClipboard(nodestr.toString().trim());
                }
            }));
            menu.addSeparator();
            menu.add(new JMenuItem(new AbstractAction("Select all nodes") {
                public void actionPerformed (ActionEvent e) {
                    TupleSet select = vis.getFocusGroup(SELECT_GROUP);
                    Iterator<VisualItem> iter;
                    select.clear();
                    iter = vis.items(GRAPH_NODES, REALNODE_PREDICATE);
                    while (iter.hasNext())
                        select.addTuple(iter.next());
                    notifySelectionChanged();
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Select all edges") {
                public void actionPerformed (ActionEvent e) {
                    TupleSet select = vis.getFocusGroup(SELECT_GROUP);
                    Iterator<VisualItem> iter;
                    iter = vis.items(GRAPH_NODES, EDGENODE_PREDICATE);
                    select.clear();
                    while (iter.hasNext())
                        select.addTuple(iter.next());
                    notifySelectionChanged();
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Select all nodes and edges") {
                public void actionPerformed (ActionEvent e) {
                    TupleSet select = vis.getFocusGroup(SELECT_GROUP);
                    Iterator<VisualItem> iter = vis.visibleItems(GRAPH_NODES);
                    while (iter.hasNext())
                        select.addTuple(iter.next());
                    notifySelectionChanged();
                }
            }));
            menu.addSeparator();
            menu.add(new JMenuItem(new AbstractAction("Deselect all") {
                public void actionPerformed (ActionEvent e) {
                    TupleSet select = vis.getFocusGroup(SELECT_GROUP);
                    select.clear();
                    notifySelectionChanged();
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Invert selection") {
                public void actionPerformed (ActionEvent e) {
                    LinkedList<VisualItem> prev = new LinkedList<VisualItem>();
                    TupleSet select = vis.getFocusGroup(SELECT_GROUP);
                    Iterator<VisualItem> iter = vis.items(SELECT_GROUP);
                    while (iter.hasNext())
                        prev.add(iter.next());
                    iter = vis.visibleItems(GRAPH_NODES);
                    while (iter.hasNext())
                        select.addTuple(iter.next());
                    iter = prev.iterator();
                    while (iter.hasNext())
                        select.removeTuple(iter.next());
                    notifySelectionChanged();
                        
                }
            }));

            menu = new JMenu("Help");
            menubar.add(menu);
            menu.add(new JMenuItem(new AbstractAction("Show mouse instructions") {
                public void actionPerformed (ActionEvent e) {
                    JOptionPane.showMessageDialog(graphWindow,
    "<html><h3>Operations on nodes and edges</h3><div style=\"font-weight: normal\"><ul>" +
    "<li><b>Double-click</b> nodes to <b>zoom</b> their neighborhood to window</li>" +
    "<li><b>Click</b> to <b>select</b> nodes</li>" +
    "<li>Hold down <code>Shift</code> for multiple selection</li>" +
    "<li><b>Drag</b> nodes to <b>re-position</b> them</li>" +
    "<li><b>Right-click</b> (or <b>Ctrl-click</b> on Mac) to open <b>context menu</b> (e.g. to view node in browser)</b></li>"+
    "</ul></div>" +
    "<h3>Navigation</h3><div style=\"font-weight: normal\"><ul>" +
    "<li><b>Drag</b> on the white background to <b>pan</b> around</li>" +
    "<li><b>Double-click</b> on the white background to <b>fit view</b> to window</li>" +
    "<li><b>Right-drag</b> up and down on the white background to <b>zoom</b></li>" +
    "<li>The <b>scroll wheel</b> can also be used to <b>zoom</b></li>" +
    "</ul></div></html>", "BMVis Mouse Commands", JOptionPane.PLAIN_MESSAGE);
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Open Biomine website") {
                public void actionPerformed (ActionEvent e) {
                    openURL(BIOMINE_URL);
                }
            }));
    
            /////////////////////
            // added by Lauri:
            addPluginMenus();
            setMnemonicsAndAccelerators();
            /////////////////////
        } // end of menu creation
        
        // Show window and zoom

        graphWindow.add(display);
        if (appletContext == null)
            ((BMGraphWindow)graphWindow).pack();
        graphWindow.setVisible(true);
        setWindowTitle();
        display.repaint();
        graphWindow.repaint();

        Timer zoomTimer = new Timer(500, new AbstractAction() {
            public void actionPerformed (ActionEvent e) {
                control.zoomToFit(GRAPH);
            }
        });
        zoomTimer.setRepeats(false);
        zoomTimer.start();

        changed = false;
        layoutEngine.setFreezeStationary(freezeLayout);
        notifyGraphLoaded();
        
        return true;
    }

    // in the current (temporary) scheme, the subclass managing the plugins shall override this */
    protected void addPluginMenus() {
        // no action by default
    }
    
    // in the current (temporary) scheme, the subclass managing the plugins shall override this */
    protected void setMnemonicsAndAccelerators() {
        // no action by default
    }
    
    public class GraphFileFilter extends FileFilter {
        public boolean accept (File f) {
            if (f.isDirectory())
                return true;
            String ext = f.getName().replaceFirst("^([^.]*[.])*", "");
            return ("bmg".equalsIgnoreCase(ext) ||
                    "txt".equalsIgnoreCase(ext) ||
                    "bmgraph".equalsIgnoreCase(ext));
        }

        public String getDescription () {
            return "Biomine graphs (*.bmg, *.bmgraph, *.txt)";
        }
    }
    private final GraphFileFilter FILTER_GRAPH = new GraphFileFilter();

    public class PNGFileFilter extends FileFilter {
        public boolean accept (File f) {
            if (f.isDirectory())
                return true;
            String ext = f.getName().replaceFirst("^([^.]*[.])*", "");
            return ("png".equalsIgnoreCase(ext));
        }

        public String getDescription () {
            return "Portable Network Graphics (*.png)";
        }
    }
    private final PNGFileFilter FILTER_PNG = new PNGFileFilter();

    private void initWindow () {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { createGUI("BMVis"); }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to create GUI!");
            System.exit(1);
        }
    }

    public void init (File graphLocation) {
        assert graphLocation != null : "Null graph location";
        this.graphLocation = graphLocation;
        graphURL = null;
        appletContext = null;
        developerMode = true;
        initWindow();
    }

    /** Overridden by subclass to notify plugins about a new graph being loaded */  
    protected void notifyGraphLoaded() {
        // no action
    }
    
    /**
     * Overridden by subclass to notify plugins about bmvis being about to 
     * quit. The current graph should already have been saved (if applicable)
     * before this gets called. 
     */
    public void notifyBeforeExit() {
        // no action
    }
    
    /** Overridden by subclass to notify plugins about a change in the selection */  
    protected void notifySelectionChanged() {
        // no action
    }
    
    public void init () {
        appletContext = getAppletContext();
        if (appletContext == null) {
            System.err.println("Failed to get AppletContext.");
            System.exit(1);
        }
        String developer = getParameter("developer_mode");
        if (developer != null && ("1".equals(developer) ||
                                  "true".equalsIgnoreCase(developer))) {
            developerMode = true;
        } else {
            developerMode = false;
        }
        String graphFile = getParameter("graph");
        if (graphFile == null)
            graphFile = "graph.bmg";
        try {
            graphURL = new URL(getDocumentBase(), graphFile);
        } catch (Exception e) {
            System.err.println("Failed to get graph URL: "+e.getMessage());
            System.exit(1);
        }
        String types = getParameter("hide_node_types");
        if (types != null && ("1".equals(types) ||
                              "true".equalsIgnoreCase(types))) {
            hideNodeTypes = true;
        } else {
            hideNodeTypes = false;
        }
        graphLocation = new File(graphURL.getFile());
        initWindow();
    }

    private void setWindowTitle (String title) {
        if (appletContext != null || graphWindow == null)
            return;
        ((BMGraphWindow)graphWindow).setTitle(title);
    }

    private void setWindowTitle () {
        String filename = graphLocation.getName();
        filename = filename.replaceFirst("[.][A-Za-z]*$", "");
        setWindowTitle(filename + (title == null ? "" : " - " + title));
    }
        

    public boolean saveGraph () {
        return saveGraph(graphLocation);
    }
    
    public boolean saveGraph (File file) {
        PrintStream output;
        try {
            output = new PrintStream(file);
            prefuseBMGraph.syncAttributesToBMGraph();
            prefuseBMGraph.updatePositionsFromVisualization(vis);
            BMGraphWriter writer = new BMGraphWriter(bmgraph, output);
            writer.writeSorted(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(graphWindow, e.getMessage(),
                "Error saving graph", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        changed = false;
        return true;
    }

    private void exportToPNGDialog () {
        if (display == null)
            return;
        fileChooser.setFileFilter(FILTER_PNG);
        int r = fileChooser.showSaveDialog(graphWindow);
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(graphWindow,
                        "File \""+file.getName()+"\" exists. Overwrite?",
                        "Overwrite file?",
                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return;
            }
            try {
                double scale = 1.7 / display.getScale();
                if (scale < 1.0)
                    scale = 1.0;
                if (scale > 3.0)
                    scale = 3.0;
                display.saveImage(new FileOutputStream(file), "PNG", scale);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(graphWindow, e.getMessage(),
                    "Error exporting PNG", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveAsDialog () {
        if (prefuseBMGraph == null)
            return;
        fileChooser.setFileFilter(FILTER_GRAPH);
        int r = fileChooser.showSaveDialog(graphWindow);
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(graphWindow,
                        "File \""+file.getName()+"\" exists. Overwrite?",
                        "Overwrite file?",
                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return;
            }
            if (saveGraph(file)) {
                graphLocation = file;
                setWindowTitle();
            }
        }
    }

    private void createFileMenu () {
        assert menubar != null : "Null menubar";

        JMenu menu = new JMenu("File");
        menubar.add(menu);
        menu.add(new JMenuItem(new AbstractAction("Open...") {
            public void actionPerformed (ActionEvent e) {
                fileChooser.setFileFilter(FILTER_GRAPH);
                int r = fileChooser.showOpenDialog(graphWindow);
                if (r == JFileChooser.APPROVE_OPTION) {
                    if (changed) {
                        switch (JOptionPane.showConfirmDialog(graphWindow,
                                "Do you wish to save the current graph first?",
                                "Save graph?",
                                JOptionPane.YES_NO_CANCEL_OPTION))
                        {
                            case JOptionPane.YES_OPTION:
                                if (!saveGraph(graphLocation)) return;
                                break;
                            case JOptionPane.CANCEL_OPTION:
                                return;
                            default:
                                break;
                        }
                    }
                    loadChosenGraph();
                }
            }
        }));
        menu.add(new JMenuItem(new AbstractAction("Save") {
            public void actionPerformed (ActionEvent e) {
                saveGraph(graphLocation);
            }
        }));
        menu.add(new JMenuItem(new AbstractAction("Save as...") {
            public void actionPerformed (ActionEvent e) { saveAsDialog(); }
        }));
        if (appletContext == null) {
            menu.addSeparator();
            menu.add(new JMenuItem(new AbstractAction("Export view to PNG...") {
                public void actionPerformed (ActionEvent e) {
                    exportToPNGDialog();
                }
            }));
        }
        menu.addSeparator();
        menu.add(new JMenuItem(new AbstractAction("Quit") {
            public void actionPerformed (ActionEvent e) {
                // DEBUG: Should this be asked when closing the window?
                if (changed) {
                    switch (JOptionPane.showConfirmDialog(graphWindow,
                            "Do you wish to save the current graph first?",
                            "Save graph?", JOptionPane.YES_NO_CANCEL_OPTION))
                    {
                        case JOptionPane.YES_OPTION:
                            if (!saveGraph(graphLocation))
                                return;
                            break;
                        case JOptionPane.CANCEL_OPTION:
                            return;
                        default:
                            break;
                    }
                }
                System.exit(0);
            }
        }));
    }
    
    private void createGUI (String title) {
        try {
            System.setProperty("com.apple.macosx.AntiAliasedTextOn", "false");
            System.setProperty("com.apple.macosx.AntiAliasedGraphicsOn", "false");
        } catch (Throwable t) {
            // Ignore property exceptions
        }
        try {
            System.setProperty("sun.java2d.opengl", "true");
        } catch (Throwable t) {
            // Ignore property exceptions
        }

        this.title = title;
        menubar = new JMenuBar();

        if (appletContext == null) {
            UILib.setPlatformLookAndFeel();
            BMGraphWindow bmgw = new BMGraphWindow(title);
            graphWindow = bmgw;
            fileChooser = new JFileChooser(".");
            fileChooser.setFileFilter(FILTER_GRAPH);
            createFileMenu();
            bmgw.setJMenuBar(menubar);
        } else {
            graphWindow = this;
            setJMenuBar(menubar);
        }

        // Load the initial graph

        if (!loadGraph(true)) {
            // Show file dialog on failure?
            System.exit(1);
        }
    }

    public static void main (String[] args) {
        BMVis bmvis = new BMVis();
        if (args.length != 1) {
            System.err.println("Error: No graph location/filename as argument!");
            System.exit(1);
        }
        bmvis.init(new File(args[0]));
    }
}
