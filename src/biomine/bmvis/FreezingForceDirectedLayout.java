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

// import java.util.HashSet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.visual.VisualItem;

public class FreezingForceDirectedLayout extends ForceDirectedLayout {
	protected FreezingForce force;
	protected boolean freezeStationary = true;
	protected Timer freezeTimer;	
	
	private BMVis bmvis;
	
	private int iteration;
	private VisualItem origin;
	private boolean slackOver;
	private int numVisualItems;
		
	private double maxDelta;
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;	

	protected void resetFreezeDetection() {
		iteration = 1;
		minX = Double.MAX_VALUE;
		minY = Double.MAX_VALUE;
		maxX = -Double.MAX_VALUE;
		maxY = -Double.MAX_VALUE;
		maxDelta = -1.0;
		slackOver = false;
		freezeTimer.stop();
		// handled.clear();			
	}

	// private HashSet<VisualItem> handled; // DEBUG
		
	public FreezingForceDirectedLayout(String graph, 
			boolean freezeStationary, int timeout, BMVis bmvis) {		
		super(graph);
		force = new FreezingForce(bmvis);
		super.getForceSimulator().addForce(force);
		this.bmvis = bmvis;
		this.freezeStationary = freezeStationary;
		numVisualItems = bmvis.getBMGraph().numNodes();
		origin = null;

		freezeTimer = new Timer(timeout, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				slackOver = true;
			}});
		freezeTimer.setRepeats(false);

		// handled = new HashSet<VisualItem>();
	}
	
	@Override
	public void setX(VisualItem item, VisualItem referrer, double x) {
		iteration++;
		
		if (origin == null) {			
			origin = item;
			resetFreezeDetection();
		} else if (item == origin || iteration > numVisualItems) {
			// System.err.println("iteration " + iteration + " numvisualitems " + numVisualItems);
			// back to origin, or we went over it
			// check for convergence
			double diameter = Math.sqrt((maxX - minX) * (maxX - minX) +
										(maxY - minY) * (maxY - minY));
			
			if (maxDelta <= (force.getParameter(0) * diameter) 
				&& freezeStationary) {
				if (slackOver) {
					// System.err.println("Frozen, maxDelta: " + maxDelta + " diameter: " + diameter);
					bmvis.setLayoutEnabled(false);
					origin = null;				
					return;
				} else if (!freezeTimer.isRunning()) {
					// System.err.println("Restarting slack timer");
					freezeTimer.restart();
				}
			} else {
				// no convergence, begin a new loop
				// System.err.println("new origin, maxDelta: " + maxDelta);
				origin = null;				
			} 			
		}

		/* DEBUG
		if (handled.contains(item)) {
			System.err.println("double handling " + item);
			System.exit(1);
		}
		handled.add(item); 
		*/

		double oldx = item.getX();
		super.setX(item, referrer, x);
		
		if (x < minX) minX = x;
		if (x > maxX) maxX = x;
		
		double delta = Math.abs(oldx - x); 
		if (delta > maxDelta) maxDelta = delta;
		// System.err.println("X-delta: " + delta);
	}

	@Override
	public void setY(VisualItem item, VisualItem referrer, double y) {
		double oldy = item.getY();
		super.setY(item, referrer, y);

		if (y < minY) minY = y;
		if (y > maxY) maxY = y;
		
		double delta = Math.abs(oldy - y);
		if (delta > maxDelta) maxDelta = delta;
		// System.err.println("Y-delta: " + delta); 
	}

	public boolean isFreezeStationary() {
		return freezeStationary;
	}

	public void setFreezeStationary(boolean freezeStationary) {
		this.freezeStationary = freezeStationary;
		resetFreezeDetection();		
	}	
}
