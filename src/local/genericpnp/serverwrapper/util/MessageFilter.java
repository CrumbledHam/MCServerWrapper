package local.genericpnp.serverwrapper.util;

/**
 * I'm very sorry that this has to exist but i suck at regular expressions
 */
public class MessageFilter {
	
	private static String getDoneMessage(String raw) {
		if(raw.contains("Done") && raw.contains("For help, type")) {
			String[] chunks = raw.split(" ");
			if(chunks[3].equals("Done") && chunks[5].equals("For")) {
				return "Server has started!";
			}
			
		}
		return null;
	}
	
	private static String getStopMessage(String raw) {
		if(raw.contains("Stopping server")) {
			return "Server has stopped!";
		}
		return null;
	}
	
	/**
	 * Gets the join message from the console log
	 * @param rawLogOutput the raw console log output
	 * @return the join message as user_name + space + joined the game.
	 */
	private static String getJoinMessage(String rawLogOutput) {
		if(rawLogOutput.contains("logged in with")) {
			String[] chunks = rawLogOutput.split(" ");
			if(chunks.length < 5) {
				chunks = null;
				return null;
			}
//			String date = chunks[0];
//			String time = chunks[1];
//			String logLevel = chunks[2];
			String user = chunks[3];
//			String ip = chunks[4];
			return user+" joined the game.";
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the disconnect message from the console log
	 * @param rawLogOutput
	 * @return the disconnect message as user_name + space + left the game.
	 */
	private static String getLeaveMessage(String rawLogOutput) {
		if(rawLogOutput.contains("lost connection")) {
			String[] chunks = rawLogOutput.split(" ");
			if(chunks.length < 5) {
				chunks = null;
				return null;
			}
			String userOrIp = chunks[3];
			if(isIP(userOrIp)) {
				return null;
			}
			
			return userOrIp+" left the game.";
			
		} 
//		else if (rawLogOutput.contains("")) { //TODO: fly kick error message disconnect case...
//			
//		} 
		else {
			return null;
		}
	}
	
	/**
	 * Alternate version of getMessage(String)
	 * @param raw raw
	 * @return shit
	 */
	private static String getMessage_alt(String raw) { //i have no way to test this at the moment...
		String[] msgr = raw.split(" ");
		//0 - date
		//1 - time
		//2 - [level]
		//3 - user:
		//4-end - msgs
		if(msgr.length >= 3 && msgr[2].equals("[INFO]") && msgr[3].indexOf(':') != -1) {
			int i = raw.indexOf(msgr[2]);
			int msgp = raw.indexOf(msgr[4]);// user
			if(i != -1 && msgp != -1) {
				String substr = raw.substring(i);
				return substr; //what could go wrong...
			}
		}
		return null;
	}
	
	/**
	 * finds a chat message from the latest log line
	 * @param raw the raw console log output
	 * @return the message as user: message raw string
	 */
	private static String getMessage(final String raw) {
		//i hate regex!
		int first = raw.indexOf('<');
		int second = raw.indexOf('>');
		if(first != -1 && second != -1) {
			String split[] = raw.split(" ");
			if(split.length == 0 || split.length < 4)  {
				split = null;
				return null;
			}
			String user = split[3];
			split = null;
			
			int start = user.indexOf('<') + 1;
			int end = user.indexOf('>');
			int k = end - start;
			if(k < 0) {
				return null;
			}
			user = user.substring(user.indexOf('<') + 1, user.indexOf('>'));
			
			String message  = raw.substring(second + 2);
			return user+": "+message;
		} else {
			return getMessage_alt(raw);
		}
		//return null;
	}
	
	private static String getConsoleMessage(String raw) {
		String split[] = raw.split(" "); //can't filter for [ or ] because logging levels
		//0  -date
		//1  -time
		//2  -logging_level
		//3  -sender
		//4-end -message as chunks split with space
		
		//in case of funny prefix
		//0 date
		//1 time
		//2 loggingLevel
		//3 sender
		//4 prefix
		//5-end message
		
		if(split.length < 4) {
			return null;
		} 
		
//		String user;
		
		if(!(split[2].startsWith("[") && split[2].endsWith("]") && split[3].startsWith("[") && split[3].endsWith("]"))) {
			return null;
		}
		//if cord message in case it has [DSC] as a prefix
		if((split[2].startsWith("[") && split[2].endsWith("]") && split[3].startsWith("[") && split[3].endsWith("]") && split[4].startsWith("[") && split[4].endsWith("]"))) {
			return null;
		}
		
		//if(user.equals("CONSOLE")) return null;
		
		StringBuilder sb = new StringBuilder();
		sb.append("[Server]");
		sb.append(' ');
		for (int i = 4; i < split.length; i++) {
			sb.append(split[i]);
			sb.append(' ');
		}
		sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}
	
	private static String getCommandResultPlayers(String line) {
		if(line.contains("Connected players:")) {
			String[] cat = line.split(" ");
			if(cat.length < 3) return null;
			String lvl = cat[2];
			String a = cat[3];
			String m = cat[4];
			if(!(lvl.equals("[INFO]") && a.equals("Connected") && m.equals("players:"))) {
				return null;
			} else {
				String substr = line.substring(line.indexOf("Connected players:"));
				return substr;
			}
		}
		return null;
	}
	
	/**
	 * TODO: this is not IPv6 compatible as if anyone even cared about that
	 * @param ipOrUser the IP or user name
	 * @return true if the specified string is an IP as per MC server spec
	 */
	private static boolean isIP(String ipOrUser) {
		if(ipOrUser.contains("/")) {
			int idx = ipOrUser.indexOf('/');
			if (idx == -1) { 
				return false; 
			} else {
				String chunks[] = ipOrUser.split("\\.");
				if(chunks.length > 4) return false;
				String c0 = chunks[0];
				c0 = c0.split("/")[1]; //remove slash
				String c1 = chunks[1];
				String c2 = chunks[2];
				String c3 = chunks[3];
				c3 = c3.split(":")[0]; //remove port
				return isInt(c0) && isInt(c1) && isInt(c2) && isInt(c3);
			}
		}
		return false;
	}
	
	private static boolean isInt(String num) {
		int x = Integer.MIN_VALUE;
		try {
			x = Integer.parseInt(num);
		} catch (Exception e) {
		}
		return x != Integer.MIN_VALUE;
	}
	
	/**
	 * filters the specified input line
	 * @param line the line of console output
	 * @param callback the MessageFilterCallback object to be notified of the command one
	 * @return null if the message didn't match criteria, otherwise the filtered message
	 */
	public static String filter(String line) {
		String msgp = getJoinMessage(line);
		if(msgp != null) {
			return msgp;
		}
		msgp = getMessage(line);
		if(msgp != null) {
			return msgp;
		}
		msgp = getLeaveMessage(line);
		if(msgp != null) {
			return msgp;
		}
		msgp = getDoneMessage(line);
		if(msgp != null) {
			return msgp;
		}
		msgp = getStopMessage(line);
		if(msgp != null) {
			return msgp;
		}
		msgp = getCommandResultPlayers(line);
		if(msgp != null) {
			return msgp;
		}
		//getConsoleMessage should be called last because it does not have string.contains() checking, which may cause some performance loss
		msgp = getConsoleMessage(line);
		if(msgp != null) {
			return msgp;
		}
		return null;
	}
}
