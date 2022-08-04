package ru.mrflaxe.textadventure.update.handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.quest.QuestSessionManager;
import ru.mrflaxe.textadventure.update.UpdateProvider;
import ru.mrflaxe.textadventure.user.User;
import ru.mrflaxe.textadventure.user.UserProvider;

public class ExitHandler extends MessageHandler {

    private final QuestSessionManager sessionManager;
    private final UserProvider userProvider;
    
    public ExitHandler(TelegramBot bot, Configuration messages, UpdateProvider updateProvider, QuestSessionManager sessionManager, UserProvider userProvider) {
        super(bot, messages, updateProvider);
        
        this.sessionManager = sessionManager;
        this.userProvider = userProvider;
    }

    @Override
    public void handle(Update update) {
        long chatID = update.message().chat().id();
        User user = userProvider.getUser(chatID);
        
        sessionManager.closeSession(user);
        
        updateProvider.returnToMainMenu(user);
    }

}
