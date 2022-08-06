package ru.mrflaxe.textadventure.database.driver;

import com.j256.ormlite.support.ConnectionSource;

public interface DatabaseDriver {
    
    ConnectionSource getConnection();
}
