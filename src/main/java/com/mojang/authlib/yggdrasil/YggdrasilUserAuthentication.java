package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.yggdrasil.request.AuthenticationRequest;
import com.mojang.authlib.yggdrasil.request.RefreshRequest;
import com.mojang.authlib.yggdrasil.response.AuthenticationResponse;
import com.mojang.authlib.yggdrasil.response.RefreshResponse;
import com.mojang.authlib.yggdrasil.response.User;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import static com.mojang.authlib.HttpAuthenticationService.constantURL;
import static com.mojang.authlib.UserType.LEGACY;
import static com.mojang.authlib.UserType.MOJANG;

public class YggdrasilUserAuthentication extends HttpUserAuthentication {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String BASE_URL = "https://authserver.mojang.com/";
    private static final URL ROUTE_AUTHENTICATE = constantURL("https://authserver.mojang.com/authenticate");
    private static final URL ROUTE_REFRESH = constantURL("https://authserver.mojang.com/refresh");
    private static final URL ROUTE_VALIDATE = constantURL("https://authserver.mojang.com/validate");
    private static final URL ROUTE_INVALIDATE = constantURL("https://authserver.mojang.com/invalidate");
    private static final URL ROUTE_SIGNOUT = constantURL("https://authserver.mojang.com/signout");
    private static final String STORAGE_KEY_ACCESS_TOKEN = "accessToken";

    private final Agent agent;
    private GameProfile[] profiles;
    private String accessToken;
    private boolean isOnline;
    
    public YggdrasilUserAuthentication(final YggdrasilAuthenticationService authenticationService, final Agent agent) {
        super(authenticationService);
        this.agent = agent;
    }
    
    @Override
    public boolean canLogIn() {
        return !this.canPlayOnline() && StringUtils.isNotBlank((CharSequence)this.getUsername()) && (StringUtils.isNotBlank((CharSequence)this.getPassword()) || StringUtils.isNotBlank((CharSequence)this.getAuthenticatedToken()));
    }
    
