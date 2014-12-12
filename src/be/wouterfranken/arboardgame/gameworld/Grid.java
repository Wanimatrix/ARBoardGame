package be.wouterfranken.arboardgame.gameworld;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;

public class Grid {
	private static final String TAG = Grid.class.getSimpleName();
	private Map<WorldCoordinate,GridNode> theGrid = new HashMap<WorldCoordinate,GridNode>();
	
	public Grid(WorldBorder border, float nodeDistance) {
		for(float x = border.getXStart();x<=border.getXEnd();x+=nodeDistance) {
			for(float y = border.getYStart();y<=border.getYEnd();y+=nodeDistance) {
				WorldCoordinate current = new WorldCoordinate(x,y);
				GridNode newNode = new GridNode(current);
				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "GRID Node generation, current node: "+current);
				WorldCoordinate left = current.getLeft();
				WorldCoordinate bottom = current.getBottom();
				if(theGrid.get(left) != null) {
					theGrid.get(left).setRight(newNode);
					newNode.setLeft(theGrid.get(left));
				} 
				if(theGrid.get(bottom) != null) {
					theGrid.get(bottom).setTop(newNode);
					newNode.setBottom(theGrid.get(bottom));
				}
				theGrid.put(current, newNode);
			}
		}
	}
	
	public GridNode getGridNode(WorldCoordinate coordinate) {
		return theGrid.get(coordinate);
	}
}
