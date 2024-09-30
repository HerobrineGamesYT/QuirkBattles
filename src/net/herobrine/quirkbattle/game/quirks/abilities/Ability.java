package net.herobrine.quirkbattle.game.quirks.abilities;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.core.ItemBuilder;
import net.herobrine.core.SkullMaker;
import net.herobrine.gamecore.*;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.event.FrostbiteEvent;
import net.herobrine.quirkbattle.event.OverheatEvent;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.NBTReader;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.Class;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public abstract class Ability implements Listener {
    // ability quirk is using
    protected Abilities ability;
    //quirk using the ability, given when initializing the ability class.
    protected net.herobrine.gamecore.Class quirk;

    //arena id
    protected int id;
    protected int slot;

    // There will be 1 Item Ability class per Quirk Battles arena, per player that uses a quirk with the ability attached...

    // Last use of the ability.
    protected long cooldown;

    // Player using the ability.
    protected UUID uuid;

    protected Arena arena;


    protected PlayerStats stats;

    protected boolean active;
    public Ability(Abilities ability, net.herobrine.gamecore.Class quirk, int id, int slot) {
        this.ability = ability;
        this.quirk = quirk;
        this.id = id;
        this.slot = slot;
        this.cooldown = 0;
        this.uuid = quirk.getUUID();
        this.arena = Manager.getArena(id);
        this.stats = arena.getQuirkBattleGame().getStats(Bukkit.getPlayer(uuid));
        this.active = true;
        Bukkit.getPluginManager().registerEvents(this, QuirkBattlesPlugin.getInstance());

    }


    // The doAbility method is REQUIRED for all abilities. To make things flexible, an ability will have 2-3 listeners
    // for each ability type. They will check if the ability type is correct and act based upon that. This way, we don't have to add more code when
    // changing the ability type, but having it in one method will allow us to not duplicate our code at the same time and make each ability class
    // unnecessarily long.
    // **WHAT ABOUT PASSIVE ABILITIES??**

    // Passive Abilities are considered special cases. We will rarely use them except for certain armor bonuses. In a lot of cases, an Item Ability for "Passive"
    // could even just be flavor text for the real functional usage of the item. Abilities of this type will not use multiple listeners and will be
    // implemented on a case-by-case basis.
    public abstract void doAbility(Player player);

    public void executeAbility(Player player) {
        if(!shouldDoAbility(player)) return;
        player.sendMessage(ChatColor.GREEN + "Doing ability " + ability);
        doAbility(player);
    }

    public boolean hasManaCost() {return ability.getCost() > 0 && !stats.useTemperature();}

    public boolean hasCooldown() {return ability.getCooldown() > 0;}

    public boolean shouldScheduleTask() {return ability.getCooldown() < 2000;}
    public long getCooldown() {return cooldown;}
    public void setCooldown(long cooldown) {this.cooldown = cooldown;}

    public int getId() {return id;}

    public void doDamageTo(Player damager, Player target, double damage, int power, CustomDeathCause cause) {
        Arena arena = Manager.getArena(damager);
        arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), damager.getUniqueId());
        arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), cause);

        double dmg = damage + (damage * ((double) power /100));
        target.damage(0);
        @SuppressWarnings("deprecation")
        EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, dmg);
        target.setLastDamageCause(event);
        Bukkit.getPluginManager().callEvent(event);

    }
    public void doDamageTo(Player damager, Player target, double damage, CustomDeathCause cause) {
        Arena arena = Manager.getArena(damager);
        arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), damager.getUniqueId());
        arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), cause);


        target.damage(0);
        @SuppressWarnings("deprecation")
        EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, damage);
        target.setLastDamageCause(event);
        Bukkit.getPluginManager().callEvent(event);

    }

    public ItemStack getItem() {
        net.herobrine.core.ItemBuilder item = new ItemBuilder(ability.getMaterial());

        item.addItemFlag(ItemFlag.HIDE_ATTRIBUTES);
        item.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        item.addItemFlag(ItemFlag.HIDE_UNBREAKABLE);
        item.setUnbreakable(true);

        item.setDisplayName(this.getAbility().getDisplay());


        item.setLore(doLore());
        ItemStack itemStack = item.build();
        NBTReader reader = new NBTReader(itemStack);
        reader.writeStringNBT("id", ability::name);
        return reader.toBukkit();
    }

    public ArrayList<String> doLore() {
        ArrayList<String> lore = new ArrayList<String>();
        //We'll add a blank line if the item has any stats, for UI cleanliness between the stats and lore.
        try {for (String string : ability.getDescription()) {lore.add(ChatColor.GRAY + string);}}
        catch(NullPointerException e) {lore.add(ChatColor.RED + "No description set.");}
        lore.add(" ");
        boolean shouldAddSpace = false;

        if (ability.getDamage() != 0) {
            lore.add(ChatColor.DARK_GRAY + "Base Damage: " + ChatColor.RED + Math.round(ability.getDamage()));
            shouldAddSpace = true;
        }
        if (ability.getDefenseBoost() != 0) {
            lore.add(ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN +  "+" + ability.getDefenseBoost());
            shouldAddSpace = true;
        }

        if (shouldAddSpace) lore.add(" ");
        if(ability.getCost() != 0 && !stats.useTemperature()) lore.add(ChatColor.DARK_GRAY + "Stamina Cost: " + ChatColor.DARK_AQUA + ability.getCost());
        if (ability.getCost() != 0 && stats.useTemperature()) lore.add(ChatColor.DARK_GRAY + "Temperature Cost: " + ChatColor.GREEN + ability.getCost());
        if (ability.getCooldown() != 0) lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GREEN + (float)ability.getCooldown() / 1000 + "s");

        return lore;
    }
    public Abilities getAbility() {return ability;}
    public net.herobrine.gamecore.Class getQuirk() {return quirk;}

    public boolean shouldDoAbility(Player player) {
        if (!arena.getState().equals(GameState.LIVE)) {
            player.sendMessage(ChatColor.RED + "The game isn't live, so you can't use the ability!");
            return false;
        }

        int health = arena.getQuirkBattleGame().getStats(player).getHealth();
        int intelligence = arena.getQuirkBattleGame().getStats(player).getIntelligence();
        int mana = arena.getQuirkBattleGame().getStats(player).getMana();

        if (this.hasManaCost() && arena.getQuirkBattleGame().getStats(player).getMana() < this.getAbility().getCost()) {
            player.sendMessage(ChatColor.RED + "Not enough stamina!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 2f);
            GameCoreMain.getInstance().sendActionBar(player, "&c&lNOT ENOUGH STAMINA");
            return false;
        }

        if (this.hasCooldown()) {
            if(System.currentTimeMillis() - cooldown <= this.getAbility().getCooldown()) {
                player.sendMessage(ChatColor.RED + "This ability is currently on cooldown!");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 2f);
                GameCoreMain.getInstance().sendActionBar(player, "&c&lON COOLDOWN");
                return false;
            }
        }

        if (stats.useTemperature() && stats.getTemp() + this.getAbility().getCost() < 0) {
            stats.setTemp(this.getAbility().getCost() + stats.getTemp());
            FrostbiteEvent frost = new FrostbiteEvent(player);
            Bukkit.getPluginManager().callEvent(frost);
            return false;
        }
        if (stats.useTemperature() && this.getAbility().getCost() + stats.getTemp() > stats.getMaxTemp()) {
            stats.setTemp(this.getAbility().getCost() + stats.getTemp());
            OverheatEvent heat = new OverheatEvent(player);
            Bukkit.getPluginManager().callEvent(heat);
            return false;
        }


        if (this.getAbility().hasSpecialCase()) {
            try {
                Class<? extends Ability> subClass = this.getClass().asSubclass(this.getClass());
                Method doesCasePassMethod = subClass.getDeclaredMethod("doesCasePass", Player.class);
                Method noPassMethod = subClass.getDeclaredMethod("doNoPass", Player.class);
                boolean passCase = (boolean) doesCasePassMethod.invoke(this, player);

                if(!passCase) {
                    noPassMethod.invoke(this, player);
                    return false;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                player.sendMessage(ChatColor.RED + "There was an error when checking for the special case! " + e.getCause());
                e.printStackTrace();
            }
        }

        if (this.hasManaCost()) {
            arena.getQuirkBattleGame().getStats(player).setMana(mana - this.getAbility().getCost());
            GameCoreMain.getInstance().sendActionBar(player, "&c" + health + "❤   " + "&3-" + this.getAbility().getCost() + " Stamina (" + this.getAbility().getDisplay() + "&3)   " + mana + "/" + intelligence + "⸎ Stamina");
        }
        if (stats.useTemperature()) stats.setTemp(stats.getTemp() + this.getAbility().getCost());
        if (this.hasCooldown() && !this.ability.waitForCooldown()) {
            this.cooldown = System.currentTimeMillis();
            doAbilityCooldown();
        }
        return true;
    }


    public void doAbilityCooldown() {
        Player player = Bukkit.getPlayer(uuid);
        ItemBuilder stack = new ItemBuilder(Material.SULPHUR);
        stack.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(ability.getDisplay()) + " (On Cooldown)");
        player.getInventory().setItem(slot, stack.build());
        if (shouldScheduleTask()) {
            float time = (((float) this.ability.getCooldown() / 1000) * 20);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (arena.getState() != GameState.LIVE) return;
                    if (!arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) return;
                    if (!isActive()) return;
                    player.getInventory().setItem(slot, getItem());
                }
            }.runTaskLater(QuirkBattlesPlugin.getInstance(), (long)time);
            return;
        }
        new BukkitRunnable() {
            int seconds = Math.round((float) ability.getCooldown() / 1000);
            @Override
            public void run() {
                if (!Manager.isPlaying(player)) {
                    cancel();
                    return;
                }
                if (Manager.getArena(player).getState() != GameState.LIVE) {
                    cancel();
                    return;
                }
                if (!isActive()) {
                    seconds--;
                    return;
                }
                if (seconds <= 0) {
                    player.getInventory().setItem(slot, getItem());
                    cancel();
                    return;
                }
                player.getInventory().setItem(slot, stack.setAmount(seconds).build());
                seconds--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);

    }

    // We'll unregister all the listeners when needed using this.
    public void remove() {
        HandlerList.unregisterAll(this);
        setActive(false);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!Manager.isPlaying(player)) return;

        if (!arena.getGame(arena.getID()).equals(Games.QUIRK_BATTTLE)) return;
        if (arena.getID() != this.getId()) return;
        if (uuid != event.getPlayer().getUniqueId()) return;
        if (!isActive()) return;

        boolean shouldAct = player.getItemInHand().isSimilar(this.getItem());

        boolean isRightClick = event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK);

        boolean isLeftClick = event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK);

        // TODO - Add Right/Left Click setting to execute ability.
       // if (this.getAbility().getType().equals(AbilityTypes.RIGHT_CLICK) && isRightClick && shouldAct) executeAbility(player);

       // if (this.getAbility().getType().equals(AbilityTypes.LEFT_CLICK) && isLeftClick && shouldAct) executeAbility(player);

    }
    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!Manager.isPlaying(player)) return;

        if (!arena.getGame(arena.getID()).equals(Games.QUIRK_BATTTLE)) return;
        if (arena.getID() != this.getId() || !arena.getState().equals(GameState.LIVE)) return;
        if (uuid != event.getPlayer().getUniqueId()) return;
        if (!isActive()) return;
        //TODO Return if player's ability settings are not set to HOTKEY.
        event.setCancelled(true);
        if (event.getNewSlot() == this.slot) executeAbility(player);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
    //TODO Sneak Abilities?
    }

    public void setActive(boolean active) {
        this.active = active;
        if (isActive()) {
            Bukkit.getPlayer(uuid).getInventory().setItem(this.slot, getItem());
        }
    }
    public boolean isActive() {return active;}

    public void spawnRGBParticles(Location loc, float red, float green, float blue, boolean sendToSelf) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.REDSTONE, true, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), red/255, green/255, blue/255, (float) 1, 0);

        if (sendToSelf) arena.sendPacket(packet);
        else {
            for (UUID uuid: arena.getPlayers()) {
                if (uuid == this.uuid) continue;
                GameCoreMain.getInstance().sendPacket(Bukkit.getPlayer(uuid), packet);
            }
        }
    }
}
