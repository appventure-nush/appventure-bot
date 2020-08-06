package app.nush;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;

public abstract class Command {
    protected DiscordClient client;

    public Command(DiscordClient client) {
        this.client = client;
    }

    public abstract String getHelpMessage(String prefix);

    public abstract String getCommand();

    public abstract void handle(Message message, String[] args);
}