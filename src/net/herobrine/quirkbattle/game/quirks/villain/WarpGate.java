package net.herobrine.quirkbattle.game.quirks.villain;

import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Forcible;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarpGate extends Class implements Quirk, Forcible {
    private final List<Ability> abilities;
    private PlayerStats stats;
    private final Arena arena;
    private Player player;
    private final UUID originalId;

    private boolean isStunned = false;

    private boolean explosivePunch = false;

    private boolean isBeingErased = false;


    public WarpGate(UUID uuid) {
        super(uuid, ClassTypes.WARPGATE);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        player = Bukkit.getPlayer(uuid);
        this.originalId = uuid;
        this.abilities = new ArrayList<>();
    }

    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(player.getUniqueId(), 200, 200, 40, 300, 300, 1);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);

        player.getInventory().clear();

        // Basic attack item
        net.herobrine.core.ItemBuilder basicAttack = new net.herobrine.core.ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.LIGHT_PURPLE + "Disorient your enemies with your powers!");
        basicAttack.setLore(ChatColor.GRAY + "Use your portals to confuse enemies!");

        player.getInventory().setItem(0, basicAttack.build());
        registerAbilities(AbilitySets.WARP_GATE);

        player.getInventory().setHeldItemSlot(0);
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
        return false;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public UUID getOriginalId() {
        return originalId;
    }

    @Override
    public void useAbilityAttack(Player target) {

    }

    @Override
    public void registerAbilities(AbilitySets set) {
        int i = 2;
        for (net.herobrine.quirkbattle.game.quirks.abilities.Abilities ability : set.getAbilities()) {
            abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
            i++;
        }
    }

    @Override
    public void doForcibleAbility(AllForOne afo) {
        Location loc = Bukkit.getPlayer(afo.getUniqueId()).getLocation();
        Vector dir = loc.getDirection().normalize();
        Vector up = new Vector(0, 1, 0);

        // If the portal is facing straight up/down, choose a safe up vector
        if (Math.abs(dir.dot(up)) > 0.99) {
            up = new Vector(1, 0, 0);
        }

        // Build the portal plane (right + up in relation to the portal's direction)
        Vector right = dir.clone().crossProduct(up).normalize();
        Vector portalUp = right.clone().crossProduct(dir).normalize();

        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 60 || afo.isBeingErased()) { cancel(); return; }

                double baseRadius = 5.0;
                double pullRadius = 5.0;

                // === 1. Dark Center Core ===
                for (int i = 0; i < 8; i++) {
                    Location core = loc.clone().add(
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3
                    );
                    abilities.get(0).spawnRGBParticles(core, 20, 0, 20, true); // dark purple/black
                }

                // === 2. Swirling Vortex in Portal Plane ===
                for (double t = 0; t < 2 * Math.PI; t += Math.PI / 15) {
                    double radius = baseRadius - (ticks % 40) * 0.05;
                    if (radius < 0.3) radius = baseRadius;

                    double x = Math.cos(t + angle) * radius;
                    double y = Math.sin(t + angle) * radius;

                    // Rotate circle into the portal's plane
                    Vector offset = right.clone().multiply(x).add(portalUp.clone().multiply(y));
                    Location particleLoc = loc.clone().add(offset);
                    abilities.get(0).spawnRGBParticles(particleLoc, 50, 0, 100, true);
                }

                // === 3. Misty Tendrils Around ===
                if (Math.random() < 0.2) {
                    for (int i = 0; i < 2; i++) {
                        double dx = (Math.random() - 0.5) * 2.5;
                        double dy = (Math.random() * 0.8);
                        double dz = (Math.random() - 0.5) * 2.5;

                        Vector tendrilOffset = right.clone().multiply(dx)
                                .add(portalUp.clone().multiply(dy))
                                .add(dir.clone().multiply(dz));

                        Location tendril = loc.clone().add(tendrilOffset);
                        abilities.get(0).spawnRGBParticles(tendril, 70, 0, 120, true);
                    }
                }

                for (LivingEntity entity : loc.getWorld().getLivingEntities()) {

                    if (entity instanceof Player) {
                        if (arena.getType().isTeamsMode()) {
                            Player target = (Player) entity;
                            if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
                        }
                    }

                    if (entity.equals(player)) continue;
                    if (entity.getLocation().distance(loc) <= pullRadius) {
                        Vector pull = loc.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.3);
                        entity.setVelocity(entity.getVelocity().add(pull));
                    }
                }

                angle += Math.PI / 40;
                ticks += 2;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }
}
