package ru.mrflaxe.textadventure.quest.message;

import org.jetbrains.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnswerOption {

    @Nullable
    private final String text;
    
    @Nullable
    private final String nextBranchID; // it's like a link to next dialogue line
}
