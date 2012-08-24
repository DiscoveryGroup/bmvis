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

package biomine.bmvis.render;

import biomine.bmvis.BMVis;
import biomine.bmvis.PrefuseBMGraph;

import prefuse.visual.VisualItem;

import java.util.HashSet;

/**
 * A NodeLabelRenderer specifically for the Biomine database. This
 * renderer contains relatively complex rules for constructing the
 * labels for nodes.
 * @author Kimmo Kulovesi
 */

public class BMNodeLabelRenderer extends NodeLabelRenderer {

    private boolean nodeLabel;
    private HashSet<String> alreadyShown;

    /**
     * Create a BMNodeLabelRenderer with given node and edge label attributes.
     * @param bmvis The application this renderer is to be used with.
     * @param nodeLabel Attribute to use for node sublabels (can be null).
     * @param edgeLabel Attribute to use for edge labels (can be null).
     */
    public BMNodeLabelRenderer (BMVis bmvis, String nodeLabel, String edgeLabel) {
        super(bmvis, nodeLabel, edgeLabel);
        this.nodeLabel = true;
        this.alreadyShown = new HashSet<String>(8, 0.9f);
    }

    /**
     * Toggle the display of the node main label on/off.
     * The main label is the label that is computed internally by this
     * class, and isn't formed by the nodeLabels list.
     * @param status The new status for the node main label (true = on).
     */
    public void setNodeLabelDisplay (boolean status) {
        nodeLabel = status;
        resetNodeLabels();
    }

    /**
     * Get the node main label display status.
     * @return True iff the node main label is being displayed.
     */
    public boolean getNodeLabelDisplay () {
        return nodeLabel;
    }

    /**
     * Get a the label for a given VisualItem.
     *
     * <p>For edgenodes, the label is formed of the attributes set in
     * the edgeLabels linked list, just like the parent class
     * NodeLabelRenderer does.
     *
     * <p>For groupnodes, the label is generated to describe the type and
     * number of members the node has.
     *
     * <p>For other nodes, the label is primarily constructed from the
     * attribute PrefuseBMGraph.LABEL_KEY, but as a fallback it can
     * be generated from the node id. Meanwhile, node aliases (set as
     * the attribute PrefuseBMGraph.ALIAS_KEY) are also displayed so as
     * to identify nodes parsed from user input. The default attribute
     * (usually organism) is also shown at the end of the label, except
     * if already displayed, or if the showing of default values is
     * disabled.
     *
     * @param item The node or edge item to obtain a label for.
     * @return The label.
     */
    public String getLabel (VisualItem item) {
        if (item.getBoolean(PrefuseBMGraph.EDGENODE_KEY))
            return super.getLabel(item);

        String text;
        String s;
        int organismPoint = -1;
        String type = item.getString(PrefuseBMGraph.TYPE_KEY);
        if (type == null)
            type = "";
        alreadyShown.clear();
        alreadyShown.add(PrefuseBMGraph.ORGANISM_KEY);

        if (item.getBoolean(PrefuseBMGraph.GROUPNODE_KEY)) {
            // Groupnodes: Type x<size>
            text = type +
                   " x" + item.getString(PrefuseBMGraph.NODESIZE_KEY);
            s = item.getString(PrefuseBMGraph.LABEL_KEY);
            if (s != null) {
                text = s + "\n" + text;
                alreadyShown.add(PrefuseBMGraph.LABEL_KEY);
            }
            organismPoint = type.length();
            alreadyShown.add(PrefuseBMGraph.TYPE_KEY);
        } else if (nodeLabel) {
            text = item.getString(PrefuseBMGraph.LABEL_KEY);
            if (text != null) {
                alreadyShown.add(PrefuseBMGraph.LABEL_KEY);
            } else {
                // No label, use DBID
                text = item.getString(PrefuseBMGraph.DBID_KEY);
                alreadyShown.add(PrefuseBMGraph.DBID_KEY);
                alreadyShown.add(PrefuseBMGraph.DB_KEY);
                alreadyShown.add(PrefuseBMGraph.ID_KEY);

                s = item.getString(PrefuseBMGraph.DB_KEY);
                if (s != null && type.contains(s)) {
                    alreadyShown.add(PrefuseBMGraph.TYPE_KEY);
                }
                organismPoint = text.length() - 1;
            }

            // Add alias
            s = item.getString(PrefuseBMGraph.ALIAS_KEY);
            if (s != null) {
                alreadyShown.add(PrefuseBMGraph.ALIAS_KEY);
                if (s.contains(text)) {
                    text = s;
                    alreadyShown.remove(PrefuseBMGraph.TYPE_KEY);
                    organismPoint = text.length() - 1;
                } else {
                    text = s + "\n" + text;
                    organismPoint = s.length();
                }
            }
            
            if (!alreadyShown.contains(PrefuseBMGraph.TYPE_KEY) &&
                text.startsWith(type)) {
                alreadyShown.add(PrefuseBMGraph.TYPE_KEY);
            }
        } else {
            text = "";
        }
        
        // Add sublabel(s)
        s = super.getLabel(item, nodeLabels.iterator(), alreadyShown);
        if (s != null && s.length() > 0) {
            if (text.length() > 0)
                text = text + "\n" + s;
            else
                text = "\n" + s;
        } else if (text.length() < 1) {
            // Don't add organism if there are no labels at all
            return text;
        }

        // Add organism
        
        s = getAttributeFrom(item, NodeLabelRenderer.DEFAULT_KEY);
        if (s != null) {
            if (nodeLabels.contains(PrefuseBMGraph.TYPE_KEY) &&
                !alreadyShown.contains(PrefuseBMGraph.TYPE_KEY)) {
                text = text.replace("\n" + type, "\n" + type + " (" + s + ")");
            } else if (organismPoint >= 0 && !text.contains(s)) {
                text = text.substring(0, organismPoint)
                        + " (" + s + ")"
                        + text.substring(organismPoint);
            }
        }
        if (text.charAt(0) == '\n')
            return text.substring(1);
        return text;
    }
}
