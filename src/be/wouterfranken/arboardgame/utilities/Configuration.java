package be.wouterfranken.arboardgame.utilities;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.OrderedMap;

import android.util.Log;
import be.wouterfranken.arboardgame.rendering.tracking.BrickTrackerConfig;

public abstract class Configuration {
	private static final String TAG = Configuration.class.getSimpleName();
	
	protected final SortedMap<String,ConfigurationItem<?>> items = new TreeMap<String,ConfigurationItem<?>>();
	
	public Configuration(ConfigurationItem<?>... items) {
		setAllItems(items);
	}
	
	@Override
	public String toString() {
		String result = BrickTrackerConfig.class.getSimpleName()+" params: ";
		for (ConfigurationItem<?> configItem : items.values()) {
			result += configItem.getShortName()+" = "+configItem.getValue()+", ";
		}
		return result;
	}
	
	public String[] getShortNames() {
		return items.keySet().toArray(new String[items.size()]);
	}
	
	public String[] getLongNames() {
		String[] result = new String[items.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = items.get(getShortNames()[i]).getLongName();
		}
		return result;
	}
	
	public String[] getDescriptions() {
		String[] result = new String[items.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = items.get(getShortNames()[i]).getDescription();
		}
		return result;
	}
	
	public Object[] getValues() {
		Object[] result = new Object[items.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = items.get(getShortNames()[i]).getValue();
		}
		return result;
	}
	
	public Object getItem(String shortName) {
		return items.get(shortName).value;
	}
	
	public void set(String shortName, String value) throws ParseException {
		Log.d(TAG, "New value to be set: "+value+", class: "+items.get(shortName).getValue().getClass()+", superClass: "
					+items.get(shortName).getValue().getClass().getSuperclass());
		if(items.get(shortName).getValue().getClass() == Boolean.class) {
			Log.d(TAG, "CLASS: "+items.get(shortName).getValue().getClass());
			items.get(shortName).setValue(Boolean.parseBoolean(value));
		}
		else if(items.get(shortName).getValue().getClass().getSuperclass() == Number.class) {
			Log.d(TAG, "SUPERCLASS: "+items.get(shortName).getValue().getClass().getSuperclass());
			Log.d(TAG, "New value string: "+value);
			Log.d(TAG, "New value parsed: "+NumberFormat.getNumberInstance(Locale.ENGLISH).parse(value));
			items.get(shortName).setValue(NumberFormat.getNumberInstance(Locale.ENGLISH).parse(value));
		}
		
	}
	
	protected void setAllItems(ConfigurationItem<?>... items) {
		for (ConfigurationItem<?> configurationItem : items) {
			this.items.put(configurationItem.getShortName(), configurationItem);
		}
	}
	
	public abstract void reset();
}
