package biomine.bmvis.render;

import biomine.bmgraph.BMEdge;
import biomine.bmgraph.BMGraph;
import biomine.bmvis.BMVis;
import biomine.bmvis.PrefuseBMGraph;

import prefuse.Visualization;
import prefuse.data.expression.Predicate;
import prefuse.render.Renderer;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.util.FontLib;
import prefuse.util.ColorLib;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Stroke;

/**
 * A renderer for nodes and edgenodes, with labels, fill and borders.
 *
 * <p>Edge nodes are nodes that are used to represent edges, but are
 * rendered as nodes to display attributes, etc. These "edge nodes"
 * are recognized by checking for the EDGENODE_KEY, which must
 * be true exactly for the set of edge nodes.
 *
 * <p>Labels are pre-computed and cached in an attribute
 * (defaulting to PrefuseBMGraph.COMPUTED_LABEL_KEY). The cached
 * labels are displayed until they are reset either by calling the
 * resetLabels() method in this class, or by setting the cache attribute
 * for the affected VisualItems to null.
 *
 * <p>For edge labels, the reversal or canonization of direction is also
 * handled, including re-mapping the ARROW_KEY values to the edges
 * that should carry the arrow reflecting the labels. However, proper
 * rendering of the edge arrows must be done by ArrowEdgeRenderer
 * or a similar rendering setup aware of directions.
 *
 * <p>Parts of the item rendering process are cached in an attribute
 * (defaulting to PrefuseBMGraph.COMPUTED_PARAMS_KEY). The renderer
 * takes care of validating the cache upon render, when necessary.
 *
 * @author Kimmo Kulovesi
 */

public class NodeLabelRenderer implements Renderer {

    /**
     * The attribute key to use for caching pre-computed labels.
     * The attribute MUST exist for all items ever rendered by this
     * renderer. The cached result must be externally invalidated by
     * unsetting the attribute. The attribute is of type String[].
     */
    public String labelCache = PrefuseBMGraph.COMPUTED_LABEL_KEY;

    /**
     * The attribute key to use for caching pre-computed parameters.
     * The attribute MUST exist for all items ever rendered by this
     * renderer. The attribute is of type Object, and must not be
     * externally touched.
     */
    public String paramCache = PrefuseBMGraph.COMPUTED_PARAMS_KEY;

    /**
     * Edgenode labeling and rendering directions.
     */
    public enum EdgeDirection {
        /**
         * Canonical direction, no reverse linknames anywhere.
         */
        CANONICAL("Canonical"),
        /**
         * Forward direction, as stored in the graph.
         */
        FORWARD("Forward"),
        /**
         * Reverse direction, opposite of what is stored in the graph.
         */
        REVERSED("Reversed");

        private String name;
        EdgeDirection (String desc) { name = desc; }

        /**
         * Get the beautified name of this direction.
         */
        public String getName () { return name; }
    }
    
    /**
     * The string which can be used to refer to the default attribute.
     * The default attribute is determined by node type using the hash
     * DEFAULT_KEY_FOR_TYPE. Using this string as an attribute name in
     * this class will refer to that default type.
     */
    public static final String DEFAULT_KEY = "_default";

    /**
     * Threshold for scale (zoom) over which precision rendering is required.
     * Before this threshold, integer precision is used. Defaults to 4.
     * (Note: With the usual limitations set to zooming, it is likely
     * that this threshold is never reached and integer precision is
     * always used.)
     */
    public static double PRECISION_THRESHOLD = 4;

    /**
     * Threshold for font size under which the label text isn't rendered.
     * Defaults to 4.
     */
    public static double LABEL_THRESHOLD = 4;

    /**
     * The mapping from node types to the "default" attribute for that type.
     * The DEFAULT_KEY attribute name can be used to refer to the
     * default attribute for each node type, set in this map. For node
     * types not having a default attribute specified explicitly,
     * the universal default can be set by mapping the empty string ("")
     * to an attribute name.
     *
     * <p>In this class it is possible to use the attribute name
     * DEFAULT_KEY to refer to the default attribute set in this hash.
     *
     * <p>By default, the default attribute is PrefuseBMGraph.ORGANISM_KEY.
     */
    public static final HashMap<String, String> DEFAULT_KEY_FOR_TYPE =
                                                new HashMap<String, String>();
    static {
        DEFAULT_KEY_FOR_TYPE.put("", PrefuseBMGraph.ORGANISM_KEY);
    }

