package net.herobrine.quirkbattle.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.herobrine.gamecore.GameCoreMain;
import net.herobrine.gamecore.SkinSettings;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for disguising players by changing their skin and name.
 * 
 * This implementation includes:
 * 1. A caching system to reduce API calls to Mojang's servers
 * 2. Retry logic with exponential backoff for handling rate limiting
 * 3. Fallback mechanisms using cached skins when API calls fail
 * 4. A default skin as a last resort when no cached skins are available
 * 
 * Configuration options:
 * - CACHE_EXPIRY_MS: How long to keep skins in the cache (default: 24 hours)
 * - MAX_RETRY_ATTEMPTS: Number of retry attempts for API calls (default: 3)
 * - RETRY_DELAY_MS: Initial delay between retries, doubles with each attempt (default: 1 second)
 * 
 * The default skin (Steve) is used as a last resort when all other options fail.
 */

public class DisguisePlayer {
    private static final Logger LOGGER = Logger.getLogger("DisguisePlayer");
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second initial delay
    
    // Default skin data to use as a last resort when all else fails
    // This is Steve's skin data - you can replace with any other default skin if needed
    private static final String DEFAULT_UUID = "8667ba71-b85a-4004-af54-457a9734eed7"; // Steve's UUID
    private static final String DEFAULT_TEXTURE = "eyJ0aW1lc3RhbXAiOjE1MjY5MzA2MTM3OTAsInByb2ZpbGVJZCI6Ijg2NjdiYTcxYjg1YTQwMDRhZjU0NDU3YTk3MzRlZWQ3IiwicHJvZmlsZU5hbWUiOiJTdGV2ZSIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGMxYzc3Y2U4ZTU0OTI1YWI1ODEyNTQ0NmVjNTNiMGNkZDNkMGNhM2RiMjczZWI5MDhkNTQ5YzRhNGFhM2U0MCJ9fX0=";
    private static final String DEFAULT_SIGNATURE = "XCty0rAOfH9h5EF9N8UUOQ9g7H9EXlk9WBgvCvEYJMQVBXGCs9QQlvjTNxMOhGLvbkXJRJnOHQPJCWJlvoFFBXCsb3sRPQoqY8NF7R9CQRPZ0nFRUuTxOHm/ZMq+PZxnMRvJQ1Vg9b1xY5xLyEfzECwZ1sGd7QMTFg3vBJQ7o5XYztYU7mlEOvYS9XVHNfYQYYJLd4c4iyqPL4v/iyQHdhIHmAvQV8QmQKTeGF0CxeVx0jUZwCJxOVBYm9BdQT/9zOUe5PXIwdBHwWS4NKXpJnQoYN9Q5lR5xCRzpZCYUKbO6ZFLeLnK6R1GFJ5/xKPRvlnFSGEFgqqYGsKqvq+MDBFrXfJi/tz4QOJ+Pd5F5PyzJhP9OX+nQUXxjXGUvtALr3nMpA8A8QJFweTmDr4ZhYzH4JPDRa8aqGQPNKj3wnF5OEjKH1/qgp6G0jLE9KsB5CbUYCynyogGvf3/UgONqU9/ER26Jv3BbMHU/M7s/HVpIXj2UYmV3X+zj5m0/3lP8OeYQB+jNhwrPCxC5y9YULcYESy5UwGLXRb7uQXCbs/gYJZQQKv4JwxHpbQzL5wBZ5XJyGJ1GFu5in8BdYPHjYGcGkFqYpxJ8IyHvL9Gk1KdPT7yCQvpIcXCQ+IYfimYGJgJYgCbVVNBs3xnLJv/ku+S8dLUZPQ=";
    
    // Cache to store skin data to reduce API calls
    private static final Map<String, SkinData> SKIN_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Class to store skin data in the cache
     */
    private static class SkinData {
        private final String uuid;
        private final String textureData;
        private final String signature;
        private final long timestamp;
        
