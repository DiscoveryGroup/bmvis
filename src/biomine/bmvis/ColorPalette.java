package biomine.bmvis;

import java.util.HashMap;

import prefuse.util.ColorLib;

/**
 * Color palette for the BMVis visualization.
 *
 * @author Kimmo Kulovesi
 */
public class ColorPalette {
    /**
     * Predefined edge type stroke colors.
     */
    public static final HashMap<String, Integer> EDGE_COLORS =
                                            new HashMap<String, Integer>();
    /**
     * Predefined node type colors.
     */
    public static final HashMap<String, Integer> NODE_COLORS =
                                            new HashMap<String, Integer>();

    /**
     * Predefined queryset colors.
     */
    public static final HashMap<String, Integer> QUERYSET_COLORS =
                                            new HashMap<String, Integer>();

    static {
        //EDGE_COLORS.put("refers_to", ColorLib.rgb(0, 0, 0));

        QUERYSET_COLORS.put("start", ColorLib.rgb(0, 175, 0));
        QUERYSET_COLORS.put("source", ColorLib.rgb(0, 175, 0));
        QUERYSET_COLORS.put("end", ColorLib.rgb(220, 0, 0));
        QUERYSET_COLORS.put("target", ColorLib.rgb(220, 0, 0));
        QUERYSET_COLORS.put("start,end", ColorLib.rgb(220, 0, 220));
        QUERYSET_COLORS.put("source,target", ColorLib.rgb(220, 0, 220));
        QUERYSET_COLORS.put("rank", ColorLib.rgb(0, 175, 220));

        NODE_COLORS.put("Node", ColorLib.rgb(240, 240, 240));

        // Saturated greens
        NODE_COLORS.put("Sequence", ColorLib.rgb(211, 247, 163));
        NODE_COLORS.put("Gene", ColorLib.rgb(211, 247, 163));
        NODE_COLORS.put("Protein", ColorLib.rgb(199, 240, 178));
        NODE_COLORS.put("Enzyme", ColorLib.rgb(193, 234, 174));
        //NODE_COLORS.put("ProteinGroup", ColorLib.rgb(199, 240, 178));
        NODE_COLORS.put("AllelicVariant", ColorLib.rgb(199, 240, 178));

        // Beige
        NODE_COLORS.put("Article", ColorLib.rgb(250, 230, 160));

        // Cyans (to do, make greener)
        NODE_COLORS.put("HomologGroup", ColorLib.rgb(187, 237, 215));
        NODE_COLORS.put("OrthologGroup", ColorLib.rgb(194, 237, 218));

        // Desaturated blues
        NODE_COLORS.put("GO", ColorLib.rgb(185, 218, 234));
        NODE_COLORS.put("BiologicalProcess", ColorLib.rgb(185, 218, 234));
        NODE_COLORS.put("MolecularFunction", ColorLib.rgb(189, 223, 239));
        NODE_COLORS.put("CellularComponent", ColorLib.rgb(174, 214, 234));

        // Light purples
        NODE_COLORS.put("Ligand", ColorLib.rgb(210, 203, 240));
        NODE_COLORS.put("Substance", ColorLib.rgb(210, 203, 240));
        NODE_COLORS.put("Compound", ColorLib.rgb(210, 203, 240));
        NODE_COLORS.put("Drug", ColorLib.rgb(203, 196, 233));
        NODE_COLORS.put("Glycan", ColorLib.rgb(198, 200, 242));

        // Pink
        NODE_COLORS.put("GenomicContext", ColorLib.rgb(255, 215, 253));

        // Blues
        NODE_COLORS.put("Locus", ColorLib.rgb(180, 196, 239)); // unused
        NODE_COLORS.put("Phenotype", ColorLib.rgb(193, 209, 255));
        //NODE_COLORS.put("Locus/Phenotype, ColorLib.rgb(195, 199, 242));
        
        // TMP MAPPING for illustration purposes BY LERONEN! Should never get to SVN!
        NODE_COLORS.put("Gene/Phenotype", ColorLib.rgb(211, 247, 163));
//        NODE_COLORS.put("Gene/Phenotype", ColorLib.rgb(193, 223, 242));

        // Greens
        NODE_COLORS.put("Family", ColorLib.rgb(179, 226, 192));        
        NODE_COLORS.put("Region", ColorLib.rgb(195, 229, 204));
        NODE_COLORS.put("Domain", ColorLib.rgb(195, 229, 204));
        NODE_COLORS.put("Repeat", ColorLib.rgb(195, 229, 204));
        NODE_COLORS.put("Site", ColorLib.rgb(190, 229, 201));
        NODE_COLORS.put("ActiveSite", ColorLib.rgb(190, 229, 201));
        NODE_COLORS.put("BindingSite", ColorLib.rgb(190, 229, 201));
        NODE_COLORS.put("PostTranslationalModification",
                        ColorLib.rgb(190, 229, 201));

        // Purple
        NODE_COLORS.put("Pathway", ColorLib.rgb(208, 185, 231));

        // Browns
        NODE_COLORS.put("Tissue", ColorLib.rgb(229, 218, 189));
        //NODE_COLORS.put("Organism", ColorLib.rgb(229, 215, 177));
        NODE_COLORS.put("MeSHHeading", ColorLib.rgb(239, 231, 176));
        NODE_COLORS.put("OMIM", ColorLib.rgb(239, 231, 176));
    }

    /**
     * Node text color.
     */
    public static final int NODE_TEXT = ColorLib.gray(0);

    /**
     * Groupnode border stroke color.
     */
    public static final int GROUPNODE_STROKE = ColorLib.gray(0);

    /**
     * Edgenode fill color.
     */
    public static final int EDGENODE_FILL = 0;

    /**
     * Default node fill color. This is generally unused, due to the use
     * of type-specific colors set in NODE_COLORS.
     */
    public static final int DEFAULT_FILL = ColorLib.rgb(200, 215, 255);

    /**
     * Selected node color.
     */
    public static final int NODE_SELECT = ColorLib.rgb(255, 100, 100);

    /**
     * Node hover highlight color.
     */
    public static final int NODE_HOVER = ColorLib.rgb(255, 155, 140);

    /**
     * Node hover neighbor highlight color.
     */
    public static final int NEIGHBOR_HOVER = ColorLib.rgb(255, 175, 0);

    /**
     * Selected node hover highlight color.
     */
    public static final int SELECT_HOVER = ColorLib.rgb(255, 120, 120);

    /**
     * Node hover neighbor highlight color for selected nodes.
     */
    public static final int SELECT_NEIGHBOR = ColorLib.rgb(255, 100, 35);

    /**
     * Default edge stroke color.
     */
    public static final int DEFAULT_EDGE_STROKE = ColorLib.rgb(15, 15, 15);

}
