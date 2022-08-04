package ru.mrflaxe.textadventure.database.model;

import java.sql.Date;
import java.util.Calendar;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.mrflaxe.textadventure.achievement.Achievement;

@Getter
@NoArgsConstructor
@DatabaseTable(tableName = "telegrambot_profile_achievements")
public class AchievementModel {

    @DatabaseField(generatedId = true)
    private int id;
    
    @DatabaseField(columnName = "date", dataType = DataType.SQL_DATE)
    private Date date;
    
    @DatabaseField(columnName = "chat_id")
    private long chatID;
    
    @DatabaseField(columnName = "achievement_id")
    private String achievementID;
    
    @DatabaseField(columnName = "achievement_name")
    private String achievementName;
    
    @DatabaseField(columnName = "achievement_description")
    private String description;
    
    public AchievementModel(long chatID, String achievementID, String achievementName, String description) {
        this.date = new Date(Calendar.getInstance().getTimeInMillis());
        this.chatID = chatID;
        this.achievementID = achievementID;
        this.achievementName = achievementName;
        this.description = description;
    }
    
    public AchievementModel(long chatID, Achievement achievement) {
        this.date = new Date(Calendar.getInstance().getTimeInMillis());
        this.chatID = chatID;
        this.achievementID = achievement.getId();
        this.achievementName = achievement.getName();
        this.description = achievement.getDescription();
    }
    
    public Achievement getAchievment() {
        return new Achievement(achievementName, description, achievementID);
    }
}
