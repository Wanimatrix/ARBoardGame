package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import be.wouterfranken.arboardgame.rendering.tracking.BrickTrackerConfig;
import android.util.Log;

@SuppressWarnings("serial")
public class LegoBrickContainer extends ArrayList<LegoBrick> {

	public LegoBrickContainer(LegoBrick brick) {
		this.add(brick);
	}
	
	public LegoBrickContainer[] mergeCheck(LegoBrickContainer[] others) {
		List<LegoBrickContainer> result = new ArrayList<LegoBrickContainer>(Arrays.asList(others));
		for (int i = 0; i < this.size(); i++) {
			Iterator<LegoBrickContainer> it = result.iterator();
			while(it.hasNext()) {
				LegoBrickContainer lc = it.next();
				List<Integer> mergeIdxes = this.get(i).mergeCheckAll(lc.toArray(new LegoBrick[lc.size()]));
				int k = 0;
				Log.d("INDEX_TEST", "New indexes");
				for (Integer idx : mergeIdxes) {
					Log.d("INDEX_TEST", "Remove index: "+idx);
					lc.remove(idx-k++);
					Log.d("INDEX_TEST", "Container size: "+lc.size());
					if(lc.size() == 0) {
						it.remove();
					}
				}
			}
		}
		
		return result.toArray(new LegoBrickContainer[result.size()]);
	}
	
	public boolean readyToBecomeRealBrick() {
		if(this.size() == 1 && this.get(0).getMergeCount() >= BrickTrackerConfig.NECESS_MERGE_COUNTS 
				&& this.get(0).getOrientations().size() >= BrickTrackerConfig.NECESS_ORIENTATIONS)
		{
			Log.d("REALBRICK", "Removal votes: "+this.get(0).getRemovalVotes());
			return true;
		} else {
			if(this.size() != 1) {
				Log.d("REALBRICK", "Reason: size == "+this.size());
			} else if (this.get(0).getMergeCount() < 5) {
				Log.d("REALBRICK", "Reason: mergeCount == "+this.get(0).getMergeCount());
			} else {
				Log.d("REALBRICK", "Reason: orientations == "+this.get(0).getOrientations().size());
				Log.d("REALBRICK", "Orientations amount: "+this.get(0).getOrientations().size());
			}
			return false;
		}
	}
}
