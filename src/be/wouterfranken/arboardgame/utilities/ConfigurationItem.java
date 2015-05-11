package be.wouterfranken.arboardgame.utilities;

public class ConfigurationItem<T> {
	public String shortName;
	public String longName;
	public String description;
	public T value;
	
	public ConfigurationItem(String shortName, String longName, String description, T value) {
		this.shortName = shortName;
		this.longName = longName;
		this.description = description;
		this.value = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getLongName() {
		return longName;
	}
	
	public String getShortName() {
		return shortName;
	}
	
	public T getValue() {
		return value;
	}
	
	public void setValue(Boolean value) {
		setValue((T)value);
	}
	
	public void setValue(Number value) {
		setValue((T)value);
	}
	
	private void setValue(T value) {
		this.value = value;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new ConfigurationItem<T>(shortName, longName, description, value);
	}
}
