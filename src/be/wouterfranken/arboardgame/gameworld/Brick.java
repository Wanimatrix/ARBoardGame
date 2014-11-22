package be.wouterfranken.arboardgame.gameworld;

import org.opencv.core.Point3;

public class Brick {
	
	private final Point3[] corners;
	
	public Brick(Point3 first, Point3 second) {
		this.corners = new Point3[]{first, second};
	}
	
	public Point3[] getCorners() {
		return corners;
	}
}
