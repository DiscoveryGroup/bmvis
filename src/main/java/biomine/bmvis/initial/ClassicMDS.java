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

package biomine.bmvis.initial;




/**
 * Implements Classical scaling the way it was specified by  
 * "Applications of Multidimensional Scaling to Graph Drawing" the dissertation 
 * of Christian Pich.
 * 
 * 
 * @author Aleksi Hartikainen
 */
public class ClassicMDS {
	
	
	public static Vec2[] solve(Matrix edges)
	{
		Vec2[] startpos = new Vec2[edges.cols()];
		for(int i=0;i<startpos.length;i++)
			startpos[i] = new Vec2(Math.random(),Math.random());
		return solve(edges,startpos);
		
	}

	public static Vec2[] solve(Matrix edges,Vec2[] startpos)
	{
		edges = edges.copy();
		int n = edges.cols();

		for(int i=0;i<n;i++)
		{
			for(int j=0;j<n;j++)
				edges.set(i,j,edges.get(i,j));//*(0.99+0.02*Math.random()));
		}
		
		Vec2[] ret = new Vec2[edges.rows()];
		Matrix jm = new Matrix(edges.rows(),edges.cols());
		jm.setIdentity();
		for(int i=0;i<jm.rows();i++)
			for(int j=0;j<jm.cols();j++)
			{
				
				jm.set(i,j,jm.get(i,j)-1.0/n);
			}
		Matrix d2 = new Matrix(n,n);
		for(int i=0;i<n;i++)
			for(int j=0;j<n;j++)
				d2.set(i,j,Math.pow(edges.get(i,j),2));
		
		Matrix b = (jm.mul(d2).mul(jm)).scaled(-0.5);
		
		
		
		//DECOMPOSE
		int D = 2;
		double[] lam = new double[D];
		Matrix[] u = new Matrix[D];
		for(int i=0;i<D;i++)
			u[i]=new Matrix(n,1);
		
		
		for(int j=0;j<n;j++)
			u[0].set(j,0,startpos[j].x);

		for(int j=0;j<n;j++)
			u[1].set(j,0,startpos[j].y);
		
		double eps = 0.01;
		boolean stop=false;
		int it = 0;
		while(!stop&&it++<100)
		{
			stop=true;
			for(int i=0;i<D;i++)
			{
				Matrix mu = b.mul(u[i]);
				u[i] = mu.scaled(1.0/mu.length());
				Matrix oldi = u[i].copy();
				for(int j=0;j<i;j++)
				{
					double d = u[i].dot(u[j])/u[j].dot(u[j]);
					u[i].substract(u[j].scaled(d));
					
				}
				double diff = oldi.minus(u[i]).length();
				if(diff/oldi.length()>eps)stop=false;
			}
			
		}
		
		for(int i=0;i<D;i++){
			lam[i]=u[i].transpose().mul(b).mul(u[i]).get(0,0);
		}
		
		for(int i=0;i<D;i++){
			u[i].scale(Math.sqrt(Math.max(0,lam[i])));
		}
		
        double maxlen =  0 ;
		for(int i=0;i<n;i++)
		{
			double x = u[0].get(i,0);
			double y = u[1].get(i,0);
			if(Double.isNaN(y))y=0;
			if(Double.isNaN(x))x=0;
			ret[i] = new Vec2(x,y);
            maxlen = Math.max(maxlen,ret[i].length());
		}
		
		
		
		
	
		return ret;
	}
}
