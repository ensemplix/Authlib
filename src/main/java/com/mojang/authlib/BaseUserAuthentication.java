package com.mojang.authlib;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mojang.authlib.UserType.LEGACY;

public abstract class BaseUserAuthentication implements UserAuthentication {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final String STORAGE_KEY_PROFILE_NAME = "displayName";
    protected static final String STORAGE_KEY_PROFILE_ID = "uuid";
    protected static final String STORAGE_KEY_PROFILE_PROPERTIES = "profileProperties";
    protected static final String STORAGE_KEY_USER_NAME = "username";
    protected static final String STORAGE_KEY_USER_ID = "userid";
    protected static final String STORAGE_KEY_USER_PROPERTIES = "userProperties";

    private final AuthenticationService authenticationService;
    private final PropertyMap userProperties = new PropertyMap();

    private GameProfile selectedProfile;
    private String userId;
    private String userName;
    private String password;
    private UserType userType;
    
    protected BaseUserAuthentication(AuthenticationService authenticationService) {
        Validate.notNull(authenticationService);

        this.authenticationService = authenticationService;
    }
    
    @Override
    public boolean canLogIn() {
        return !canPlayOnline() && StringUtils.isNotBlank(getUsername()) && StringUtils.isNotBlank(getPassword());
    }
    
    @Override
    public void logOut() {
        password = null;
        userId = null;

        setSelectedProfile(null);
        userProperties.clear();
        setUserType(null);
    }
    
    @Override
    public boolean isLoggedIn() {
        return getSelectedProfile() != null;
    }
    
    @Override
    public void setUsername(String userName) {
        if(isLoggedIn() && canPlayOnline()) {
            throw new IllegalStateException("Cannot change username whilst logged in & online");
        }

        this.userName = userName;
    }
    
    @Override
    public void setPassword(final String password) {
        if(isLoggedIn() && canPlayOnline() && StringUtils.isNotBlank(password)) {
            throw new IllegalStateException("Cannot set password whilst logged in & online");
        }

        this.password = password;
    }
    
    protected String getUsername() {
        return userName;
    }
    
    protected String getPassword() {
        return password;
    }

    @Override
    public void loadFromStorage(Map<String, Object> credentials) {
        logOut();

        setUsername(String.valueOf(credentials.get("username")));

        if(credentials.containsKey("userid")) {
            userId = String.valueOf(credentials.get("userid"));
        } else {
            userId = userName;
        }

        if(credentials.containsKey("userProperties")) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> list = (List<Map<String, String>>) credentials.get("userProperties");

                for(Map<String, String> propertyMap : list) {
                    String name = propertyMap.get("name");
                    String value = propertyMap.get("value");
                    String signature = propertyMap.get("signature");

                    if(signature == null) {
                        userProperties.put(name, new Property(name, value));
                    } else {
                        userProperties.put(name, new Property(name, value, signature));
                    }
                }
            } catch(Throwable t) {
                LOGGER.warn("Couldn't deserialize user properties", t);
            }
        }

        if(credentials.containsKey("displayName") && credentials.containsKey("uuid")) {
            GameProfile profile = new GameProfile(UUIDTypeAdapter.fromString(
                    String.valueOf(credentials.get("uuid"))),
                    String.valueOf(credentials.get("displayName"))
            );

            if(credentials.containsKey("profileProperties")) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> list = (List<Map<String, String>>) credentials.get("profileProperties");

                    for(Map<String, String> propertyMap : list) {
                        String name = propertyMap.get("name");
                        String value = propertyMap.get("value");
                        String signature = propertyMap.get("signature");

                        if(signature == null) {
                            profile.getProperties().put(name, new Property(name, value));
                        } else {
                            profile.getProperties().put(name, new Property(name, value, signature));
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.warn("Couldn't deserialize profile properties", t);
                }
            }

            setSelectedProfile(profile);
        }
    }
    
    @Override
    public Map<String, Object> saveForStorage() {
        Map<String, Object> result = new HashMap<>();

        if(userName != null) {
            result.put("username", userName);
        }

        if(userId != null) {
            result.put("userid", userId);
        } else if(getUsername() != null) {
            result.put("username", userName);
        }

        if(!this.getUserProperties().isEmpty()) {
            List<Map<String, String>> properties = new ArrayList<>();

            for(Property userProperty : getUserProperties().values()) {
                Map<String, String> property = new HashMap<>();
                property.put("name", userProperty.getName());
                property.put("value", userProperty.getValue());
                property.put("signature", userProperty.getSignature());
                properties.add(property);
            }
            result.put("userProperties", properties);
        }

        GameProfile selectedProfile = this.getSelectedProfile();

        if(selectedProfile != null) {
            result.put("displayName", selectedProfile.getName());
            result.put("uuid", selectedProfile.getId());

            List<Map<String, String>> properties2 = new ArrayList<>();

            for(Property profileProperty : selectedProfile.getProperties().values()) {
                Map<String, String> property2 = new HashMap<>();

                property2.put("name", profileProperty.getName());
                property2.put("value", profileProperty.getValue());
                property2.put("signature", profileProperty.getSignature());
                properties2.add(property2);
            }

            if(!properties2.isEmpty()) {
                result.put("profileProperties", properties2);
            }
        }

        return result;
    }

    protected void setSelectedProfile(GameProfile selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    @Override
    public GameProfile getSelectedProfile() {
        return selectedProfile;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName());
        result.append("{");

        if(isLoggedIn()) {
            result.append("Logged in as ");
            result.append(getUsername());

            if(getSelectedProfile() != null) {
                result.append(" / ");
                result.append(getSelectedProfile());
                result.append(" - ");

                if(canPlayOnline()) {
                    result.append("Online");
                } else {
                    result.append("Offline");
                }
            }
        } else {
            result.append("Not logged in");
        }

        result.append("}");
        return result.toString();
    }
    
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }
    
    @Override
    public String getUserID() {
        return userId;
    }
    
    @Override
    public PropertyMap getUserProperties() {
        if(isLoggedIn()) {
            PropertyMap result = new PropertyMap();
            result.putAll(userProperties);
            return result;
        }

        return new PropertyMap();
    }
    
    protected PropertyMap getModifiableUserProperties() {
        return userProperties;
    }
    
    @Override
    public UserType getUserType() {
        if(this.isLoggedIn()) {
            return (userType == null) ? LEGACY : userType;
        }

        return null;
    }
    
    protected void setUserType(UserType userType) {
        this.userType = userType;
    }
    
    protected void setUserId(String userId) {
        this.userId = userId;
    }

}
