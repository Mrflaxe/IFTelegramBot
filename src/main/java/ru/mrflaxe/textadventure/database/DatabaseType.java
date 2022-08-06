package ru.mrflaxe.textadventure.database;

import lombok.Getter;
import ru.mrflaxe.textadventure.database.driver.MySQLDriver;
import ru.mrflaxe.textadventure.database.driver.SQLiteDriver;

public enum DatabaseType {
    
    MYSQL("my_sql", MySQLDriver.class),
    SQLITE("sqlite", SQLiteDriver.class);
    
    @Getter
    private final String type;
    
    @Getter
    private final Class<?> driverClass;
    
    private DatabaseType(String type, Class<?> driverClass) {
        this.type = type;
        this.driverClass = driverClass;
    }
}
