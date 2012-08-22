package biomine.bmvis.initial;
public final class Vec2
{
	public double x,y;
    
    public Vec2(double xx,double yy)
    {
        x=xx;
        y=yy;
    }
    
    public Vec2 copy()
    {
        Vec2 v = new Vec2(x,y);
        return v;
    }

    public double length2()
    {
        return (x*x+y*y);
    }
    public double dist(Vec2 v)
    {
    	return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y));
    }
    public double length()
    {
        return Math.sqrt(length2());
    }
    public double dot(Vec2 o)
    {
        return x*o.x+y*o.y;
    }

    public Vec2 plus(Vec2 o)
    {
        Vec2 ret = new Vec2(x+o.x,y+o.y);
        return ret;
    }
    public Vec2 minus(Vec2 o)
    {
        Vec2 ret = new Vec2(x-o.x,y-o.y);
        return ret;
    }
    public void scale(double d)
    {
        x*=d;
        y*=d;
    }
    public Vec2 scaled(double d)
    {
        Vec2 v = copy();
        v.scale(d);
        return v;
    }
    public void add(Vec2 v)
    {
    	x+=v.x;
    	y+=v.y;
    }
    public void substract(Vec2 v)
    {
    	x-=v.x;
    	y-=v.y;
    }
    public String toString(){
    	return ""+x+","+y;
    }
}
