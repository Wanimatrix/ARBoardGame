package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;

import org.apache.commons.lang.builder.HashCodeBuilder;

public class LemmingPath extends ArrayList<WorldCoordinate> {
	private static final long serialVersionUID = -5059287462594666196L;
	
	@Override
	public int hashCode() {
		HashCodeBuilder builder = new HashCodeBuilder();
		for (WorldCoordinate pathNode : this) {
			builder.append(pathNode);
		}
		return builder.toHashCode();
	}
}
