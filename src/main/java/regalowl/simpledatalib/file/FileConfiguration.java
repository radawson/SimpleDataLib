package regalowl.simpledatalib.file;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import regalowl.simpledatalib.SimpleDataLib;
import regalowl.simpledatalib.events.LogEvent;
import regalowl.simpledatalib.events.LogLevel;

public class FileConfiguration {
	
	private SimpleDataLib sdl;
	private ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();
	private Yaml yaml;
	private File file;
	private Path filePath;
	private boolean broken;
	
    public FileConfiguration(SimpleDataLib sdl, File file) {
    	this.sdl = sdl;
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        LoaderOptions loaderOptions = new LoaderOptions();
        yaml = new Yaml(new SafeConstructor(loaderOptions), new Representer(options), options);
        this.file = file;
        this.filePath = file.toPath();
        broken = false;
    }
    
    
    /**
     * For testing only
     */
    public FileConfiguration(SimpleDataLib sdl) {
    	this.sdl = sdl;
        broken = false;
    }
    
	@SuppressWarnings("unchecked")
	public void load() {
		try {
			if (!Files.exists(filePath)) {
				return;
			}
			Object input = yaml.load(Files.newBufferedReader(filePath, StandardCharsets.UTF_8));
			if (input != null) {
				HashMap<String, Object> iData = (HashMap<String, Object>) input;
				data.putAll(iData);
			}
		} catch (Exception e) {
			broken = true;
			sdl.getEventPublisher().fireEvent(new LogEvent("", e, LogLevel.ERROR));
		}
	}

	public void save() {
		if (broken) return;
		try {
			Files.createDirectories(filePath.getParent());
			HashMap<String, Object> oData = new HashMap<>();
			oData.putAll(data);
			try (var writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
				yaml.dump(oData, writer);
			}
		} catch (Exception e) {
			sdl.getEventPublisher().fireEvent(new LogEvent("", e, LogLevel.ERROR));
		}
	}
	
	public Set<String> getKeys() {
		//TODO
		return null;
	}
	
	
	public Set<String> getTopLevelKeys() {
		return data.keySet();
	}
	
	public String getName() {
		return file.getName();
	}
	
	@SuppressWarnings("unchecked")
	private Object getData(String key) {
		if (key == null || key.equals("")) return null;
		if (key.contains(".")) {
			String[] keys = key.split(Pattern.quote("."));
			if (!data.containsKey(keys[0])) return null;
			HashMap<String, Object> map = (HashMap<String, Object>) data.get(keys[0]);
			Object o = null;
			for (int i = 1; i < keys.length; i++) {
				o = map.get(keys[i]);
				if (o instanceof HashMap) {
					map = (HashMap<String, Object>)o;
				}
			}
			return o;
		} else {
			return data.get(key);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void set(String key, Object value) {
		if (key == null || key.equals("")) return;
		if (key.contains(".")) {
			String[] keys = key.split(Pattern.quote("."));
			if (!data.containsKey(keys[0])) data.put(keys[0], new HashMap<String, Object>());
			HashMap<String, Object> map = (HashMap<String, Object>) data.get(keys[0]);
			int depth = keys.length - 1;
			int cDepth = 1;
			Object o = null;
			while (cDepth < depth) {
				o = map.get(keys[cDepth]);
				if (o == null) {
					map.put(keys[cDepth], new HashMap<String, Object>());
					o = map.get(keys[cDepth]);
				}
				if (o instanceof HashMap) {
					map = (HashMap<String, Object>)o;
				}
				cDepth++;
			}
			if (value == null) {
				map.remove(keys[keys.length-1]);
			} else {
				map.put(keys[keys.length-1], value);
			}
		} else {
			if (value == null) {
				data.remove(key);
			} else {
				data.put(key, value);
			}
		}
	}

	public void setDefault(String key, Object value) {
		if (!isSet(key)) set(key, value);
	}
	
	public boolean isSet(String key) {
		if (getData(key) == null) return false;
		return true;
	}
	
	
	
	public String getString(String path) {
		Object o = getData(path);
		if (o instanceof String) {
			return (String) o;
		}
		return null;
	}
	
	public int getInt(String path) {
		return convertInteger(getData(path));
	}
	
	public long getLong(String path) {
		return convertLong(getData(path));
	}
	
	public double getDouble(String path) {
		return convertDouble(getData(path));
	}
	
	public boolean getBoolean(String path) {
		Object o = getData(path);
		if (o == null) {
			return false;
		} else if (o instanceof Boolean) {
			return (Boolean)o;
		} else {
			return false;
		}
	}
	
	
	private int convertInteger(Object o) {
		if (o == null) {
			return 0;
		} else if (o instanceof Byte) {
			return (int)(Byte)o;
		} else if (o instanceof Integer) {
			return (Integer)o;
		} else if (o instanceof Double) {
			return (int)(double)(Double)o;
		} else if (o instanceof Float) {
			return (int)(float)(Float)o;
		} else if (o instanceof Long) {
			return (int)(long)(Long)o;
		} else {
			return 0;
		}
	}

	private double convertDouble(Object o) {
		if (o == null) {
			return 0;
		} else if (o instanceof Float) {
			return (double)(Float)o;
		} else if (o instanceof Double) {
			return (Double)o;
		} else if (o instanceof Byte) {
			return (double)(Byte)o;
		} else if (o instanceof Integer) {
			return (double)(Integer)o;
		} else if (o instanceof Long) {
			return (double)(Long)o;
		} else {
			return 0;
		}
	}
	
	private long convertLong(Object o) {
		if (o == null) {
			return 0;
		} else if (o instanceof Float) {
			return (long)(float)(Float)o;
		} else if (o instanceof Double) {
			return (long)(double)(Double)o;
		} else if (o instanceof Byte) {
			return (long)(Byte)o;
		} else if (o instanceof Integer) {
			return (long)(Integer)o;
		} else if (o instanceof Long) {
			return (long)(Long)o;
		} else {
			return 0;
		}
	}

}