    /**
     * The mapping from node types to default attribute values for that type.
     * The default attribute for node types is determined by
     * DEFAULT_KEY_FOR_TYPE. This map determines the default value of
     * that default attribute by node type. This default value may
     * either be used to disable the display of the default value, or to
     * set the default value that is displayed for the nodes not having
     * a default attribute.
     *
     * <p>For example, by default suffix attribute is organism, and by
     * default the value of "hsa" (homo sapiens, human) is not shown for
     * genes or protein, which default to being human genes or proteins
     * in the Biomine project's graphs.
     */
    public static final HashMap<String, String> DEFAULT_VALUE_FOR_TYPE =
                                                new HashMap<String, String>();
    
    static {
        DEFAULT_VALUE_FOR_TYPE.put("Gene", "hsa");
        DEFAULT_VALUE_FOR_TYPE.put("Protein", "hsa");
    }

    /**
     * Arc size of the rounder corner.
     * Must be positive (not checked). Defaults to 24.
     */
    public int arcSize;
    
    /**
     * Width of the padding between the content and border.
     * Must be non-negative (not checked). Defaults to 20.
     */
    public int padWidth;

    /**
     * Height of the padding between the content and border.
     * Must be positive (not checked). Defaults to 5.
     */
    public int padHeight;

    /**
     * The list of attributes to render as edgenode labels.
     * See the description of getLabel() for list syntax.
     * @see biomine.bmvis.render.NodeLabelRenderer#getLabel
     */
    public final LinkedList<String> edgeLabels;

    /**
     * The list of attributes to render as node labels.
     * See the description of getLabel() for list syntax.
     * @see biomine.bmvis.render.NodeLabelRenderer#getLabel
     */
    public final LinkedList<String> nodeLabels;

    /**
     * Create a NodeLabelRenderer with given node and edge label attributes.
     * @param bmvis The application this renderer is to be used with.
     * @param nodeLabel Attribute to use for node sublabels (can be null).
     * @param edgeLabel Attribute to use for edge labels (can be null).
     */
    public NodeLabelRenderer (BMVis bmvis, String nodeLabel, String edgeLabel) {
        assert bmvis != null : "Null bmvis";
        edgeLabels = new LinkedList<String>();
        nodeLabels = new LinkedList<String>();
        if (nodeLabel != null)
            nodeLabels.add(nodeLabel);
        if (edgeLabel != null)
            edgeLabels.add(edgeLabel);
        this.edgeDirection = EdgeDirection.FORWARD;
        this.bmvis = bmvis;
        this.vis = bmvis.getVisualization();
        this.showDefaultValue = false;
        this.arcSize = 24;
        this.padWidth = 20;
        this.padHeight = 5;
        itemShape = new RoundRectangle2D.Double();
    }

    private boolean showDefaultValue;
    private BMVis bmvis;
    private Visualization vis;
    private EdgeDirection edgeDirection;
    
    /**
     * Reset the pre-computed labels in the graph according to a given filter.
     *
     * <p>This must be called after changing the label attributes. The
     * labels for individual nodes must be reset when the node
     * attributes change, by setting the cached label to null (the
     * attribute used for the cache is set by the constructor).
     *
     * <p>If only either of the node or edge labels are changed, the
     * predicate filter should be used to match only realnodes
     * (REALNODE_PREDICATE) or edgenodes (EDGENODE_PREDICATE).
     *
     * @param filter Filter that matches the nodes to be reset, null for all.
     */
    public void resetLabels (Predicate filter) {
        VisualItem item;
        Iterator<VisualItem> iter;

        if (filter == null)
            iter = vis.items(BMVis.GRAPH_NODES);
        else
            iter = vis.items(BMVis.GRAPH_NODES, filter);
        while (iter.hasNext()) {
            item = iter.next();
            item.set(labelCache, null);
        }
        vis.repaint();
    }

