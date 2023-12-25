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
	private int exitValue = Integer.MIN_VALUE;
	private BufferedReader procInput;
	private PrintWriter procOutput;
	private List<String> processOutQueue = Collections.synchronizedList(new ArrayList<>());
	private MCSWrapper handler;
	private String[] argCache;
	private File workDirCache;
	private long procTimeout = 1000L;
	
	public ServerProcessMonitor(MCSWrapper mcsWrapper) {
		this.handler = mcsWrapper;
	}
	
	public void startProcess(File workDir, String... cmdline) throws IOException {
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
		
		Thread mon = new Thread("Server Process Monitor") {
			@Override
			public void run() {
				try {
					exitValue = proc.waitFor();
				} catch (InterruptedException e) {
				}
				onProcessTerminate(exitValue);
			}
		};
		mon.start();
		
		Thread inhandler = new Thread("Server Input Handler") {
			@Override
			public void run() {
				while(proc != null && proc.isAlive()) {
					try {
						String s = null;
						while ((s = procInput.readLine()) != null) {
							checkUnexpectedException(s);
							String x = MessageFilter.filter(s);
							if (x != null) {
								handler.sendMessageToCord(x);
							}
							if(handler.redirectProcessOut) {
								System.err.println(s);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					try {
						Thread.sleep(2L);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		inhandler.start();
		
		Thread outhandler = new Thread("Server Output Handler") {
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
		outhandler.start();
	}
	
	/**
	 * sends the specified line of text to the process's standard input
	 * @param command the line
	 */
	public void sendCommand(String command) {
		if(this.proc != null && this.proc.isAlive()) {
			this.processOutQueue.add(command + System.lineSeparator());
		}
	}
	
	public void broadcastMessage(String message) {
		this.sendCommand("say [DSC] \247f"+message);
	}
	
	private void onProcessTerminate(int exitValue) {
		if(this.handler.autoRestartServer && this.handler.wantToQuit) {
			this.restart();
		}
	}
	
	public boolean executeCommand(String command) {
		if(command.equals("list")) {
			this.sendCommand("list");
			return true;
		}
		return false;
	}
	
	private void restart() {
		try {
			this.stop();
			
			if(this.proc != null || this.proc.isAlive()) {
				this.stopForcibly(); //try force stop if it's still running for some reason...
			}
			
			this.startProcess(workDirCache, argCache);
			this.handler.sendMessageToCord("The server process was restarted!");
		} catch (Exception e) {
			System.err.println("Failed to restart process");
			throw new RuntimeException(e);
		}
	}
	
	public void stopForcibly() {
		try {
			this.procInput.close();
			this.procOutput.close();
		} catch (Exception e) {
		}
		
		if(this.proc != null && this.proc.isAlive()) {
			this.proc.destroyForcibly();
		}
		
		this.started = false;
		this.proc = null;
	}
	
	public void stop() {
		this.sendCommand("stop");
		try {
			Thread.sleep(this.procTimeout); // wait for process to close, otherwise terminate it manually
		} catch (Exception e) {
		}
		
		try {
			this.procInput.close();
			this.procOutput.close();
		} catch (Exception e) {
		}
		
		if(this.proc != null && this.proc.isAlive()) {
			this.proc.destroy();
		}
		this.started = false;
		this.proc = null;
	}
	
	public int getExitValue() {
		if(this.exitValue == Integer.MIN_VALUE) throw new IllegalStateException("Process not yet terminated!");
		return this.exitValue;
	}
	
	public boolean isStarted() {
		return this.started;
	}
	
	private void checkUnexpectedException(String raw) {
		if(raw.contains("[SEVERE] Unexpected exception")) {
			String[] spl = raw.split(" ");
			if(spl.length >= 5 && spl[2].equals("[SEVERE]") && spl[3].equals("Unexpected") && spl[4].equals("exception")) {
				this.stopForcibly(); //kill it with fire!
			}
		}
	}
	
	public void setProcessTimeout(long timeout) {
		this.procTimeout = timeout;
	}
}
