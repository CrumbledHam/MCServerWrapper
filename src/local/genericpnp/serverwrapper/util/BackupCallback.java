package local.genericpnp.serverwrapper.util;

public interface BackupCallback {
	/**
	 * called on successful or failed world backup attempt
	 * @param e will be null if nothing failed
	 */
	void backupResult(Exception e);
}
