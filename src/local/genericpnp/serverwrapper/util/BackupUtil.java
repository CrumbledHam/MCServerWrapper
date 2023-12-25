package local.genericpnp.serverwrapper.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.EnumSet;

import local.genericpnp.serverwrapper.MCSWrapper;
import local.genericpnp.serverwrapper.ServerProcessMonitor;

public class BackupUtil {
	/**
	 * this method is asynchronous
	 * @param mon server process monitor
	 * @param cb callback to get notified of success or fail
	 */
	public static void performBackup(ServerProcessMonitor mon, File workDir, BackupCallback cb) {
		new Thread("Backup Thread") {

			@Override
			public void run() {
				try {
					//disable level saving
					mon.sendCommand("save-off");
					sleep(1000L);
					//flush unsaved data
					mon.sendCommand("save-all");
					sleep(1000L);
					//copy files
					File workDirWorld = new File(workDir, "world");
					File backupsDir = new File(workDir, "backups");
					File currentBackupDir = new File(backupsDir, "backup-"+MCSWrapper.BACKUP_DATE_FORMAT.format(new Date()));
					if(!backupsDir.exists()) { 
						if(!backupsDir.mkdir()) throw new IOException("Failed to create backups directory");
					}
					if(!currentBackupDir.exists()) { 
						if(!currentBackupDir.mkdir()) throw new IOException("Failed to create current backup directory");
					}

					Path source = workDirWorld.toPath();
					Path target = currentBackupDir.toPath();

					Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							Path targetdir = target.resolve(source.relativize(dir));
							try {
								Files.copy(dir, targetdir);
							} catch (FileAlreadyExistsException e) {
								if (!Files.isDirectory(targetdir))
									throw e;
							}
							return FileVisitResult.CONTINUE;
						}
						
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.copy(file, target.resolve(source.relativize(file)));
							return FileVisitResult.CONTINUE;
						}
					});
					sleep(1000L);
					
					cb.backupResult(null);
					return;
				} catch (Exception e) {
					cb.backupResult(e);
					return;
				} finally {
					//re-enable level saving
					mon.sendCommand("save-on");
				}
			};

		}.start();
	}
}
