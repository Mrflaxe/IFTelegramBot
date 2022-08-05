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
import ru.mrflaxe.textadventure.update.handlers.AchievementHandler;
import ru.mrflaxe.textadventure.update.handlers.ExitHandler;
import ru.mrflaxe.textadventure.update.handlers.InfoHandler;
import ru.mrflaxe.textadventure.update.handlers.MessageHandler;
import ru.mrflaxe.textadventure.update.handlers.PlayHandler;
import ru.mrflaxe.textadventure.update.handlers.StartHandler;
import ru.mrflaxe.textadventure.update.handlers.UnknownCommandHandler;
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
    private MessageHandler unknownCommandHandler;
    
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
        this.unknownCommandHandler = new UnknownCommandHandler(telegramBot, messages, this);
        
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
            // Gets text of the message
            String textMessage = update.message().text();
            // Gets id of the chat
            long chatID = update.message().chat().id();
            
            // Gets user from provider
            User user = userProvider.getUser(chatID);
            
            // If it's new user save his data in cache
            // And if he is new for database save there
            if(user == null) {
                String name = update.message().chat().firstName();
                user = new User(chatID, name, databaseManager);
                userProvider.addUser(chatID, user);
                
                if(!databaseManager.hasProfile(chatID)) {
                    // Saving new profile to database
                    databaseManager.createAndSaveProfile(chatID, name);
                }
            }
            
            // If cooldown still going asks user to wait
            if(cooldown.isCooldown()) {
                String message = messages.getString("wait", true);
                
                SendMessage request = new SendMessage(user.getChatID(), message);
                request.parseMode(ParseMode.HTML);
                
                telegramBot.execute(request);
                return;
            }
            
            // Relaunches cooldown and handles the message
            cooldown.setCooldown();
            
            // If user playing quest already
            if(questSessions.hasSession(user)) {
                // If user tries to leave game
                if(textMessage.equals("/exit")) {
                    // Handling exit command
                    commandHandlers.get(textMessage).handle(update);
                    return;
                }
                
                // In other cases the message can be or answer option or just some random text
                // Anyway will handle it in quest handler
                questSessions.handle(update.message(), user);
                return;
            }
            
            // If text is not null it can be a command
            if(textMessage != null) {
                // If commandHandlers contains this message as a key so it's a command
                // Will handle it
                if(commandHandlers.containsKey(textMessage)) {
                    commandHandlers.get(textMessage).handle(update);
                    return;
                // Otherwise will say user that bot doesn't know such command
                } else {
                    unknownCommandHandler.handle(update);
                    return;
                }
            }
        });
        
        return CONFIRMED_UPDATES_ALL;
    }
    
    /**
     * Returns user to bot main menu
     * @param user - whom to return
     */
    public void returnToMainMenu(User user) {
        String message = messages.getString("menu.message", true);
        long chatID = user.getChatID();
        
        ProfileModel profile = user.getUserProfile();
        
        boolean userHasSave = databaseManager.hasQuestSave(profile);
        String playButton;
        
        // If user has save showing one button
        // If not showing another. Simple
        if(!userHasSave) {
            playButton = this.playButton;
        } else {
            playButton = this.continueButton;
        }

        // Creating new replyKeyboard with menu buttons
        Keyboard replKeyboardMarkup = new ReplyKeyboardMarkup(
                new KeyboardButton[]{
                        new KeyboardButton(playButton)
                }, new KeyboardButton[] {
                        new KeyboardButton(profileButton),
                        new KeyboardButton(infoButton)
                }).resizeKeyboard(true);
        
        // Sending message and keyboard
        SendMessage request = new SendMessage(chatID, message).replyMarkup(replKeyboardMarkup);
        request.parseMode(ParseMode.HTML);
        
        telegramBot.execute(request);
    }
    
    private void registerButtons() {
        this.profileButton = messages.getString("menu.keyboard.achievement", true);
        this.infoButton = messages.getString("menu.keyboard.info", true);
        this.playButton = messages.getString("menu.keyboard.play.start", true);
        this.continueButton = messages.getString("menu.keyboard.play.continue", true);
        
        // Adds alternative triggers for keyboard buttons
        addAlternativeCommandTrigger(profileButton, "/achievement");
        addAlternativeCommandTrigger(infoButton, "/info");
        addAlternativeCommandTrigger(playButton, "/play");
        addAlternativeCommandTrigger(continueButton, "/play");
    }
    
    // links handlers with command triggers
    private void initializeHandlers() {
        commandHandlers.put("/achievement", new AchievementHandler(telegramBot, messages, this, databaseManager));
        commandHandlers.put("/info", new InfoHandler(telegramBot, messages, this));
        commandHandlers.put("/play", new PlayHandler(telegramBot, messages, this, userProvider, questSessions));
        commandHandlers.put("/start", new StartHandler(telegramBot, messages, this, userProvider));
        commandHandlers.put("/exit", new ExitHandler(telegramBot, messages, this, questSessions, userProvider));
    }
    
    /**
     * Adds alternative command trigger for existing command
     * @param alternative - alternative command
     * @param existing - existing command to link with alternate
     */
    public void addAlternativeCommandTrigger(String alternative, String existing) {
        MessageHandler handler = commandHandlers.get(existing);
        commandHandlers.put(alternative, handler);
    }
}
