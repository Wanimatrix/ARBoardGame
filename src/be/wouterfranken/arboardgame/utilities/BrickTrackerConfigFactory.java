package be.wouterfranken.arboardgame.utilities;

import be.wouterfranken.arboardgame.rendering.tracking.BrickTrackerConfig;

public class BrickTrackerConfigFactory extends ConfigFactory {
	private static BrickTrackerConfigFactory instance = new BrickTrackerConfigFactory();

	public BrickTrackerConfigFactory() {
		configuration = new BrickTrackerConfig();
	}
	
	public static BrickTrackerConfig getConfiguration() {
		return (BrickTrackerConfig)instance.configuration;
	}


}
