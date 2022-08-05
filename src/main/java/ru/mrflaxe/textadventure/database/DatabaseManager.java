package ru.mrflaxe.textadventure.database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

import ru.mrflaxe.textadventure.database.model.AchievementModel;
import ru.mrflaxe.textadventure.database.model.ProfileModel;
import ru.mrflaxe.textadventure.database.model.SaveModel;
import ru.mrflaxe.textadventure.user.User;

public class DatabaseManager {
    
    private final ConnectionSource connection;

    private final Dao<ProfileModel, String> profileDao;
    private final Dao<SaveModel, String> saveDao;
    private final Dao<AchievementModel, String> achievementDao;
    
    public DatabaseManager(ConnectionSource connection) throws SQLException {
        this.connection = connection;
        
        this.profileDao = DaoManager.createDao(connection, ProfileModel.class);
        this.saveDao = DaoManager.createDao(connection, SaveModel.class);
        this.achievementDao = DaoManager.createDao(connection, AchievementModel.class);
    }
    
    /**
     * Closes the database connection
     */
    public void shutdown() {
        try {
            this.connection.close();
        } catch (IOException e) {
            System.err.println("Failed to close database connection:");
            e.printStackTrace();
        }
    }
    
    /**
     * Creates new profile model and save model for this profile.
     * @param chatID
     * @param userName
     * @return new ProfileModel of this user
     */
    public ProfileModel createNewProfile(long chatID, String userName) {
        SaveModel newSaveModel = new SaveModel();
        saveQuestSave(newSaveModel);
        
        String saveID = "" + newSaveModel.getId();
        
        return new ProfileModel(chatID, userName, saveID);
    }
    
    /**
     * Saves changes in profile model or saves the model if it doesn't exist in database yet.
     * @param profile - profile to save
     */
    public void saveProfile(ProfileModel profile) {
        try {
            profileDao.createOrUpdate(profile);
        } catch (SQLException e) {
            errorLog("save", ProfileModel.class, e);
        }
    }
    
    /**
     * Lazy method. <br>
     * Creates ProfileModel object and saves it in database by self.
     * @param chatID
     * @param userName
     * @return created profile model
     */
    public ProfileModel createAndSaveProfile(long chatID, String userName) {
        ProfileModel newProfile = createNewProfile(chatID, userName);
        saveProfile(newProfile);
        
        return newProfile;
    }
    
    /**
     * Gets profile model from database by given chat id.
     * @param chatID - chat id to identify the profile
     * @return profile model if found or null
     */
    @Nullable
    public ProfileModel getProfile(long chatID) {
        try {
            ProfileModel profile = profileDao.queryForId("" + chatID);
            
            if(profile != null) {
                return profile;
            }
            
            return null;
        } catch (SQLException e) {
            errorLog("get", ProfileModel.class, e);
            return null;
        }
    }
    
    /**
     * Checks if database contains profile with given chat id.
     * @param chatID - chat id to check
     * @return true if contains. Otherwise false
     */
    public boolean hasProfile(long chatID) {
        return getProfile(chatID) != null;
    }
    
    /**
     * Gets all profiles contained in database.
     * @return all profile models in list
     */
    public List<ProfileModel> getProfiles() {
        try {
            return profileDao.queryForAll();
        } catch (SQLException e) {
            errorLog("get", List.class, e);
            return null;
        }
    }
    
    /**
     * Deletes profile model from databse.
     * @param profile - profile to delete
     */
    public void deleteProfile(ProfileModel profile) {
        SaveModel save = getQuestSave(profile);
        
        if(save != null) {
            deleteQuestSave(save);
        }
        
        try {
            profileDao.delete(profile);
        } catch (SQLException e) {
            errorLog("delete", ProfileModel.class, e);
        }
    }
    
    /**
     * Saves changes in save model or saves the model if it doesn't exist in database yet.
     * @param save - save to save :D
     */
    public void saveQuestSave(SaveModel save) {
        try {
            saveDao.createOrUpdate(save);
        } catch (SQLException e) {
            errorLog("save", SaveModel.class, e);
        }
    }
    
