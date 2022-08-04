package ru.mrflaxe.textadventure.achievement;

import java.util.HashMap;
import java.util.Map;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.configuration.ConfigurationSection;
import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.database.model.AchievementModel;
import ru.mrflaxe.textadventure.user.User;

public class AchievmentManager {

    private final Configuration achievments;
    private final DatabaseManager databaseManager;
    
    private final Map<String, Achievement> achievmentContainer;
    
    public AchievmentManager(Configuration achievments, DatabaseManager databaseManager) {
        this.achievments = achievments;
        this.databaseManager = databaseManager;
        
        this.achievmentContainer = new HashMap<>();
        initializeAchievments();
    }
    
    public Achievement getAchievement(String achievmentID) {
        return achievmentContainer.get(achievmentID);
    }
    
    public void addAchievement(User user, Achievement achievment) {
        long chatID = user.getChatID();
        
        AchievementModel achievmentModel = new AchievementModel(chatID, achievment);
        databaseManager.saveAchievment(achievmentModel);
    }
    
    private void initializeAchievments() {
        Map<String, ConfigurationSection> subsections = achievments.getAllSubsections();
        
        subsections.entrySet().stream()
            .forEach(set -> {
                String id = set.getKey();
                
                ConfigurationSection section = set.getValue();
                String name = section.getString("name");
                String description = section.getString("description");
                
                Achievement achievment = new Achievement(id, name, description);
                achievmentContainer.put(id, achievment);
            });
    }
    
}
