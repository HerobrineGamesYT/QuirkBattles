package net.herobrine.quirkbattle.event;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.core.LevelRewards;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.hero.Explosion;
import net.herobrine.quirkbattle.game.quirks.hero.Hardening;
import net.herobrine.quirkbattle.game.quirks.hero.OneForAll;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

public class QuirkBattlesListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();

            Arena arena;
            if (Manager.isPlaying(player)) arena = Manager.getArena(player);

            else return;

            if (arena.getGame(arena.getID()).equals(Games.QUIRK_BATTTLE) && arena.getState().equals(GameState.LIVE)) {
                   /* We'll deal damage by calling damage events with the amount of raw damage we would like to deal, HOWEVER, we will never let the player die in the raw MC fashion. We shall run a calculation to
                   determine how much damage we should be dealing to the player's health in the game, and how that should reflect on their health bar.
                   In the event that we must "kill" the player, we'll just simply teleport them and do all the yada yada we need to do.*/

                int health = arena.getQuirkBattleGame().getStats(player).getHealth();
                int maxHealth = arena.getQuirkBattleGame().getStats(player).getMaxHealth();
                int defense = arena.getQuirkBattleGame().getStats(player).getDefense();
                double damage = e.getDamage();
                e.setCancelled(true);
                if (damage == 0) return;
                if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL) || e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) return;
                player.damage(0);
                double damageReduction = (double)defense / ((double)defense + 100);
                int trueDamage;
                if (defense != 0) trueDamage = (int)Math.round(damage - (damage * damageReduction));
                else trueDamage = (int)Math.round(damage);
                player.sendMessage(ChatColor.GREEN + "You just took " + ChatColor.RED + trueDamage + "❁ Damage!");
                player.sendMessage(ChatColor.GREEN + "Cause:  "+ e.getCause());
                if (e.getCause().equals(EntityDamageEvent.DamageCause.CUSTOM)) {
                    player.sendMessage(ChatColor.GREEN + "Custom Cause: " + arena.getQuirkBattleGame().getCustomDeathCause().get(player.getUniqueId()));
                    if (arena.getQuirkBattleGame().getLastAbilityAttacker().containsKey(player.getUniqueId())) player.sendMessage(ChatColor.GREEN + "Last Attacker: " + Bukkit.getPlayer(arena.getQuirkBattleGame().getLastAbilityAttacker().get(player.getUniqueId())).getName());
                }
                int newHealth = health - trueDamage;
                if(!(newHealth <= 0)) {
                    arena.getQuirkBattleGame().getStats(player).setHealth(newHealth);
                    arena.getQuirkBattleGame().updatePlayerStats(player);

                    if (arena.getClass(player).equals(ClassTypes.HARDENING)) {
                        // Potential Nerf: Damage -> True Damage = Less Stamina Gained.
                        Hardening hardening = (Hardening) arena.getClasses().get(player.getUniqueId());
                        hardening.giveStaminaBoostForDamage(damage);
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "You've been knocked out!");
                    handleDeath(player, Bukkit.getPlayer(arena.getQuirkBattleGame().getLastAbilityAttacker().get(player.getUniqueId())), arena);
                    Bukkit.getScheduler().runTask(QuirkBattlesPlugin.getInstance(), () -> arena.setSpectator(player));
                    arena.getQuirkBattleGame().getAlivePlayers().remove(player.getUniqueId());
                    arena.getQuirkBattleGame().isGameOver();
                }
                e.setDamage(0);
              //  double healthPercent = (double)newHealth / (double)maxHealth;
              //  double playerHealth = player.getMaxHealth() * healthPercent;
                // if (!(playerHealth <= 1)) player.setHealth(playerHealth);
              //  else player.setHealth(1);
            }

        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            Player player = (Player) e.getDamager();
            Player target = (Player) e.getEntity();

            Arena arena;
            if (Manager.isPlaying(player)) arena = Manager.getArena(player);

            else return;
            if (arena.getGame().equals(Games.QUIRK_BATTTLE) && arena.getState().equals(GameState.LIVE)) {
                Class kit = arena.getClasses().get(player.getUniqueId());
                Quirk quirk = (Quirk) kit;
                e.setCancelled(true);
                e.setDamage(0);
                if (arena.getType().equals(GameType.ONE_V_ONE)) {
                    double damage = arena.getClass(player).getBaseDamage();
                    if (quirk.shouldUseAbilityAttack()) {
                        quirk.useAbilityAttack(target);
                        return;
                    }
                    if (hasPowerUpClass(player, arena)) {
                        player.sendMessage(ChatColor.GREEN + "You just attacked someone while using One For All!");
                        damage =  damage + (arena.getClass(player).getBaseDamage() * ((double) arena.getQuirkBattleGame().getStats(player).getMana() /100));;
                        OneForAll ofa = (OneForAll) arena.getClasses().get(player.getUniqueId());
                        ofa.resetPower();
                    }
                   else if (arena.getClass(player).equals(ClassTypes.EXPLOSION)) {
                        Explosion explosion = (Explosion) arena.getClasses().get(player.getUniqueId());
                        explosion.giveStaminaBoost(2);
                    }
                    @SuppressWarnings("deprecation")
                    EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, damage);
                    arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.GENERAL_ATTACK);
                    arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                    Bukkit.getPluginManager().callEvent(event);
                    target.setLastDamageCause(event);
                }
                else {
                    if (arena.getTeam(player) != arena.getTeam(target)) {
                        double damage = arena.getClass(player).getBaseDamage();
                        if (quirk.shouldUseAbilityAttack()) {
                            quirk.useAbilityAttack(target);
                            return;
                        }
                        if (hasPowerUpClass(player, arena)) {
                            damage =  damage + (arena.getClass(player).getBaseDamage() * ((double) arena.getQuirkBattleGame().getStats(player).getMana() /100));;
                            OneForAll ofa = (OneForAll) arena.getClasses().get(player.getUniqueId());
                            ofa.resetPower();
                        }
                       else if (arena.getClass(player).equals(ClassTypes.EXPLOSION)) {
                            Explosion explosion = (Explosion) arena.getClasses().get(player.getUniqueId());
                            explosion.giveStaminaBoost(2);
                        }
                        @SuppressWarnings("deprecation")
                        EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, damage);
                        arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.GENERAL_ATTACK);
                        arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                        Bukkit.getPluginManager().callEvent(event);
                        target.setLastDamageCause(event);
                    }
                }
            }
        }
    }


    public boolean hasPowerUpClass(Player player, Arena arena) {
      return arena.getClass(player).equals(ClassTypes.ONEFORALL);
    }

    public void handleDeath(Player player, Player killer, Arena arena) {
       player.playSound(player.getLocation(), Sound.BAT_DEATH, 1f, 1f);
        if (player != killer && killer != null) {
            killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
            LevelRewards prestige = HerobrinePVPCore.getFileManager().getPrestige(HerobrinePVPCore.getFileManager().getPlayerLevel(killer.getUniqueId()));
            int baseKillCoins = 25;
            int earnedCoins = (int)Math.round(baseKillCoins * prestige.getGameCoinMultiplier());

            HerobrinePVPCore.getFileManager().addCoins(killer, earnedCoins);
            killer.sendMessage(ChatColor.YELLOW + "+" + earnedCoins + " coins! (Kill)");
        }
       switch (arena.getQuirkBattleGame().getCustomDeathCause().get(player.getUniqueId())) {
           case SHOOT_STYLE:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " just fell victim to " + HerobrinePVPCore.getRankColor(killer) + killer.getName() + "'s " + HerobrinePVPCore.translateString("&a&lShoot Style &r&7attack!"));
               else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " just fell victim to "+ arena.getTeam(killer).getColor() + killer.getName() + "'s " + HerobrinePVPCore.translateString("&a&lShoot Style&r &7attack!"));
               break;
           case DETRIOT_SMASH:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + HerobrinePVPCore.translateString(" &7just got &6&lDETROIT SMASH'D &r&7by ") + HerobrinePVPCore.getRankColor(killer) + killer.getName());
               else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + HerobrinePVPCore.translateString(" &7just got &6&lDETROIT SMASH'D &r&7by ") + arena.getTeam(player).getColor() + killer.getName());
               break;
           case ONE_FOR_ALL_SELF:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " couldn't handle the power of " + HerobrinePVPCore.translateString("&6&lOne For All"));
               else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " couldn't handle the power of " + HerobrinePVPCore.translateString("&6&lOne For All"));
               break;
           case GENERAL_ATTACK:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getRankColor(killer) + killer.getName() + ChatColor.GRAY + " eliminated " + HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " with their pure strength.");
               else arena.sendMessage(arena.getTeam(killer).getColor() + killer.getName() + ChatColor.GRAY + " eliminated " + arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " with their pure strength.");
               break;
           case HOWITZER_IMPACT:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getRankColor(killer) + killer.getName() + ChatColor.GRAY + " just eliminated " + HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " with " + HerobrinePVPCore.translateString("&6&lHOWITZER IMPACT!"));
               else arena.sendMessage(arena.getTeam(killer).getColor() + killer.getName() + ChatColor.GRAY + " just eliminated " + arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " with " + HerobrinePVPCore.translateString("&6&lHOWITZER IMPACT!"));
               break;
           case EXPLOSION_DASH:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.translateString("&6&lBOOM! ") + HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " just ate " + HerobrinePVPCore.getRankColor(killer) + killer.getName() + ChatColor.GRAY + "'s explosive dust.");
               else arena.sendMessage(HerobrinePVPCore.translateString("&6&lBOOM! ") + arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " just ate " + arena.getTeam(killer).getColor() + killer.getName() + ChatColor.GRAY + "'s explosive dust.");
               break;
           case EXPLOSION_PUNCH:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.translateString("&6&lEXPLOSION! ") + HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " just got exploded by " + HerobrinePVPCore.getRankColor(killer) + killer.getName());
               else arena.sendMessage(HerobrinePVPCore.translateString("&6&lEXPLOSION! ") + arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " just got exploded by " + arena.getTeam(killer).getColor() + killer.getName());
               break;
           case SHARP_CLAW:
               Hardening killClass =  (Hardening) arena.getClasses().get(killer.getUniqueId());
               boolean isUnbreakable = killClass.isUnbreakable();
               if (arena.getType().equals(GameType.ONE_V_ONE)) {
                   if (isUnbreakable) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + HerobrinePVPCore.translateString(" &7couldn't handle the &c&lUNBREAKABLE &r&7power of ") + HerobrinePVPCore.getRankColor(killer) + killer.getName() + ChatColor.GRAY + "!");
                   else arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + HerobrinePVPCore.translateString(" &7just got &cclawed&r &7by ") + HerobrinePVPCore.getRankColor(killer) + killer.getName());
               }
               else {
                   if (isUnbreakable) arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + HerobrinePVPCore.translateString(" &7couldn't handle the &c&lUNBREAKABLE &r&7power of ") + arena.getTeam(killer).getColor() + killer.getName() + ChatColor.GRAY + "!");
                   else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + HerobrinePVPCore.translateString(" &7just got &cclawed&r &7by ") + arena.getTeam(killer).getColor() + killer.getName());
               }
               break;
           case STONE_CHARGE:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.translateString("&c&lCLACK! ") + HerobrinePVPCore.getRankColor(player) + player.getName() + HerobrinePVPCore.translateString(" &7 just got splattered by ") + HerobrinePVPCore.getRankColor(killer) + killer.getName() + HerobrinePVPCore.translateString("&7's &c&lStone Charge&r&7!"));
               else arena.sendMessage(HerobrinePVPCore.translateString("&c&lCLACK! ") + arena.getTeam(player).getColor() + player.getName() + HerobrinePVPCore.translateString(" &7 just got splattered by ") + arena.getTeam(killer).getColor() + killer.getName() + HerobrinePVPCore.translateString("&7's &c&lStone Charge&r&7!"));
            break;
           case OUTSIDE_MAP:
               if (killer == null) {
                   if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " left the arena.");
                   else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " left the arena.");
               }
               else {
                   if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " left the arena while fighting " + HerobrinePVPCore.getRankColor(killer) + killer.getName());
                   else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " left the arena while fighting " + arena.getTeam(killer).getColor() + killer.getName());
               }
               break;
           default:
               if (arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GRAY + " has died.");
               else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.GRAY + " has died.");
               return;

       }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (Manager.isPlaying(e.getEntity())) {
            Arena arena = Manager.getArena(e.getEntity());

            if (arena.getGame().equals(Games.QUIRK_BATTTLE)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        e.getEntity().spigot().respawn();
                    }
                }.runTaskLater(QuirkBattlesPlugin.getInstance(), 2L);
            }
        }
    }

}
