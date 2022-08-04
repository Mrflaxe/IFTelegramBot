package ru.mrflaxe.textadventure.quest.message.branch;

import java.util.List;

public class EndingBranch extends QuestBranch implements Ending {

    public EndingBranch(String id, List<String> lines) {
        super(id, lines);
        
    }

}
