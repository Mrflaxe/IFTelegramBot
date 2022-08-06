package ru.mrflaxe.textadventure.database.driver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.db.SqliteDatabaseType;
import com.j256.ormlite.support.ConnectionSource;

import ru.mrflaxe.textadventure.configuration.Configuration;

public class SQLiteDriver implements DatabaseDriver {
    
    private final String URL;
    
    public SQLiteDriver(Configuration config) {
        this.URL = getURL(config);
    }
    
    @Override
    public ConnectionSource getConnection() {
        DatabaseType databaseType = new SqliteDatabaseType();
        databaseType.loadDriver();
        
        try {
            ConnectionSource connection = new JdbcConnectionSource(URL, databaseType);
            
            return connection;
        } catch (SQLException e) {
            System.err.println("Failed to establish connection to SQLite database");
            return null;
        }
    }
    
    private String getURL(Configuration config) {
        String fileName = config.getString("database.file-name");
        Path currentRelativePath = Paths.get("");
        String path = currentRelativePath.toAbsolutePath().toString();
        
        String URL = "jdbc:sqlite:" + path + "/" + fileName;
        
        if(fileName == null || fileName.isEmpty()) {
            System.err.println("Failed to load SQLite driver: 'file-name' missing");
            return null;
        }
        
        return URL;
    }
    
}
