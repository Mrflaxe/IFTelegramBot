package ru.mrflaxe.textadventure.user;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.mrflaxe.textadventure.achievement.Achievement;
import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.database.model.AchievementModel;
import ru.mrflaxe.textadventure.database.model.ProfileModel;
import ru.mrflaxe.textadventure.database.model.SaveModel;

@AllArgsConstructor
public class User {
        
    @Getter
    private final long chatID;
    
    @Getter
    private final String firstName;

    private final DatabaseManager databaseManager;
    
    public ProfileModel getUserProfile() {
        return databaseManager.getProfile(chatID);
    }
    
    public SaveModel getUserSave() {
        ProfileModel profile = getUserProfile();
        return databaseManager.getQuestSave(profile);
    }
    
    public boolean hasAchievement(Achievement achievement) {
        List<AchievementModel> achievements = databaseManager.getCertainAchievments(achievement.getId());
        
        return !achievements.stream()
                .filter(model -> model.getChatID() == chatID)
                .collect(Collectors.toList())
                .isEmpty();
    }
    
}
