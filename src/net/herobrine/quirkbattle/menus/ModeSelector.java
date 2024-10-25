package net.herobrine.quirkbattle.menus;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.GameCoreMain;
import net.herobrine.gamecore.GameType;
import net.herobrine.gamecore.Games;
import net.herobrine.quirkbattle.util.NBTReader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;


public class ModeSelector implements Listener {
    // Inventory builder is in HBPVP-Core.
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().contains(HerobrinePVPCore.translateString("&aQuirk Battles &7- &6Mode Selector")) && e.getCurrentItem() != null) {
            if (e.getCurrentItem().getType() == Material.AIR || e.getCurrentItem().getType() == Material.STAINED_GLASS_PANE)
                return;

            NBTReader reader = new NBTReader(e.getCurrentItem());
            if (!reader.getStringNBT("id").isPresent()) return;
            GameType type = GameType.valueOf(reader.getStringNBT("id").get());
            GameCoreMain.getInstance().startQueue((Player) e.getWhoClicked(), Games.QUIRK_BATTTLE, type);
            e.getWhoClicked().closeInventory();
        }
    }


}
