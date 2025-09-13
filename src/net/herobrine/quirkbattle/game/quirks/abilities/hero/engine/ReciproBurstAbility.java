package net.herobrine.quirkbattle.game.quirks.abilities.hero.engine;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.Engine;
import net.herobrine.quirkbattle.util.Quirk;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ReciproBurstAbility extends Ability implements SpecialCase {

    private final Engine engine;

    public ReciproBurstAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.engine = (Engine) quirk;
    }


    @Override
    public void doAbility(Player player) {
    if (arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.translateString("&a&lWATCH OUT! ") + arena.getTeam(player).getColor() + player.getName() + ChatColor.YELLOW + " has activated their " + ability.getDisplay() + ChatColor.YELLOW + " ability!");
    else arena.sendMessage(HerobrinePVPCore.translateString("&a&lWATCH OUT! ") + HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.YELLOW + " has activated their " + ability.getDisplay() + ChatColor.YELLOW + " ability!");
    player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 1f, 1f);
    player.getWorld().createExplosion(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 2f, false, false);
    doExplosionCollision(player.getLocation());
    player.sendMessage(ChatColor.GREEN + "Your engines explode with extreme speed! For the next 10 seconds, or until you overheat, make all your enemies eat your firey dust!");
    engine.setBoostLevel(4);
    doVFX(player);
    }

    public void doVFX(Player player) {
    new BukkitRunnable() {
        int ticks = 0;
        @Override
        public void run() {
            if (ticks > 200 || engine.getActiveBoosts() < 4) {
                cancel();
                if (arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.translateString("&a&lPHEW! ") + arena.getTeam(player).getColor() + player.getName() + "'s " + ability.getDisplay() + ChatColor.YELLOW + " is over.");
                else arena.sendMessage(HerobrinePVPCore.translateString("&a&lPHEW! ") + HerobrinePVPCore.getRankColor(player) + player.getName() + "'s " + ability.getDisplay() + ChatColor.YELLOW + " is over.");
                player.sendMessage(ChatColor.RED + "Your Recipro Burst is no longer active.");
                player.playSound(player.getLocation(), Sound.WITHER_SHOOT, .6f, .8f);
                engine.setBoostLevel(0);
                setCooldown(System.currentTimeMillis());
                doAbilityCooldown();
                return;
            }

            Vector vec = player.getLocation().getDirection();
            vec.normalize().multiply(-1);
            Location loc = player.getLocation().add(vec);
            spawnParticle(loc, EnumParticle.FLAME, true);
            spawnParticle(loc, EnumParticle.FLAME, true);
            spawnParticle(loc, EnumParticle.FLAME, true);
            if (ticks % 3 == 0) doCollision(loc);
            ticks++;
        }
    }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);

    }

    public void doCollision(Location loc) {
    for (Entity ent : loc.getWorld().getNearbyEntities(loc, ability.getRadius(), 1, ability.getRadius())) {
        if (!(ent instanceof Player)) continue;
        Player player = (Player) ent;
        Player caster = Bukkit.getPlayer(uuid);
        if (player.getUniqueId() == caster.getUniqueId()) continue;
        if (arena.getType().isTeamsMode()){
            if (arena.getTeam(player).equals(arena.getTeam(caster))) continue;
        }
        doDamageTo(caster, player, ability.getDamage(), CustomDeathCause.RECIPRO_BURST);

        player.playSound(player.getLocation(), Sound.FIZZ, 1f, 1f);

        spawnRGBParticles(player.getPlayer().getEyeLocation().add(0, 1.5, 0),  179, 67, 27, false);

    }
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
            doDamageTo(caster, player, 15, CustomDeathCause.RECIPRO_BURST);

            player.playSound(player.getLocation(), Sound.FIZZ, 1f, 1f);


        }
    }


    @Override
    public boolean doesCasePass(Player player) {
        return engine.getActiveBoosts() != 4;
    }

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "You already have Recipro Burst active!");
    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}
