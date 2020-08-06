package app.nush.commands;

import app.nush.Command;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

public class Ping extends Command {
    public Ping(DiscordClient client) {
        super(client);
    }

    @Override
    public String getHelpMessage(String prefix) {
        return "`" + prefix + "ping`\nPing the bot!";
    }

    @Override
    public String getCommand() {
        return "ping";
    }

    @Override
    public void handle(Message message, String[] args) {
        final MessageChannel channel = message.getChannel().block();
        assert channel != null;
        channel.createMessage("Pong!").block();
    }
}
