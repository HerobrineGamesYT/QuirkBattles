package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class IceWallAbility extends Ability {
    public IceWallAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }
    Player player = Bukkit.getPlayer(quirk.getUUID());
    @Override
    public void doAbility(Player player) {

    }



    public void doVFX() {

    }
}
