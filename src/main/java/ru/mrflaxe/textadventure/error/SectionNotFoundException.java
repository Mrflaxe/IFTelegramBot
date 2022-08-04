package ru.mrflaxe.textadventure.error;

public class SectionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4812707496963430542L;

    public SectionNotFoundException(String sectionName, String fileName) {
        super("Can't find section " + sectionName + " in configuration '" + fileName + "'");
    }

}
