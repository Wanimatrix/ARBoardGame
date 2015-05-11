package be.wouterfranken.arboardgame.rendering.tracking;

import android.util.Log;
import be.wouterfranken.arboardgame.utilities.Configuration;
import be.wouterfranken.arboardgame.utilities.ConfigurationItem;

public class BrickTrackerConfig extends Configuration {
	
	private static ConfigurationItem<?>[] INITIAL_CONFIG_ITEMS = 
		{
			new ConfigurationItem<Long>("MC", "NECESS_MERGE_COUNTS", "Necessary merges", 3l),
			new ConfigurationItem<Long>("ORI", "NECESS_ORIENTATIONS", "Necessary orientations", 18l),
			new ConfigurationItem<Double>("REM_THR", "OVERLAP_REMOVAL_THRESHOLD", "Overlap removal threshold", 0.75),
			new ConfigurationItem<Boolean>("FB", "FALSEBRICK_DETECTION", "Falsebrick detection", false),
			new ConfigurationItem<Long>("FB_THR", "FALSEBRICK_AMOUNT_THRESHOLD", "Falsebrick detection amount threshold", 3l),
			new ConfigurationItem<Double>("REMV_B", "NECESS_REMOVAL_VOTES_BASE", "Removal Votes base", 100.0),
			new ConfigurationItem<Double>("REMV_PS", "NECESS_REMOVAL_VOTES_POWSUBT", "Removal Votes subtracted power", 0.5),
			new ConfigurationItem<Double>("REMV_A", "NECESS_REMOVAL_VOTES_ADD", "Removal Votes addition", 3.0),
			new ConfigurationItem<Double>("APDP", "APPROX_POLY_DP_PARAM", "Approx PolyDP parameter", 0.02)
		};
	
	public BrickTrackerConfig() {
		super(INITIAL_CONFIG_ITEMS);
	}
	
	public int getNecessRemovalVotes(float x) {
		return (int) (Math.pow((Double)items.get("REMV_B").getValue(), (x-(Double)items.get("REMV_PS").getValue()))+(Double)items.get("REMV_A").getValue());
	}
	
	@Override
	public void reset() {
		Log.d("RESET PARAMS", "PARAMS WERE RESET!");
		items.clear();
		setAllItems(INITIAL_CONFIG_ITEMS);
	}
}
