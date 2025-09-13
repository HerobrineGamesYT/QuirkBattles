package net.herobrine.quirkbattle.game.quirks.abilities.villain.blueflame;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IncinerateAbility extends Ability implements SpecialCase {
    private final Player player;
    private boolean isAuraActive = false;
    private int addToY = 0;
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final int duration = 10;

    public IncinerateAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.player = Bukkit.getPlayer(uuid);
    }

    @Override
    public void doAbility(Player player) {
        // Play sound effects
        player.playSound(player.getLocation(), Sound.FIZZ, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5f, 1.5f);

        // Send messages
        player.sendMessage(ChatColor.BLUE + "You explode with flames and start incinerating everything around you!");
        if (arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.translateString("&a&lWATCH OUT! ") + arena.getTeam(player).getColor() + player.getName() + ChatColor.YELLOW + " has activated their " + ability.getDisplay() + ChatColor.YELLOW + " ability!");
        else arena.sendMessage(HerobrinePVPCore.translateString("&b&lWATCH OUT! ") + HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.YELLOW + " has activated their " + ability.getDisplay() + ChatColor.YELLOW + " ability!");
        player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 1f, 1f);
        player.getWorld().createExplosion(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 2f, false, false);
        doExplosionCollision(player.getLocation());
        // Create the decay aura
        doVFX();
    }
    public void doExplosionCollision(Location loc) {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
            if (!(ent instanceof Player)) continue;
            Player player = (Player) ent;
            Player caster = Bukkit.getPlayer(uuid);
            if (player.getUniqueId() == caster.getUniqueId()) continue;
            if (arena.getType().isTeamsMode()){
                if (arena.getTeam(player).equals(arena.getTeam(caster))) continue;
            }

            doDamageTo(caster, player, 30, CustomDeathCause.INCINERATE);
        }
    }

    public void doVFX() {
        isAuraActive = true;
        lastDamageTime.clear();

        // Create visual effect and damage application for the decay aura
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20; // Convert seconds to ticks (20 ticks per second)

            @Override
            public void run() {
                if (ticks >= maxTicks || !isAuraActive || !isActive() ||  arena.getState() != GameState.LIVE ||
                        !arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                    isAuraActive = false;
                    if (isActive()) {
                        setCooldown(System.currentTimeMillis());
                        doAbilityCooldown();
                    }

                    player.sendMessage(ChatColor.RED + "Your incineration flames have worn off!");
                    player.playSound(player.getLocation(), Sound.FIZZ, 0.5f, 0.5f);
                    cancel();
                    return;
                }

                // Create visual effect for the aura
                if (ticks % 5 == 0) { // Every 5 ticks (0.25 seconds) to reduce particle load
                    createAuraVisualEffect();
                }

                // Apply damage to nearby players every second
                if (ticks % 20 == 0) { // Every 20 ticks (1 second)
                    applyAuraDamage();
                }

                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }


    private void createAuraVisualEffect() {
        Location center = player.getLocation();

        if (addToY > 3) addToY = 0;
        for (double t = 0; t < 1000; t += 0.5) {
            double x = ability.getRadius() * Math.sin(t);
            double z = ability.getRadius() * Math.cos(t);

            spawnRGBParticles(new Location(center.getWorld(), center.getX() + x, center.getY() + addToY, center.getZ() + z), 66, 135, 245, false);
            spawnRGBParticles(new Location(center.getWorld(), center.getX() + x, center.getY(), center.getZ() + z), 135, 206, 250, true);
        }
        addToY = addToY + 1;
    }

    private void applyAuraDamage() {
        // Get nearby players
        for (Entity entity : player.getNearbyEntities(ability.getRadius(), ability.getRadius(), ability.getRadius())) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;

            // Skip spectators and dead players
            if (arena.getSpectators().contains(target.getUniqueId())) continue;
            //if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;

            // Skip teammates in team modes
            if (arena.getType().isTeamsMode() && arena.getTeam(player).equals(arena.getTeam(target))) continue;

            // Skip self
            if (target == player) continue;

            // Apply damage
            doDamageTo(player, target, ability.getDamage(), CustomDeathCause.INCINERATE);

            // Send messages only the first time a player is damaged
            if (!lastDamageTime.containsKey(target.getUniqueId())) {
                target.sendMessage(ChatColor.BLUE + player.getName() + ChatColor.GRAY + "'s flames are damaging you!");
                player.sendMessage(ChatColor.BLUE + "Your flames are damaging " + target.getName() + "!");
            }

            // Update last damage time
            lastDamageTime.put(target.getUniqueId(), System.currentTimeMillis());

            // Play sound effect at target
            target.playSound(target.getLocation(), Sound.FIZZ, 0.5f, 0.5f);

            // Visual effect at target
            target.getWorld().playEffect(target.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.LAPIS_BLOCK);
        }
    }



    @Override
    public boolean doesCasePass(Player player) {
        if (ability.getCost() + stats.getTemp() > stats.getMaxTemp()) {
            return false;
        }

        return !isAuraActive;
    }

    public void stopAura() {
        if (isAuraActive) {
            player.sendMessage(ChatColor.RED + "Your incineration flames have worn off!");
            player.playSound(player.getLocation(), Sound.FIZZ, 0.5f, 0.5f);
        }
        isAuraActive = false;
    }

    @Override
    public void doNoPass(Player player) {

        if (isAuraActive) player.sendMessage(ChatColor.RED + "You already have your incinerate ability active!");
        else player.sendMessage(ChatColor.RED + "Do you want to burn yourself?? Cool down a bit by attacking before using this!");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}
