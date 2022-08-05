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
        initializeTextSections();
    }
    
    @Nullable
    public QuestBranch getBranch(String id) {
        return branches.get(id);
    }
    
    private void initializeTextSections() {
        // Creating quest folder if not exist yet
        File questFolder = new File("configs" + File.separator + "quest");
        Path qusetFolderPath = questFolder.toPath();
        
        try {
            Files.createDirectories(qusetFolderPath);
        } catch (IOException e) {}
        
        
        // Creating demo configuration if not exist TODO add config setting to disable this thing
        Path demoConfigPath = qusetFolderPath.resolve("quest.yml");
        
        if(!Files.isRegularFile(demoConfigPath)) {
            InputStream resource = this.getClass()
                    .getResourceAsStream("/quest/quest.yml");
            
            if(resource == null) {
                System.err.println("resource is null");
            }
            
            try {
                Files.createFile(demoConfigPath);
                Files.copy(resource, demoConfigPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied a configuration file from internal resource to: " + demoConfigPath);
            } catch (IOException e) {
                System.err.println("Failed to create " + demoConfigPath + " file.");
            }
        }
        
        File[] files = questFolder.listFiles();
        
        for (File file : files) {
            String fileName = file.getName();
            
            if(!fileName.endsWith(".yml")) {
                return;
            }
            
            Configuration questConfig = new Configuration(qusetFolderPath, fileName);
            questConfig.refresh();
            
            readQuestConfig(questConfig);
        }
        
        return;
    }
    
    private void readQuestConfig(Configuration questConfig) {
        Map<String, ConfigurationSection> questBranches = questConfig.getAllSubsections();
        
        questBranches.entrySet().stream().forEach(set -> {
            String branchId = set.getKey();
            ConfigurationSection branchSection = set.getValue();
            
            List<String> lines = branchSection.getStringList("lines", true);
            
            // If branch has section ending means this branch doesn't have
            // answer options.
            if(branchSection.containsSection("ending")) {
                boolean ending = branchSection.getBoolean("ending");
                
                if(ending) {
                    // If ending has achievment
                    if(branchSection.containsSection("achievement")) {
                        String achievementID = branchSection.getString("achievement");
                        Achievement achievement = achievementManager.getAchievement(achievementID);
                        
                        QuestBranch achievementBranch = new EndingAchievementBranch(branchId, lines, achievement);
                        branches.put(branchId, achievementBranch);
                        return;
                    }
                    
                    QuestBranch branchData = new EndingBranch(branchId, lines);
                    branches.put(branchId, branchData);
                    return;
                }
            }
            
            List<AnswerOption> answerOptions = getAnswerOptions(branchSection);
            
            if(branchSection.containsSection("achievement")) {
                String achievementID = branchSection.getString("achievement");
                Achievement achievement = achievementManager.getAchievement(achievementID);
                
                QuestBranch achievementBranch = new AchievementBranch(branchId, lines, answerOptions, achievement);
                branches.put(branchId, achievementBranch);
                return;
            }
            
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
