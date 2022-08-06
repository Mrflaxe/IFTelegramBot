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

    // Name of yaml file
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
        // Getting config file
        File configFile = configFolder.resolve(this.fileName).toFile();
        InputStream input = new FileInputStream(configFile);
        
        Yaml yaml = new Yaml();
        
        // transfering data form yaml file to hashMap object
        HashMap<String, Object> yamlContent = yaml.load(input);
        HashMap<String, ConfigurationSection> content = new HashMap<>();
        
        if(yamlContent == null || yamlContent.isEmpty()) {
            return null;
        }
        
        // Creating configuration sections
        yamlContent.entrySet().forEach(set -> {
            String sectionName = set.getKey();
            // data is any value goes after the announcement of the section.
            // This data can be either another section, or some string, boolean or integer
            Object data = set.getValue();
            
            // ConfigurationSection like common Configuration class but has a lot more features for work
            ConfigurationSection section = new ConfigurationSection(fileName, configFolder, sectionName, sectionName, data);
            content.put(sectionName, section);
        });
        
        return content;
    }
    
    /**
     * Loads data from configuration file to this object.
     * If file of this configuration does not exist, this method will create one by resource sample
     */
    public void refresh() {
        // If 'configs' folder does not exist creates new one.
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
        
        // If config file does not exist creates new one by resource sample
        if(!Files.isRegularFile(config)) {
            InputStream resource = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream(this.fileName);
            
            try {
                Files.createFile(config);
                Files.copy(resource, config, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied a configuration file data from internal resource to: " + config);
            } catch (IOException e) {
                System.err.println("Failed to create " + fileName + " file.");
                return;
            }
        }
        
        // Now loads content of configuration data to this object.
        try {
            this.content = getContent();
            System.out.println(fileName + " config reloaded.");
        } catch (FileNotFoundException e) {
            System.err.println("Can't read " + config.toString() + " file");
            return;
        }
        
    }
    
    /**
     * Gets ConfigurationSection by given section path
     * 
     * @param sectionPath - path to section
     * @return ConfigurationSection by given path
     */
    public ConfigurationSection getSection(String sectionPath) {
        if(sectionPath == null || sectionPath.isEmpty()) {
            return null;
        }
        
        // Splits the path to array of sections names
        String[] sections = sectionPath.split("\\.");
        
        // If this array have only one section name, we can get this section from this object's content
        if(sections.length == 1) {
            return content.get(sections[0]);
        }
        
        // Otherwise gets requested section from subsection
        ConfigurationSection currentSection = content.get(sections[0]);
        // #replaceFirst() is necessary to avoid replacing similar string regex in sectionPath
        String updatedSectionPath = sectionPath.replaceFirst(sections[0], "");
        
        return currentSection.getSection(updatedSectionPath);
    }
    
    /**
     * Gets all main sections from this configuration.
     * Those sections don't have parent section.
     * @return Map<String, ConfigurationSection> sections
     */
    public Map<String, ConfigurationSection> getAllSubsections() {
        return getAllSubsections("");
    }
    
    /**
     * Gets all subsections from given section.
     * 
     * @param parentSectionPath - path to section where collect subsections
     * @return Map<String, ConfigurationSection> sections
     */
    public Map<String, ConfigurationSection> getAllSubsections(String parentSectionPath) {
        if(parentSectionPath == null) {
            return null;
        }
        
        if(parentSectionPath.equals("")) {
            return content;
        }
        
        String[] sections = parentSectionPath.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        // There necessary to replace first dot cause after deleting main section name
        // the dot remains.
        // If don't replace this dot result path will
        String updatedSectionPath = parentSectionPath.replaceFirst(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getSection(updatedSectionPath).getAllSubSections();
    }
    
    /**
     * Gets int value from given section
     * @param section - section contains value
     * @return int value
     */
    public int getInt(String section) {
        String[] sections = section.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        // If somewhere I made mistake with section path I will notified about it right here.
        // This also works if user accidentally renamed any section in config file.
        if(currentSection == null) {
            throw new SectionNotFoundException(section, fileName);
        }
        
        String updatedSectionPath = section.replaceFirst(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getInt(updatedSectionPath);
    }
    
    /**
     * Gets string value from given section. <br>
     * If specialSymbolsFormatting is true gotten string will be formatted for html parsing.
     * For example if user writes: <br>
     * "{@code <b><foo>&bar</b>}" <br>
     * this method will return: <br>
     * "{@code <b>&lt;foo&gt;&#38;bar</b>}" <br>
     * 
     * @param section - section contains value
     * @param specialSymbolsFormatting
     * @return formatted string value
     */
    public String getString(String section, boolean specialSymbolsFormatting) {
        String[] sections = section.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        if(currentSection == null) {
            throw new SectionNotFoundException(section, fileName);
        }
        
        String updatedSectionPath = section.replaceFirst(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getString(updatedSectionPath, specialSymbolsFormatting);
    }
    /**
     * Gets string value from given section.
     * @param section - section contains value
     * @return string value
     */
    public String getString(String section) {
        return getString(section, false);
    }
    
    /**
     * Gets list of string value from given section. <br>
     * If specialSymbolsFormatting is true gotten string will be formatted for html parsing.
     * For example if user writes: <br>
     * "{@code <b><foo>&bar</b>}" <br>
     * this method will return: <br>
     * "{@code <b>&lt;foo&gt;&#38;bar</b>}" <br>
     * 
     * @param section - section contains value
     * @param specialSymbolsFormatting
     * @return list of string from given section
     */
    public List<String> getStringList(String section, boolean specialSymbolsFormatting) {
        String[] sections = section.split("\\.");
        ConfigurationSection currentSection = content.get(sections[0]);
        
        if(currentSection == null) {
            throw new SectionNotFoundException(section, fileName);
        }
        
        String updatedSectionPath = section.replaceFirst(sections[0], "").replaceFirst(".", "");
        
        return currentSection.getStringList(updatedSectionPath);
    }
    
    /**
     * Gets list of sting value from given section.
     * @param section - section contains value
     * @return list of string from given section
     */
    public List<String> getStringList(String section) {
        return getStringList(section, false);
    }
    
    /**
     * Gets boolean value from given section
     * @param section - section contains value
     * @return boolean value contained in given section
     */
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
