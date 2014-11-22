package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Point;

import android.util.Log;

public class World {
	private static final String TAG = World.class.getSimpleName();
	
	private List<Brick> bricks = new ArrayList<Brick>();
	private Map<WorldCoordinate,WorldNode> theWorld = new HashMap<WorldCoordinate,WorldNode>();
	
	public World() {
		// Generate the world, fully linked.
		for(float x = WorldConfig.BORDER.getXStart();x<=WorldConfig.BORDER.getXEnd();x+=0.1f) {
			for(float y = WorldConfig.BORDER.getYStart();y<=WorldConfig.BORDER.getYEnd();y+=0.1f) {
				WorldNode newNode = new WorldNode();
				WorldCoordinate current = new WorldCoordinate(x,y);
				WorldCoordinate left = new WorldCoordinate(x-0.1f,y);
				WorldCoordinate bottom = new WorldCoordinate(x,y-0.1f);
				if(theWorld.get(left) != null) {
					theWorld.get(left).setRight(current);
					newNode.setLeft(left);
				} else if(theWorld.get(bottom) != null) {
					theWorld.get(bottom).setTop(current);
					newNode.setBottom(bottom);
				}
				theWorld.put(current, newNode);
			}
		}
	}
	
	public void addBrick(Brick brick) {
		bricks.add(brick);
	}
	
	public WorldNode getNode(WorldCoordinate co) {
		return theWorld.get(co);
	}
}
