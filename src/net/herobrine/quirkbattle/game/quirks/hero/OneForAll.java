package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.core.ItemBuilder;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import net.herobrine.gamecore.GameCoreMain;
import net.herobrine.gamecore.GameState;
import net.herobrine.gamecore.GameType;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.event.QuirkErasureEvent;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.SwitchAbilitySetTest;
import net.herobrine.quirkbattle.game.stats.EnhancedPlayerStats;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Awakened;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.Switchable;
import net.herobrine.quirkbattle.util.ofa.FaJinSystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class OneForAll extends Class implements Quirk, Switchable, Awakened {
    private PlayerStats stats;
    private final Arena arena;
    private long lastStaminaCharge = 0;
    private final OneForAll thisForRunnable = this;
    private boolean canPowerUp = true;
    private final List<Ability> transferList = new ArrayList<>();
    private final List<Ability> secondaryAbilities = new ArrayList<>();
    private boolean isBeingErased = false;
    private final List<Ability> abilities;
    private final boolean isAwakened;
    private boolean isSwitcherActive = false;
    private AbilitySets currentSet;
    private AbilitySets[] availableSets;

    // Enhanced OFA specific fields
    private FaJinSystem faJinSystem;
    private boolean gearshiftActive = false;
    private long gearshiftStartTime = 0;
    private int gearLevel = 0; // 0 = first, 1 = second, 2 = third, 3 = top
    private boolean isStunned = false;
    private int minimumPower = 0;
    private boolean hasChargedOnce = false;
    private BukkitTask dangerSenseTask;
    private BukkitTask gearshiftTask;
    private BukkitTask powerChargeTask;

    public OneForAll(UUID uuid) {
        super(uuid, ClassTypes.ONEFORALL);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        this.abilities = new ArrayList<>();
        this.isAwakened = true;
                //arena.getType().equals(GameType.HEROES_VS_VILLAINS);

        if (isAwakened) {
            this.faJinSystem = new FaJinSystem(Bukkit.getPlayer(uuid));
            this.availableSets = new AbilitySets[]{AbilitySets.OFA_AWAKENED_1, AbilitySets.OFA_AWAKENED_2};
        } else {
            this.availableSets = new AbilitySets[]{AbilitySets.ONE_FOR_ALL};
        }
    }

    @Override
    public void onStart(Player player) {
        if (isAwakened) {
            initializeAwakened(player);
        } else {
            // Normal OFA initialization
            stats = new PlayerStats(player.getUniqueId(), 200, 200, 50, 0, 100, 6);
            arena.getQuirkBattleGame().getPlayerStatsMap().put(player.getUniqueId(), stats);
            player.getInventory().clear();

            ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
            defaultHeldItem.setDisplayName(ChatColor.GREEN + "Hold right click to charge power!");
            defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);

            player.getInventory().setHeldItemSlot(0);
            player.getInventory().setItem(0, defaultHeldItem.build());
            registerAbilities(AbilitySets.ONE_FOR_ALL);
        }
    }

    @Override
    public void initializeAwakened(Player player) {
        // Enhanced OFA stats
        Awakened.AwakendStats awakenedStats = getAwakenedStats();
        stats = new EnhancedPlayerStats(player.getUniqueId(),
                awakenedStats.health,
                awakenedStats.maxHealth,
                awakenedStats.defense,
                0,
                awakenedStats.intelligence,
                awakenedStats.baseDamage);

        ((EnhancedPlayerStats) stats).setFaJinSystem(faJinSystem);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(player.getUniqueId(), stats);
        player.getInventory().clear();

        // Enhanced visual indicator
        ItemBuilder defaultHeldItem = new ItemBuilder(Material.BLAZE_ROD);
        defaultHeldItem.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "ONE FOR ALL - AWAKENED");
        defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        defaultHeldItem.addEnchant(Enchantment.DURABILITY, 1);

        player.getInventory().setHeldItemSlot(0);
        player.getInventory().setItem(0, defaultHeldItem.build());

        // Register enhanced ability set
        registerAbilities(AbilitySets.OFA_AWAKENED_1);

        // Start Danger Sense
        startDangerSense(player);

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ONE FOR ALL AWAKENED!");
        player.sendMessage(ChatColor.YELLOW + "You feel the power of the previous holders flowing through you!");
        player.sendMessage(ChatColor.YELLOW + "Let's defeat " + ChatColor.RED + "All For One " + ChatColor.YELLOW + "and save the world!");
        player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0f, 1.5f);
    }

    private void startDangerSense(Player player) {
        dangerSenseTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE || isStunned) {
                    if (arena.getState() != GameState.LIVE) cancel();
                    return;
                }

                // Check for enemies behind the player
                Location playerLoc = player.getLocation();
                Vector playerDir = playerLoc.getDirection();

                for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                    if (!(entity instanceof Player)) continue;
                    Player target = (Player) entity;

                    if (arena.getSpectators().contains(target.getUniqueId())) continue;
                    if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;

                    // Check if enemy team
                    if (arena.getType().isTeamsMode()) {
                        if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
                    }

                    // Check if behind player
                    Vector toTarget = target.getLocation().toVector().subtract(playerLoc.toVector());
                    double dot = playerDir.dot(toTarget.normalize());

                    if (dot < -0.3) { // Target is behind (dot product negative)
                        // Heartbeat effect
                        player.playSound(player.getLocation(), Sound.NOTE_BASS_DRUM, 0.8f, 0.5f);
                        GameCoreMain.getInstance().sendActionBar(player,
                                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "⚠ DANGER SENSE ⚠");
                        break; // Only alert once per tick
                    }
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 10L);
    }



    private String getGearDisplay() {
        switch (gearLevel) {
            case 1:
                return ChatColor.YELLOW + "⚙ SECOND GEAR";
            case 2:
                return ChatColor.GOLD + "⚙ THIRD GEAR";
            case 3:
                return ChatColor.RED + "" + ChatColor.BOLD + "⚙ TOP GEAR";
            default:
                return ChatColor.WHITE + "⚙ LOW GEAR";
        }
    }

    private void startEnhancedPowerCharging(Player player) {
        powerChargeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE || !gearshiftActive) {
                    cancel();
                    return;

                }

                // Auto charge +5 power per second in Gearshift
                if (stats.getMana() < 120) {
                    int newPower = Math.min(stats.getMana() + 5, 120);
                    stats.setManaSpecial(newPower);

                    float speed = .2F + (.2F * newPower / 15);
                    if (speed > 1) speed = 1;
                    player.setWalkSpeed(speed);
                    player.setLevel(newPower);
                    player.setExp((float) newPower / 120f);
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }

    @Override
    public void registerAbilities(AbilitySets set) {
        this.currentSet = set;
        int i = 2;
        for (Abilities ability : set.getAbilities()) {
            abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
            i++;
        }

        if (!isSwitcherActive && arena.getType().equals(GameType.ONE_V_ONE)) {
            arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.OFA_ABILITY_SWITCH_TEST, this, i);
            isSwitcherActive = true;
        }
    }

    public void resetPower() {
        Player player = Bukkit.getPlayer(uuid);

        if (isAwakened) {
            // Enhanced OFA doesn't go below 20% after first charge
            if (hasChargedOnce && !gearshiftActive) {
                minimumPower = 20;
                stats.setMana(minimumPower);
            } else if (gearshiftActive) {
                // In Gearshift, power doesn't reset
                return;
            } else {
                stats.setMana(0);
            }
        } else {
            // Normal OFA behavior
            rollForDamage((double) stats.getMana() / 100);
            stats.setMana(0);
        }

        player.setWalkSpeed(.2F + (.2F * stats.getMana() / 10));
        player.setLevel(stats.getMana());
        player.setExp((float) stats.getMana() / (float) stats.getIntelligence());

        updateDefaultItem();
    }

    public void resetPowerNoRoll() {
        Player player = Bukkit.getPlayer(uuid);

        if (isAwakened && hasChargedOnce) {
            stats.setMana(minimumPower);
        } else {
            stats.setMana(0);
        }

        player.setWalkSpeed(.2F);
        player.setLevel(stats.getMana());
        player.setExp((float) stats.getMana() / (float) stats.getIntelligence());
        updateDefaultItem();
    }

    private void updateDefaultItem() {
        Player player = Bukkit.getPlayer(uuid);
        ItemBuilder defaultHeldItem;

        if (isAwakened) {
            defaultHeldItem = new ItemBuilder(Material.BLAZE_ROD);
            if (stats.getMana() == 120 && gearshiftActive) {
                defaultHeldItem.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "MAXIMUM POWER!");
            } else if (gearshiftActive) {
                defaultHeldItem.setDisplayName(ChatColor.GOLD + "Gearshift Active - " + getGearDisplay());
            } else {
                defaultHeldItem.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "ONE FOR ALL - AWAKENED");
            }
        } else {
            defaultHeldItem = new ItemBuilder(Material.STICK);
            if (stats.getMana() == 100) {
                defaultHeldItem.setDisplayName(ChatColor.GREEN + "Power is full!");
                defaultHeldItem.addEnchant(Enchantment.DURABILITY, 1);
            } else {
                defaultHeldItem.setDisplayName(ChatColor.GREEN + "Hold right click to charge power!");
            }
        }

        defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        player.getInventory().setItem(0, defaultHeldItem.build());
    }

    public void rollForDamage(double power) {
        if (isAwakened) return; // No self-damage in awakened form

        Player player = Bukkit.getPlayer(uuid);
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        double chance = rand.nextDouble();

        if (chance <= power) {
            player.sendMessage(HerobrinePVPCore.translateString("&6&lWOAH! &r&7Be careful, your power is very unstable!"));
            player.damage(0);
            Manager.getArena(player).getQuirkBattleGame().getCustomDeathCause().put(player.getUniqueId(), CustomDeathCause.ONE_FOR_ALL_SELF);
            Manager.getArena(player).getQuirkBattleGame().getLastAbilityAttacker().put(player.getUniqueId(), player.getUniqueId());
            double damage = 10 + ((double) (10 * stats.getMana()) / 100);
            EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, damage);
            Bukkit.getPluginManager().callEvent(event);
            player.setLastDamageCause(event);
        }
    }

    public void spawnRGBParticles(Location loc, int red, int blue, int green, boolean sendToSelf) {getAbilities().get(0).spawnRGBParticles(loc, red, blue, green, sendToSelf);}


    private void startPowerUpHelix(Player player) {
        new BukkitRunnable() {
            double helixAngle = 0;

            @Override
            public void run() {
                if (!canPowerUp || stats.getMana() == 0 || arena.getState() != GameState.LIVE) {
                    cancel();
                    return;
                }

                Location playerLoc = player.getLocation();

                // Create double helix effect
                for (int i = 0; i < 2; i++) {
                    double angle = helixAngle + (i * Math.PI);

                    for (double height = 0; height <= 2; height += 0.3) {
                        double spiralRadius = 0.5 + (stats.getMana() * 0.005);
                        double x = Math.cos(angle + height * 2) * spiralRadius;
                        double z = Math.sin(angle + height * 2) * spiralRadius;

                        Location helixLoc = playerLoc.clone().add(x, height, z);

                        if (isAwakened) {
                            // Full Cowling enhanced - darker green with red lightning
                            spawnRGBParticles(helixLoc, 30,109,82, true); // Darker green
                            if (Math.random() < 0.5) {
                                spawnRGBParticles(helixLoc, 255, 0, 0, true); // Red lightning
                            }
                            if (Math.random() < 0.2) {
                                spawnRGBParticles(helixLoc, 15, 163, 103, true); // Bright green
                            }
                        } else {
                            // Normal Full Cowling - green
                            spawnRGBParticles(helixLoc, 30,109,82, true); // Bright green
                            if (Math.random() < 0.2) {
                                spawnRGBParticles(helixLoc, 255, 0, 0, true); // Red lightning
                            }
                        }
                    }
                }

                // Lightning burst effect at higher power
                if (stats.getMana() > 50 && Math.random() < 0.2) {
                    double burstHeight = Math.random() * 2;
                    Location burstLoc = playerLoc.clone().add(0, burstHeight, 0);

                    for (int i = 0; i < 3; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double radius = Math.random() * 0.8;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        Location particleLoc = burstLoc.clone().add(x, 0, z);
                        spawnRGBParticles(particleLoc, 255, 0, 0, true); // Red lightning burst

                    }
                }

                helixAngle += Math.PI / 10;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();

        if (arena.getState() != GameState.LIVE) return;

        // Check if the entity being damaged is a player
        if (!(event.getEntity() instanceof Player)) return;
        Player target = (Player) event.getEntity();

        // Skip spectators and dead players
        if (arena.getSpectators().contains(target.getUniqueId())) return;
        //  if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) return;

        // Skip teammates in team modes
        if (arena.getType().isTeamsMode() && arena.getTeam(damager).equals(arena.getTeam(target))) return;

        if (getFaJinSystem() != null) {
            if (!gearshiftActive) faJinSystem.addEnergy(5);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getPlayer().getUniqueId() != this.uuid) return;
        if (!Manager.isPlaying(e.getPlayer())) return;
        if (Manager.getArena(e.getPlayer()).getState() != GameState.LIVE) return;

        Player player = e.getPlayer();
        boolean shouldChargePower = e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK);

        if (shouldChargePower && System.currentTimeMillis() - lastStaminaCharge >= 100 && canPowerUp && !isStunned) {
            lastStaminaCharge = System.currentTimeMillis();

            int maxPower = isAwakened && gearshiftActive ? 120 : 100;
            int chargeRate = isAwakened ? 2 : 1; // Faster charging in awakened

            if (stats.getMana() < maxPower) {
                stats.setMana(Math.min(stats.getMana() + chargeRate, maxPower));

                if (isAwakened && !hasChargedOnce && stats.getMana() >= 20) {
                    hasChargedOnce = true;
                    minimumPower = 20;
                    startPowerUpHelix(player);
                }
                else if (!isAwakened && !hasChargedOnce) {
                    if (stats.getMana() > 0) {
                        hasChargedOnce = true;
                        startPowerUpHelix(player);
                    }
                }


                float speed = .2F + (.2F * stats.getMana() / 10);
                if (speed > 1) speed = 1;
                player.setWalkSpeed(speed);
                player.setLevel(stats.getMana());
                player.setExp((float) stats.getMana() / (float) maxPower);

                if (stats.getMana() == maxPower) {
                    updateDefaultItem();
                }
            }
            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.5f, stats.getMana() / 50f);
        }
    }


    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!isStunned || e.getPlayer().getUniqueId() != uuid) return;
        if (arena.getState() != GameState.LIVE) {
            isStunned = false;
            return;
        }

        // Prevent movement while stunned
        if (e.getTo().getX() != e.getFrom().getX() || e.getTo().getZ() != e.getFrom().getZ()) {
            e.setTo(new Location(e.getFrom().getWorld(), e.getFrom().getX(),
                    e.getTo().getY(), e.getFrom().getZ(), e.getTo().getYaw(), e.getTo().getPitch()));
        }
    }

    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean isBeingErased() {
        return isBeingErased;
    }

    @Override
    public boolean shouldUseAbilityAttack() {
        return isAwakened && gearshiftActive;
    }

    @Override
    public void useAbilityAttack(Player target) {

        Player player = Bukkit.getPlayer(uuid);
        // Add Fa-Jin energy on successful hit
        faJinSystem.addEnergy(10);

        // Handle Transmission attack in Gearshift
        // Increase gear level
        if (gearLevel < 3) {
                gearLevel++;
                updateDefaultItem();
                ((EnhancedPlayerStats) stats).setGearDisplay(getGearDisplay());
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "GEAR SHIFT: " + getGearDisplay());

                // Add speed boost
                PotionEffect speed = new PotionEffect(PotionEffectType.SPEED,
                        Integer.MAX_VALUE, 6 + gearLevel, false, false);
                player.addPotionEffect(speed, true);

                // Transmission damage (increases with each gear)
                int transmissionDamage = 20 + (gearLevel * 15);

                getAbilities().get(0).doDamageTo(player, target, transmissionDamage, CustomDeathCause.TRANSMISSION);

                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 0.5f + (gearLevel * 0.3f));
                target.sendMessage(ChatColor.RED + "You were hit by " + getGearDisplay() + "!");
            }

    }

    @Override
    public void switchAbilitySet(AbilitySets set) {
        if (secondaryAbilities.isEmpty()) {
            for (Ability ability : abilities) {
                secondaryAbilities.add(ability);
                ability.setActive(false);
            }
            abilities.clear();
            registerAbilities(set);
        } else {
            for (Ability ability : abilities) {
                ability.setActive(false);
                transferList.add(ability);
            }
            abilities.clear();
            for (Ability ability : secondaryAbilities) {
                ability.setActive(true);
                abilities.add(ability);
            }
            secondaryAbilities.clear();
            secondaryAbilities.addAll(transferList);
            transferList.clear();

            for (Ability ability : secondaryAbilities) {
                ability.setActive(false);
            }

            this.currentSet = set;
        }
    }

    @Override
    public AbilitySets getCurrentSet() {
        return currentSet;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public UUID getOriginalId() {
        return getUUID();
    }

    @Override
    public AbilitySets[] getAvailableSets() {
        return availableSets;
    }

    @EventHandler
    public void onErase(QuirkErasureEvent e) {
        if (e.getQuirk() != this) return;
        if (e.isErasing()) {
            resetPowerNoRoll();
            canPowerUp = false;
            isBeingErased = true;

            if (isAwakened && gearshiftActive) {
                deactivateGearshift();
            }
        }
        if (!e.isErasing()) {
            canPowerUp = true;
            isBeingErased = false;
        }
    }

    // Awakened interface methods
    @Override
    public boolean isAwakened() {
        return isAwakened;
    }

    @Override
    public Awakened.AwakendStats getAwakenedStats() {
        return new Awakened.AwakendStats(500, 500, 80, 6, 0, 120);
    }

    @Override
    public void handleAwakenedTick() {
        if (!isAwakened || !gearshiftActive) return;

        Player player = Bukkit.getPlayer(uuid);

        // Check Gearshift duration (5 minutes)
        if (System.currentTimeMillis() - gearshiftStartTime >= 120000L) {
            deactivateGearshift();
            startStunPeriod(player);
        }
    }

    @Override
    public void cleanupAwakened() {
        if (dangerSenseTask != null) {
            dangerSenseTask.cancel();
        }
        if (gearshiftTask != null) {
            gearshiftTask.cancel();
        }
        if (powerChargeTask != null) {
            powerChargeTask.cancel();
        }

        if (gearshiftActive) {
            deactivateGearshift();
        }
    }

    // Enhanced OFA specific methods
    public void activateGearshift() {
        if (gearshiftActive || isStunned) return;

        Player player = Bukkit.getPlayer(uuid);
        gearshiftActive = true;
        gearshiftStartTime = System.currentTimeMillis();
        gearLevel = 0;
        startEnhancedPowerCharging(player);
        // Update the gear display if using EnhancedPlayerStats
        if (stats instanceof EnhancedPlayerStats) {
            ((EnhancedPlayerStats) stats).setGearDisplay(getGearDisplay());
        }

        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 5, false, false);
        player.addPotionEffect(speed, true);

        // Visual/audio effects
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "GEARSHIFT ACTIVATED!");
        player.sendMessage(ChatColor.YELLOW + "You have 2 minutes before the drawback!");
        player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 1.0f, 1.5f);

        // Start monitoring
        gearshiftTask = new BukkitRunnable() {
            int seconds = 0;

            @Override
            public void run() {
                if (!gearshiftActive || arena.getState() != GameState.LIVE) {
                    cancel();
                    return;
                }

                seconds++;

                // Warning messages
                if (seconds == 60) { // 1 minute warning
                    player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "WARNING: 1 minute of Gearshift remaining!");
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 0.5f);
                } else if (seconds == 90) { // 30 seconds
                    player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "WARNING: 30 seconds of Gearshift remaining!");
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 0.5f);
                } else if (seconds >= 110 && seconds < 120) { // Final countdown
                    int remaining = 120 - seconds;
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + remaining + "...");
                    player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                }

                handleAwakenedTick();
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);

        updateDefaultItem();
    }

    private void deactivateGearshift() {
        if (!gearshiftActive) return;

        Player player = Bukkit.getPlayer(uuid);
        gearshiftActive = false;
        gearLevel = 0;

        // Remove speed effect
        player.removePotionEffect(PotionEffectType.SPEED);

        // Reset power to minimum
        stats.setMana(minimumPower);
        player.setWalkSpeed(.2F);

        if (gearshiftTask != null) {
            gearshiftTask.cancel();
        }

        updateDefaultItem();
    }

    private void startStunPeriod(Player player) {
        isStunned = true;
        canPowerUp = false;

        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "GEARSHIFT DRAWBACK!");
        player.sendMessage(ChatColor.DARK_RED + "You are stunned for 30 seconds!");
        player.playSound(player.getLocation(), Sound.ANVIL_LAND, 1.0f, 0.5f);

        // Apply slowness and weakness
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 600, 10, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 600, 10, false, false));

        // Disable all abilities
        for (Ability ability : abilities) {
            ability.setActive(false);
        }
        SwitchAbilitySetTest switcher = (SwitchAbilitySetTest) arena.getQuirkBattleGame().getAbilityManager().getAbilityFromQuirk(this, Abilities.OFA_ABILITY_SWITCH_TEST);
        switcher.setActive(false);


        // Put Gearshift on permanent cooldown
        // TODO: Add Gearshift ability to cooldown

        new BukkitRunnable() {
            int seconds = 30;

            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE) {
                    isStunned = false;
                    cancel();
                    return;
                }

                seconds--;

                if (seconds <= 0) {
                    isStunned = false;
                    canPowerUp = true;

                    // Re-enable abilities
                    for (Ability ability : abilities) {
                        ability.setActive(true);
                    }
                    SwitchAbilitySetTest switcher = (SwitchAbilitySetTest) arena.getQuirkBattleGame().getAbilityManager().getAbilityFromQuirk(thisForRunnable, Abilities.OFA_ABILITY_SWITCH_TEST);
                    switcher.setActive(true);

                    player.removePotionEffect(PotionEffectType.SLOW);
                    player.removePotionEffect(PotionEffectType.WEAKNESS);

                    player.sendMessage(ChatColor.GREEN + "You've recovered from the Gearshift drawback!");
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                    cancel();
                } else if (seconds % 5 == 0) {
                    player.sendMessage(ChatColor.YELLOW + "Stunned for " + seconds + " more seconds...");
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }

    // Getters for other classes to check state
    public FaJinSystem getFaJinSystem() {
        return faJinSystem;
    }

    public boolean isGearshiftActive() {
        return gearshiftActive;
    }

    public int getGearLevel() {
        return gearLevel;
    }

    public boolean isStunned() {
        return isStunned;
    }


}