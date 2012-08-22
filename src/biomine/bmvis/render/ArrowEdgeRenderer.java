package biomine.bmvis.render;

import biomine.bmvis.PrefuseBMGraph;

import prefuse.render.Renderer;
import prefuse.util.ColorLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;

import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An renderer for BMVis, aware of edge directions.
 * Edge rendering directions are determined by the current edge labeling
 * direction, as well as by the ARROW_KEY and REVERSED_EDGE_KEY
 * attributes, which must be set externally.
 * @author Kimmo Kulovesi
 */

public class ArrowEdgeRenderer implements Renderer {

    /**
     * Arrowhead width for new ArrowEdgeRenderers.
     * Must be positive (not checked). Doesn't affect already existing
     * instances.
     */
    public static int ARROW_WIDTH = 8;

    /**
     * Arrowhead height for new ArrowEdgeRenderers.
     * Must be positive (not checked). Doesn't affect already existing
     * instances.
     */
    public static int ARROW_HEIGHT = 12;

    private Stroke prevStroke;
    private NodeLabelRenderer.EdgeDirection edgeDirection;
    private static final double HALF_PI = Math.PI / 2.0;
    private boolean isForward;
    private float[] arrowhead;
    private float[] points;
    private int[] xpoints;
    private int[] ypoints;
    private AffineTransform atrans, gtrans;
    private Rectangle2D sourceBounds, targetBounds;
    private float x1, x2, y1, y2, dd, x2_x1, y2_y1;

    /**
     * Create a new ArrowEdgeRenderer, defaulting to FORWARD direction.
     */
    public ArrowEdgeRenderer () {
        edgeDirection = NodeLabelRenderer.EdgeDirection.FORWARD;
        atrans = new AffineTransform();
        xpoints = new int[3];
        ypoints = new int[3];
        points = new float[8];
        arrowhead = new float[8];
        arrowhead[0] = arrowhead[1] = arrowhead[6] = 0.0f;
        arrowhead[4] = ARROW_WIDTH / 2.0f;
        arrowhead[2] = -arrowhead[4];
        arrowhead[3] = arrowhead[5] = (float)(-ARROW_HEIGHT);
        arrowhead[7] = -ARROW_HEIGHT * 0.8f;
    }

    /**
     * Set the edge rendering direction.
     * Note that the ARROW_KEY-values must first be set correctly to
     * reflect the rendering direction, e.g. by calling setEdgeDirection
     * in NodeLabelRenderer.
     * @param dir The direction for edge arrows.
     */
    public void setEdgeDirection (NodeLabelRenderer.EdgeDirection dir) {
        edgeDirection = dir;
    }

    /*
     * Requires the edge to be set as (x1,y1) to (x2,y2), with
     * x2_x1 set to (x2 - x1) and y2_y1 set to (y2 - y1). Another line
     * segment is given as the argument (u1, u2 are x-coordinates,
     * v1, v2 y-coordinates). Returns true of false according to
     * intersection status, in case of true sets (x2, y2) to the
     * point of intersection.
     */
    private boolean edgeIntersects (float u1, float v1, float u2, float v2) {
        float ub = (v2-v1) * x2_x1 - (u2-u1) * y2_y1;
        if (ub == 0)
            return false; // Avoid division by zero
        float ua = ((u2-u1) * (y1-v1) - (v2-v1) * (x1-u1)) / ub;
              ub = ((x2_x1) * (y1-v1) - (y2_y1) * (x1-u1)) / ub;
        if (ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1) {
            x2 = x1 + ua * x2_x1;
            y2 = y1 + ua * y2_y1;
            return true;
        }
        return false;
    }

    /**
     * The threshold zoom level (scale) below which the arrowhead is not drawn.
     * Defaults to 0.35.
     */
    public static final float ARROWHEAD_THRESHOLD = 0.35f;

