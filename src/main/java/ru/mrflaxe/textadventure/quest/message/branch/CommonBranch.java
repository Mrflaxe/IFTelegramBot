package ru.mrflaxe.textadventure.quest.message.branch;

import java.util.List;

import lombok.Getter;
import ru.mrflaxe.textadventure.quest.message.AnswerOption;

@Getter
public class CommonBranch extends QuestBranch implements ProvideAnswers {
    
    private final List<AnswerOption> answerOptions;
    
    public CommonBranch(String id, List<String> lines, List<AnswerOption> answerOptions) {
        super(id, lines);
        
        this.answerOptions = answerOptions;
    }
    
    public AnswerOption getAnswerOption(int index) throws ArrayIndexOutOfBoundsException {
        return answerOptions.get(index);
    }
}