    @Override
    public void logIn() throws AuthenticationException {
        if (StringUtils.isBlank((CharSequence)this.getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if (StringUtils.isNotBlank((CharSequence)this.getAuthenticatedToken())) {
            this.logInWithToken();
        }
        else {
            if (!StringUtils.isNotBlank((CharSequence)this.getPassword())) {
                throw new InvalidCredentialsException("Invalid password");
            }
            this.logInWithPassword();
        }
    }
    
    protected void logInWithPassword() throws AuthenticationException {
        if(StringUtils.isBlank(getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if(StringUtils.isBlank(getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        LOGGER.info("Logging in with username & password");
        AuthenticationRequest request = new AuthenticationRequest(this, getUsername(), getPassword());
        AuthenticationResponse response = getAuthenticationService().makeRequest(ROUTE_AUTHENTICATE, request, AuthenticationResponse.class);

        if(!response.getClientToken().equals(getAuthenticationService().getClientToken())) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
        }

        if(response.getSelectedProfile() != null) {
            setUserType(response.getSelectedProfile().isLegacy() ? LEGACY : MOJANG);
        }
        else if(ArrayUtils.isNotEmpty(response.getAvailableProfiles())) {
            setUserType(response.getAvailableProfiles()[0].isLegacy() ? LEGACY : MOJANG);
        }

        User user = response.getUser();

        if(user != null && user.getId() != null) {
            setUserId(user.getId());
        } else {
            setUserId(getUsername());
        }

        this.isOnline = true;
        this.accessToken = response.getAccessToken();
        this.profiles = response.getAvailableProfiles();
        this.setSelectedProfile(response.getSelectedProfile());
        this.getModifiableUserProperties().clear();
        this.updateUserProperties(user);
    }
    
    protected void updateUserProperties(final User user) {
        if (user == null) {
            return;
        }
        if (user.getProperties() != null) {
            this.getModifiableUserProperties().putAll(user.getProperties());
        }
    }

    protected void logInWithToken() throws AuthenticationException {
        if(StringUtils.isBlank(getUserID())) {
            if(!StringUtils.isBlank(getUsername())) {
                throw new InvalidCredentialsException("Invalid uuid & username");
            }

            setUserId(this.getUsername());
        }

        if(StringUtils.isBlank(this.getAuthenticatedToken())) {
            throw new InvalidCredentialsException("Invalid access token");
        }

        LOGGER.info("Logging in with access token");
        RefreshRequest request = new RefreshRequest(this);
        RefreshResponse response = this.getAuthenticationService().makeRequest(ROUTE_REFRESH, request, RefreshResponse.class);

        if(!response.getClientToken().equals(this.getAuthenticationService().getClientToken())) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
        }

        if(response.getSelectedProfile() != null) {
            setUserType(response.getSelectedProfile().isLegacy() ? LEGACY : MOJANG);
        }

        else if(ArrayUtils.isNotEmpty(response.getAvailableProfiles())) {
            setUserType(response.getAvailableProfiles()[0].isLegacy() ? LEGACY : MOJANG);
        }

        if(response.getUser() != null && response.getUser().getId() != null) {
            setUserId(response.getUser().getId());
        } else {
            setUserId(getUsername());
        }

        isOnline = true;
        accessToken = response.getAccessToken();
        profiles = response.getAvailableProfiles();
        setSelectedProfile(response.getSelectedProfile());
        getModifiableUserProperties().clear();
        updateUserProperties(response.getUser());
    }
    
    @Override
    public void logOut() {
        super.logOut();
        this.accessToken = null;
        this.profiles = null;
        this.isOnline = false;
    }
    
    @Override
    public GameProfile[] getAvailableProfiles() {
        return this.profiles;
    }
    
    @Override
    public boolean isLoggedIn() {
        return StringUtils.isNotBlank((CharSequence)this.accessToken);
    }
    
    @Override
    public boolean canPlayOnline() {
        return this.isLoggedIn() && this.getSelectedProfile() != null && this.isOnline;
    }
    
    @Override
    public void selectGameProfile(GameProfile profile) throws AuthenticationException {
        if(!isLoggedIn()) {
            throw new AuthenticationException("Cannot change game profile whilst not logged in");
        }

        if(getSelectedProfile() != null) {
            throw new AuthenticationException("Cannot change game profile. You must log out and back in.");
        }

        if(profile == null || !ArrayUtils.contains((Object[])this.profiles, (Object)profile)) {
            throw new IllegalArgumentException("Invalid profile '" + profile + "'");
        }

        RefreshRequest request = new RefreshRequest(this, profile);
        RefreshResponse response = getAuthenticationService().makeRequest(ROUTE_REFRESH, request, RefreshResponse.class);
        if(!response.getClientToken().equals(this.getAuthenticationService().getClientToken())) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
        }
        isOnline = true;
        accessToken = response.getAccessToken();
        setSelectedProfile(response.getSelectedProfile());
    }
    
    @Override
    public void loadFromStorage(Map<String, Object> credentials) {
        super.loadFromStorage(credentials);
        accessToken = String.valueOf(credentials.get("accessToken"));
    }
    
    @Override
    public Map<String, Object> saveForStorage() {
        Map<String, Object> result = super.saveForStorage();

        if(StringUtils.isNotBlank(getAuthenticatedToken())) {
            result.put("accessToken", getAuthenticatedToken());
        }

        return result;
    }
    
    @Deprecated
    public String getSessionToken() {
        if(isLoggedIn() && getSelectedProfile() != null && canPlayOnline()) {
            return String.format("token:%s:%s", getAuthenticatedToken(), getSelectedProfile().getId());
        }

        return null;
    }
    
    @Override
    public String getAuthenticatedToken() {
        return accessToken;
    }
    
    public Agent getAgent() {
        return agent;
    }
    
    @Override
    public String toString() {
        return "YggdrasilAuthenticationService{" +
                "agent=" + agent + ", " +
                "profiles=" + Arrays.toString(profiles) + ", " +
                "selectedProfile=" + getSelectedProfile() + ", " +
                "username='" + getUsername() + '\'' + ", " +
                "isLoggedIn=" + isLoggedIn() + ", " +
                "userType=" + getUserType() + ", " +
                "canPlayOnline=" + canPlayOnline() + ", " +
                "accessToken='" + accessToken + '\'' + ", " +
                "clientToken='" + getAuthenticationService().getClientToken() + '\'' + '}';
    }
    
    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService)super.getAuthenticationService();
    }

}
