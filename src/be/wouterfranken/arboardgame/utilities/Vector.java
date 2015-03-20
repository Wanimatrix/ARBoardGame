package be.wouterfranken.arboardgame.utilities;

import org.opencv.core.Point;

public class Vector extends Point {
	
	public Vector() {
		super();
	}
	
	public Vector(Point p) {
		super(p.x, p.y);
	}
	
	public Vector(Point a, Point b) {
		super(b.x-a.x, b.y-a.y);
	}
	
	public Vector(double x, double y) {
		super(x, y);
	}
	
	public double getNorm() {
		return Math.sqrt(x*x+y*y);
	}
	
	public Vector normalize() {
		double norm = getNorm();
		return new Vector(x/norm,y/norm);
	}
	
	public Vector resize(float length) {
		Vector result = this.normalize();
		return new Vector(result.x*length,result.y*length);
	}
	
	public Vector subtract(Vector other) {
		return new Vector(x-other.x, y-other.y);
	}
	
	public Point add(Point p) {
		return new Vector(x+p.x, y+p.y);
	}
	
	public float getAngleWithHorVector() {
		float angleDeg = (float) (Math.acos(x)*(180.0f/Math.PI)); 
	    return (y > 0) ? (angleDeg*-1) : angleDeg;
	}
}
