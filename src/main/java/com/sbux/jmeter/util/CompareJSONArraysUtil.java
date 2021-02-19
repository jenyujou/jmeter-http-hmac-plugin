package com.sbux.jmeter.util;

import java.util.HashSet;
import java.util.Set;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class CompareJSONArraysUtil {
    public static Set<JsonElement> setOfElements(JsonArray arr) { 
        Set<JsonElement> set = new HashSet<JsonElement>(); 
        for (JsonElement j: arr) { 
            set.add(j); 
        } 
        return set; 
    }
}
