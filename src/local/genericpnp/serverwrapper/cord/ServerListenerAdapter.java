package local.genericpnp.serverwrapper.cord;

import local.genericpnp.serverwrapper.MCSWrapper;
import local.genericpnp.serverwrapper.util.Util;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ServerListenerAdapter extends ListenerAdapter {
	private final MCSWrapper main;
	
	public ServerListenerAdapter(MCSWrapper m) {
		this.main = m;
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(event.getChannel().getIdLong() == this.main.channelId) {
			User user;
			if(!(user = event.getAuthor()).isBot()) {
				String raw = event.getMessage().getContentStripped();
				String clean = Util.sanitize(raw, true);
				if(clean.startsWith(main.commandPrefix)) {
					main.sendCommand(clean.substring(1), user.getIdLong());
					return;
				}
				String username = Util.sanitize(user.getEffectiveName(), false);
				String out = username+": "+clean;
				String[] content = Util.splitOnLineLimit(Util.removeColorCodes(out), Util.MAXLEN);
				for (String string : content) {
					this.main.sendMessageToGame(string);
				}
			}
		}
	}
}
