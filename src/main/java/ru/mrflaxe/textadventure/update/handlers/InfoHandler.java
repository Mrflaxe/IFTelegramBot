package ru.mrflaxe.textadventure.update.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.configuration.ConfigurationSection;
import ru.mrflaxe.textadventure.update.UpdateProvider;

public class InfoHandler extends MessageHandler {
    
    public InfoHandler(TelegramBot bot, Configuration messages, UpdateProvider updateProvider) {
        super(bot, messages, updateProvider);
    }

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        String message = buildInfoMessage();
        
        SendMessage request = new SendMessage(chatId, message);
        request.parseMode(ParseMode.HTML);
        request.disableWebPagePreview(true);
        
        bot.execute(request);
    }

    private String buildInfoMessage() {
        String author = messages.getString("info.author", true);
        String github = messages.getString("info.github", true);
        String head = messages.getString("info.head", true);
        String pattern = messages.getString("info.pattern", true);
        
        List<String> commands = new ArrayList<>();
        Map<String, ConfigurationSection> subsections = messages.getAllSubsections("info.commands");
        
        subsections.entrySet().stream()
            .forEach(set -> {
                String command = set.getKey();
                String description = set.getValue().getString(true);
                
                String commandInfo = pattern.replace("%command%", "/" + command);
                commandInfo = commandInfo.replace("%description%", description);
                
                commands.add(commandInfo);
            });
        
        String result = author + "\n" +
                github + "\n"
                + "\n"
                + head + "\n";
        
        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i);
            
            result = result + command + "\n";
        }
        
        return result;
    }
}
