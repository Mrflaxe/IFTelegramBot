package ru.mrflaxe.textadventure.quest.message.branch;

import java.util.List;

import lombok.Getter;
import ru.mrflaxe.textadventure.achievement.Achievement;
import ru.mrflaxe.textadventure.quest.message.AnswerOption;

@Getter
public class AchievementBranch extends QuestBranch implements ProvideAchievement, ProvideAnswers {

    private final List<AnswerOption> answerOptions;
    private final Achievement achievement;
    
    public AchievementBranch(String id, List<String> lines, List<AnswerOption> answerOptions, Achievement achievement) {
        super(id, lines);
        
        this.answerOptions = answerOptions;
        this.achievement = achievement;
    }

    @Override
    public AnswerOption getAnswerOption(int index) throws ArrayIndexOutOfBoundsException {
        return answerOptions.get(index);
    }
}
