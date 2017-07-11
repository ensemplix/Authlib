package com.mojang.authlib.legacy;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.mojang.authlib.HttpAuthenticationService.buildQuery;
import static com.mojang.authlib.HttpAuthenticationService.constantURL;
import static com.mojang.authlib.UserType.LEGACY;

public class LegacyUserAuthentication extends HttpUserAuthentication {

    private static final URL AUTHENTICATION_URL = constantURL("https://login.minecraft.net");
    private static final int AUTHENTICATION_VERSION = 14;
    private static final int RESPONSE_PART_PROFILE_NAME = 2;
    private static final int RESPONSE_PART_SESSION_TOKEN = 3;
    private static final int RESPONSE_PART_PROFILE_ID = 4;
    private String sessionToken;
    
    protected LegacyUserAuthentication(LegacyAuthenticationService authenticationService) {
        super(authenticationService);
    }
    
    @Override
    public void logIn() throws AuthenticationException {
        if(StringUtils.isBlank(getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if(StringUtils.isBlank(getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        Map<String, Object> args = new HashMap<>();
        args.put("user", getUsername());
        args.put("password", getPassword());
        args.put("version", 14);

        String response;
        try {
            response = this.getAuthenticationService().performPostRequest(LegacyUserAuthentication.AUTHENTICATION_URL, buildQuery(args), "application/x-www-form-urlencoded").trim();
        }
        catch(IOException e) {
            throw new AuthenticationException("Authentication server is not responding", e);
        }

        String[] split = response.split(":");

        if(split.length != 5) {
            throw new InvalidCredentialsException(response);
        }

        String profileId = split[4];
        String profileName = split[2];
        String sessionToken = split[3];

        if(StringUtils.isBlank(profileId) || StringUtils.isBlank(profileName) || StringUtils.isBlank(sessionToken)) {
            throw new AuthenticationException("Unknown response from authentication server: " + response);
        }

        setSelectedProfile(new GameProfile(UUIDTypeAdapter.fromString(profileId), profileName));
        this.sessionToken = sessionToken;
        setUserType(LEGACY);
    }
    
    @Override
    public void logOut() {
        super.logOut();
        sessionToken = null;
    }
    
    @Override
    public boolean canPlayOnline() {
        return isLoggedIn() && getSelectedProfile() != null && getAuthenticatedToken() != null;
    }
    
    @Override
    public GameProfile[] getAvailableProfiles() {
        if(getSelectedProfile() != null) {
            return new GameProfile[] { getSelectedProfile() };
        }

        return new GameProfile[0];
    }
    
    @Override
    public void selectGameProfile(GameProfile profile) throws AuthenticationException {
        throw new UnsupportedOperationException("Game profiles cannot be changed in the legacy authentication service");
    }
    
    @Override
    public String getAuthenticatedToken() {
        return this.sessionToken;
    }
    
    @Override
    public String getUserID() {
        return this.getUsername();
    }
    
    @Override
    public LegacyAuthenticationService getAuthenticationService() {
        return (LegacyAuthenticationService) super.getAuthenticationService();
    }

}