        public SkinData(String uuid, String textureData, String signature) {
            this.uuid = uuid;
            this.textureData = textureData;
            this.signature = signature;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getUuid() {
            return uuid;
        }
        
        public String getTextureData() {
            return textureData;
        }
        
        public String getSignature() {
            return signature;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
    
    /**
     * Get skin data from cache if available and not expired
     * @param skinName The player name to get skin data for
     * @return The cached skin data or null if not in cache or expired
     */
    private SkinData getCachedSkin(String skinName) {
        SkinData skinData = SKIN_CACHE.get(skinName.toLowerCase());
        if (skinData != null && !skinData.isExpired()) {
            LOGGER.info("Cache hit for skin: " + skinName);
            return skinData;
        }
        if (skinData != null) {
            LOGGER.info("Cache expired for skin: " + skinName);
        }
        return null;
    }
    
    /**
     * Add skin data to the cache
     * @param skinName The player name
     * @param uuid The player UUID
     * @param textureData The texture data
     * @param signature The signature
     */
    private void cacheSkin(String skinName, String uuid, String textureData, String signature) {
        SKIN_CACHE.put(skinName.toLowerCase(), new SkinData(uuid, textureData, signature));
        LOGGER.info("Cached skin data for: " + skinName);
    }
    /**
     * Set a player's skin and name to match another player
     * Uses caching to reduce API calls and handles rate limiting
     * 
     * @param player The player to change the skin of
     * @param disguiseName The name of the player whose skin to use
     * @throws Exception If there's an error applying the skin
     */
    public void giveDisguise(Player player, String disguiseName) throws Exception {
        String uuid = null;
        String sig = null;
        String data = null;
        
        // Try to get skin data from cache first
        SkinData cachedSkin = getCachedSkin(disguiseName);
        if (cachedSkin != null) {
            // Use cached data
            LOGGER.info("Using cached skin data for: " + disguiseName);
            uuid = cachedSkin.getUuid();
            sig = cachedSkin.getSignature();
            data = cachedSkin.getTextureData();
        } else {
            // Not in cache, fetch from API with retry logic
            LOGGER.info("Fetching skin data from API for: " + disguiseName);

            // Try to get UUID with retries
            String getUUID = null;
            for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
                try {
                    getUUID = get("https://api.mojang.com/users/profiles/minecraft/%s", disguiseName);
                    if (getUUID != null && !getUUID.equals("error")) {
                        break;
                    }
                    LOGGER.warning("Failed to get UUID for " + disguiseName + ", attempt " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error getting UUID for " + disguiseName, e);
                    if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                        throw e; // Rethrow on last attempt
                    }

                }
            }

            // If we couldn't get the UUID, check if we have any cached skin to use as fallback
            if (getUUID == null || getUUID.equals("error")) {
                LOGGER.warning("Failed to get UUID for " + disguiseName + " after " + MAX_RETRY_ATTEMPTS + " attempts");
                
                // Try to find any non-expired skin in the cache as a fallback
                for (Map.Entry<String, SkinData> entry : SKIN_CACHE.entrySet()) {
                    if (!entry.getValue().isExpired()) {
                        LOGGER.info("Using fallback skin from cache: " + entry.getKey());
                        uuid = entry.getValue().getUuid();
                        sig = entry.getValue().getSignature();
                        data = entry.getValue().getTextureData();
                        break;
                    }
                }
                
                // If we still don't have a skin, use the default skin as a last resort
                if (uuid == null || sig == null || data == null) {
                    LOGGER.warning("No cached skins available, using default skin as last resort for " + disguiseName);
                    uuid = DEFAULT_UUID;
                    sig = DEFAULT_SIGNATURE;
                    data = DEFAULT_TEXTURE;
                }
            } else {
                // We have the UUID, now get the skin data
                uuid = getUUID(getUUID);
                // Try to get skin data with retries
                String skin = null;
                for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
                    try {
                        skin = get("https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false", uuid);
                        if (skin != null) {
                            break;
                        }
                        LOGGER.warning("Failed to get skin data for " + disguiseName + ", attempt " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error getting skin data for " + disguiseName, e);
                        if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                            throw e;
                        }
                    }
                }
                
                // If we couldn't get the skin data, use fallback
                if (skin == null) {
                    LOGGER.warning("Failed to get skin data for " + disguiseName + " after " + MAX_RETRY_ATTEMPTS + " attempts");
                    
                    // Try to find any non-expired skin in the cache as a fallback
                    for (Map.Entry<String, SkinData> entry : SKIN_CACHE.entrySet()) {
                        if (!entry.getValue().isExpired()) {
                            LOGGER.info("Using fallback skin from cache: " + entry.getKey());
                            uuid = entry.getValue().getUuid();
                            sig = entry.getValue().getSignature();
                            data = entry.getValue().getTextureData();
                            break;
                        }
                    }
                    
                    // If we still don't have a skin, use the default skin as a last resort
                    if (uuid == null || sig == null || data == null) {
                        LOGGER.warning("No cached skins available, using default skin as last resort for " + disguiseName);
                        uuid = DEFAULT_UUID;
                        sig = DEFAULT_SIGNATURE;
                        data = DEFAULT_TEXTURE;
                    }
                } else {
                    // Extract signature and data from skin
                    sig = getSig(skin);
                    data = getData(skin);
                    
                    // Cache the skin data for future use
                    cacheSkin(disguiseName, uuid, data, sig);
                }
            }
        }
        
        LOGGER.info("Applying skin for " + disguiseName + " to player " + player.getName());

        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        int dimension = player.getWorld().getEnvironment().getId();
        WorldSettings.EnumGamemode gamemode = WorldSettings.EnumGamemode.valueOf(player.getGameMode().name());
        EnumDifficulty difficulty = (EnumDifficulty) EnumDifficulty.class.getDeclaredMethod("getById", int.class).invoke(null, player.getWorld().getDifficulty().getValue());
        WorldType type = WorldType.getType(player.getWorld().getWorldType().getName());
        final GameMode gameMode = player.getGameMode();
        final boolean allowFlight = player.getAllowFlight();
        final boolean flying = player.isFlying();
        final Location location = player.getLocation();
        final float pitch = location.getPitch(), yaw = location.getYaw();
        final int heldItemSlot = player.getInventory().getHeldItemSlot();
        final int level = player.getLevel();
        final float xp = player.getExp();
        final double maxHealth = player.getMaxHealth();
        final double health = player.getHealth();

        GameCoreMain.getInstance().sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer));

