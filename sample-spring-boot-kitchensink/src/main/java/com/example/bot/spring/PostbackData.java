package com.example.bot.spring;

import java.util.HashMap;
import java.util.Map;

public class PostbackData {
    private Map<String, String> dataMap = new HashMap<>();

    public PostbackData(String[] data) {
        if(data.length % 2 == 0){
            for(int index = 0; index < data.length; index+=2){
                dataMap.put(data[index], data[index+1]);
            }
        }
    }

    public String getMapping(String key) {
        if (dataMap.containsKey(key)){
            return dataMap.get(key);
        }
        else return null;
    }
}
