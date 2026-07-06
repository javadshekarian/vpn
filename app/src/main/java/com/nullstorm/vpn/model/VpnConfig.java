package com.nullstorm.vpn.model;

import com.nullstorm.vpn.parser.dto.entities.ProfileItem;

public class VpnConfig {
    private final String name;
    private final String content;
    private final String guid;
    private ProfileItem profile;

    public VpnConfig(String name, String content, String guid, ProfileItem profile) {
        this.name = name;
        this.content = content;
        this.guid = guid;
        this.profile = profile;
    }

    public String getName(){
        return name;
    }
    public String getContent(){
        return content;
    }
    public String getGuid(){ return guid; }
    public ProfileItem getProfile() { return profile; };
}