package ru.mrflaxe.textadventure;

import java.sql.SQLException;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.console.ConsoleRequestHandler;
import ru.mrflaxe.textadventure.database.Database;
import ru.mrflaxe.textadventure.database.DatabaseManager;
import ru.mrflaxe.textadventure.database.model.AchievementModel;
import ru.mrflaxe.textadventure.database.model.ProfileModel;
import ru.mrflaxe.textadventure.database.model.SaveModel;

public class Main {
    
    public static void main(String[] args) {
        Configuration messages = new Configuration("messages.yml");
        Configuration config = new Configuration("config.yml");
        Configuration achievements = new Configuration("achievements.yml");
        
        Database database = new Database(config);
        initializeDatabase(database);
        
        DatabaseManager databaseManager = null;
        
        try {
            databaseManager = new DatabaseManager(database.getConnection());
        } catch (SQLException e) {
            System.err.println("Failed to create DatabaseManager");
        }
        
        MyBot bot = new MyBot(messages, config, achievements, databaseManager);
        bot.launch();
        
        new ConsoleRequestHandler(bot).run();
    }
    
    private static void initializeDatabase(Database database) {
        database.establishConnection();
        
        database.createTable(ProfileModel.class);
        database.createTable(SaveModel.class);
        database.createTable(AchievementModel.class);
    }
}
