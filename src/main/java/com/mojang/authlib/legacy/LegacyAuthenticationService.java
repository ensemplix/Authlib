package com.mojang.authlib.legacy;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import org.apache.commons.lang3.Validate;

import java.net.Proxy;

import static com.mojang.authlib.Agent.MINECRAFT;

public class LegacyAuthenticationService extends HttpAuthenticationService {

    protected LegacyAuthenticationService(Proxy proxy) {
        super(proxy);
    }
    
    @Override
    public LegacyUserAuthentication createUserAuthentication(Agent agent) {
        Validate.notNull((Object)agent);

        if(agent != MINECRAFT) {
            throw new IllegalArgumentException("Legacy authentication cannot handle anything but Minecraft");
        }

        return new LegacyUserAuthentication(this);
    }
    
    @Override
    public LegacyMinecraftSessionService createMinecraftSessionService() {
        return new LegacyMinecraftSessionService(this);
    }
    
    @Override
    public GameProfileRepository createProfileRepository() {
        throw new UnsupportedOperationException("Legacy authentication service has no profile repository");
    }

}
