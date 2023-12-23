package local.genericpnp.serverwrapper.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class PropertiesConfig {
	private Properties conf = new Properties();
	private File confFile;
	private String comments;
	private int createFailCount;

	public PropertiesConfig(File configFile) {
		this(configFile, "Properties File");
	}

	public PropertiesConfig(File configfile, String comments) {
		this.confFile = configfile;
		this.comments = comments;
		this.load();
	}

	private void load() {
		if(confFile.exists()) {
			try {
				this.conf.load(new FileInputStream(confFile));
			} catch (Exception e) {
				System.err.println("Failed to create "+confFile);
				this.recreate();
			}
		}
	}

	public void recreate() {
		this.createFailCount++;
		if(this.createFailCount < 5) throw new RuntimeException("Failed to create config!");
		System.err.println("Creating new config");
		this.save();
	}

	public void save() {
		try {
			this.conf.store(new FileOutputStream(confFile), comments);
		} catch (Exception e) {
			System.err.println("Failed to save config");
			e.printStackTrace();
			this.recreate();
		}
	}

	public String setComments(String comments) {
		return this.comments = comments;
	}

	public String get(String key, String defaultValue) {
		if(!this.conf.containsKey(key)) {
			this.conf.setProperty(key, defaultValue);
			this.save();
		}
		return this.conf.getProperty(key, defaultValue);
	}

	public int get(String key, int defaultValue) {
		try {
			return Integer.parseInt(this.get(key, String.valueOf(defaultValue)));
		} catch (Exception exception) {
			this.conf.setProperty(key, String.valueOf(defaultValue));
			return defaultValue;
		}
	}

	public boolean get(String key, boolean defaultValue) {
		try {
			return Boolean.parseBoolean(this.get(key, String.valueOf(defaultValue)));
		} catch (Exception exception) {
			this.conf.setProperty(key, String.valueOf(defaultValue));
			return defaultValue;
		}
	}

	public long get(String key, long defaultValue) {
	    try {
	      return Long.parseLong(get(key, String.valueOf(defaultValue)));
	    } catch (Exception exception) {
	      this.conf.setProperty(key, String.valueOf(defaultValue));
	      return defaultValue;
	    } 
	  }

	public void reload() {
		this.load();
	}
}
