package be.wouterfranken.arboardgame.utilities;

public class Utilities {
	
	/**
	 * Returns the index, using circular indexing.
	 * @param 	index
	 * 		  	The index that must be translated to a circular index.
	 * @param 	max
	 * 			The maximum possible index.
	 */
	public static int getCircularIndex(int index, int max) {
		return (index + max) % (max);
	}
}
