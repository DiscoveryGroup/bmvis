package biomine.bmvis;

import prefuse.util.force.AbstractForce;
import prefuse.util.force.ForceItem;

public class FreezingForce extends AbstractForce {
	final public static float MIN_EPSILON = 0.0f; 
	final public static float DEFAULT_EPSILON = 0.0001f;
	final public static float MAX_EPSILON = 0.001f;
	public static String[] paramNames;
	private BMVis bmvis;
	
	public FreezingForce(BMVis bmvis) {
		this.bmvis = bmvis;
		
		params = new float[1];
		minValues = new float[1];
		maxValues = new float[1];
		
		params[0] = DEFAULT_EPSILON;
		
		paramNames = new String[1];
		paramNames[0] = "Epsilon";
		super.setMinValue(0, 0.0f);
		super.setMaxValue(0, MAX_EPSILON);
	}
	
	@Override
	protected String[] getParameterNames() {
		return paramNames;
	}

	@Override
	public void getForce(ForceItem item) {
		// no-op
	}
	
	@Override
	public boolean isItemForce() {
        return true;
    }

	@Override
	public void setParameter(int i, float val) {
		super.setParameter(i, val);
		bmvis.setLayoutEnabled(bmvis.enableAutomaticLayout);
	}	
}
