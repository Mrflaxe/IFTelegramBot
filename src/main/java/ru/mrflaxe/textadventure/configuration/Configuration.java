package ru.mrflaxe.textadventure.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import lombok.Setter;
import ru.mrflaxe.textadventure.error.SectionNotFoundException;

public class Configuration {

    private final String fileName;
    
    @Setter
    private Path configFolder;
    private HashMap<String, ConfigurationSection> content;
    
    public Configuration(Path folderPath, String fileName) {
        this.fileName = fileName;
        this.configFolder = folderPath;
    }
    
    public Configuration(String fileName) {
        this.configFolder = getDefaultConfigFolder();
        this.fileName = fileName;

        refresh();
    }
    
    private Path getDefaultConfigFolder() {
        File configFolder = new File("configs");
        
        try {
            Files.createDirectories(configFolder.toPath());
        } catch (IOException e) {}
        
        return configFolder.toPath();
    }
    
    private HashMap<String, ConfigurationSection> getContent() throws FileNotFoundException {
        File configFile = configFolder.resolve(this.fileName).toFile();
        InputStream input = new FileInputStream(configFile);
        
        Yaml yaml = new Yaml();
        
        HashMap<String, Object> yamlContent = yaml.load(input);
        HashMap<String, ConfigurationSection> content = new HashMap<>();
        
        if(yamlContent == null || yamlContent.isEmpty()) {
            return null;
        }
        
        yamlContent.entrySet().forEach(set -> {
            String sectionName = set.getKey();
            Object data = set.getValue();
            
            ConfigurationSection section = new ConfigurationSection(fileName, configFolder, sectionName, sectionName, data);
            content.put(sectionName, section);
        });
        
        return content;
    }
    
    public void refresh() {
        if(!Files.isDirectory(configFolder)) {
            try {
                Files.createFile(configFolder);
                System.out.println("Created a new data folder in: " + configFolder);
            } catch (IOException ex) {
                System.err.println("Couldn't create a data folder in " + configFolder + "!");
                return;
            }

        }
        
        Path config = configFolder.resolve(this.fileName);
        
        if(!Files.isRegularFile(config)) {
            InputStream resource = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream(this.fileName);
            
            try {
                Files.createFile(config);
                Files.copy(resource, config, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied a configuration file from internal resource to: " + config);
            } catch (IOException e) {
                System.err.println("Failed to create " + fileName + " file.");
                return;
            }
        }
        
        try {
            this.content = getContent();
            System.out.println(fileName + " config reloaded.");
        } catch (FileNotFoundException e) {
            System.err.println("Can't read " + config.toString() + " file");
            return;
        }
        
    }
    
    public ConfigurationSection getSection(String section) {
        if(section == null || section.isEmpty()) {
            return null;
        }
        
        String[] sections = section.split("\\.");
        
        if(sections.length == 1) {
            return content.get(sections[0]);
        }
        
        ConfigurationSection currentSection = content.get(sections[0]);
        String updatedSectionPath = section.replace(sections[0], "");
        
        return currentSection.getSection(updatedSectionPath);
    }
    
    public Map<String, ConfigurationSection> getAllSubsections() {
        return getAllSubsections("");
    }
    
    public Map<String, ConfigurationSection> getAllSubsections(String parentSectionPath) {
        if(parentSectionPath == null) {
            return null;
        }
        
        if(parentSectionPath.equals("")) {
            return content;
        }
        
        String[] sections = parentSectionPath.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        String updatedSectionPath = parentSectionPath.replace(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getSection(updatedSectionPath).getAllSubSections();
    }
    
    public int getInt(String section) {
        String[] sections = section.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        if(currentSection == null) {
            throw new SectionNotFoundException(section, fileName);
        }
        
        String updatedSectionPath = section.replace(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getInt(updatedSectionPath);
    }
    
    public String getString(String section) {
        String[] sections = section.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        if(currentSection == null) {
            throw new SectionNotFoundException(section, fileName);
        }
        
        String updatedSectionPath = section.replace(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getString(updatedSectionPath);
    }
    
    public List<String> getStringList(String section) {
        String[] sections = section.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        if(currentSection == null) {
            throw new SectionNotFoundException(section, fileName);
        }
        
        String updatedSectionPath = section.replace(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getStringList(updatedSectionPath);
    }
    
    public boolean getBoolean(String section) {
        String[] sections = section.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        if(currentSection == null) {
            throw new SectionNotFoundException(section, fileName);
        }
        
        String updatedSectionPath = section.replace(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getBoolean(updatedSectionPath);
    }
}
