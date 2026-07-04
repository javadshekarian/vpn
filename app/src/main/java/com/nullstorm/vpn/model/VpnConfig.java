package com.nullstorm.vpn.model;

import com.nullstorm.vpn.parser.dto.entities.ProfileItem;

public class VpnConfig {
    private final String name;
    private final String content;
    private ProfileItem profile;

    public VpnConfig(String name, String content, ProfileItem profile) {
        this.name = name;
        this.content = content;
        this.profile = profile;
    }

    public String getName(){
        return name;
    }
    public String getContent(){
        return content;
    }
    public ProfileItem getProfile() { return profile; };
}