package be.wouterfranken.arboardgame.gameworld;

import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.utilities.Color;

public class WorldConfig {
	
	// World configuration options
	public final static WorldBorder BORDER = new WorldBorder(-AppConfig.BOARD_SIZE[0]/2, -AppConfig.BOARD_SIZE[1]/2, AppConfig.BOARD_SIZE[0]/2, AppConfig.BOARD_SIZE[1]/2);
	public final static float NODE_DISTANCE = 0.5f;
	
	// Brick configuration options
	public final static float BRICK_PERIMETER = 0.5f;
	
	// Star configuration options
	public final static float STAR_PERIMETER = 2;
	public final static float STAR_SIZE = 1f;
	public final static float[][] STAR_GENERATION_AREA = 
			new float[][]{new float[]{-5f,-6.5f},new float[]{5f,6.5f}};
	public final static int STAR_AMOUNT_PER_LEMMING = 1;
	
	// Lemming Configuration options
	public final static WorldCoordinate STARTPOINT = new WorldCoordinate(0, -11.5f);
	public final static WorldCoordinate ENDPOINT = new WorldCoordinate(0, 11.5f);
	public final static int LEMMINGS_AMOUNT = 5;
	public final static float LEMMINGS_SPEED_WITH_STARS = 0.5f;
	public final static float LEMMINGS_SPEED_NO_STARS = 1.2f;
	public final static float LEMMINGS_SIZE = 0.5f;
	public final static Color LEMMINGS_COLOR = new Color(0, 1, 0, 1);
	public final static float LEMMING_HEIGHT = 0.1f;
	
	// TWO POSSIBLE CONFIGURATIONS:
	//	* Generate a new lemming only after arrival of the previous one: ONE_PER_ONE = TRUE
	//  * Generate a new lemming after the previous one has a certain distance of the start.
	public final static boolean ONE_PER_ONE = true;
	public final static float LEMMING_DISTANCE = 10f;
}