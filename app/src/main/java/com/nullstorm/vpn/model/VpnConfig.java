package com.nullstorm.vpn.model;

public class VpnConfig {
    private final String name;
    private final String content;

    public VpnConfig(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName(){
        return name;
    }

    public String getContent(){
        return content;
    }
}