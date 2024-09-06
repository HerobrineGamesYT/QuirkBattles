package net.herobrine.quirkbattle.menus;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.core.ItemTypes;
import net.herobrine.gamecore.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuirkSelector implements Listener {
    public void applyQuirkSelector(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GRAY + "Choose your quirk!");
        int[] fillers = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46,47,48,49,50,51,52,53};
        // we'll only add up to 20 quirks.
        int[] classSlots = new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42};

        ItemBuilder filler = new ItemBuilder(Material.STAINED_GLASS_PANE, (short) 7);
        filler.setDisplayName(ChatColor.GRAY + " ");
        for (int slot : fillers) {
            gui.setItem(slot, filler.build());
        }
        int i = 0;
        for (ClassTypes quirk : ClassTypes.values()) {
            if (quirk.getGame().equals(Games.QUIRK_BATTTLE)) {
                ItemBuilder item = new ItemBuilder(quirk.getMaterial());
                item.setDisplayName(quirk.getDisplay());

                item.setLore(Arrays.asList(quirk.getDescription()));

                if (Manager.hasKit(player)) {
                    if (Manager.getArena(player).getClass(player).equals(quirk)) {
                        List<String> lore = new ArrayList<>(Arrays.asList(quirk.getDescription()));
                        lore.add("");
                        lore.add(ChatColor.GREEN + "You have this quirk selected!");
                        item.addEnchant(Enchantment.DURABILITY, 1);
                        item.addItemFlag(ItemFlag.HIDE_ENCHANTS);
                        item.setLore(lore);
                    }
                }

                if (quirk.isUnlockable() && !HerobrinePVPCore.getFileManager().isItemUnlocked(ItemTypes.CLASS,
                        quirk.toString(), player.getUniqueId())) {

                    List<String> lore = new ArrayList<>(Arrays.asList(quirk.getDescription()));
                    lore.add("");
                    lore.add(ChatColor.RED + "You do not have this class unlocked!");
                    item.setLore(lore);
                }
                gui.setItem(classSlots[i], item.build());
                i++;
            }
        }

        player.openInventory(gui);
    }


    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        if (e.getView().getTitle()
                .contains(ChatColor.translateAlternateColorCodes('&', "&7Choose your quirk!"))
                 && e.getCurrentItem() != null) {

            String classString = null;
            if (e.getCurrentItem().getType() != Material.AIR && e.getCurrentItem().getType() != Material.STAINED_GLASS_PANE)
                classString = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName().toUpperCase());
            if (classString == null) return;
            classString = classString.replaceAll("\\s", "");
            classString = classString.replaceAll("BATTLE", "");

            ClassTypes type;
            if (e.getCurrentItem().getType().equals(Material.BLAZE_ROD)) type = ClassTypes.ICYHOT;
            else type = ClassTypes.valueOf(classString);

            if (Manager.hasKit(player) && Manager.getKit(player).equals(type)) {
                player.sendMessage(ChatColor.GREEN + "You already have this quirk selected!");
            } else {
                if (type.isUnlockable() && !HerobrinePVPCore.getFileManager().isItemUnlocked(ItemTypes.CLASS,
                        type.toString(), player.getUniqueId())) {
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                    player.sendMessage(ChatColor.RED + "You do not have this quirk unlocked!");
                } else Manager.getArena(player).setClass(player.getUniqueId(), type);
            }

            e.setCancelled(true);
            player.closeInventory();
        } else {
            if (Manager.isPlaying(player)) {
                Arena arena = Manager.getArena(player);
                if (arena.getGame(arena.getID()).equals(Games.QUIRK_BATTTLE)) e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
        if (player.getItemInHand() != null && player.getItemInHand().getType() != Material.AIR) {
            if (player.getItemInHand().getItemMeta() != null
                    && player.getItemInHand().getItemMeta().getDisplayName() != null) {
                if (Manager.isPlaying(player)) {
                    if (Manager.getArena(player).getGame().equals(Games.QUIRK_BATTTLE) && player.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.AQUA + "Class Selector")) applyQuirkSelector(player);
                }
            }

        }
    }
}