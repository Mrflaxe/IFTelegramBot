package ru.mrflaxe.textadventure.database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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
    
    public void shutdown() {
        try {
            this.connection.close();
        } catch (IOException e) {
            System.err.println("Failed to close database connection:");
            e.printStackTrace();
        }
    }
    
    public ProfileModel createNewProfile(long chatID, String userName) {
        SaveModel newSaveModel = new SaveModel();
        saveQuestSave(newSaveModel);
        
        String saveID = "" + newSaveModel.getId();
        
        return new ProfileModel(chatID, userName, saveID);
    }
    
    public void saveProfile(ProfileModel profile) {
        try {
            profileDao.createOrUpdate(profile);
        } catch (SQLException e) {
            errorLog("save", ProfileModel.class, e);
        }
    }
    
    public ProfileModel createAndSaveProfile(long chatID, String userName) {
        ProfileModel newProfile = createNewProfile(chatID, userName);
        saveProfile(newProfile);
        
        return newProfile;
    }
    
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
    
    public boolean hasProfile(long chatID) {
        return getProfile(chatID) != null;
    }
    
    public List<ProfileModel> getProfiles() {
        try {
            return profileDao.queryForAll();
        } catch (SQLException e) {
            errorLog("get", List.class, e);
            return null;
        }
    }
    
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
    
    public void saveQuestSave(SaveModel save) {
        try {
            saveDao.createOrUpdate(save);
        } catch (SQLException e) {
            errorLog("save", SaveModel.class, e);
        }
    }
    
    public boolean hasQuestSave(ProfileModel profile) {
        SaveModel save = getQuestSave(profile);
        if(save == null) {
            return false;
        }
        
        return save.getLastBranchID() != null;
    }
    
    public SaveModel getQuestSave(ProfileModel profile) {
        String saveId = profile.getSaveID();
        
        try {
            return saveDao.queryForId(saveId);
        } catch (SQLException e) {
            errorLog("get", SaveModel.class, e);
            return null;
        }
    }
    
    public void updateSave(User user, String newBrachID) {
        ProfileModel profile = user.getUserProfile();
        
        SaveModel save = getQuestSave(profile);
        save.setLastBranchID(newBrachID);
        
        saveQuestSave(save);
    }
    
    public void deleteQuestSave(SaveModel save) {
        try {
            saveDao.delete(save);
        } catch (SQLException e) {
            errorLog("delete", SaveModel.class, e);
        }
    }
    
    public void clearQuestSave(SaveModel save) {
        save.setLastBranchID(null);
        saveQuestSave(save);
    }
    
    public void saveAchievment(AchievementModel achievement) {
        try {
            achievementDao.createOrUpdate(achievement);
        } catch (SQLException e) {
            errorLog("save", AchievementModel.class, e);
        }
    }
    
    public List<AchievementModel> getAchievements(long chatID) {
        try {
            return achievementDao.queryForEq("chat_id", chatID);
        } catch (SQLException e) {
            errorLog("get list of", AchievementModel.class, e);
            return null;
        }
    }
    
    public List<AchievementModel> getCertainAchievments(String achievementID) {
        try {
            return achievementDao.queryForEq("achievement_id", achievementID);
        } catch (SQLException e) {
            errorLog("get list of", AchievementModel.class, e);
            return null;
        }
    }
    
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
    
    public boolean isOnlyOneOwner(String achievementID) {
        return getCertainAchievments(achievementID).size() == 1;
    }
    
    private void errorLog(String action, Class<?> model, SQLException e) {
        System.err.println("Failed to " + action + " " + model.getName() + " while working with database.");
        System.err.println(e.getMessage());
        System.err.println("SQLstate is: " + e.getSQLState());
    }
}
