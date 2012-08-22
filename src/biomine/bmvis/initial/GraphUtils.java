package biomine.bmvis.initial;

import java.util.HashMap;

import biomine.bmgraph.BMEdge;
import biomine.bmgraph.BMGraph;
import biomine.bmgraph.BMNode;
import biomine.bmvis.PrefuseBMGraph;

public class GraphUtils {

	public static double[][] shortestPaths(double[][] e) {

		int n = e.length;
		double[][] s = new double[n][n];

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (e[i][j] == 0.0)
					s[i][j] = 1000000;
				else
					s[i][j] = e[i][j];
			}

		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				for (int k = 0; k < n; k++) {
					if (j == k)
						continue;
					s[j][k] = Math.min(s[j][k], s[j][i] + s[i][k]);
				}
			}
		}
		for (int i = 0; i < n; i++)
			s[i][i] = 0;
		return s;
	}

	public static Matrix shortestPaths(Matrix e) {
		return new Matrix(shortestPaths(e.getArray()));
	}

	public static Matrix bmToMatrix(BMGraph b) {
		Matrix ret = new Matrix(b.numNodes(), b.numNodes());
		int i = 0;
		HashMap<BMNode, Integer> nodeToId = new HashMap<BMNode, Integer>();
		for (BMNode node : b.getNodes()) {
			nodeToId.put(node,i);
			i++;
		}
		for(BMEdge edge : b.getEdges() ) {
			int from = nodeToId.get(edge.getSource());
			int to = nodeToId.get(edge.getTarget());
			String goodString = edge.get(PrefuseBMGraph.GOODNESS_KEY);
			double good = 1;
			if(goodString!=null){
				good = Double.parseDouble(goodString);
			}
			/* else{
		//		System.err.println("No good");
			} */
			ret.set(from,to,1/good);//0.2-Mat	h.log(good));
			ret.set(to,from,1/good);//0.2-Math.log(good));
		}
		

		return ret;
	}

	public static Vec2[] normalized(Vec2[] points) {
		Vec2[] ret = new Vec2[points.length];
		double maxlen = 0;
		for (int i = 0; i < points.length; i++)
			maxlen = Math.max(maxlen, points[i].length());
		for (int i = 0; i < points.length; i++)
			ret[i] = points[i].scaled(1 / maxlen);
		return ret;
	}
}
