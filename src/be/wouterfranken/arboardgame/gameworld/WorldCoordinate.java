package be.wouterfranken.arboardgame.gameworld;

import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class WorldCoordinate {
	
	public final float x;
	public final float y;
	
	public WorldCoordinate(float x, float y) {
		this.x = Math.round((x) * 10) / 10.0f;
		this.y = Math.round((y) * 10) / 10.0f;
	}
	
	public float distance(WorldCoordinate other) {
		return MathUtilities.distance(x, y, other.x, other.y);
	}
	
	@Override
	public int hashCode() {
		int xToInt = (int) (x*10);
		int yToInt = (int) (y*10);
	    return (xToInt * 31) ^ yToInt;
	  }
	
	@Override
	public boolean equals(Object o) {
		if(o.getClass() == this.getClass())
			return ((WorldCoordinate)o).x == this.x && ((WorldCoordinate)o).y == this.y;
		return false;
	}
	
	@Override
	public String toString() {
		return "("+x+","+y+")";
	}
}
