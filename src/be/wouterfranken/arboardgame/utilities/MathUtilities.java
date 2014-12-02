package be.wouterfranken.arboardgame.utilities;

public class MathUtilities {
	/**
	 * Calculates the angle (in degrees) between vectors (point0-point1) and (point2-point1)
	 */
	public static float angle(float[] point0, float[] point1, float[] point2) {
		float[] vec1 = MathUtilities.vector(point1, point0);
		float[] vec2 = MathUtilities.vector(point1, point2);
		float angle = (float) Math.acos(dot(vec1, vec2)/(norm(vec1)*norm(vec2)));
		return (float) (angle * (180/Math.PI));
	}
	
	/**
	 * Returns the point1-point0 vector.
	 */
	public static float[] vector(float[] point0, float[] point1) {
		return new float[]{point1[0]-point0[0],point1[1]-point0[1],point1[2]-point0[2]};
	}
	
	/**
	 * Returns the dot product of two given vectors.
	 */
	public static float dot(float[] vec1, float[] vec2) {
		return vec1[0]*vec2[0]+vec1[1]*vec2[1]+vec1[2]*vec2[2];
	}
	
	/**
	 * Returns the cross product of two given vectors.
	 */
	public static float[] cross(float[] vec1, float[] vec2) {
		return new float[]{vec1[1]*vec2[2]-vec1[2]*vec2[1],vec1[2]*vec2[0]-vec1[0]*vec2[2],vec1[0]*vec2[1]-vec1[1]*vec2[0]};
	}
	
	/**
	 * Returns the mean between two vectors
	 */
	public static float[] mean(float[] vec1, float[] vec2) {
		return new float[]{(vec1[0]+vec2[0])/2,(vec1[1]+vec2[1])/2,(vec1[2]+vec2[2])/2};
	}
	
	/**
	 * Returns a point that is the endpoint of given vector when its startpoint is the given startPoint.
	 */
	public static float[] vectorToPoint(float[] vec, float[] startPoint) {
		return new float[]{startPoint[0] + vec[0],startPoint[1] + vec[1],startPoint[2] + vec[2]};
	}
	
	/**
	 * Multiplies given vector with given scalar.
	 */
	public static float[] multiply(float[] vec, float scalar) {
		return new float[]{vec[0]*scalar,vec[1]*scalar,vec[2]*scalar};
	}
	
	/**
	 * Resizes the vector: the resulting vector has given size as norm.
	 */
	public static float[] resize(float[] vec, float size) {
		float norm = norm(vec);
		return multiply(multiply(vec, 1.0f/norm),size);
	}
	
	/**
	 * Returns the norm of given vector.
	 */
	public static float norm(float[] vec) {
		return (float) Math.sqrt(vec[0]*vec[0]+vec[1]*vec[1]+vec[2]*vec[2]);
	}
	
	/**
	 * Returns 
	 * 		-1 if given point is right of the edge (edge1-edge0).
	 * 		0 if given point is on the edge (edge1-edge0).
	 * 		1 if given point is left of the edge (edge1-edge0).
	 */
	public static int pointLeftOfEdge(float[] point, float[] edgeP0, float[] edgeP1) {
		float A = -(edgeP1[1]-edgeP0[1]);
		float B = (edgeP1[0]-edgeP0[0]);
		float C = -(A*edgeP0[0] + B*edgeP0[1]);
		float D = A*point[0] + B*point[1] + C;
		if(D < 0) return -1;
		else if (D == 0) return 0;
		else return 1;
	}
	
	public static float distance(float x0, float y0, float x1, float y1) {
		return (float) Math.sqrt((x1 - x0)*(x1 - x0) + (y1 - y0)*(y1 - y0));
	}
}
