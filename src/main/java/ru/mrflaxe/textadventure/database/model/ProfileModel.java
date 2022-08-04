package ru.mrflaxe.textadventure.database.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@DatabaseTable(tableName = "telegrambot_profiles")
public class ProfileModel {

    @DatabaseField(id = true, columnName = "chat_id")
    private long chatId;
    
    @Setter
    @DatabaseField(columnName = "user_name")
    private String name;
    
    @DatabaseField(columnName = "saves_id")
    private String saveID;
    
    public ProfileModel(long chatID, String userName, String saveID) {
        this.chatId = chatID;
        this.name = userName;
        this.saveID = saveID;
    }
}
