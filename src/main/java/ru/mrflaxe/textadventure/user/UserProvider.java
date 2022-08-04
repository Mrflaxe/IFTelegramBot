package ru.mrflaxe.textadventure.user;

import java.util.HashMap;
import java.util.Map;

import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.database.model.ProfileModel;

public class UserProvider {
    
    private final DatabaseManager databaseManager;
    private final Map<Long, User> users;
    
    public UserProvider(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.users = new HashMap<>();
        
    }
    
    public User getUser(long chatID) {
        if(users.containsKey(chatID)) {
            return users.get(chatID);
        }
        
        ProfileModel profile = databaseManager.getProfile(chatID);
        
        if(profile == null) {
            return null;
        }
        
        return loadUser(profile);
    }
    
    public void addUser(long chatID, User user) {
        users.put(chatID, user);
    }
    
    private User loadUser(ProfileModel profile) {
        long chatID = profile.getChatId();
        String firstName = profile.getName();
        
        User user = new User(chatID, firstName, databaseManager);
        users.put(chatID, user);
        
        return user;
    }
}
