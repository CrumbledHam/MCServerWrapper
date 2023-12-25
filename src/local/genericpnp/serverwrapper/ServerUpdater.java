package local.genericpnp.serverwrapper;
/**
 * ServerUpdater goals: check for updates every 2 days or something (cache the hash of the previous JAR)
 * Download the latest jar and check if the hash is different, if it is, restart the server with the new jar
 * TODO implement serverUpdater
 * */
public class ServerUpdater {
	@SuppressWarnings("unused")
	private MCSWrapper handle;

	public ServerUpdater(MCSWrapper main) {
		this.handle = main;
	}
	
	public void checkupdates() {
		
	}
	
	public void autoUpdate() {}
	
	public void manualUpdate() {}
	
	public void stopUpdateChecking() {}
	
}
