package be.wouterfranken.arboardgame.gameworld;

import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class WorldCoordinate {
	
	public final float x;
	public final float y;
	
	public static WorldCoordinate getClosestWorldCoordinate(float[] point, boolean floorX, boolean floorY) {
		float xWco = (float) (floorX ? Math.floor(point[0]) : Math.ceil(point[0]));
		float yWco = (float) (floorY ? Math.floor(point[1]) : Math.ceil(point[1]));
		while(Math.abs(xWco-point[0]) >= WorldConfig.NODE_DISTANCE) xWco = floorX ? xWco + WorldConfig.NODE_DISTANCE : xWco - WorldConfig.NODE_DISTANCE;
		while(Math.abs(yWco-point[1]) >= WorldConfig.NODE_DISTANCE) yWco = floorY ? yWco + WorldConfig.NODE_DISTANCE : yWco - WorldConfig.NODE_DISTANCE;
		return new WorldCoordinate(xWco, yWco);
	}
	
	public WorldCoordinate(float x, float y) {
		this.x = Math.round((x) * 10) / 10.0f;
		this.y = Math.round((y) * 10) / 10.0f;
	}
	
	public float distance(WorldCoordinate other) {
		return MathUtilities.distance(x, y, other.x, other.y);
	}
	
	public WorldCoordinate getLeft() {
		if(x-WorldConfig.NODE_DISTANCE < WorldConfig.BORDER.getXStart()) return null;
		return new WorldCoordinate(x-WorldConfig.NODE_DISTANCE, y);
	}
	
	public WorldCoordinate getRight() {
		if(x+WorldConfig.NODE_DISTANCE > WorldConfig.BORDER.getXEnd()) return null;
		return new WorldCoordinate(x+WorldConfig.NODE_DISTANCE, y);
	}
	
	public WorldCoordinate getTop() {
		if(y+WorldConfig.NODE_DISTANCE > WorldConfig.BORDER.getYEnd()) return null;
		return new WorldCoordinate(x, y+WorldConfig.NODE_DISTANCE);
	}
	
	public WorldCoordinate getBottom() {
		if(y-WorldConfig.NODE_DISTANCE < WorldConfig.BORDER.getYStart()) return null;
		return new WorldCoordinate(x, y-WorldConfig.NODE_DISTANCE);
	}
	
	@Override
	public int hashCode() {
		float tmpX = x - WorldConfig.BORDER.getXStart();
		float tmpY = y - WorldConfig.BORDER.getYStart();
		int xToInt = (int) (tmpX*10/(WorldConfig.NODE_DISTANCE*10));
		int yToInt = (int) (tmpY*10/(WorldConfig.NODE_DISTANCE*10));
		return xToInt * 1000 + yToInt;
//	    return (xToInt * 31) ^ yToInt;
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
