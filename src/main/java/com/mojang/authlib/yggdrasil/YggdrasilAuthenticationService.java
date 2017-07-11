package com.mojang.authlib.yggdrasil;

import com.google.gson.*;
import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.URL;
import java.util.UUID;

public class YggdrasilAuthenticationService extends HttpAuthenticationService {

    private final String clientToken;
    private final Gson gson;
    
    public YggdrasilAuthenticationService(final Proxy proxy, final String clientToken) {
        super(proxy);
        this.clientToken = clientToken;

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(GameProfile.class, new GameProfileSerializer());
        builder.registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer());
        builder.registerTypeAdapter(UUID.class, new UUIDTypeAdapter());
        builder.registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsResponse.Serializer());

        this.gson = builder.create();
    }
    
    @Override
    public UserAuthentication createUserAuthentication(Agent agent) {
        return new YggdrasilUserAuthentication(this, agent);
    }
    
    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new YggdrasilMinecraftSessionService(this);
    }
    
    @Override
    public GameProfileRepository createProfileRepository() {
        return new YggdrasilGameProfileRepository(this);
    }
    
    protected <T extends Response> T makeRequest(URL url, Object input, Class<T> classOfT)
            throws AuthenticationException, JsonParseException {

        try {
            String jsonResult = (input == null) ? performGetRequest(url) : performPostRequest(url, gson.toJson(input), "application/json");

            @SuppressWarnings("unchecked")
            T result = (T)this.gson.fromJson(jsonResult, classOfT);

            if(result == null) {
                return null;
            }

            if(!StringUtils.isNotBlank((CharSequence)result.getError())) {
                return result;
            }
            if("UserMigratedException".equals(result.getCause())) {
                throw new UserMigratedException(result.getErrorMessage());
            }
            if(result.getError().equals("ForbiddenOperationException")) {
                throw new InvalidCredentialsException(result.getErrorMessage());
            }

            throw new AuthenticationException(result.getErrorMessage());
        } catch (IOException | IllegalStateException e) {
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        }
    }
    
    public String getClientToken() {
        return clientToken;
    }
    
    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {

        public GameProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonObject object = (JsonObject)json;
            UUID id = object.has("id") ? context.deserialize(object.get("id"), UUID.class) : null;
            String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            return new GameProfile(id, name);
        }
        
        public JsonElement serialize(GameProfile src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();

            if(src.getId() != null) {
                result.add("id", context.serialize(src.getId()));
            }

            if(src.getName() != null) {
                result.addProperty("name", src.getName());
            }

            return result;
        }
    }
}
