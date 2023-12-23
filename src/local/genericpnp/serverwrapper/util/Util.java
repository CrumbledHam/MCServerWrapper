package local.genericpnp.serverwrapper.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Util {
	public static final String ALLOWED_CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~\u2302ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»";
	private static final int MAX_LENGTH = 1997; //just to be safe, remove 3 from the actual max length
	public static final int MAXLEN = 104; //max game chat length excluding header
	private Util() {} 
	
	private static boolean isInvalidCharacter(char c, String arr) {
		for (int i = 0; i < arr.length(); i++) {
			if(arr.charAt(i) == c) return false;
		}
		return true;
	}
	
	/**
	 * splits a string on length and line breaks
	 * @param input to split
	 * @param splitAfter length limit to split on
	 * @return array of strings split with LF ("\n")
	 */
	public static String[] splitOnLineLimit(String input, int splitAfter) {
		String output = "";
		for(int i = 0; i< input.length(); i++)
		{
		    if(i % splitAfter == 0 && i != 0)
		    {
		        output += "\n";
		    }
		    output += input.charAt(i);
		}
		return output.split("\n");
	}
	
	/**
	 * Gets the stack trace of the provided throwable as a string
	 * @param throwable the provided throwable
	 * @return stack trace as a string (limited to (max content length) characters)
	 */
	public static String getStackTraceAsString(Throwable throwable) {
		String output ="Unexpected throwable parsing command: ";
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
		output += sw.toString();
		if(output.length() > MAX_LENGTH) {
			 output.substring(0, MAX_LENGTH);
			 return output;
		}
		return output;
	}
	
	/**
	 * removes the game color codes from the specified string
	 * @param string the string to remove color codes from
	 * @return a string without color codes
	 */
	public static String removeColorCodes(String string) {
		char[] charArr = string.toCharArray();
		String out = "";
		for (int i = 0; i < charArr.length; i++) {
			if(charArr[i] == '\247') {
				i++;
			} else {
				out += charArr[i];
			}
		}
		return out;
	}
	
	/** Sanitizes a String from non-ASCII characters (Unicode remover)
	 * 
	 * @param input input string to sanitize
	 * @return a String in ASCII only format
	 * */
	public static void sanitize(String input) {
		sanitize(input, false);
	}
	
	/** Sanitizes a String from non-ASCII characters (Unicode remover)
	 * 
	 * @param input input string to sanitize
	 * @param fixLF set to false if you don't want to keep LF ('\n')
	 * @return a String in ASCII only format
	 * 
	 * */
	public static String sanitize(String input, boolean fixLF) {
		char[] charArr = input.toCharArray();
		String out = "";
		for (int i = 0; i < charArr.length; i++) {
			if(isInvalidCharacter(charArr[i], ALLOWED_CHARS)) {
				if(fixLF && charArr[i] == '\n') {
					out += '\n';
				} else {
					out += '?';
				}
			} else {
				out += charArr[i];
			}
		}
		return out;
	}
}
