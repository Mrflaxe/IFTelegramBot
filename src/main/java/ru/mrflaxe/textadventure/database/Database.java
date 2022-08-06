package ru.mrflaxe.textadventure.database;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import lombok.Getter;
import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.database.driver.DatabaseDriver;

public class Database {

    private final Configuration config;
    
    @Getter
    private ConnectionSource connection;
    
    public Database(Configuration config) {
        this.config = config;
    }
    
    public void establishConnection() {
        System.out.println("");
        System.out.println("Trying to establish database connection.");
        
        this.connection = getDriver().getConnection();
        
        if(connection == null) {
            return;
        }
        
        System.out.println("Connection established!");
        System.out.println("");
    }
    
    private DatabaseDriver getDriver() {
        String type = config.getString("database.type").toLowerCase();
        
        DatabaseType[] values = DatabaseType.values();
        
        for (int i = 0; i < values.length; i++) {
            String typeExample = values[i].getType();
            if(!type.equals(typeExample)) {
                continue;
            }
            
            Class<?> driverClass = values[i].getDriverClass();
            
            try {
                Constructor<?> driverConstructor = driverClass.getConstructor(Configuration.class);
                return (DatabaseDriver) driverConstructor.newInstance(config);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        
        return null;
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
