package local.genericpnp.serverwrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import local.genericpnp.serverwrapper.cord.ServerListenerAdapter;
import local.genericpnp.serverwrapper.util.BackupUtil;
import local.genericpnp.serverwrapper.util.BackupCallback;
import local.genericpnp.serverwrapper.util.PropertiesConfig;
import local.genericpnp.serverwrapper.util.Util;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class MCSWrapper implements Runnable, BackupCallback {
	public static final SimpleDateFormat BACKUP_DATE_FORMAT = new SimpleDateFormat("ddMMyyyy-hhmmss");
	private ServerProcessMonitor procMon = new ServerProcessMonitor(this);
	public boolean processStarted = false;
	private PropertiesConfig config;
	private int maxmem;
	private int minmem;
	private String javaExecutablePath;
	private String jarPath;
	private boolean serverNoGui = true;
	private boolean overrideWorkingDir;
	private String workingDirString;
	private File workDir;
	public long channelId;
	private String token;
	public boolean autoRestartServer;
	public boolean wantToQuit = false;
	public JDA api;
	public boolean redirectProcessOut;
	public String commandPrefix;
	public String updateUrl;
	public boolean autoUpdate;
	public long updateCheckIntervalMS;
	public boolean manualUpdates;
	public String javaagent;
	/**
	 * Set this to true in the properties to indicate the server will terminate on an unexpected exception,
	 * for a regular server it is recommended to leave this set to false. However, 
	 * if the server has been modified to quit on an unexpected exception, this can be set to true
	 */
	public boolean serverQuitsAfterCrashing;
	private long adminUserId;
	public boolean debug;
	
	private void loadConfig() {
		System.out.println("Loading config...");
		this.config = new PropertiesConfig(new File(this.workDir, "mcswrapper.properties"), "MC Server wrapper properties");
		this.maxmem = this.config.get("maxMem", 4096);
		this.minmem = this.config.get("minMem", 4096);
		this.javaExecutablePath = this.config.get("javaExecutablePath", "java");
		this.javaagent = this.config.get("javaagent", "none")
		this.overrideWorkingDir = this.config.get("overrideWorkDir", false);
		if(this.overrideWorkingDir) {
			this.workingDirString = this.config.get("overrideWorkDirPath", ".");
		} else {
			this.workingDirString = System.getProperty("user.dir", ".");
		}
		this.workDir = new File(this.workingDirString);
		this.jarPath = this.config.get("serverJarPath", "server.jar");
		this.channelId = this.config.get("channelId", 0L);
		this.token = this.config.get("token", "REPLACE_WITH_ACTUAL_BOT_TOKEN");
		this.autoRestartServer = this.config.get("autoRestartServer", true);
		this.redirectProcessOut = this.config.get("redirectProcessOutput", true);
		this.commandPrefix = this.config.get("commandPrefix", ";");
		this.procMon.setProcessQuitTimeout(this.config.get("procesTimeoutMillis", 4000L));
		this.autoUpdate = this.config.get("doAutoUpdates", false);
		this.manualUpdates = this.config.get("enableManualUpdates", true);
		this.updateUrl = this.config.get("updateUrl", "empty");
		this.updateCheckIntervalMS = this.config.get("updateCheckIntervalMillis", 360000L);
		this.serverQuitsAfterCrashing = this.config.get("serverQuitsAfterCrashing", false);
		this.adminUserId = this.config.get("adminUserId", -1L);
		this.debug = this.config.get("debugMode", false);
		System.out.println("Finished loading config.");
	}
	
	private boolean initJDA() {
		try {
			this.api = JDABuilder.createLight(token).enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT).build();
			this.api.addEventListener(new ServerListenerAdapter(this));
			this.api.awaitReady();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private void startProcess() throws Exception {
		this.procMon.startProcess(this.workDir, this.javaExecutablePath, "-Xmx"+maxmem+"M", "-Xms"+minmem+"M", "-jar", this.jarPath, "nogui");
		processStarted = true;
	}
	
	
	@Override
	public void run() {
		this.loadConfig();
		
		System.out.println("Initializing JDA...");
		if(this.initJDA()) {
			System.out.println("JDA initialized.");
		} else {
			System.err.println("Failed to initialize JDA, make sure the token and channel id are correct!");
			return;
		}
		
		System.out.println("Starting server process...");
		try {
			this.startProcess();
		} catch (Exception e) {
			System.err.println("Failed to start server process!");
			e.printStackTrace();
			return;
		}
				
		try {
	         BufferedReader eee = new BufferedReader(new InputStreamReader(System.in));
	         String x = null;

	         while((x = eee.readLine()) != null) { //temp thing for testing functions, replace with a better command system later
	        	 if(x.equals("quit") || x.equals("exit")) {
	        		 	System.out.println("Quitting!");
						this.wantToQuit = true;
						this.procMon.stop();
						this.api.shutdown();
						System.in.close();
					} else if (x.equals("restart")) {
						this.procMon.stop();
					} else if (x.startsWith("cmd")) {
						String c = x.substring(x.indexOf(' ') + 1);
						this.procMon.sendCommand(c);
					} else if(x.startsWith("msg")) {
						String c = x.substring(x.indexOf(' ') + 1);
						this.procMon.broadcastMessageToAll(c);
					} else if (x.equals("help")) {
						System.out.println("Valid commands:");
						System.out.println("help	shows this message");
						System.out.println("quit	closes the program and the server gracefully");
						System.out.println("exit	alias for 'quit'");
						System.out.println("restart	restarts the server");
						System.out.println("msg	<message>	broadcasts the specified message to everyone");
						System.out.println("cmd	<command>	sends the command as a minecraft server command");
					} else {
						System.out.println("Invalid command!");
					}
	        	 if(this.wantToQuit) break;
	         }

	         System.err.println("No more direct console input is possible.");
	         return;
	      } catch (IOException e) {
	    	  System.err.println(e+": No more direct console input is possible.");
	    	  return;
	      } finally {
	    	  System.exit(0);
	      }
	}
	
	public void sendCommand(String command, long userId) {
		if (userId == this.adminUserId && command.equals("performbackup")) {
			sendMessageToCord("Starting backup on server...");
			BackupUtil.performBackup(procMon, workDir, this);
			return;
		}
		if(!this.procMon.executeCommand(command)) {
			this.sendMessageToCord("Invalid command.");
		}
	}
	
	public void sendMessageToGame(String message) {
		this.procMon.broadcastMessageFromCord(message);
	}
	
	public void sendMessageToCord(String message) {
		if(!this.wantToQuit) {
			try {
				this.api.awaitReady();
				this.api.getTextChannelById(channelId).sendMessage(message).queue();
			} catch (Exception e) {
				System.err.println("Failed to send message:");
				e.printStackTrace();
			}	
		}
	}
	
	public static void main(String[] args) {
		MCSWrapper w = new MCSWrapper();
		Thread thread = new Thread(w, "Main thread");
		thread.start();
	}

	@Override
	public void backupResult(Exception e) {
		if(e == null) {
			sendMessageToCord("Level backup succeeded.");
		} else {
			System.err.println("Server level backup operation failed:");
			e.printStackTrace();
			String mesg;
			if(this.debug) {
				mesg = Util.getStackTraceAsString("Level backup failed: ", e);
			} else {
				mesg = "Level backup failed. See server console for details";
			}
			sendMessageToCord(mesg);
		}
	}
}