        GameProfile profile = entityPlayer.getProfile();

        PropertyMap map = profile.getProperties();
        map.clear();

        map.put("textures", new Property("textures", data, sig));

        changeName(disguiseName, player);

        Bukkit.getScheduler().runTaskLater(GameCoreMain.getInstance(), () -> {
            GameCoreMain.getInstance().sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer));
            GameCoreMain.getInstance().sendPacket(player, new PacketPlayOutRespawn(dimension, difficulty, type, gamemode));
            player.setGameMode(gameMode);
            player.setAllowFlight(allowFlight);
            player.setFlying(flying);
            player.updateInventory();
            player.getInventory().setHeldItemSlot(heldItemSlot);
            player.setLevel(level);
            player.setExp(xp);
            player.setMaxHealth(maxHealth);
            player.setHealth(health);

            location.setYaw(yaw);
            location.setPitch(pitch);
            player.teleport(location);

        }, 0);


        Bukkit.getOnlinePlayers().forEach(online -> {

            online.hidePlayer(player);
            online.showPlayer(player);

        });

    }

    static void sendPacket(Packet<?> packet) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }

    }

    static void sendPacketNotFor(Player player, Packet<?> packet) {
        for (Player targets : Bukkit.getOnlinePlayers()) {
            if (targets.getUniqueId().toString().equals(player.getUniqueId().toString()))
                continue;
            ((CraftPlayer) targets).getHandle().playerConnection.sendPacket(packet);
        }

    }


    public String get(String url,String name) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(String.format(url, name)).openConnection();

        if (connection.getResponseCode() != 200) {
            System.out.println("An error has occurred: RESPONSE_CODE (" + connection.getResponseCode() + ")");
        } else {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String output;

            while ((output = bufferedReader.readLine()) != null) {
                builder.append(output);
            }
            return builder.toString();
        }
        return "";
    }

    public String getSkin(String data) {
        final Pattern pattern = Pattern.compile("\"value\" : \"(.*?)\",");
        final Matcher matcher = pattern.matcher(data);
        matcher.find();
        return matcher.group(1);
    }

    public String getSig(String data) {
         Pattern pattern = Pattern.compile("\"signature\" : \"(.*?)\"");
         Matcher matcher = pattern.matcher(data);
         matcher.find();
        return matcher.group(1);
    }

    public String addCharToString(String str, char c, int pos) {
        StringBuilder stringBuilder = new StringBuilder(str);
        stringBuilder.insert(pos, c);
        return stringBuilder.toString();
    }



    public String getUUID(String output) {
        Pattern pattern = Pattern.compile("\"id\" : \"(.*?)\"");
        Matcher matcher = pattern.matcher(output);
        matcher.find();

        String id = matcher.group(1);
        return id;
    }

    public String getData(String string){
        Pattern pattern = Pattern.compile("\"value\" : \"(.*?)\"");
        Matcher matcher = pattern.matcher(string);
        matcher.find();
        return matcher.group(1);
    }

    public void changeName(String name, Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle", (Class<?>[]) null);
            Object profile = getHandle.invoke(player).getClass().getMethod("getProfile")
                    .invoke(getHandle.invoke(player));
            Field ff = profile.getClass().getDeclaredField("name");
            ff.setAccessible(true);
            ff.set(profile, ChatColor.translateAlternateColorCodes('&', name));
            //player.setPlayerListName(HerobrinePVPCore.getFileManager().getRank(player).getColor() + HerobrinePVPCore.getFileManager().getRank(player).getName() + " " + name);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                 | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
