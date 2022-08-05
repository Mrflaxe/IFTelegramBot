package ru.mrflaxe.textadventure.quest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;

import ru.mrflaxe.textadventure.achievement.Achievement;
import ru.mrflaxe.textadventure.achievement.AchievmentManager;
import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.database.model.ProfileModel;
import ru.mrflaxe.textadventure.database.model.SaveModel;
import ru.mrflaxe.textadventure.quest.message.AnswerOption;
import ru.mrflaxe.textadventure.quest.message.BranchContainer;
import ru.mrflaxe.textadventure.quest.message.branch.Ending;
import ru.mrflaxe.textadventure.quest.message.branch.ProvideAchievement;
import ru.mrflaxe.textadventure.quest.message.branch.ProvideAnswers;
import ru.mrflaxe.textadventure.quest.message.branch.QuestBranch;
import ru.mrflaxe.textadventure.update.UpdateProvider;
import ru.mrflaxe.textadventure.user.User;

public class QuestSessionManager {

    private final UpdateProvider updateProvider;
    private final DatabaseManager databaseManager;
    private final AchievmentManager achievementManager;
    private final BranchContainer branchContainer;
    private final Configuration config;
    private final Configuration messages;
    private final TelegramBot bot;
    
    private final String ANSWER_OPTION_PATTERN;
    
    private final Map<User, QuestBranch> activePlayerData;
    
    public QuestSessionManager(
            UpdateProvider updateProvider,
            DatabaseManager databaseManager,
            AchievmentManager achievmentManager,
            Configuration config,
            Configuration messages,
            TelegramBot bot
            ) {
        this.updateProvider = updateProvider;
        this.databaseManager = databaseManager;
        this.achievementManager = achievmentManager;
        this.branchContainer = new BranchContainer(achievmentManager);
        this.config = config;
        this.messages = messages;
        this.bot = bot;
        
        this.ANSWER_OPTION_PATTERN = messages.getString("quest.answer-options-pattern");
        
        this.activePlayerData = new HashMap<>();
    }
    
    public boolean hasSession(User user) {
        return activePlayerData.containsKey(user);
    }
    
    public void openSession(User user) {
        ProfileModel profile = databaseManager.getProfile(user.getChatID());
        SaveModel save = databaseManager.getQuestSave(profile);
        
        if(save == null) {
            System.err.println(profile.getChatId() + " profile save is null!");
            return;
        }
        
        String lastBranchID = save.getLastBranchID();
        
        if(lastBranchID == null) {
            QuestBranch startBranch = branchContainer.getBranch("start");
            sendBranch(user, startBranch);
            return;
        }
        
        QuestBranch lastBranch = branchContainer.getBranch(lastBranchID);
        if(lastBranch == null) {
            System.err.println("Failed to get brnach by id '" + lastBranchID + "'.");
            return;
        }
        
        sendBranch(user, lastBranch);
        return;
    }
    
    public void closeSession(User user) {
        if(!activePlayerData.containsKey(user)) {
            return;
        }
        
        QuestBranch branch = activePlayerData.get(user);
        String branchID = branch.getId();
        
        databaseManager.updateSave(user, branchID);
        
        activePlayerData.remove(user);
    }
    
    public void closeAllSessions() {
        List<User> activePlayers = activePlayerData.keySet().stream()
                .collect(Collectors.toList());
        
        activePlayers.forEach(this::closeSession);
    }
    
    public Set<User> getActivePlayers() {
        return activePlayerData.keySet();
    }
    