    /**
     * Reset all labels in the graph.
     */
    public void resetLabels () {
        resetLabels(null);
    }

    /**
     * Reset the labels of all real nodes in the graph.
     */
    public void resetNodeLabels () {
        resetLabels(BMVis.REALNODE_PREDICATE);
    }

    /**
     * Reset the labels of all edgenodes in the graph.
     */
    public void resetEdgeLabels () {
        resetLabels(BMVis.EDGENODE_PREDICATE);
    }

    private void setAttributeInList (LinkedList<String> list,
                                String old, String attribute) {
        int i = -1;
        if (old != null)
            i = list.indexOf(old);
        if (i >= 0) {
            if (attribute == null)
                list.remove(i);
            else
                list.set(i, attribute);
        } else if (attribute != null) {
            list.addLast(attribute);
        }
    }

    /**
     * Add a new edge label attribute to the end of the list.
     * @param attribute Attribute to add for edge labels.
     */
    public void addEdgeLabel (String attribute) {
        if (attribute == null)
            return;
        edgeLabels.remove(attribute);
        edgeLabels.addLast(attribute);
        resetEdgeLabels();
    }

    /**
     * Add a new node label attribute to the end of the list.
     * @param attribute Attribute to add for node labels.
     */
    public void addNodeLabel (String attribute) {
        if (attribute == null)
            return;
        nodeLabels.remove(attribute);
        nodeLabels.addLast(attribute);
        resetNodeLabels();
    }

    /**
     * Replace an edge label attribute with another. If the old
     * attribute does not exist, the new is added to the end of the
     * list.
     * @param old Old edge label attribute to replace.
     * @param attribute New attribute for edge labels.
     */
    public void replaceEdgeLabel (String old, String attribute) {
        setAttributeInList(edgeLabels, old, attribute);
        resetEdgeLabels();
    }

    /**
     * Replace a node label attribute with another. If the old attribute
     * does not exist, the new is added to the end of the list.
     * @param old Old node label attribute to replace.
     * @param attribute New attribute for node labels.
     */
    public void replaceNodeLabel (String old, String attribute) {
        setAttributeInList(nodeLabels, old, attribute);
        resetNodeLabels();
    }

    /**
     * Remove an attribute from the edge label.
     * @param attribute Attribute to remove.
     */
    public void removeEdgeLabel (String attribute) {
        edgeLabels.remove(attribute);
        resetEdgeLabels();
    }

    /**
     * Remove an attribute from the node label.
     * @param attribute Attribute to remove.
     */
    public void removeNodeLabel (String attribute) {
        nodeLabels.remove(attribute);
        resetNodeLabels();
    }

    /**
     * Clear node label (nodeLabels).
     */
    public void clearNodeLabel () {
        nodeLabels.clear();
        resetNodeLabels();
    }

    /**
     * Clear edge label (edgeLabels).
     */
    public void clearEdgeLabel () {
        edgeLabels.clear();
        resetNodeLabels();
    }

    /**
     * Set the node labels to contain only one attribute.
     * @param attribute The attribute to make up the new node labels list.
     */
    public void setNodeLabelTo (String attribute) {
        clearNodeLabel();
        if (attribute != null)
            nodeLabels.add(attribute);
    }

    /**
     * Set the edge labels to contain only one attribute.
     * @param attribute The attribute to make up the new edge labels list.
     */
    public void setEdgeLabelTo (String attribute) {
        clearEdgeLabel();
        if (attribute != null)
            edgeLabels.add(attribute);
    }


    /**
     * Get the edge labeling direction.
     * @return The current direction for rendering edge labels.
     */
    public EdgeDirection getEdgeDirection () { return edgeDirection; }

