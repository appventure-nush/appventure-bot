package app.nush;

import app.nush.commands.Ping;
import com.google.gson.Gson;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;

import java.io.InputStreamReader;
import java.util.Arrays;

public class AppventureBot {
    public static void main(String... system_args) {
        final Config config = Config.getConfig();
        final DiscordClient client = DiscordClient.create(config.token);
        final GatewayDiscordClient gateway = client.login().block();

        final Command[] commands = getCommands(client);

        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            final Message message = event.getMessage();
            String content = message.getContent();
            if (!content.startsWith(config.prefix)) return;
            content = content.substring(1);
            String[] split = content.split("\\s+");
            String cmd = split[0];
            String[] args = Arrays.copyOfRange(split, 1, split.length);
            if (!message.getAuthor().isPresent() || message.getAuthor().get().isBot()) return;
            for (Command command : commands) {
                if (command.getCommand().equalsIgnoreCase(cmd)) {
                    command.handle(message, args);
                    break;
                }
            }
        });

        gateway.onDisconnect().block();
    }

    public static Command[] getCommands(DiscordClient client) {
        return new Command[]{
                new Ping(client)
        };
    }

    private static class Config {
        public String token;
        public String prefix;

        public static Config getConfig() {
            return new Gson().fromJson(new InputStreamReader(AppventureBot.class.getResourceAsStream("/config.json")), Config.class);
        }
    }
}
