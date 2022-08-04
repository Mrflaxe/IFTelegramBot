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
import lombok.RequiredArgsConstructor;
import ru.mrflaxe.textadventure.error.SectionNotFoundException;

@RequiredArgsConstructor
public class ConfigurationSection {

    @Getter
    private final String fileName;
    
    @Getter
    private final Path filePath;
    
    @Getter
    private final String sectionPath;
    
    @Getter
    private final String name;
    
    private final Object containedData;
    
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
                throw new SectionNotFoundException(sectionPath, fileName);
            }
            
            return null;
        }

        return subsection.getSection(sectionPath.replace(sections[0], "").replaceFirst(".", ""), sneakyThrows);
    }
    
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
    
    public int getInt() {
        return getInt("");
    }
    
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
    
    public String getString() {
        return getString("");
    }
    
    public String getString(String sectionPath) {
        if(sectionPath.isEmpty()) {
            try {
                String data = (String) containedData;
                return data;
            } catch (ClassCastException exception) {
                return "config_error";
            }
        }
        
        ConfigurationSection subsection = getSection(sectionPath);
        
        if(subsection == null) {
            throw new SectionNotFoundException(sectionPath, fileName);
        }
        
        return subsection.getString();
    }
    
    public List<String> getStringList() {
        return getStringList("");
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String sectionPath) {
        if(sectionPath.isEmpty()) {
            try {
                Collection<Object> collection = (Collection<Object>) containedData;
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
        
        return subsection.getStringList();
    }
    
    public boolean getBoolean() {
        return getBoolean("");
    }
    
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