    /**
     * Set the edge labeling direction.
     * Note that this will also set the arrow directions in the graph
     * accordingly (PrefuseBMGRaph.ARROW_KEY). For them to render
     * correctly, the EdgeRenderer must be set up to render the proper
     * arrow directions, however.
     * @param dir The direction for edge labels.
     */
    public void setEdgeDirection (EdgeDirection dir) {
        if (edgeDirection == dir)
            return;
        edgeDirection = dir;

        VisualItem item;
        EdgeItem out, in;
        boolean doReverse;
        Iterator<VisualItem> iter;
        BMGraph bmgraph = bmvis.getGraph().getBMGraph();

        doReverse = (edgeDirection == EdgeDirection.REVERSED);
        iter = vis.items(BMVis.GRAPH_NODES, BMVis.EDGENODE_PREDICATE);
        while (iter.hasNext()) {
            item = iter.next();
            item.set(labelCache, null);
            out = (EdgeItem)(((((NodeItem)item).outEdges())).next());
            in = (EdgeItem)(((((NodeItem)item).inEdges())).next());
            if (!bmgraph.isSymmetricType(out.getString(
                                                PrefuseBMGraph.TYPE_KEY))) {
                if (edgeDirection == EdgeDirection.CANONICAL) {
                    doReverse = item.getBoolean(
                            PrefuseBMGraph.REVERSED_EDGE_KEY);
                }
                out.setBoolean(PrefuseBMGraph.ARROW_KEY, !doReverse);
                in.setBoolean(PrefuseBMGraph.ARROW_KEY, doReverse);
            }
        }
    }

    /**
     * Toggle the display of default attribute values.
     * The map DEFAULT_KEY_FOR_TYPE determines the "default attribute"
     * by node type. The map DEFAULT_VALUE_FOR_TYPE determines the
     * default value of that attribute. This can be used to either
     * disable the display of default values (as unnecessary) or to
     * force a default value for nodes not having a value for their
     * default attribute. The default is not to display the default
     * value of the default attribute. Note that the decision to not
     * display the default value applies only when the attribute is
     * referred to as DEFAULT_KEY, not for the true name of the
     * attribute.
     * @param status Should default attributes' default values be displayed.
     */
    public void setShowDefaultValue (boolean status) {
        showDefaultValue = status;
        resetLabels();
    }

    private String getReadableEdgeType (VisualItem item) {
        BMGraph bmgraph = bmvis.getGraph().getBMGraph();
        return bmgraph.getReadableType((BMEdge)item.get(
                    PrefuseBMGraph.BMENTITY_LINK_KEY)).replace('_', ' ');
    }

    private String getReadableReverseEdgeType (VisualItem item) {
        BMGraph bmgraph = bmvis.getGraph().getBMGraph();
        return bmgraph.getReadableReverseType((BMEdge)item.get(
                    PrefuseBMGraph.BMENTITY_LINK_KEY)).replace('_', ' ');
    }

    /**
     * Get the default attribute name for a given node type.
     * @param type Node type.
     * @return The default attribute name for the given node type, or
     * null if none set.
     */
    public String getDefaultAttribute (String type) {
        String s = DEFAULT_KEY_FOR_TYPE.get(type);
        if (s == null)
            return DEFAULT_KEY_FOR_TYPE.get("");
        return s;
    }

    /**
     * Get the default attribute's default valuefor a given node type.
     * @param type Node type.
     * @return The default attribute's default value for the given node type, or
     * null if none set.
     */
    public String getDefaultValue (String type) {
        String s = DEFAULT_VALUE_FOR_TYPE.get(type);
        if (s == null)
            return DEFAULT_VALUE_FOR_TYPE.get("");
        return s;
    }

