package ru.mrflaxe.textadventure.update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;

import lombok.Getter;
import ru.mrflaxe.textadventure.achievement.AchievmentManager;
import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.database.model.ProfileModel;
import ru.mrflaxe.textadventure.quest.QuestSessionManager;
import ru.mrflaxe.textadventure.tool.Cooldown;
import ru.mrflaxe.textadventure.update.handlers.ExitHandler;
import ru.mrflaxe.textadventure.update.handlers.InfoHandler;
import ru.mrflaxe.textadventure.update.handlers.MessageHandler;
import ru.mrflaxe.textadventure.update.handlers.PlayHandler;
import ru.mrflaxe.textadventure.update.handlers.ProfileHandler;
import ru.mrflaxe.textadventure.update.handlers.StartHandler;
import ru.mrflaxe.textadventure.user.User;
import ru.mrflaxe.textadventure.user.UserProvider;

public class UpdateProvider implements UpdatesListener {

    private final TelegramBot telegramBot;
    private final Configuration messages;
    private final DatabaseManager databaseManager;
    private final UserProvider userProvider;
    
    @Getter
    private final QuestSessionManager questSessions;
    
    private Map<String, MessageHandler> commandHandlers;
    
    private String profileButton;
    private String infoButton;
    private String playButton;
    private String continueButton;
    
    private Cooldown cooldown;
    
    public UpdateProvider(
            TelegramBot telegramBot,
            Configuration config,
            Configuration messages,
            DatabaseManager databaseManager,
            AchievmentManager achievmentManager,
            UserProvider userProvider
            ) {
        this.commandHandlers = new HashMap<>();
        
        this.telegramBot = telegramBot;
        this.messages = messages;
        this.databaseManager = databaseManager;
        this.userProvider = userProvider;
        
        this.questSessions = new QuestSessionManager(
                this,
                databaseManager,
                achievmentManager,
                config,
                messages,
                telegramBot
                );
        
        int cooldown = config.getInt("send-cooldown");
        this.cooldown = new Cooldown(cooldown);
        
        initializeHandlers();
        registerButtons();
    }
    
    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            String textMessage = update.message().text();
            long chatID = update.message().chat().id();
            
            User user = userProvider.getUser(chatID);
            
            // If it's new user save his data in cache
            if(user == null) {
                String name = update.message().chat().firstName();
                user = new User(chatID, name, databaseManager);
                userProvider.addUser(chatID, user);
                
                if(!databaseManager.hasProfile(chatID)) {
                    // Saving new profile to database
                    databaseManager.createAndSaveProfile(chatID, name);
                }
            }
            
            if(cooldown.isCooldown()) {
                String message = messages.getString("wait");
                
                SendMessage request = new SendMessage(user.getChatID(), message);
                request.parseMode(ParseMode.HTML);
                
                telegramBot.execute(request);
                return;
            }
            
            cooldown.setCooldown();
            
            if(questSessions.hasSession(user)) {
                if(textMessage.equals("/exit")) {
                    commandHandlers.get(textMessage).handle(update);
                    return;
                }
                
                questSessions.handle(update.message(), user);
                return;
            }
            
            if(textMessage != null) {
                if(commandHandlers.containsKey(textMessage)) {
                    commandHandlers.get(textMessage).handle(update);
                } else {
                    // TODO incorrect command handler
                }
            }
        });
        
        return CONFIRMED_UPDATES_ALL;
    }
    
    public void returnToMainMenu(User user) {
        String message = messages.getString("menu.message");
        long chatID = user.getChatID();
        
        // Saving new profile to database
        ProfileModel profile = databaseManager.getProfile(chatID);
        
        boolean userHasSave = databaseManager.hasQuestSave(profile);
        String playButton;
        
        if(!userHasSave) {
            playButton = this.playButton;
        } else {
            playButton = this.continueButton;
        }

        Keyboard replKeyboardMarkup = new ReplyKeyboardMarkup(
                new KeyboardButton[]{
                        new KeyboardButton(playButton)
                }, new KeyboardButton[] {
                        new KeyboardButton(profileButton),
                        new KeyboardButton(infoButton)
                }).resizeKeyboard(true);
        
        SendMessage request = new SendMessage(chatID, message).replyMarkup(replKeyboardMarkup);
        ParseMode parseMode = ParseMode.HTML;
        request.parseMode(parseMode);
        
        telegramBot.execute(request);
    }
    
    private void registerButtons() {
        this.profileButton = messages.getString("menu.keyboard.profile");
        this.infoButton = messages.getString("menu.keyboard.info");
        this.playButton = messages.getString("menu.keyboard.play.start");
        this.continueButton = messages.getString("menu.keyboard.play.continue");
        
        addAlternativeCommandHandler(profileButton, "/profile");
        addAlternativeCommandHandler(infoButton, "/info");
        addAlternativeCommandHandler(playButton, "/play");
        addAlternativeCommandHandler(continueButton, "/play");
    }
    
    private void initializeHandlers() {
        commandHandlers.put("/profile", new ProfileHandler(telegramBot, messages, this));
        commandHandlers.put("/info", new InfoHandler(telegramBot, messages, this));
        commandHandlers.put("/play", new PlayHandler(telegramBot, messages, this, userProvider, questSessions));
        commandHandlers.put("/start", new StartHandler(telegramBot, messages, this, userProvider));
        commandHandlers.put("/exit", new ExitHandler(telegramBot, messages, this, questSessions, userProvider));
    }
    
    public void addAlternativeCommandHandler(String alternative, String existing) {
        MessageHandler handler = commandHandlers.get(existing);
        commandHandlers.put(alternative, handler);
    }
}