    /**
     * Checks if profile has active quest save.
     * @param profile - profile to check for save
     * @return true if save exist or false
     */
    public boolean hasQuestSave(ProfileModel profile) {
        SaveModel save = getQuestSave(profile);
        if(save == null) {
            return false;
        }
        
        return save.getLastBranchID() != null;
    }
    
    /**
     * Gets save model for profile from database
     * @param profile - profile what provides save
     * @return save model or null if something will go wrong
     */
    public SaveModel getQuestSave(ProfileModel profile) {
        String saveId = profile.getSaveID();
        
        try {
            return saveDao.queryForId(saveId);
        } catch (SQLException e) {
            errorLog("get", SaveModel.class, e);
            return null;
        }
    }
    
    /**
     * Saves new branch id in the save model of given user.
     * @param user - user whose save
     * @param newBrachID - new branch id to save
     */
    public void updateSave(User user, String newBrachID) {
        ProfileModel profile = user.getUserProfile();
        
        SaveModel save = getQuestSave(profile);
        save.setLastBranchID(newBrachID);
        
        saveQuestSave(save);
    }
    
    /**
     * Deletes save model from database.
     * @param save
     */
    public void deleteQuestSave(SaveModel save) {
        try {
            saveDao.delete(save);
        } catch (SQLException e) {
            errorLog("delete", SaveModel.class, e);
        }
    }
    
    /**
     * Clears branch id from given save model and saves it in database.
     * @param save
     */
    public void clearQuestSave(SaveModel save) {
        save.setLastBranchID(null);
        saveQuestSave(save);
    }
    
    /**
     * Saves changes in achievement model or saves the model if it doesn't exist in database yet.
     * @param achievement - achievement to save
     */
    public void saveAchievment(AchievementModel achievement) {
        try {
            achievementDao.createOrUpdate(achievement);
        } catch (SQLException e) {
            errorLog("save", AchievementModel.class, e);
        }
    }
    
    /**
     * Gets all achievements by given chat id from database.
     * @param chatID - id to filter achievements
     * @return list of recieved achievements
     */
    @Nullable
    public List<AchievementModel> getAchievements(long chatID) {
        try {
            return achievementDao.queryForEq("chat_id", chatID);
        } catch (SQLException e) {
            errorLog("get list of", AchievementModel.class, e);
            return null;
        }
    }
    
    /**
     * Gets all obtained achievement with given achievement id.
     * @param achievementID - achievement id to filter achievements
     * @return list of currently obtained achievements with given id.
     */
    public List<AchievementModel> getCertainAchievments(String achievementID) {
        try {
            return achievementDao.queryForEq("achievement_id", achievementID);
        } catch (SQLException e) {
            errorLog("get list of", AchievementModel.class, e);
            return null;
        }
    }
    
    /**
     * Gets percent of users who obtained achievement with given id.
     * @param achievmentID - achievemnt to calculate percent.
     * @return percent of users who obtained achievement with given id
     */
    public float getAchievmentPercent(String achievmentID) {
        float allProfiles = getProfiles().size();
        
        List<AchievementModel> certainAchievments = getCertainAchievments(achievmentID);
        
        if(certainAchievments == null || certainAchievments.isEmpty()) {
            return 0f;
        }
        
        float givenAchivments = certainAchievments.size();
        float percent = givenAchivments/allProfiles * 100;
        
        return percent;
    }
    
    /**
     * Checks if givem achievement currently have only one person.
     * @param achievementID - achievement to check
     * @return true if does or false
     */
    public boolean isOnlyOneOwner(String achievementID) {
        return getCertainAchievments(achievementID).size() == 1;
    }
    
    private void errorLog(String action, Class<?> model, SQLException e) {
        System.err.println("Failed to " + action + " " + model.getName() + " while working with database.");
        System.err.println(e.getMessage());
        System.err.println("SQLstate is: " + e.getSQLState());
    }
}
