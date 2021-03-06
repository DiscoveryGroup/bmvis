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

package biomine.bmvis.action;

import biomine.bmvis.PrefuseBMGraph;

import prefuse.action.assignment.ColorAction;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;
import prefuse.util.ColorLib;

import java.util.HashMap;
import java.util.Map;

/**
 * Color action to set the color for edges according to an attribute.
 * This action can store precomputed color values in another attribute,
 * including those from other ColorActions add():ed to this one. If
 * precomputed colors are stored, the attribute used to store the
 * precomputed value must be invalidated by setting it to "null", or
 * changed colors will not be computed.
 * @author Kimmo Kulovesi
 */

public class EdgeAttributeColorAction extends ColorAction {

    /**
     * Attribute used to set the color.
     */
    protected String attribute;

    /**
     * Attribute used to override the color with express RGB values.
     */
    protected String rgbAttribute;

    /**
     * Field used to save precomputed color value.
     */
    protected String precomputed;

    /**
     * Default color.
     */
    protected Integer defaultColor;

    /**
     * Color palette.
     */
    protected HashMap<String, Integer> colorPalette;

    /**
     * Create a new EdgeAttributeColorAction.
     * Note that the attribute and precomputed fields must exist in the
     * schema, they are not checked. The color palette map is always
     * copied to a new internal map, so changes to the original won't
     * affect this class.
     * @param group The data group processed by this action.
     * @param attribute The attribute to use for mapping colors.
     * @param precomputed The attribute to store precomputed colors in, if any.
     * @param colorPalette The mapping of attribute values to colors.
     * @param defaultColor The default color for non-mapped edge types.
     */
    public EdgeAttributeColorAction (String group, String field,
                                String attribute, String precomputed,
                                Map<String, Integer> colorPalette,
                                int defaultColor) {
        this(group, field, attribute, precomputed, colorPalette,
                defaultColor, null);
    }

    /**
     * Create a new EdgeAttributeColorAction.
     * Note that the attribute and precomputed fields must exist in the
     * schema, they are not checked. The color palette map is always
     * copied to a new internal map, so changes to the original won't
     * affect this class.
     * @param group The data group processed by this action.
     * @param attribute The attribute to use for mapping colors.
     * @param precomputed The attribute to store precomputed colors in, if any.
     * @param colorPalette The mapping of attribute values to colors.
     * @param defaultColor The default color for non-mapped edge types.
     * @param rgbAttribute Optional attribute which may contain an
     * overriding color String as "R/G/B" with components ranging from
     * 0 to 255 inclusive.
     */
    public EdgeAttributeColorAction (String group, String field,
                                String attribute, String precomputed,
                                Map<String, Integer> colorPalette,
                                int defaultColor,
                                String rgbAttribute) {
        super(group, field);
        assert attribute != null : "Null attribute";

        this.attribute = attribute;
        this.rgbAttribute = rgbAttribute;
        this.precomputed = precomputed;

        if (colorPalette == null)
            this.colorPalette = new HashMap<String, Integer>();
        else
            this.colorPalette = new HashMap<String, Integer>(colorPalette);

        this.defaultColor = new Integer(defaultColor);
    }

    /**
     * Get the color for a given VisualItem.
     * @param item The item.
     * @return Color of the item.
     */
    public int getColor (VisualItem item) {
        Integer color = null;
        
        if (precomputed == null ||
            (color = (Integer)item.get(precomputed)) == null) {
            Object o = lookup(item);
            if (o != null) {
                if (o instanceof ColorAction) {
                    color = ((ColorAction)o).getColor(item);
                } else if (o instanceof Integer) {
                    color = (Integer)o;
                } else if (o instanceof String) {
                    color = NodeAttributeColorAction.colorFromString((String) o, defaultColor);
                } else {
                    color = defaultColor;
                }
            } else {
                String type = null;

                if (rgbAttribute != null && item.canGetString(rgbAttribute)) {
                    type = item.getString(rgbAttribute);
                    if (type == null || type.length() == 0) {
                        type = null;
                    } else {
                        color = NodeAttributeColorAction.colorFromString(type, defaultColor);
                    }
                }
                if (type == null) {
                    type = item.getString(attribute);
                    color = colorPalette.get(type);
                    if (color == null)
                        color = defaultColor;
                }
            }
            if (precomputed != null)
                item.set(precomputed, color);
        }

        return color.intValue();
    }

    /**
     * Get the color palette (attribute values to colors) mapping.
     * This palette can be modified to affect the behavior of this
     * class, but if precomputed values are used, changes won't take
     * effect automatically.
     * @return The modifiable color palette (values to Prefuse colors).
     */
    public HashMap<String, Integer> getColorPalette () {
        return colorPalette;
    }

}
