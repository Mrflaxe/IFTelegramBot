package ru.mrflaxe.textadventure;

import java.util.Set;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;

import ru.mrflaxe.textadventure.achievement.AchievmentManager;
import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.quest.QuestSessionManager;
import ru.mrflaxe.textadventure.update.UpdateProvider;
import ru.mrflaxe.textadventure.user.User;
import ru.mrflaxe.textadventure.user.UserProvider;

public class MyBot {
    
    private final TelegramBot myBot;
    private final Configuration config;
    private final Configuration messages;
    private final DatabaseManager databaseManager;
    private final UserProvider userProvider;
    private final AchievmentManager achievmentManager;
    private final UpdateProvider updateProvider;
    private final QuestSessionManager sessionManager;
    
    public MyBot(Configuration messages, Configuration config, Configuration achievments, DatabaseManager databaseManager) {
        String token = config.getString("bot-token");
        
        this.myBot = new TelegramBot(token);
        this.config = config;
        this.messages = messages;
        this.databaseManager = databaseManager;
        this.userProvider = new UserProvider(databaseManager);
        this.achievmentManager = new AchievmentManager(achievments, databaseManager);
        this.updateProvider = new UpdateProvider(myBot, config, messages, databaseManager, achievmentManager, userProvider);
        this.sessionManager = updateProvider.getQuestSessions();
    }
    
    public void launch() {
        registerUpdateListener();
    }
    
    private void registerUpdateListener() {
        myBot.setUpdatesListener(updateProvider, new GetUpdates());
    }
    
    public void disable() {
        Set<User> activeUsers = sessionManager.getActivePlayers();
        String notify = messages.getString("on-disable");
        
        activeUsers.forEach(user -> {
            long chatID = user.getChatID();
            
            updateProvider.returnToMainMenu(user);
            
            SendMessage request = new SendMessage(chatID, notify);
            request.parseMode(ParseMode.HTML);
            
            myBot.execute(request);
        });
        
        sessionManager.closeAllSessions();
        databaseManager.shutdown();
        
        System.exit(0);
    }
}
