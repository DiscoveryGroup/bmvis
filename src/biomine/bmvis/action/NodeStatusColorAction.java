package biomine.bmvis.action;

import biomine.bmvis.BMVis;
import biomine.bmvis.ColorPalette;
import biomine.bmvis.PrefuseBMGraph;

import prefuse.data.tuple.DefaultTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;

import java.util.Map;

/**
 * Color action to set the color for nodes according to an attribute,
 * as well as their selection, highlight and hover status.
 * 
 * The order of precedence for the colors are (highest priority first):
 * selected hover, selected highlight, selected, hover, highlight,
 * and finally the attribute color as set by parent class
 * NodeAttributeColorAction.
 *
 * @author Kimmo Kulovesi
 */

public class NodeStatusColorAction extends NodeAttributeColorAction {

    private TupleSet selectGroup;
    private TupleSet neighborGroup;
    private int selectColor, hoverColor, neighborColor;
    private int selectNeighborColor, selectHoverColor;

    /**
     * Create a new NodeStatusColorAction.
     * @param group The data group processed by this action.
     * @param field The color field set by this action.
     * @param selected The TupleSet of the currently selected nodes.
     * @param highlight The TupleSet of the currently highlighted nodes.
     * @param attribute The attribute used to set the node color.
     * @param precomputed The attribute field used to save precomputed color.
     * @param colorPalette The mapping of attribute values to colors.
     * @param defaultColor The default color for non-mapped node types.
     * @param selectColor The color for selected nodes.
     * @param hoverColor The color for hovered nodes.
     * @param highlightColor The color for highlight nodes.
     * @param selectHoverColor The color for hovered and selected nodes.
     * @param selectHighlightColor The color for highlighted and selected nodes.
     */
    public NodeStatusColorAction (String group, String field,
                                TupleSet selected, TupleSet highlight,
                                String attribute, String precomputed,
                                Map<String, Integer> colorPalette,
                                int defaultColor,
                                int edgenodeColor,
                                int selectColor,
                                int hoverColor,
                                int highlightColor,
                                int selectHoverColor,
                                int selectHighlightColor) {
        this(group, field, selected, highlight, attribute,
                precomputed, colorPalette, defaultColor, edgenodeColor,
                null, selectColor, hoverColor, highlightColor,
                selectHoverColor, selectHighlightColor);
    }

    /**
     * Create a new NodeStatusColorAction.
     * @param group The data group processed by this action.
     * @param field The color field set by this action.
     * @param selected The TupleSet of the currently selected nodes.
     * @param highlight The TupleSet of the currently highlighted nodes.
     * @param attribute The attribute used to set the node color.
     * @param precomputed The attribute field used to save precomputed color.
     * @param colorPalette The mapping of attribute values to colors.
     * @param defaultColor The default color for non-mapped node types.
     * @param rgbAttribute An optional attribute (may be null for none)
     * to override underlying attribute-based color with a raw RGB value
     * given as a String of the format "R/G/B" with values ranging from
     * 0 to 255 inclusive.
     * @param selectColor The color for selected nodes.
     * @param hoverColor The color for hovered nodes.
     * @param highlightColor The color for highlight nodes.
     * @param selectHoverColor The color for hovered and selected nodes.
     * @param selectHighlightColor The color for highlighted and selected nodes.
     */
    public NodeStatusColorAction (String group, String field,
                                TupleSet selected, TupleSet highlight,
                                String attribute, String precomputed,
                                Map<String, Integer> colorPalette,
                                int defaultColor,
                                int edgenodeColor,
                                String rgbAttribute,
                                int selectColor,
                                int hoverColor,
                                int highlightColor,
                                int selectHoverColor,
                                int selectHighlightColor) {
        super(group, field, attribute, precomputed, colorPalette,
              defaultColor, edgenodeColor, rgbAttribute);

        if (selected == null)
            selected = new DefaultTupleSet();
        if (highlight == null)
            highlight = new DefaultTupleSet();

        this.selectGroup = selected;
        this.neighborGroup = highlight;
        this.selectColor = selectColor;
        this.hoverColor = hoverColor;
        this.neighborColor = highlightColor;
        this.selectHoverColor = selectHoverColor;
        this.selectNeighborColor = selectHighlightColor;
    }

    /**
     * Set the default node color.
     * @param color New default node color.
     */
    public void setDefaultColor (int color) {
        defaultColor = color;
    }

    /**
     * Set the edgenode color.
     * @param color New edgenode color.
     */
    public void setEdgenodeColor (int color) {
        edgenodeColor = color;
    }

    /**
     * Set the node hover color.
     * @param color New node hover color.
     */
    public void setHoverColor (int color) {
        hoverColor = color;
    }

    /**
     * Set the node highlight color.
     * @param color New node highlight color.
     */
    public void setHighlightColor (int color) {
        neighborColor = color;
    }

    /**
     * Set the selected node color.
     * @param color New selected node color.
     */
    public void setSelectColor (int color) {
        selectColor = color;
    }

    /**
     * Set the selected node highlight color.
     * @param color New selected node highlight color.
     */
    public void setSelectHighlightColor (int color) {
        selectNeighborColor = color;
    }

    /**
     * Set the selected node hover color.
     * @param color New selected node hover color.
     */
    public void setSelectHoverColor (int color) {
        selectHoverColor = color;
    }

    /**
     * Create a new action to set the color by node type, using BMVis defaults.
     * The default colors are determined in the class ColorPalette, and
     * will be used to set the VisualItem.FILLCOLOR color attribute.
     * Node colors are determined by the PrefuseBMGraph.TYPE_KEY
     * attribute, and stored in PrefuseBMGraph.COMPUTED_FILL_COLOR_KEY.
     * The selected and highlighted groups are obtained from BMVis,
     * as the BMVis.SELECT_GROUP and BMVis.NEIGHBOR_GROUP.
     * @param group The data group processed by this action.
     * @param vis The BMVis this action is used with.
     */
    public NodeStatusColorAction (String group, BMVis vis) {
        this(group, VisualItem.FILLCOLOR,
                vis.ensureHasFocusGroup(BMVis.SELECT_GROUP),
                vis.ensureHasFocusGroup(BMVis.NEIGHBOR_GROUP),
                PrefuseBMGraph.TYPE_KEY, PrefuseBMGraph.COMPUTED_FILL_COLOR_KEY,
                ColorPalette.NODE_COLORS,
                ColorPalette.DEFAULT_FILL,
                ColorPalette.EDGENODE_FILL,
                PrefuseBMGraph.RGB_FILL_COLOR_KEY,
                ColorPalette.NODE_SELECT,
                ColorPalette.NODE_HOVER,
                ColorPalette.NEIGHBOR_HOVER,
                ColorPalette.SELECT_HOVER,
                ColorPalette.SELECT_NEIGHBOR);
    }


    /**
     * Get the color for a given VisualItem.
     * @param item The item.
     * @return Color of the item.
     */
    public int getColor (VisualItem item) {
        if (selectGroup.containsTuple(item)) {
            if (item.isHover())
                return selectHoverColor;
            return (neighborGroup.containsTuple(item)
                    ? selectNeighborColor : selectColor);
        } else if (item.isHover()) {
            return hoverColor;
        } else if (neighborGroup.containsTuple(item)) {
            return neighborColor;
        }
        return super.getColor(item);
    }
}