    public void sendBranch(User user, QuestBranch branch) {
        activePlayerData.put(user, branch);
        List<String> lines = branch.getLines();
        int cooldownSec = config.getInt("message-cooldown");
        long chatID = user.getChatID();
        
        ChatAction action = ChatAction.typing;
        Keyboard replKeyboardMarkup = new ReplyKeyboardRemove();
        
        SendChatAction requestTyping = new SendChatAction(chatID, action);
        bot.execute(requestTyping);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // Sending line
            runTaskTimer(() -> {
                SendMessage requestMessage = new SendMessage(chatID, line);
                ParseMode parseMode = ParseMode.HTML;
                
                requestMessage.parseMode(parseMode);
                requestMessage.replyMarkup(replKeyboardMarkup);
                bot.execute(requestMessage);
            }, cooldownSec * (i + 1) * 1000, user);
            
            // If not last message sending "typing" status
            if(i + 1 != lines.size()) {
                runTaskTimer(() -> {
                    bot.execute(requestTyping);
                }, cooldownSec * (i + 1) * 1000 + 50, user);
                
                continue;
            }
            
            // This delay sets timer for the time when last line will be sended
            int lastLineTiming = cooldownSec * (lines.size() + 1) * 1000;
            
            if(branch instanceof ProvideAchievement) {
                runTaskTimer(() -> {
                    ProvideAchievement achievementBranch = (ProvideAchievement) branch;
                    Achievement achievement = achievementBranch.getAchievement();
                    
                    // If uset already has this achievement no reason to give another one
                    if(user.hasAchievement(achievement)) {
                        return;
                    }
                    
                    achievementManager.addAchievement(user, achievement);
                    sendAchievementNotice(chatID, achievement);
                }, lastLineTiming, user);
                
                ProvideAchievement achievementBranch = (ProvideAchievement) branch;
                Achievement achievement = achievementBranch.getAchievement();
                
             // It's needed for showing achievement notify
                if(!user.hasAchievement(achievement)) {
                    lastLineTiming = cooldownSec * (lines.size() + 2) * 1000; //Increase multiplyer by 1 for extra time
                }
            }
            
            // If ending returns user to main menu
            if(branch instanceof Ending) {
                runTaskTimer(() -> {
                    activePlayerData.remove(user);
                    
                    // User complete the quest. He don't need saves yet
                    databaseManager.clearQuestSave(user.getUserSave());
                    updateProvider.returnToMainMenu(user);
                }, lastLineTiming, user);
                
                return;
            }
            
            if(branch instanceof ProvideAnswers) {
                runTaskTimer(() -> {
                    sendAnswerOptions(chatID, (ProvideAnswers) branch);
                }, lastLineTiming, user);
            }
        }
    }
    
    public void handle(Message message, User user) {
        String text = message.text();
        int answerNumber;
        
        try {
            answerNumber = Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return;
        }
        
        // Because it's handling I sure that this message is answer option
        // In other cases I just return.
        ProvideAnswers currentBrunch;
        
        try {
            currentBrunch = (ProvideAnswers) activePlayerData.get(user);
        } catch (ClassCastException ignored) {
            return;
        }
        
        if(currentBrunch == null) {
            return;
        }
        
        AnswerOption answerOption;
        
        try {
            answerOption = currentBrunch.getAnswerOption(answerNumber - 1);
        } catch (IndexOutOfBoundsException exception) {
            return;
        }
        
        String link = answerOption.getNextBranchID();
        QuestBranch nextBranch = branchContainer.getBranch(link);
        
        sendBranch(user, nextBranch);
    }
    
    private void sendAnswerOptions(long chatID, ProvideAnswers branch) {
        String message = ANSWER_OPTION_PATTERN + "\n";
        List<AnswerOption> answerOptions = branch.getAnswerOptions();
        
        KeyboardButton[] keyboard = new KeyboardButton[answerOptions.size()];
        
        for (int i = 0; i < answerOptions.size(); i++) {
            int number = i + 1;
            String answerOption = answerOptions.get(i).getText();
            String answerOptionNumber = number + ". " + answerOption;
            
            message = message + answerOptionNumber + "\n";
            keyboard[i] = new KeyboardButton("" + number);
        }
        
        Keyboard replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboard).resizeKeyboard(true);
        SendMessage request = new SendMessage(chatID, message);
        
        request.parseMode(ParseMode.HTML);
        request.replyMarkup(replyKeyboardMarkup);
        
        bot.execute(request);
    }
    
    private void sendAchievementNotice(long chatID, Achievement achievement) {
        String title = messages.getString("achievement.obtained", true);
        
        String achivementName = achievement.getName();
        String achievementDescription = achievement.getDescription();
        
        String message = title + "\n\n" + achivementName + "\n" + achievementDescription + "\n ";
        
        SendMessage request = new SendMessage(chatID, message);
        request.parseMode(ParseMode.HTML);
        
        bot.execute(request);
    }
    
    private Thread runTaskTimer(Runnable task, int delay, User user) {
        Thread thread =  new Thread(() -> {
           try {
               Thread.sleep(delay);
               
               if(!activePlayerData.containsKey(user)) {
                   return;
               }
               
               task.run();
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
        });
        
        thread.start();
        
        return thread;
    }
}
