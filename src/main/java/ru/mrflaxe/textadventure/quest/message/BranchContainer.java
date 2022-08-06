package ru.mrflaxe.textadventure.quest.message;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ru.mrflaxe.textadventure.achievement.Achievement;
import ru.mrflaxe.textadventure.achievement.AchievmentManager;
import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.configuration.ConfigurationSection;
import ru.mrflaxe.textadventure.quest.message.branch.AchievementBranch;
import ru.mrflaxe.textadventure.quest.message.branch.CommonBranch;
import ru.mrflaxe.textadventure.quest.message.branch.EndingAchievementBranch;
import ru.mrflaxe.textadventure.quest.message.branch.EndingBranch;
import ru.mrflaxe.textadventure.quest.message.branch.QuestBranch;

public class BranchContainer {

    private final AchievmentManager achievementManager;
    
    private final Map<String, QuestBranch> branches; // String is identificator;
    
    public BranchContainer(AchievmentManager achievmentManager) {
        this.achievementManager = achievmentManager;
        
        branches = new HashMap<>();
        initializeQuestBranches();
    }
    
    /**
     * Returns branch by given id if exist or null.
     * @param id of branch
     * @return branch by given id
     */
    @Nullable
    public QuestBranch getBranch(String id) {
        return branches.get(id);
    }
    
    private void initializeQuestBranches() {
        // Creating quest folder if not exist yet
        File questFolder = new File("configs" + File.separator + "quest");
        Path qusetFolderPath = questFolder.toPath();
        
        try {
            Files.createDirectories(qusetFolderPath);
        } catch (IOException e) {}
        
        
        // Creating demo configuration if not exist TODO add config setting to disable this thing
        Path questConfigPath = qusetFolderPath.resolve("quest.yml");
        
        // If quest file does not exist will create sample from resources
        if(!Files.isRegularFile(questConfigPath)) {
            InputStream resource = this.getClass()
                    .getResourceAsStream("/quest/quest.yml");
            
            if(resource == null) {
                System.err.println("resource is null");
            }
            
            try {
                Files.createFile(questConfigPath);
                Files.copy(resource, questConfigPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied a configuration file from internal resource to: " + questConfigPath);
            } catch (IOException e) {
                System.err.println("Failed to create " + questConfigPath + " file.");
            }
        }
        
        File[] files = questFolder.listFiles();
        
        // In quest folder can be more than one yaml files.
        // It's allow to avoid big files which hard to manage
        // TODO add possibility to group files in folders for better navigation.
        for (File file : files) {
            String fileName = file.getName();
            
            if(!fileName.endsWith(".yml")) {
                return;
            }
            
            Configuration questConfig = new Configuration(qusetFolderPath, fileName);
            questConfig.refresh();
            
            retrieveBranchesFromConfig(questConfig);
        }
        
        return;
    }
    
    
    private void retrieveBranchesFromConfig(Configuration questConfig) {
        Map<String, ConfigurationSection> questBranches = questConfig.getAllSubsections();
        
        // Each entry set is single branch
        // String is id of this branch and configuration section contains all other information
        questBranches.entrySet().stream().forEach(set -> {
            String branchId = set.getKey();
            ConfigurationSection branchSection = set.getValue();
            
            // Getting lines from config section
            List<String> lines = branchSection.getStringList("lines", true);
            
            // If branch has section ending means this branch doesn't have answer options.
            // Only EndingAchievementBranch and EndingBranch objects can not have answer options.
            if(branchSection.containsSection("ending")) {
                boolean ending = branchSection.getBoolean("ending");
                
                if(ending) {
                    // If ending has achievment will created EndingAchievementBranch object.
                    if(branchSection.containsSection("achievement")) {
                        String achievementID = branchSection.getString("achievement");
                        Achievement achievement = achievementManager.getAchievement(achievementID);
                        
                        QuestBranch achievementBranch = new EndingAchievementBranch(branchId, lines, achievement);
                        branches.put(branchId, achievementBranch);
                        return;
                    }
                    
                    // Else will created just EndingBranch
                    QuestBranch branchData = new EndingBranch(branchId, lines);
                    branches.put(branchId, branchData);
                    return;
                }
            }
            
            List<AnswerOption> answerOptions = getAnswerOptions(branchSection);
            
            // Now if common branch have achievement section will create AchievementBranch object
            if(branchSection.containsSection("achievement")) {
                String achievementID = branchSection.getString("achievement");
                Achievement achievement = achievementManager.getAchievement(achievementID);
                
                QuestBranch achievementBranch = new AchievementBranch(branchId, lines, answerOptions, achievement);
                branches.put(branchId, achievementBranch);
                return;
            }
            
            // Otherwise just CommonBranch
            QuestBranch branchData = new CommonBranch(branchId, lines, answerOptions);
            branches.put(branchId, branchData);
        });
    }
    
    private List<AnswerOption> getAnswerOptions(ConfigurationSection branchSection) {
        ConfigurationSection answerSection = branchSection.getSection("answer-options");
        Map<String, ConfigurationSection> subSections = answerSection.getAllSubSections();
        
        List<AnswerOption> answerOptions = new ArrayList<>();
        
        subSections.values().forEach(value -> {
            String text = value.getString("text", true);
            String nextBranchId = value.getString("link");
            
            AnswerOption answerOption = new AnswerOption(text, nextBranchId);
            answerOptions.add(answerOption);
        });
        
        return answerOptions;
    }
}
