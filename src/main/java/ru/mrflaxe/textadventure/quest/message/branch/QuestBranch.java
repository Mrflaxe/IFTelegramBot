package ru.mrflaxe.textadventure.quest.message.branch;

import java.util.List;

import lombok.Getter;

@Getter
public abstract class QuestBranch {

    private final String id;
    
    private final List<String> lines;
    
    public QuestBranch(String id, List<String> lines) {
        this.id = id;
        this.lines = lines;
    }
}
