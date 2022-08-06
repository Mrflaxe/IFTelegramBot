package ru.mrflaxe.textadventure.database.driver;

import java.sql.SQLException;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.db.MysqlDatabaseType;
import com.j256.ormlite.support.ConnectionSource;

import ru.mrflaxe.textadventure.configuration.Configuration;

public class MySQLDriver implements DatabaseDriver {
    
    private final String URL;
    private final String USER;
    private final String PASSWORD;
    
    public MySQLDriver(Configuration config) {
        String host = config.getString("database.host");
        int port = config.getInt("database.port");
        String name = config.getString("database.name");
        
        this.URL = "jdbc:mysql://" + host + ":" + port + "/" + name + "?useUnicode=true&serverTimezone=UTC";
        this.USER = config.getString("database.user");
        this.PASSWORD = config.getString("database.password");
    }
    
    
    @Override
    public ConnectionSource getConnection() {
        if(URL == null || URL.isEmpty()) {
            System.err.println("URL is null or empty");
        }
        
        if(USER == null || USER.isEmpty()) {
            System.err.println("USER is null or empty");
        }
        
        if(PASSWORD == null || PASSWORD.isEmpty()) {
            System.err.println("PASSWORD is null or empty");
        }
        
        DatabaseType databaseType = new MysqlDatabaseType();
        databaseType.loadDriver();
        
        try {
            ConnectionSource connection = new JdbcConnectionSource(URL, USER, PASSWORD, databaseType);
            
            return connection;
        } catch (SQLException e) {
            System.err.println("Failed to establish connection to MySQL database");
            return null;
        }
    }
}