    /**
     * Get a String representation of an attribute from a VisualItem
     * representing a node or an edgenode. This converts the type
     * attributes of edgenodes according to the edge rendering
     * direction set by setEdgeDirection(). The attribute DEFAULT_KEY
     * can be used to refer to the default attribute for each node type,
     * as set in the map DEFAULT_KEY_FOR_TYPE.
     * @param item VisualItem representing a node or an edgenode.
     * @param attribute The attribute to get.
     * @return A String representation of the attribute, or null if not set.
     */
    public String getAttributeFrom (VisualItem item, String attribute) {
        String s;
        String defaultType = null;
        if (attribute == null)
            return null;

        if (DEFAULT_KEY.equals(attribute)) {
            defaultType = item.getString(PrefuseBMGraph.TYPE_KEY);
            attribute = getDefaultAttribute(defaultType);
        }
        if (attribute == null || !item.canGetString(attribute))
            return null;

        if (item.getBoolean(PrefuseBMGraph.EDGENODE_KEY) &&
            PrefuseBMGraph.TYPE_KEY.equals(attribute)) {
            switch (edgeDirection) {
                case REVERSED:
                    if (item.getBoolean(PrefuseBMGraph.REVERSED_EDGE_KEY))
                        return getReadableEdgeType(item);
                    else
                        return getReadableReverseEdgeType(item);
                case CANONICAL:
                    if (item.getBoolean(PrefuseBMGraph.REVERSED_EDGE_KEY))
                        return getReadableEdgeType(item);
                case FORWARD:
                default:
                    // Default behavior
            }
        } else if (item.get(attribute) == null) {
            if (showDefaultValue && defaultType != null)
                return getDefaultValue(defaultType);
            return null;
        }
        s = item.getString(attribute);
        if (!showDefaultValue && defaultType != null) {
            if (s == null || s.equals(getDefaultValue(defaultType)))
                return null;
        }
        return s;
    }

    /**
     * Make the label for a given VisualItem based on a list of attributes.
     * 
     * <p>The list of attributes is essentially a list of attribute
     * names, each of which makes up one line of the label. There are
     * a number of additional rules that can be used to make more
     * complex rules for renderding the label:
     *
     * <p>Attributes prefixed with a dash ("-") are separated from
     * the previous label item with a space instead of a newline.
     *
     * <p>To render an attribute in parentheses on the same line as the
     * previous attribute, the name in the list can be prefixed with an
     * open parentheses character, "(". For example "(key_name" would
     * render as "previous (key_value)".
     *
     * <p>To include the attribute name in the label, prefix the name
     * with an equals-sign ("="); "=key" would render as "key=value".
     *
     * <p>To add a literal string to the label (instead of the value of
     * the attribute thus named), prefix the string in the list with
     * double quotes ("). The literal string can furthermore be prefixed
     * with a single space to place it on the same line as the previous
     * item instead of the next line.
     *
     * <p>For conditional rendering of labels, any of the above options
     * can furthermore be prefixed with a bar ("|") causing it to render
     * only when the previous attribute resulted in the empty string.
     * The inverse of this is the ampersand ("&amp;") prefix, which
     * will render only when the previous one also rendered.
     *
     * <p>As an example, ("key", "|\"not set") would render either as
     * "key_value" (when set) or "not set". Likewise, ("key1", "&amp;(key2")
     * would render as "key1_value" or "key1_value (key_value2)", or
     * not at all.
     *
     * <p>The exclamation point ("!") prefix causes everything after the
     * previous linefeed (or start of label) to be deleted if the
     * attribute is not rendered. It can be used to delete preceeding
     * attributes which would be meaningless without this one. The
     * inverse of this is the question mark ("?") prefix, which replaces
     * everything after the previous linefeed with this attribute if it
     * renders. These can be can be used as prefixes for any of the
     * above options.
     *
     * <p>Note that the chain ("key1", "&amp;key2", "|key3") could
     * render as the empty string (key1 and key3 both unset), or
     * as "value1" (key1 set, key2 unset), or as "value1\nvalue1"
     * (key1 and key2 both set), or as "value3" (key1 unset). That is,
     * "|key3" will then be alternative to "key1", not "&amp;key2".
     * There is no way to say the opposite with these rules. Still, the
     * limitations make this implementation relatively fast
     * and simple. For more complex rules, consider subclassing.
     *
     * <p>The order of the prefixes matters, i.e. "!" or "?" must come
     * first, then "|" or "&amp;", and then any one of the remaining
     * options before the attribute name (or literal string).
     *
     * @param item The item for which to make a label.
     * @param attributes Iterator for the list of attributes from which to
     * make the label.
     * @param exclude Exclude the viewing of these attributes (can be null).
     * @return The label for item (never null).
     */
    public String getLabel (VisualItem item, Iterator<String> attributes,
                               Set<String> exclude) {
        String key;
        String text = "";
        String s = null;
        String prev = null;
        char condition;
        boolean noLinefeed = true;
        boolean parentheses, includeName;

        // DEBUG: All the fancy stuff in here is pretty much untested

        if (attributes == null)
            return text;
        while (attributes.hasNext()) {
            key = attributes.next();
            if (key == null || key.length() < 1)
                continue;
            includeName = false;
            parentheses = false;
            s = null;
            condition = ' ';
            switch (key.charAt(0)) {
                case '!':
                case '?':
                    condition = key.charAt(0);
                    key = key.substring(1);
                default:
                    break;
            }
            assert key.length() > 0 : "invalid attribute syntax";
            switch (key.charAt(0)) {
                case '|':
                    if (prev != null)
                        continue;
                    key = key.substring(1);
                case '&':
                    if (prev == null)
                        continue;
                    key = key.substring(1);
                default:
                    break;
            }
            assert key.length() > 0 : "invalid attribute syntax";
            switch (key.charAt(0)) {
                case '\"':
                    s = key.substring(1);
                    assert s.length() > 0 : "invalid literal label syntax";
                    if (s.charAt(0) == ' ') {
                        s = s.substring(1);
                        assert s.length() > 0 : "invalid literal label syntax";
                        noLinefeed = true;
                    }
                    break;
                case '=':
                    includeName = true;
                    key = key.substring(1);
                    break;
                case '(':
                    parentheses = true;
                case '-':
                    noLinefeed = true;
                    key = key.substring(1);
                default:
                    break;
            }
            assert key.length() > 0 : "invalid attribute syntax";
            if (s == null)
                s = getAttributeFrom(item, key);
            if (s == null || s.length() == 0
                || (exclude != null && exclude.contains(key))) {
                if (condition == '!' && text.length() > 0) {
                    int lf = text.lastIndexOf('\n');
                    if (lf < 0)
                        text = "";
                    else
                        text = text.substring(0, lf);
                }
                prev = null;
            } else {
                prev = s;
                if (condition == '?') {
                    int lf = text.lastIndexOf('\n');
                    if (lf < 0)
                        text = "";
                    else
                        text = text.substring(0, lf);
                }
                if (includeName)
                    s = key + "=" + s;
                if (parentheses)
                    s = "(" + s + ")";
                if (text.length() > 0)
                    text = text + (noLinefeed ? " " : "\n");
                text = text + s;
                noLinefeed = false;
            }
        }
        return text;
    }

