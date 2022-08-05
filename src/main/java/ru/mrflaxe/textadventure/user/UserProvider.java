package ru.mrflaxe.textadventure.user;

import java.util.HashMap;
import java.util.Map;

import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.database.model.ProfileModel;

public class UserProvider {
    
    private final DatabaseManager databaseManager;
    private final Map<Long, User> users; // cache
    
    public UserProvider(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.users = new HashMap<>();
        
    }
    
    /**
     * Gets user data from cache or from databse if cache doesn't contain user data.
     * @param chatID
     * @return user
     */
    public User getUser(long chatID) {
        // If user contained in cache
        if(users.containsKey(chatID)) {
            return users.get(chatID);
        }
        
        // Gets user data from database
        ProfileModel profile = databaseManager.getProfile(chatID);
        
        if(profile == null) {
            return null;
        }
        
        return loadUser(profile);
    }
    
    /**
     * Adds user to cache
     * @param chatID
     * @param user
     */
    public void addUser(long chatID, User user) {
        users.put(chatID, user);
    }
    
    /**
     * Loads user to cache
     * @param profile - profile model from database
     * @return user
     */
    private User loadUser(ProfileModel profile) {
        long chatID = profile.getChatId();
        String firstName = profile.getName();
        
        User user = new User(chatID, firstName, databaseManager);
        users.put(chatID, user);
        
        return user;
    }
}
