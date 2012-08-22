package biomine.bmvis.initial;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;

import biomine.bmgraph.BMEdge;
import biomine.bmgraph.BMGraph;
import biomine.bmgraph.BMGraphUtils;
import biomine.bmgraph.BMNode;
import biomine.bmvis.PrefuseBMGraph;

class SquarePacker {
	ArrayList<boolean[]> arr;
	int w;

	SquarePacker(int w) {
		this.w = w;
		arr = new ArrayList<boolean[]>();

	}

	Point pack(int s) {
		if (s > w)
			return null;
		Point ret = new Point(0, 0);
		for (int y = 0; y < arr.size() - s+1; y++) {
			for (int x = 0; x < w - s+1; x++) {
					
				boolean fits = true;
				packLoop: for (int i = 0; i < s; i++) {
					for (int j = 0; j < s; j++) {
						if (arr.get(y + i)[x + j]) {
							fits = false;
							break packLoop;
						}
					}
				}
				if (fits) {
					for (int i = 0; i < s; i++) {
						for (int j = 0; j < s; j++) {
							arr.get(y + i)[x + j] = true;
						}
					}
					ret.x = x;
					ret.y = y;
					return ret;
				}
			}
		}
		int asize = arr.size() + 2;
		for (int i = 0; i < asize; i++)
			arr.add(new boolean[w]);

		return pack(s);
	}
}

public class GraphLayout {

	private static int graphWidth(BMGraph g){
		if(g.getNodes().size()<2)return 1;
		return 2*(int)Math.pow(g.getNodes().size(),0.7);
	}
	private static void ccr(BMNode n, BMGraph newGraph, BMGraph graph) {
		if (newGraph.hasNode(n)) {
			return;
		}
		newGraph.ensureHasNode(n);
		// System.out.println("Node:"+n.getId());
		// System.out.println(newGraph.getNodes().size()+" nodes");
		for (BMEdge e : graph.getNodeEdges(n)) {

			// System.out.println("edge "+e.getFrom().getId()+" -> "
			// +e.getTo());
			ccr(e.getTo(), newGraph, graph);
			ccr(e.getFrom(), newGraph, graph);
			newGraph.ensureHasEdge(e);
		}
	}

	private static BMGraph[] connectedComponents(BMGraph b) {
		HashSet<BMNode> handled = new HashSet<BMNode>();
		ArrayList<BMGraph> al = new ArrayList<BMGraph>();
		for (BMNode node : b.getNodes()) {
			if (handled.contains(node))
				continue;
			BMGraph newGraph = new BMGraph();
			ccr(node, newGraph, b);
			for (BMNode in : newGraph.getNodes())
				handled.add(in);
			al.add(newGraph);

		}
		BMGraph[] ret = new BMGraph[al.size()];
		al.toArray(ret);
		return ret;
	}
	/**
	 * Uses distance scaling to solve initial layout.
	 * @param b
	 */
	public static void solvePositions(BMGraph b) {
		BMGraph[] components = connectedComponents(b);
		for (int i = 0; i < components.length; i++) {
			solveComponentPositions(components[i]);
		}

		Comparator<BMGraph> graphCmp = new Comparator<BMGraph>() {
			public int compare(BMGraph arg0, BMGraph arg1) {
				// TODO Auto-generated method stub
				return arg1.getNodes().size() - arg0.getNodes().size();
			}
		};
		Arrays.sort(components, graphCmp);
	//	System.out.println("kekeke");
		SquarePacker pack = new SquarePacker(graphWidth(components[0]));
		for (int i = 0; i < components.length; i++) {
			int wh = graphWidth(components[i]);
			Point p = pack.pack(wh);
			
			for (BMNode cnode : components[i].getNodes()) {
				String[] posstr = cnode.get(PrefuseBMGraph.POS_KEY).split(",");
				double x = Double.parseDouble(posstr[0]);
				double y = Double.parseDouble(posstr[1]);
				BMNode node = b.getNode(cnode);
				x = (p.x + 0.5 * (1 + x*0.9) * wh) * 120;
				y = (p.y + 0.5 * (1 + y*0.9) * wh) * 120;
			
				node.put(PrefuseBMGraph.POS_KEY, x + "," + y);
			}
			
		}
	//	System.out.println(components.length + " components");
	}
	
	private  static void solveComponentPositions(BMGraph b) {
		Matrix m = GraphUtils.bmToMatrix(b);
		int i = 0;

		if (b.getNodes().size() < 2 ) {
			for (BMNode n : b.getNodes())
				n.put(PrefuseBMGraph.POS_KEY, "0.0,0.0");
				

			return;
		}
		
		if (b.getNodes().size() > 300 ) {
			//Too large graph: would use too much time
			for (BMNode n : b.getNodes()){
				//n.put(PrefuseBMGraph.POS_KEY, "0.0,0.0");//(Math.random()-0.5)+","+(Math.random()-0.5));
				n.put(PrefuseBMGraph.POS_KEY, (Math.random()-0.5)+","+(Math.random()-0.5));
				
				n.put(PrefuseBMGraph.PINNED_KEY, "0");
			}


			return;
		}
		if(b.getNodes().size()==2){
			double x = -0.75;
			for (BMNode n : b.getNodes()){
				n.put(PrefuseBMGraph.POS_KEY, x+",0.0");
				x+=1.5;
			}
			return;
		}
		BMNode[] idToNode = new BMNode[m.cols()];
		for (BMNode node : b.getNodes()) {
			idToNode[i] = node;
			i++;
		}
		
		

		int n = m.cols();
		Vec2[] startpos = new Vec2[n];
		Random random = new Random();
		for (i = 0; i < n; i++) {
			int hash = idToNode[i].getId().hashCode();
			random.setSeed(hash);
			startpos[i] = new Vec2(0, 0);
			startpos[i].x = random.nextDouble();
			startpos[i].y = random.nextDouble();
		}
		StressMinimizer sm = new StressMinimizer(m, startpos);
		sm.iterate();
		Vec2[] pos = sm.getPositions();// GraphUtils.normalized(sm.p);
		pos = GraphUtils.normalized(pos);

		for (i = 0; i < n; i++) {
			double scale = 1;
			Vec2 p = pos[i].scaled(scale);
			idToNode[i].put(PrefuseBMGraph.POS_KEY, p.x + "," + p.y);
			idToNode[i].put(PrefuseBMGraph.PINNED_KEY, "0");
		}

		
	}

	public static void main(String[] args) {
		BMGraph b = null;
		try {
			if (args.length > 0) {
				b = BMGraphUtils.readBMGraph(args[0]);
			} else {
				b = BMGraphUtils.readBMGraph(System.in);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (b == null) {
			System.exit(1);
		}

		solvePositions(b);

		try {
			if (args.length > 0) {
				BMGraphUtils.writeBMGraph(b, args[0]);
			} else {
				BMGraphUtils.writeBMGraph(b, System.out);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
