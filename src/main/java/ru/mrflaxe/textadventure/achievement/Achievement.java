package ru.mrflaxe.textadventure.achievement;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Achievement {

    @Getter
    private final String id;
    
    @Getter
    private final String name;
    
    @Getter
    private final String description;
    
    public String getText() {
        // Maybe set here some pattern for comfortable config changes
        return name + "\n" + description;
    }
}
