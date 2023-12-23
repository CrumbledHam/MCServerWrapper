package local.genericpnp.serverwrapper.util;

public interface MessageFilterCallback { //probably will need this for handling commands or something...
	void invoke(String command_result);
}
