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

public final class Matrix {
	private double[][] data;
	private int c,r;
	
	public double[][] getArray()
	{
		return data;
	}
	int rows()
	{
		return r;
	}
	int cols()
	{
		return c;
	}
	public Matrix(double[][] arr)
	{
		r=arr.length;
		c=arr[0].length;
		data = arr;
		for(int i=0;i<arr.length;i++)
			if(arr[i].length!=c)
				throw new IllegalArgumentException(
						"variable number of columns in array");
	}

	public Matrix(int r,int c)
	{
		this.r=r;
		this.c=c;
		data = new double[r][c];
	}
	
	double dot(Matrix m)
	{
		if(cols()!=m.cols() || rows()!=m.rows())
			throw new IllegalArgumentException(
					"Incompatible matrix sizes for addition"
			);
		double s = 0;
		for(int i=0;i<r;i++)
		{
			for(int j=0;j<c;j++)
				s+=get(i,j)*m.get(i,j);
		}
		return s;
	}
	double length()
	{
		double s = 0;
		for(int i=0;i<r;i++)
			for(int j=0;j<c;j++)
				s+=data[i][j]*data[i][j];
		return Math.sqrt(s);
	}
	double get(int row,int col)
	{
		return data[row][col];
	}
	void set(int row,int col,double val)
	{
		data[row][col]=val;
	}
	Matrix mul(Matrix m)
	{
		if(cols()!=m.rows())
			throw new IllegalArgumentException(
					"Incompatible matrix sizes for multiplication"
			);
		
		Matrix ret = new Matrix(rows(),m.cols());
		for(int i=0;i<rows();i++)
		{
			for(int j=0;j<m.cols();j++)
			{
				double v = 0;
				for(int k=0;k<cols();k++)
				{
					v+=get(i,k)*m.get(k,j);
				}
				ret.set(i,j,v);
			}
		}
		return ret;
	}
	Matrix copy()
	{
		Matrix ret = new Matrix(rows(),cols());
		for(int i=0;i<rows();i++)
			for(int j=0;j<cols();j++)
				ret.set(i,j,get(i,j));
		return ret;
	}
	void scale(double d)
	{
		
		for(int i=0;i<rows();i++)
		for(int j=0;j<cols();j++)
			set(i,j,get(i,j)*d);
	}
	Matrix scaled(double d)
	{
		Matrix ret = new Matrix(rows(),cols());
		
		for(int i=0;i<rows();i++)
		for(int j=0;j<cols();j++)
			ret.set(i,j,get(i,j)*d);
		return ret;
	}
	Matrix plus(Matrix m)
	{
		if(cols()!=m.cols() || rows()!=m.rows())
		throw new IllegalArgumentException(
				"Incompatible matrix sizes for addition"
		);
		Matrix ret = new Matrix(m.rows(),m.cols());
		for(int i=0;i<m.rows();i++)
			for(int j=0;j<m.cols();j++)
				ret.set(i,j,get(i,j)+m.get(i,j));
		return ret;
	}
	void substract(Matrix m){
		for(int i=0;i<m.rows();i++)
			for(int j=0;j<m.cols();j++)
				set(i,j,get(i,j)-m.get(i,j));
		
	}
	void add(Matrix m){
		for(int i=0;i<m.rows();i++)
			for(int j=0;j<m.cols();j++)
				set(i,j,get(i,j)+m.get(i,j));
		
	}
	Matrix minus(Matrix m)
	{
		if(cols()!=m.cols() || rows()!=m.rows())
		throw new IllegalArgumentException(
				"Incompatible matrix sizes for addition"
		);
		Matrix ret = this.copy();
		ret.substract(m);
		return ret;
	}
	void setIdentity()
	{
		for(int i=0;i<r;i++)
		{
			for(int j=0;j<c;j++)
			{
				data[i][j]=0;
			}
			data[i][i]=1;
		}
	}
	
	public String toString(){
		StringBuffer buf = new StringBuffer();
		for(int i=0;i<r;i++){
			for(int j=0;j<c;j++)
				buf.append(get(i,j)+" ");
			buf.append(";");
		}
		return buf.toString();
	}
	
	Matrix transpose()
	{
		Matrix ret = new Matrix(c,r);
		for(int i=0;i<r;i++)
			for(int j=0;j<c;j++)
				ret.set(j,i,get(i,j));
		return ret;
	}
	
	
}
