package ru.mrflaxe.textadventure.database;

import java.sql.SQLException;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.db.MysqlDatabaseType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import lombok.Getter;
import ru.mrflaxe.textadventure.configuration.Configuration;

public class Database {

    private final String URL;
    private final String USER;
    private final String PASSWORD;
    
    @Getter
    private ConnectionSource connection;
    
    public Database(Configuration config) {
        String host = config.getString("database.host");
        int port = config.getInt("database.port");
        String name = config.getString("database.name");
        
        this.URL = "jdbc:mysql://" + host + ":" + port + "/" + name + "?useUnicode=true&serverTimezone=UTC";
        this.USER = config.getString("database.user");
        this.PASSWORD = config.getString("database.password");
    }
    
    public void establishConnection() {
        
        if(URL == null || URL.isEmpty()) {
            System.err.println("URL is null or empty");
        }
        
        if(USER == null || USER.isEmpty()) {
            System.err.println("USER is null or empty");
        }
        
        if(PASSWORD == null || PASSWORD.isEmpty()) {
            System.err.println("PASSWORD is null or empty");
        }
        
        System.out.println("");
        System.out.println("Trying to establish database connection.");
        System.out.println("Connection URL = " + URL);
        
        DatabaseType databaseType = new MysqlDatabaseType();
        databaseType.loadDriver();
        
        try {
            this.connection = new JdbcConnectionSource(URL, USER, PASSWORD, databaseType);
            System.out.println("Connection established!");
            System.out.println("");
        } catch (SQLException e) {
            System.err.println("Failed to establish connection to database");
        }
    }
    
    public Database createTable(Class<?> daoClass) {
        try {
            TableUtils.createTableIfNotExists(connection, daoClass);
            return this;
        } catch (SQLException e) {
            System.err.println("Failed to create table: " + e.getMessage());
            return this;
        }
    }
}