    /**
     * Get a the label for a given VisualItem.
     * <p>No attributes are excluded and the attributes that make up the
     * label are selected by the node type: edgeLabels for edgenodes
     * and nodeLabels for real nodes.
     *
     * <p>It is recommended that subclasses override this method for
     * implementing custom label rules.
     *
     * @param item The node or edge item to obtain a label for.
     * @return The label (never null);
     */
    public String getLabel (VisualItem item) {
        return getLabel(item,
                        (item.getBoolean(PrefuseBMGraph.EDGENODE_KEY)
                            ? edgeLabels.iterator()
                            : nodeLabels.iterator()), null);
    }


    /**
     * Used internally for storing item shape in a single object.
     */
    protected RoundRectangle2D itemShape;

    /**
     * Used internally for storing the item stroke reference.
     */
    protected BasicStroke itemStroke;

    /**
     * Used internally for restoring the previous stroke.
     */
    protected Stroke prevStroke;

    /**
     * Used internally for storing the item label text reference.
     */
    protected String[] itemText;

    /**
     * Used internally for storing the item parameters reference.
     */
    protected ItemParams itemParams;

    /**
     * Used internally for storing the height of the label text (one line).
     */
    protected int textHeight;

    /**
     * Used internally for storing the font metrics reference.
     */
    protected FontMetrics fontMetrics;

    /**
     * Used internally for storing line numbers for the item labels.
     */
    protected int textLine;

    /**
     * Used internally for storing the item font reference.
     */
    protected Font itemFont;

    /**
     * Used internally for storing the item size.
     */
    protected double itemSize;

    /**
     * Used internally for storing the item width.
     */
    protected int itemWidth;

    /**
     * Used internally for storing the item height.
     */
    protected int itemHeight;

