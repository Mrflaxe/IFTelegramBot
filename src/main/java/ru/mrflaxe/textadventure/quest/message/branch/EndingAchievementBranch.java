package ru.mrflaxe.textadventure.quest.message.branch;

import java.util.List;

import lombok.Getter;
import ru.mrflaxe.textadventure.achievement.Achievement;

@Getter
public class EndingAchievementBranch extends QuestBranch implements Ending, ProvideAchievement {

    private final Achievement achievement;
    
    public EndingAchievementBranch(String id, List<String> lines, Achievement achievement) {
        super(id, lines);
        
        this.achievement = achievement;
    }
    
}
