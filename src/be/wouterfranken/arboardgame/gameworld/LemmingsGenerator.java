package be.wouterfranken.arboardgame.gameworld;

import java.util.List;

import org.opencv.core.Point;

import be.wouterfranken.arboardgame.rendering.meshes.CubeMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.utilities.Color;

public class LemmingsGenerator {
	private List<MeshObject> lemmings;
	private List<LemmingPath> lemmingPaths;
	private int size;
	private float speed;
	private int amount;
	private float height;
	private WorldCoordinate start;
	private WorldCoordinate end;
	private Color color;
	
	private World w;
	
	public LemmingsGenerator(World w) {
		this.speed = WorldConfig.LEMMINGS_SPEED;
		this.amount = WorldConfig.LEMMINGS_AMOUNT;
		this.size = WorldConfig.LEMMINGS_SIZE;
		this.start = WorldConfig.STARTPOINT;
		this.end = WorldConfig.ENDPOINT;
		this.color = WorldConfig.LEMMINGS_COLOR;
		this.height = WorldConfig.LEMMING_HEIGHT;
		this.w = w;
	}
	
	private void frameTick() {
		if(lemmings.isEmpty()) {
			Pathfinder.findPath(start, end, w);
			lemmings.add(new CubeMesh(size, start, height, new RenderOptions(true, color)));
		}
	}
}