    /**
     * Used internally for storing the zoom level (scale).
     */
    protected double zoom;

    protected double dx, dy;
    private double dz;
    private int ix, iy, iw, iz, ii;
    private AffineTransform atrans;

    // Minimum size of an item shape
    private static final double MIN_SIZE = 10.0;

    /**
     * The class used for storing the cached rendering parameters for
     * each item.
     */
    protected class ItemParams {
        public ItemParams() {
            itemSize = 0;
            textHeight = 0;
            itemWidth = 0;
            itemHeight = 0;
            textWidth = null;
            itemFont = null;
            fontMetrics = null;
            scaledFont = null;
        }
        public int itemWidth, itemHeight;
        public double itemSize;
        public double arcSize;
        public int textHeight;
        public int[] textWidth;
        public Font itemFont, scaledFont;
        public FontMetrics fontMetrics;
    }

    /**
     * Set the ItemParameters of an item.
     * Implementation note: this sets itemParams, itemText,
     * fontMetrics, textHeight (whole label), itemWidth, itemHeight
     * itemFont, and itemSize, as well as dx and dy (top left corner).
     * Any other internal variables may become unset.
     * @param item The item to set the ItemParameters of.
     */
    protected void setParameters (VisualItem item) {
        itemSize = item.getSize();
        itemFont = item.getFont();

        // Ensure there's an itemParams cache for each item
        itemParams = (ItemParams)item.get(paramCache);
        if (itemParams == null) {
            itemParams = new ItemParams();
            item.set(paramCache, itemParams);
        }
        itemText = (String[])item.get(labelCache);

        if (itemText != null && itemFont == itemParams.itemFont &&
                itemSize == itemParams.itemSize) {
            // Everything was cached in this size; use the cached values
            textHeight = itemParams.textHeight;
            fontMetrics = itemParams.fontMetrics;
            itemFont = itemParams.scaledFont;
            itemWidth = itemParams.itemWidth;
            itemHeight = itemParams.itemHeight;
        } else {
            // Something has changed since caching, recalculate
            itemFont = FontLib.getFont(itemFont.getName(),
                                       itemFont.getStyle(),
                                       itemSize * itemFont.getSize());
            fontMetrics = DEFAULT_GRAPHICS.getFontMetrics(itemFont);
            itemParams.scaledFont = itemFont;
            itemParams.fontMetrics = fontMetrics;
            if (itemText == null) {
                itemText = getLabel(item).split("\n");
                item.set(labelCache, itemText);
            }

            // Find out the length of each line of text when rendered
            iw = 0;
            textLine = itemText.length;
            textHeight = fontMetrics.getHeight();
            itemHeight = (textHeight * textLine) + (padHeight * 2);
            itemParams.textHeight = textHeight;
            itemParams.textWidth = new int[textLine];
            while (textLine != 0) {
                --textLine;
                ix = fontMetrics.stringWidth(itemText[textLine]);
                itemParams.textWidth[textLine] = ix;
                if (ix > iw)
                    iw = ix;
            }

            // Size the label to fit all the text
            itemWidth = iw + (padWidth * 2);
            iw = (int)(MIN_SIZE * itemSize + 1.5);
            if (itemWidth < iw)
                itemWidth = iw;
            if (itemHeight < iw)
                itemHeight = iw;
            itemParams.itemWidth = itemWidth;
            itemParams.itemHeight = itemHeight;
            itemParams.itemSize = itemSize;
            itemParams.itemFont = itemFont;
            itemParams.arcSize = itemSize * arcSize;
        }

        // Compute the position of item top left corner
        dx = item.getX() - (itemWidth / 2.0);
        dy = item.getY() - (itemHeight / 2.0);
        // Note: This renderer would work just fine if we assumed that
        // the getX, getY position is the corner, but other components
        // would have to factor width and height into the equation to
        // figure out the center, so it is better to do it here once
        // rather than separately everywhere else
    }

