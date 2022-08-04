package ru.mrflaxe.textadventure.database.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@DatabaseTable(tableName = "telegrambot_profiles_saves")
public class SaveModel {

    @DatabaseField(generatedId = true, columnName = "id")
    private int id;
    
    @Setter
    @DatabaseField(columnName = "last_branch_id")
    private String lastBranchID;
}
