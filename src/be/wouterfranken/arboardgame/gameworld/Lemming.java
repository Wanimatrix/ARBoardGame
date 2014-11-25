package be.wouterfranken.arboardgame.gameworld;

import java.util.List;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.CubeMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class Lemming {
	private static final String TAG = Lemming.class.getSimpleName();
	
	private MeshObject mesh;
	private List<WorldCoordinate> path;
	
	private float locationX;
	private float locationY;
	private int size;
	private float height;
	private float speed; // cm/s
//	private Color color;
	
	public Lemming(int size, float height, float locationX, float locationY, float speed, List<WorldCoordinate> path, Color color) {
		mesh = new CubeMesh(size, locationX, locationY, height, new RenderOptions(true, color));
		this.locationX = locationX;
		this.locationY = locationY;
		this.size = size;
		this.height = height;
//		this.color = color;
		this.speed = speed;
		this.path = path;
	}
	
	public float getLocationX() {
		return locationX;
	}
	
	public float getLocationY() {
		return locationY;
	}
	
	public MeshObject getMesh() {
		return mesh;
	}
	
	public void updateLocation() {
		float fps = AppConfig.FPS_RANGE[1]/1000.0f; // max frame/sec
		float todoDistance = speed/fps;
//		int i = 1;
		Log.d(TAG, "Distance todo: "+todoDistance);
		
		float newLocationX = locationX;
		float newLocationY = locationY;
		float distance;
		while(todoDistance > 0) {
			if(path.size() == 1) {
				newLocationX = path.get(0).x;
				newLocationY = path.get(0).y;
				break;
			}
			WorldCoordinate to = path.get(1);
			float distToEnd = MathUtilities.distance(newLocationX,newLocationY,to.x,to.y);
			if(distToEnd < todoDistance) {
				distance = distToEnd;
				todoDistance -= distToEnd;
				path.remove(0);
			} else {
				distance = todoDistance;
				todoDistance = 0;
			}
			float slope = (to.y-newLocationY)/(to.x-newLocationX);
			Log.d(TAG, "Slope: "+slope);
			if(Float.isInfinite(slope)) {
				newLocationY += distance;
			} else if(Math.abs(slope) == 0){
				newLocationX += distance;
			} else {
				newLocationX = (float) (newLocationX+Math.signum(to.x-newLocationX)*(distance/Math.sqrt(1+slope*slope)));
				newLocationY = (float) (newLocationY+Math.signum(to.y-newLocationY)*(distance/Math.sqrt(1+slope*slope))*slope);
			}
		}
		Log.d(TAG, "Lemming location updated: from ("+locationX+","+locationY+"), to ("+newLocationX+","+newLocationY+")");
		locationX = newLocationX;
		locationY = newLocationY;
		mesh = new CubeMesh(size, locationX, locationY, height, mesh.getRenderOptions());
	}
	
//	public void setPath(List<WorldCoordinate> path) {
//		this.path = new LemmingPath(path);	
//	}
}
