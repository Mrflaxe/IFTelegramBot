package ru.mrflaxe.textadventure.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

public class HTMLSymbolFormatter {
    
    private final Map<String, String> replacers;
    
    public HTMLSymbolFormatter() {
        this.replacers = new HashMap<>();
        initializeReplacers();
    }
    
    public String formmat(String string) {
        
        List<Entry<String, String>> sets = replacers.entrySet().stream()
            .collect(Collectors.toList());
        
        for (int i = 0; i < sets.size(); i++) {
            Entry<String, String> set = sets.get(i);
            
            String tag = set.getKey();
            String replacer = set.getValue();
            
            string = string.replace(tag, replacer);
        }
        
        string = StringEscapeUtils.escapeHtml4(string);
        
        for (int i = 0; i < sets.size(); i++) {
            Entry<String, String> set = sets.get(i);
            
            String replacer = set.getValue();
            String tag = set.getKey();
            
            string = string.replace(replacer, tag);
        }
        
        return string;
    }
    
    private void initializeReplacers() {
        replacers.put("<b>", "#bold");
        replacers.put("</b>", "#/bold");
        replacers.put("<i>", "#italic");
        replacers.put("</i>", "#/italic");
        replacers.put("<code>", "#code");
        replacers.put("</code>", "#/code");
        replacers.put("<s>", "#strike");
        replacers.put("</s>", "#/strike");
        replacers.put("<u>", "#underline");
        replacers.put("</u>", "#/underline");
        
        replacers.put("—", "#dash");
    }
}