    public void render (Graphics2D g, VisualItem item) {
        getEndPoints((EdgeItem)item);
        prevStroke = g.getStroke();
        g.setStroke(item.getStroke());
        g.setPaint(ColorLib.getColor(item.getStrokeColor()));

        if (!item.getBoolean(PrefuseBMGraph.ARROW_KEY)) {
            // No arrow to draw, just draw the line and return
            g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
            g.setStroke(prevStroke);
            return;
        }

        gtrans = g.getTransform();
        dd = (float)Math.max(gtrans.getScaleX(), gtrans.getScaleY());
        if (dd < ARROWHEAD_THRESHOLD) {
            // Don't draw the arrowhead when zoomed far out
            g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
            g.setStroke(prevStroke);
            return;
        }

        // Obtain the edge direction for drawing the arrow
        switch (edgeDirection) {
            case CANONICAL:
                isForward = !item.getBoolean(PrefuseBMGraph.REVERSED_EDGE_KEY);
                break;
            case REVERSED:
                isForward = false;
                break;
            default:
                isForward = true;
                break;
        }
        if (!isForward) {
            dd = x1; x1 = x2; x2 = dd;
            dd = y1; y1 = y2; y2 = dd;
            targetBounds = sourceBounds;
        }
        // Set the arrow's tip against the target node's bounding box
        x2_x1 = x2 - x1;
        y2_y1 = y2 - y1;
        // Check intersection against nearest horizontal edge
        if (y1 < y2)
            dd = (float)targetBounds.getMinY(); // Top edge
        else
            dd = (float)targetBounds.getMaxY(); // Bottom edge
        if (!edgeIntersects((float)targetBounds.getMinX(), dd,
                            (float)targetBounds.getMaxX(), dd)) {
            // If no intersection, check against nearest vertical edge
            if (x1 < x2)
                dd = (float)targetBounds.getMinX(); // Left edge
            else
                dd = (float)targetBounds.getMaxX(); // Right edge
            edgeIntersects(dd, (float)targetBounds.getMinY(),
                           dd, (float)targetBounds.getMaxY());
        }
        // Translate the arrowhead to proper orientation
        atrans.setToTranslation(x2, y2);
        atrans.rotate(-HALF_PI + Math.atan2(y2_y1, x2_x1));
        atrans.transform(arrowhead, 0, points, 0, 4);
        xpoints[0] = (int)points[0];
        ypoints[0] = (int)points[1];
        xpoints[1] = (int)points[2];
        ypoints[1] = (int)points[3];
        xpoints[2] = (int)points[4];
        ypoints[2] = (int)points[5];

        // Draw the line and then the arrowhead
        g.drawLine((int)x1, (int)y1, (int)points[6], (int)points[7]);
        g.fillPolygon(xpoints, ypoints, 3);
        g.setStroke(prevStroke);
    }

    /**
     * Returns false (unsupported operation). This is intended to be
     * used with non-interactive edges, and thus it is deemed
     * unnecessary to check whether or not points lie within the edges.
     * Since the edges are usually very thin, it is simply assumed
     * that no point is inside them, and false is always resumed.
     * @param p A point to "check".
     * @param item A VisualItem to check against.
     * @return False - checking for points within edges is unsupported.
     */
    public boolean locatePoint (Point2D p, VisualItem item) {
        return false;
    }

    /*
     * Sets (x1, y1) and (x2, y2) to the edge source and target center
     * points, accordingly.
     */
    private void getEndPoints (EdgeItem item) {
        sourceBounds = item.getSourceItem().getBounds();
        targetBounds = item.getTargetItem().getBounds();
        x1 = (float)sourceBounds.getX() + ((float)sourceBounds.getWidth() / 2f);
        x2 = (float)targetBounds.getX() + ((float)targetBounds.getWidth() / 2f);
        y1 = (float)sourceBounds.getY() + ((float)sourceBounds.getHeight() / 2f);
        y2 = (float)targetBounds.getY() + ((float)targetBounds.getHeight() / 2f);
    }

    /**
     * Set the bounding rectangle of the given edge.
     * The bounding rectangle is computed very fast, simply by finding
     * the largest possible rectangle occupied by a reasonably-sized
     * edge. Note that the edge stroke width must not exceed half of the
     * arrowhead width for this to be reasonably accurate in all cases.
     * @param item The EdgeItem for which to set the bounds.
     */
    public void setBounds (VisualItem item) {
        getEndPoints((EdgeItem)item);
        if (x1 > x2) { dd = x1; x1 = x2; x2 = dd; }
        if (y1 > y2) { dd = y1; y1 = y2; y2 = dd; }
        item.setBounds(x1 - arrowhead[4] - 1, y1 - arrowhead[4] - 1,
                       (arrowhead[4] * 2) + (x2 - x1) + 2,
                       (arrowhead[4] * 2) + (y2 - y1) + 2);
    }

}
