package be.wouterfranken.arboardgame.gameworld;

import org.opencv.core.Point;

public class WorldBorder {
	private Point xBounds;
	private Point yBounds;
	
	public WorldBorder(float x0, float y0, float x1, float y1) {
		xBounds = x0 < x1 ? new Point(x0,x1) : new Point(x1,x0);
		yBounds =  y0 < y1 ? new Point(y0,y1) : new Point(y1,y0);
	}
	
	public float getXStart() {
		return (float) xBounds.x;
	}
	
	public float getXEnd() {
		return (float) xBounds.y;
	}
	
	public float getYStart() {
		return (float) yBounds.x;
	}
	
	public float getYEnd() {
		return (float) yBounds.y;
	}
}
