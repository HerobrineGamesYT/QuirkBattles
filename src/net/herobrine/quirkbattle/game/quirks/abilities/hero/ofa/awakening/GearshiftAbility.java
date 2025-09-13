package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa;

import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.OneForAll;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class GearshiftAbility extends Ability implements SpecialCase {

    private final OneForAll ofa;
    private boolean isOnPermanentCooldown = false;

    public GearshiftAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.ofa = (OneForAll) quirk;
    }

    @Override
    public void doAbility(Player player) {
        ofa.activateGearshift();

        // Set permanent cooldown (for rest of match)
        isOnPermanentCooldown = true;
        setCooldown(System.currentTimeMillis());

        // Visual indicator that it's permanently on cooldown
        player.getInventory().setItem(slot, getErasedItem());
    }

    @Override
    public boolean doesCasePass(Player player) {
        if (isOnPermanentCooldown) {
            player.sendMessage(ChatColor.RED + "Gearshift can only be used once per match!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            return false;
        }

        if (ofa.isStunned()) {
            player.sendMessage(ChatColor.RED + "You cannot use Gearshift while stunned!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            return false;
        }

        if (ofa.isGearshiftActive()) {
            player.sendMessage(ChatColor.RED + "Gearshift is already active!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            return false;
        }

        return true;
    }

    @Override
    public void doNoPass(Player player) {
        // Messages handled in doesCasePass
    }
}