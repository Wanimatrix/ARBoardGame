package be.wouterfranken.arboardgame.gameworld;

import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.utilities.Color;

public class WorldConfig {
	
	public final static WorldBorder BORDER = new WorldBorder(-AppConfig.BOARD_SIZE[0]/2, -AppConfig.BOARD_SIZE[1]/2, AppConfig.BOARD_SIZE[0]/2, AppConfig.BOARD_SIZE[1]/2);
	public final static int LEMMINGS_AMOUNT = 50;
	public final static WorldCoordinate STARTPOINT = new WorldCoordinate(0, -11.9f);
	public final static WorldCoordinate ENDPOINT = new WorldCoordinate(0, 11.9f);
	public final static float LEMMINGS_SPEED = 2;
	public final static int LEMMINGS_SIZE = 2;
	public final static Color LEMMINGS_COLOR = new Color(0, 1, 0, 1);
	public final static float LEMMING_HEIGHT = 1.2f;
	public final static float LEMMING_DISTANCE = 4f;
}