    /**
     * Render a given item to a graphics surface.
     * Implementation note: this sets itemParams, itemText,
     * fontMetrics, itemFont, itemSize, itemWidth, itemHeight,
     * and itemStroke (if stroke is drawn).
     * Any other internal variables may become unset.
     *
     * <p>Note that stroke won't be drawn if the fill color is "0".
     * @param g The graphics surface to render on.
     * @param item The item to render.
     */
    public void render (Graphics2D g, VisualItem item) {
        atrans = g.getTransform();
        zoom = Math.max(atrans.getScaleX(), atrans.getScaleY());
        setParameters(item);

        // Render the shape
        ii = item.getFillColor();
        if (ii != 0) {
            g.setPaint(ColorLib.getColor(ii));
            if (zoom > PRECISION_THRESHOLD) {
                dz = itemParams.arcSize;
                itemShape.setRoundRect(dx, dy, itemWidth, itemHeight, dz, dz);
                // High precision (stable image when zoomed in)
                g.fill(itemShape);
                ii = item.getStrokeColor();
                if (ii != 0 && ((itemStroke = item.getStroke()) != null)) {
                    prevStroke = g.getStroke();
                    g.setStroke(itemStroke);
                    g.setPaint(ColorLib.getColor(ii));
                    g.draw(itemShape);
                    g.setStroke(prevStroke);
                }
            } else {
                // Low precision (suffices when zoomed out)
                ix = (int)dx;
                iy = (int)dy;
                iz = (int)(itemParams.arcSize);
                g.fillRoundRect(ix, iy, itemWidth, itemHeight, iz, iz);
                ii = item.getStrokeColor();
                if (ii != 0 && ((itemStroke = item.getStroke()) != null)) {
                    prevStroke = g.getStroke();
                    g.setStroke(itemStroke);
                    g.setPaint(ColorLib.getColor(ii));
                    g.drawRoundRect(ix, iy, itemWidth, itemHeight, iz, iz);
                    g.setStroke(prevStroke);
                }
            }
        }
        
        // Render the label text
        if (textHeight * zoom < LABEL_THRESHOLD)
            return;
        textLine = itemText.length;
        if (textLine == 0)
            return;
        iy = (textHeight * textLine) + fontMetrics.getAscent();
        ix = item.getTextColor();
        if (ix == 0)
            return;

        g.setPaint(ColorLib.getColor(ix));
        g.setFont(itemFont);
        dy += padHeight; 
        if (zoom > PRECISION_THRESHOLD) {
            // Zoomed in, use high precision mode for a stable image
            dy += iy;
            while (textLine != 0) {
                --textLine;
                dz = dx + ((itemWidth - itemParams.textWidth[textLine]) / 2);
                dy -= textHeight;
                g.drawString(itemText[textLine], (float)dz, (float)dy);
            }
        } else {
            // Zoomed out, integer precision suffices
            iy += (int)dy;
            ix = (int)dx;
            while (textLine != 0) {
                --textLine;
                ii = ix + (itemWidth - itemParams.textWidth[textLine]) / 2;
                iy -= textHeight;
                g.drawString(itemText[textLine], ii, iy);
            }
        }
    }

    /**
     * Set the bounds for a given item, based on its rendered size.
     * Implementation note: Sets everything that setParameters sets,
     * and also itemStroke (if any is to be drawn).
     * @param item The item to set the bounds for (with item.setBounds()).
     */
    public void setBounds (VisualItem item) {
        setParameters(item);
        dz = 1.0;
        ii = item.getStrokeColor();
        if (ii != 0) {
            itemStroke = item.getStroke();
            if (itemStroke != null) {
                dz += itemStroke.getLineWidth();
            }
        }
        item.setBounds(dx - dz,              dy - dz,
                       itemWidth + (dz * 2), itemHeight + (dz * 2));
    }

    /**
     * Check if a given point is within the shape of an item.
     * @param p The point to check.
     * @param item The item to check against.
     * @return True iff the point lies within the item's shape.
     */
    public boolean locatePoint (Point2D p, VisualItem item) {
        if (!item.getBounds().contains(p))
            return false;
        setParameters(item);
        dz = itemParams.arcSize;
        itemShape.setRoundRect(dx, dy, itemWidth, itemHeight, dz, dz);
        return itemShape.contains(p);
    }
}
