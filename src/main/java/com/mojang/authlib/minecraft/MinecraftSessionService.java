package com.mojang.authlib.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import java.util.Map;

public interface MinecraftSessionService {

    void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException;

    GameProfile hasJoinedServer(GameProfile profile, String serverId) throws AuthenticationUnavailableException;

    Map<Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure);

    GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure);

}
