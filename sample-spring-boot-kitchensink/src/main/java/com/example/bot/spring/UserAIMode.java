package com.example.bot.spring;

import java.util.HashMap;

public class UserAIMode {
    public static UserAIMode getAIMode() {
        return AIMode;
    }

    private static UserAIMode AIMode = new UserAIMode();
    private HashMap<String, String> AIMap = new HashMap<>();

    public void addAIMap(String userId, String mode) {
        AIMap.put(userId, mode);
    }

    public void removeAIMap(String userId) {
        AIMap.remove(userId);
    }

    public String checkAImap(String userId){
        if(AIMap.containsKey(userId)) {
            return AIMap.get(userId);
        }
        else return null;
    }

}
