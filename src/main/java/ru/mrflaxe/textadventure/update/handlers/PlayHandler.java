package ru.mrflaxe.textadventure.update.handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.quest.QuestSessionManager;
import ru.mrflaxe.textadventure.update.UpdateProvider;
import ru.mrflaxe.textadventure.user.User;
import ru.mrflaxe.textadventure.user.UserProvider;

public class PlayHandler extends MessageHandler {

    private final UserProvider userProvider;
    private final QuestSessionManager questSessions;
    
    public PlayHandler(
            TelegramBot bot,
            Configuration messages,
            UpdateProvider updateProvider,
            UserProvider userProvider,
            QuestSessionManager questSessions
            ) {
        super(bot, messages, updateProvider);
        
        this.userProvider = userProvider;
        this.questSessions = questSessions;
    }

    @Override
    public void handle(Update update) {
        long chatID = update.message().chat().id();
        User user = userProvider.getUser(chatID);
        
        if(questSessions.hasSession(user)) {
            return;
        }
        
        questSessions.openSession(user);
    }

}
