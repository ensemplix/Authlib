package com.mojang.authlib.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;

import java.util.Map;

public interface MinecraftSessionService {

    void joinServer(GameProfile profile, String p1, String p2) throws AuthenticationException;

    GameProfile hasJoinedServer(GameProfile profile, String p1) throws AuthenticationUnavailableException;

    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile p0, boolean p1);

    GameProfile fillProfileProperties(GameProfile profile, boolean p1);

}
