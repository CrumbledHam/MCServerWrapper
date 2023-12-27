package local.genericpnp.serverwrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import local.genericpnp.serverwrapper.util.MessageFilter;

/**
 *Monitors the state of the MinecraftServer process. 
 *TODO: add a watchDog timer to prevent freeze bug, i,e if server doesn't respond to command in an expected time span, kill it and restart it
 */
public class ServerProcessMonitor {
	private Process proc;
	private boolean started = false;
	private BufferedReader procInput;
	private PrintWriter procOutput;
	private List<String> processOutQueue = Collections.synchronizedList(new ArrayList<>());
	private MCSWrapper handler;
	private String[] argCache;
	private File workDirCache;
	private long procTimeout = 1000L;
	
	/**
	 * Creates an instance of this class
	 * @param mcsWrapper the parent object
	 */
	public ServerProcessMonitor(MCSWrapper mcsWrapper) {
		this.handler = mcsWrapper;
	}
	
	/**
	 * Starts the process with the specified arguments
	 * @param workDir the process' working directory
	 * @param cmdline the process' command line
	 * @throws IOException if the process fails to start as per {@linkplain java.lang.ProcessBuilder#start()} or argument array being empty
	 */
	public void startProcess(File workDir, String... cmdline) throws IOException {
		if(cmdline.length == 0) {
			throw new IOException("Argument count must be greater than 0!");
		}
		if(argCache == null) {
			argCache = cmdline;
		}
		if(workDirCache == null) {
			workDirCache = workDir;
		}
		ProcessBuilder pb = new ProcessBuilder(cmdline);
		pb.directory(workDir);
		pb.redirectErrorStream(true);
		proc = pb.start();
		this.procInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		this.procOutput = new PrintWriter(new OutputStreamWriter(proc.getOutputStream()));
		this.started = true;
		
		Thread monitorThread = new Thread("Server Process Monitor") {
			@Override
			public void run() {
				try {
					proc.waitFor();
				} catch (InterruptedException e) {
				}
				if(!proc.isAlive()) {
					onProcessTerminate();
				}
			}
		};
		monitorThread.start();
		
		Thread inputThread = new Thread("Server Input Handler") {
			@Override
			public void run() {
				try {
					String rawLine = null;
					while((proc != null && proc.isAlive()) && (rawLine = procInput.readLine()) != null) {
						checkUnexpectedException(rawLine);
						String filtered = MessageFilter.filter(rawLine);
						if (filtered != null) {
							handler.sendMessageToCord(filtered);
						}
						if(handler.redirectProcessOut) {
							System.err.println(rawLine);
						}
						Thread.sleep(2L);
					}
				} catch (Exception e) {
				}
			}
		};
		inputThread.start();
		
		Thread outputThread = new Thread("Server Output Handler") {
			@Override
			public void run() {
				while(proc != null && proc.isAlive()) {
					if(!processOutQueue.isEmpty()) {
						procOutput.print(processOutQueue.remove(0));
						procOutput.flush();
					}
					try {
						Thread.sleep(2L);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		outputThread.start();
	}
	
	private void onProcessTerminate() {
		if(this.handler.autoRestartServer && !this.handler.wantToQuit) {
			this.restart();
		}
	}
	
	private void restart() {
		try {
			this.stop();
			this.handler.sendMessageToCord("The server is being restarted...");
			this.startProcess(workDirCache, argCache);
		} catch (Exception e) {
			System.err.println("Failed to restart process");
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Checks the raw log output for the "Unexpected exception"-message since the server by default cannot restart itself on an exception
	 * @param raw the raw line of log output
	 */
	private void checkUnexpectedException(String raw) {
		if(!this.handler.serverQuitsAfterCrashing) {
			if(raw.contains("[SEVERE] Unexpected exception")) {
				String[] spl = raw.split(" ");
				if(spl.length >= 5 && spl[2].equals("[SEVERE]") && spl[3].equals("Unexpected") && spl[4].equals("exception")) {
					this.stop(); //kill it with fire!
				}
			}
		}
	}
	
	/**
	 * Sends the specified line of text to the process's standard input 
	 * (line feed is added automatically to press the virtual enter key on the terminal)
	 * @param command the line
	 */
	public void sendCommand(String command) {
		if(this.proc != null && this.proc.isAlive()) {
			this.processOutQueue.add(command + System.lineSeparator());
		}
	}
	
	/**
	 * Sends the specified message to the console and shows it for all players
	 * @param message the message
	 */
	public void broadcastMessageFromCord(String message) {
		this.sendCommand("say [DSC] \247f"+message);
	}
	
	/**
	 * Sends the specified message to the console and shows it for all players
	 * @param message the message
	 */
	public void broadcastMessageToAll(String message) {
		this.sendCommand("say "+message);
	}
	
	/**
	 * executes the command (this is to prevent unauthorized user input)
	 * @param command
	 * @return true if succeeded
	 */
	public boolean executeCommand(String command) {
		if(command.equals("list")) {
			this.sendCommand("list");
			return true;
		}
		return false;
	}
	
	/**
	 * Shuts down the process and releases any resources
	 */
	public void stop() {
		this.sendCommand("stop");
		try {
			Thread.sleep(this.procTimeout); // wait for process to close if it is listening to console input, otherwise terminate it manually
		} catch (Exception e) {
		}
		
		if(this.proc != null && this.proc.isAlive()) {
			this.proc.destroyForcibly();
		}
		this.started = false;
		
		this.proc = null;
		
		try {
			this.procInput.close();
			this.procOutput.close();
		} catch (Exception e) {
		}
		this.procInput = null;
		this.procOutput = null;
	}
	
	/**
	 * Gets the exit value of the process
	 * @return the exit value of the process
	 * @exception IllegalThreadStateException if process is not yet started or has not yet terminated
	 */
	public int getExitValue() {
		if(this.proc == null) {
			throw new IllegalThreadStateException("Process not yet started");
		}
		return this.proc.exitValue();
	}
	
	/**
	 * Checks if the process has been started
	 * @return true if the process has been started
	 */
	public boolean started() {
		return this.started;
	}
	
	/**
	 * Sets the process quit timeout to the specified amount in milliseconds
	 * @param timeout timeout in milliseconds
	 */
	public void setProcessQuitTimeout(long timeout) {
		this.procTimeout = timeout;
	}
}
