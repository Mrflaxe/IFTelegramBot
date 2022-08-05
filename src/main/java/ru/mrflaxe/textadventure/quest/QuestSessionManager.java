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
    
    private final String ANSWER_OPTION_HEADER;
    
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
        
        this.ANSWER_OPTION_HEADER = messages.getString("quest.answer-options.head");
        
        this.activePlayerData = new HashMap<>();
    }
    
    /**
     * Returns true if user now playing quest <br>
     * Otherwise false
     * @param user - user to check
     * @return true if user playing the quest or false
     */
    public boolean hasSession(User user) {
        return activePlayerData.containsKey(user);
    }
    
    /**
     * Starts or continues user game from the last branch.
     * @param user - who gonna play
     */
    public void openSession(User user) {
        ProfileModel profile = databaseManager.getProfile(user.getChatID());
        SaveModel save = databaseManager.getQuestSave(profile);
        
        // This is error because all users have save model by default.
        // Last branch id in this models can be null, but save object should be.
        if(save == null) {
            System.err.println(profile.getChatId() + " profile save is null!");
            return;
        }
        
        // Now getting lastbranch id
        String lastBranchID = save.getLastBranchID();
        
        // If it's equals null means that the user just don't have any save yet.
        // Starts new game
        if(lastBranchID == null) {
            // All quests should starts with 'start' branch.
            QuestBranch startBranch = branchContainer.getBranch("start");
            sendBranch(user, startBranch);
            return;
        }
        
        // In this case user have save so will start from it.
        QuestBranch lastBranch = branchContainer.getBranch(lastBranchID);
        
        // But in some cases this save may be damaged.
        // Anything happens.
        if(lastBranch == null) {
            System.err.println("Failed to get brnach by id '" + lastBranchID + "'.");
            return;
        }
        
        sendBranch(user, lastBranch);
        return;
    }
    
    /**
     * Removes user from active players and updates save.
     * @param user
     */
    public void closeSession(User user) {
        // If user already not active player
        if(!activePlayerData.containsKey(user)) {
            return;
        }
        
        // Saving progress
        QuestBranch branch = activePlayerData.get(user);
        String branchID = branch.getId();
        
        databaseManager.updateSave(user, branchID);
        
        // Finally removing from active players
        activePlayerData.remove(user);
    }
    
    /**
     * Closes all active sessions for all users who plays now
     */
    public void closeAllSessions() {
        List<User> activePlayers = activePlayerData.keySet().stream()
                .collect(Collectors.toList());
        
        activePlayers.forEach(this::closeSession);
    }
    
    public Set<User> getActivePlayers() {
        return activePlayerData.keySet();
    }
    
    
    public void sendBranch(User user, QuestBranch branch) {
        // saves or updates current branch
        activePlayerData.put(user, branch);
        // Gets branch lines
        List<String> lines = branch.getLines();
        int cooldownSec = config.getInt("message-cooldown");
        long chatID = user.getChatID();
        
        ChatAction action = ChatAction.typing;
        // Removes keyboard if it was sended by previous command
        Keyboard replKeyboardMarkup = new ReplyKeyboardRemove();
        
        // Sending typing status
        SendChatAction requestTyping = new SendChatAction(chatID, action);
        bot.execute(requestTyping);
        
        // Now sending each line with cooldown delay
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // Sending line goes first
            runTaskTimer(() -> {
                SendMessage requestMessage = new SendMessage(chatID, line);
                
                requestMessage.parseMode(ParseMode.HTML);
                requestMessage.replyMarkup(replKeyboardMarkup);
                bot.execute(requestMessage);
            }, cooldownSec * (i + 1) * 1000, user);
            
            // If it was not last line sending "typing" status again
            if(i + 1 != lines.size()) {
                runTaskTimer(() -> {
                    bot.execute(requestTyping);
                }, cooldownSec * (i + 1) * 1000 + 50, user);
                
                continue;
            }
            
            // This delay sets timer for the time when last line will be sended
            int lastLineTiming = cooldownSec * (lines.size() + 1) * 1000;
            
            // If this branch provides achievement will give it after sending all lines
            if(branch instanceof ProvideAchievement) {
                runTaskTimer(() -> {
                    ProvideAchievement achievementBranch = (ProvideAchievement) branch;
                    Achievement achievement = achievementBranch.getAchievement();
                    
                    // If user already has this achievement no reason to give another one
                    if(user.hasAchievement(achievement)) {
                        return;
                    }
                    
                    achievementManager.addAchievement(user, achievement);
                    sendAchievementNotice(chatID, achievement);
                }, lastLineTiming, user);
                
                ProvideAchievement achievementBranch = (ProvideAchievement) branch;
                Achievement achievement = achievementBranch.getAchievement();
                
             // For showing achievement notify necessery extra time
                if(!user.hasAchievement(achievement)) {
                    lastLineTiming = cooldownSec * (lines.size() + 2) * 1000; //Increase multiplyer by 1 for extra time
                }
            }
            
            // If ending returns user to main menu. Game is over.
            if(branch instanceof Ending) {
                runTaskTimer(() -> {
                    activePlayerData.remove(user);
                    
                    // User complete the quest. He don't need saves anymore
                    databaseManager.clearQuestSave(user.getUserSave());
                    updateProvider.returnToMainMenu(user);
                }, lastLineTiming, user);
                
                return;
            }
            
            // If branch provides answer options (it always does except ending branches),
            // send asnwer options
            if(branch instanceof ProvideAnswers) {
                runTaskTimer(() -> {
                    sendAnswerOptions(chatID, (ProvideAnswers) branch);
                }, lastLineTiming, user);
            }
        }
    }
    
    /**
     * Handling answer option responses
     * @param message
     * @param user
     */
    public void handle(Message message, User user) {
        String text = message.text();
        int answerNumber;
        
        // If answer is not integer skipping this answer
        try {
            answerNumber = Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return;
        }
        
        // Can cast to ProvideAnswers because it's handling method so I sure that this message is answer option.
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
        
        // Now I can send user to next branch according to his choice.
        String link = answerOption.getNextBranchID();
        QuestBranch nextBranch = branchContainer.getBranch(link);
        
        sendBranch(user, nextBranch);
    }
    
    // I'm too lazy to comment this algorithm.
    // It's pretty simple to understand
    private void sendAnswerOptions(long chatID, ProvideAnswers branch) {
        String message = ANSWER_OPTION_HEADER + "\n";
        List<AnswerOption> answerOptions = branch.getAnswerOptions();
        
        KeyboardButton[] keyboard = new KeyboardButton[answerOptions.size()];
        
        // For each answer option
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
