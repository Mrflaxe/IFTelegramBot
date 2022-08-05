package ru.mrflaxe.textadventure.update.handlers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;

import ru.mrflaxe.textadventure.achievement.Achievement;
import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.database.model.AchievementModel;
import ru.mrflaxe.textadventure.update.UpdateProvider;

public class AchievementHandler extends MessageHandler {

    private final DatabaseManager databaseManager;
    
    public AchievementHandler(TelegramBot bot, Configuration messages, UpdateProvider updateProvider, DatabaseManager databaseManager) {
        super(bot, messages, updateProvider);
        
        this.databaseManager = databaseManager;
    }

    @Override
    public void handle(Update update) {
        Long chatID = update.message().chat().id();
        
        List<AchievementModel> achievementModels = databaseManager.getAchievements(chatID);
        List<Achievement> achievements = achievementModels.stream()
                .map(AchievementModel::getAchievment)
                .collect(Collectors.toList());
        
        String message = buildMessage(achievements);
        
        SendMessage request = new SendMessage(chatID, message);
        request.parseMode(ParseMode.HTML);
        
        bot.execute(request);
    }
    
    private String buildMessage(List<Achievement> achievements) {
        String head = messages.getString("achievement.list.head");
        
        if(achievements == null || achievements.isEmpty()) {
            String emptyAchievements = messages.getString("achievement.list.no-achievement");
            
            return head + "\n"
                    + "\n"
                    + emptyAchievements;
        }
        
        String percentPattern = messages.getString("achievement.list.percent-pattern");
        List<String> achievementsInfo = new ArrayList<>();
        
        achievements.forEach(achievement -> {
            String achievementID = achievement.getId();
            String name = achievement.getName();
            String description = achievement.getDescription();
            float percent = databaseManager.getAchievmentPercent(achievementID);
            
            DecimalFormat formatter = new DecimalFormat("##.##");
            String textPrecent = formatter.format(percent);
            
            String precentInfo = percentPattern.replace("%percent%", "" + textPrecent);
            
            if(databaseManager.isOnlyOneOwner(achievementID)) {
                precentInfo = messages.getString("achievement.list.the-only-one");
            }
            
            String achievementInfo = name + "\n"
                    + description + "\n"
                    + precentInfo;
            
            achievementsInfo.add(achievementInfo);
        });
        
        String message = head + "\n";
        
        for (int i = 0; i < achievementsInfo.size(); i++) {
            String achivementInfo = achievementsInfo.get(i);
            
            message = message + "\n"
                    + achivementInfo + "\n";
        }
        
        return message;
    }
}
