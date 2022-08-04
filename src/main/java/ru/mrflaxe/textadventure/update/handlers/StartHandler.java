package ru.mrflaxe.textadventure.update.handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.update.UpdateProvider;
import ru.mrflaxe.textadventure.user.User;
import ru.mrflaxe.textadventure.user.UserProvider;

public class StartHandler extends MessageHandler {
    
    private final UserProvider userProvider;
    
    public StartHandler(TelegramBot bot, Configuration messages, UpdateProvider updateProvider, UserProvider userProvider) {
        super(bot, messages, updateProvider);
        
        this.userProvider = userProvider;
    }
    
    @Override
    public void handle(Update update) {
        Long chatID = update.message().chat().id();
        String name = update.message().chat().firstName();
        User user = userProvider.getUser(chatID);
        
        updateProvider.returnToMainMenu(user);
        
        String text = messages.getString("welcome");
        
        if(text == null || text.isEmpty()) {
            text = "Default_message";
        }
        
        text = text.replace("%name%", name);
        
        SendMessage request = new SendMessage(chatID, text);
        request.parseMode(ParseMode.HTML);
        
        bot.execute(request);
    }
}
