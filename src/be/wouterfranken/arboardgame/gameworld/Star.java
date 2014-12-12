package be.wouterfranken.arboardgame.gameworld;

import java.util.Random;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.CubeMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.utilities.Color;

public class Star {
	private static final Random random = new Random();
	private static final String TAG = Star.class.getSimpleName();
	
	private final WorldCoordinate position;
	private MeshObject mesh;
	
	public Star() {
		float startX = WorldConfig.STAR_GENERATION_AREA[0][0];
		float startY = WorldConfig.STAR_GENERATION_AREA[0][1];
		float endX = WorldConfig.STAR_GENERATION_AREA[1][0];
		float endY = WorldConfig.STAR_GENERATION_AREA[1][1];
		int xAmount = (int) (Math.abs(endX-startX)*(1.0f/WorldConfig.NODE_DISTANCE));
		int yAmount = (int) (Math.abs(endY-startY)*(1.0f/WorldConfig.NODE_DISTANCE));
		float x = random.nextInt(xAmount)*WorldConfig.NODE_DISTANCE+startX;
		float y = random.nextInt(yAmount)*WorldConfig.NODE_DISTANCE+startY;
		
		position = new WorldCoordinate(x, y);
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Star position: "+position.toString());
		
		mesh = new CubeMesh(WorldConfig.STAR_SIZE, position.x, position.y, 1, new RenderOptions(true, new Color(1, 1, 0, 1), true));
	}
	
	public MeshObject getMesh() {
		return mesh;
	}
	
	public WorldCoordinate getPosition() {
		return position;
	}
}
