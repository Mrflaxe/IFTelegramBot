package ru.mrflaxe.textadventure.configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;

import lombok.Getter;
import ru.mrflaxe.textadventure.error.SectionNotFoundException;
import ru.mrflaxe.textadventure.tool.HTMLSymbolFormatter;

public class ConfigurationSection {

    @Getter
    private final String fileName;
    
    @Getter
    private final Path filePath;
    
    @Getter
    // Path to this section from main section
    private final String sectionPath;
    
    @Getter
    // Name of this section
    private final String name;
    
    private final Object containedData;
    
    private HTMLSymbolFormatter formatter;
    
    public ConfigurationSection(String fileName, Path filePath, String sectionPath, String sectionName, Object data) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.sectionPath = sectionPath;
        this.name = sectionName;
        this.containedData = data;
        
        this.formatter = new HTMLSymbolFormatter();
    }

    /**
     * Gives ConfigurationSection if exist from giving section path
     * @param sectionPath path
     * @return ConfigurationSection section
     */
    public ConfigurationSection getSection(String sectionPath) throws SectionNotFoundException {
        return getSection(sectionPath, false);
    }
    
    public ConfigurationSection getSection(String sectionPath, boolean sneakyThrows) {
        Map<String, ConfigurationSection> content = getAllSubSections();
        
        String[] sections = sectionPath.split("\\.");
        
        if(sections.length == 1) { // If length is 1 means no more other subSections.
            return content.get(sections[0]);
        }
        
        ConfigurationSection subsection = content.get(sections[0]);
        
        if(subsection == null) {
            if(!sneakyThrows) {
                throw new SectionNotFoundException(sections[0], fileName);
            }
            
            return null;
        }

        return subsection.getSection(sectionPath.replaceFirst(sections[0], "").replaceFirst(".", ""), sneakyThrows);
    }
    
    /**
     * Checks if this section contains given subsection
     * @param sectionPath - path to subsection
     * @return true if contains otherwise false
     */
    public boolean containsSection(String sectionPath) {
        return getSection(sectionPath, true) == null ? false : true;
    }
    
    public Map<String, ConfigurationSection> getAllSubSections() {
        Yaml yaml = new Yaml();
        HashMap<String, Object> parsedData;
        
        try {
            String dumped = yaml.dump(containedData);
            
            parsedData = yaml.load(dumped);
        } catch (ClassCastException exception) {
            System.out.println("class cast exception");
            return null; // In this case data is not another section so I just return null
        }
        
        HashMap<String, ConfigurationSection> content = new HashMap<>();
        
        parsedData.entrySet().forEach(set -> {
            String sectionName = set.getKey();
            Object data = set.getValue();
            
            ConfigurationSection configurationSection = new ConfigurationSection(fileName, filePath, sectionPath + "." + sectionName, sectionName, data);
            content.put(sectionName, configurationSection);
        });
        
        return content;
    }
    
    /**
     * Gets int value from current section
     * @return int value
     */
    public int getInt() {
        return getInt("");
    }
    
    /**
     * Gets int value from given section
     * @param sectionPath - section contains value
     * @return int value
     */
    public int getInt(String sectionPath) {
        
        // If sectionPath param is empty just return a contained value if exist
        // sectionPath param will be empty if the current section is a section with the necessary data.
        if(sectionPath.isEmpty()) {
            try { // checks if current section does have any value and the value is not int
                int data = (Integer) containedData;
                return data;
            } catch (ClassCastException | NumberFormatException exception) {
                return -1;
            }
        }
        
        ConfigurationSection subsection = getSection(sectionPath);
        
        if(subsection == null) {
            throw new SectionNotFoundException(sectionPath, fileName);
        }
        
        return subsection.getInt();
    }
    
    public String getString(boolean specialSymbolsFormatting) {
        return getString("", specialSymbolsFormatting);
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
    public String getString(String sectionPath, boolean specialSymbolsFormatting) {
        if(sectionPath.isEmpty()) {
            try {
                String data = (String) containedData;
                
                if(specialSymbolsFormatting) {
                    data = formatter.formmat(data);
                }
                
                return data;
            } catch (ClassCastException exception) {
                return "config_error";
            }
        }
        
        ConfigurationSection subsection = getSection(sectionPath);
        
        if(subsection == null) {
            throw new SectionNotFoundException(sectionPath, fileName);
        }
        
        return subsection.getString(specialSymbolsFormatting);
    }
    
    /**
     * Gets string value from given section. <br>
     * @param sectionPath
     * @return string value from given section
     */
    public String getString(String sectionPath) {
        return getString(sectionPath, false);
    }
    
    /**
     * Gets list of string value from current section. <br>
     * If specialSymbolsFormatting is true gotten string will be formatted for html parsing.
     * For example if user writes: <br>
     * "{@code <b><foo>&bar</b>}" <br>
     * this method will return next string: <br>
     * "{@code <b>&lt;foo&gt;&#38;bar</b>}" <br>
     * @param specialSymbolsFormatting
     * @return list of string from given section
     */
    public List<String> getStringList(boolean specialSymbolsFormatting) {
        return getStringList("", specialSymbolsFormatting);
    }
    
    /**
     * Gets list of string value from given section. <br>
     * If specialSymbolsFormatting is true gotten string will be formatted for html parsing.
     * For example if user writes: <br>
     * "{@code <b><foo>&bar</b>}" <br>
     * this method will return next string: <br>
     * "{@code <b>&lt;foo&gt;&#38;bar</b>}" <br>
     * 
     * @param section - section contains value
     * @param specialSymbolsFormatting
     * @return list of string from given section
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String sectionPath, boolean specialSymbolsFormatting) {
        if(sectionPath.isEmpty()) {
            try {
                Collection<Object> collection = (Collection<Object>) containedData;
                
                if(specialSymbolsFormatting) {
                    return collection.stream()
                            .map(object -> formatter.formmat((String) object))
                            .collect(Collectors.toList());
                }
                
                return collection.stream()
                        .map(object -> (String) object)
                        .collect(Collectors.toList());
                
            } catch (ClassCastException exception) {
                List<String> list = new ArrayList<>();
                list.add("config_error");
                return list;
            }
        }
        
        ConfigurationSection subsection = getSection(sectionPath);
        
        if(subsection == null) {
            throw new SectionNotFoundException(sectionPath, fileName);
        }
        
        return subsection.getStringList(specialSymbolsFormatting);
    }
    
    /**
     * Gets list of string value from current section.
     * @param sectionPath
     * @return list of string from given section
     */
    public List<String> getStringList(String sectionPath) {
        return getStringList(sectionPath, false);
    }
    
    /**
     * Gets boolean value from current section
     * @return boolean value contained in given section
     */
    public boolean getBoolean() {
        return getBoolean("");
    }
    
    /**
     * Gets boolean value from given section
     * @param section - section contains value
     * @return boolean value contained in given section
     */
    public boolean getBoolean(String sectionPath) {
        if(sectionPath.isEmpty()) {
            try {
                boolean data = (Boolean) containedData;
                return data;
            } catch (ClassCastException exception) {
                return false;
            }
        }
        
        ConfigurationSection subsection = getSection(sectionPath);
        
        if(subsection == null) {
            throw new SectionNotFoundException(sectionPath, fileName);
        }
        
        return subsection.getBoolean();
    }
}