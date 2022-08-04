package ru.mrflaxe.textadventure.quest.message.branch;

import java.util.List;

import ru.mrflaxe.textadventure.quest.message.AnswerOption;

public interface ProvideAnswers {

    List<AnswerOption> getAnswerOptions();
    
    AnswerOption getAnswerOption(int index);
}
