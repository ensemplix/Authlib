package com.mojang.authlib.yggdrasil.response;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import java.util.Map;
import java.util.UUID;

public class MinecraftTexturesPayload {
    private long timestamp;
    private UUID profileId;
    private String profileName;
    private boolean isPublic;
    private Map<Type, MinecraftProfileTexture> textures;


    public long getTimestamp() {
        return timestamp;
    }
    
    public UUID getProfileId() {
        return profileId;
    }
    
    public String getProfileName() {
        return profileName;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public Map<Type, MinecraftProfileTexture> getTextures() {
        return textures;
    }

}
