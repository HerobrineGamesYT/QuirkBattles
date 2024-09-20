package net.herobrine.quirkbattle.game.quirks.abilities;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion.ExplosionDashAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion.ExplosionPunchAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion.HowitzerImpactAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening.SharpClawAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening.StoneChargeAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening.UnbreakableAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire.FireAbilityTest;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice.IceAbilityTest;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice.IceWallAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.AirPropulsionAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.DetroitSmashAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.ShootStyleAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.SwitchAbilitySetTest;
import net.herobrine.quirkbattle.game.quirks.hero.Explosion;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;

public class QuirkAbilityManager {
    ArrayList<Ability> abilities = new ArrayList<>();
    private int id;
    public QuirkAbilityManager(int id) {
        this.id = id;
    }
   public void unregisterAbilities() {
    for (Ability ability : abilities) {ability.remove();}
    abilities.clear();
    }

    public Ability getAbilityFromQuirk(Class quirk, Abilities desiredAbility) {
        for (Ability ability : abilities) {
            if (ability.getQuirk() == quirk && ability.getAbility() == desiredAbility) return ability;
        }
        return null;
    }
    public Ability registerAbility(Abilities ability, Class quirk, int slot) {
        switch (ability) {
            case DETROIT_SMASH:
                DetroitSmashAbility smash = new DetroitSmashAbility(ability, quirk, id, slot);
                abilities.add(smash);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, smash.getItem());
                return smash;
            case SHOOT_STYLE:
                ShootStyleAbility shoot = new ShootStyleAbility(ability, quirk, id, slot);
                abilities.add(shoot);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, shoot.getItem());
                return shoot;
            case AIR_PROPULSION:
                AirPropulsionAbility air = new AirPropulsionAbility(ability, quirk, id, slot);
                abilities.add(air);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, air.getItem());
                return air;
            case EXPLOSION_DASH:
                ExplosionDashAbility dash = new ExplosionDashAbility(ability, quirk, id, slot);
                abilities.add(dash);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, dash.getItem());
                return dash;
            case EXPLOSION_PUNCH:
                ExplosionPunchAbility punch = new ExplosionPunchAbility(ability, quirk, id, slot);
                abilities.add(punch);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, punch.getItem());
                return punch;
            case HOWITZER_IMPACT:
                HowitzerImpactAbility howitzer = new HowitzerImpactAbility(ability, quirk, id, slot);
                abilities.add(howitzer);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, howitzer.getItem());
                return howitzer;
            case SHARP_CLAW:
                SharpClawAbility claw = new SharpClawAbility(ability, quirk, id, slot);
                abilities.add(claw);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, claw.getItem());
                return claw;
            case STONE_CHARGE:
                StoneChargeAbility stone = new StoneChargeAbility(ability, quirk, id, slot);
                abilities.add(stone);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, stone.getItem());
                return stone;
            case UNBREAKABLE:
                UnbreakableAbility unbreakable = new UnbreakableAbility(ability, quirk, id, slot);
                abilities.add(unbreakable);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, unbreakable.getItem());
                return unbreakable;
            case OFA_ABILITY_SWITCH_TEST:
                SwitchAbilitySetTest switchTest = new SwitchAbilitySetTest(ability, quirk, id, slot);
                abilities.add(switchTest);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, switchTest.getItem());
                return switchTest;
            case ICE_ABILITY_TEST:
                IceAbilityTest iceTest = new IceAbilityTest(ability, quirk, id, slot);
                abilities.add(iceTest);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, iceTest.getItem());
                return iceTest;
            case FIRE_ABILITY_TEST:
                FireAbilityTest fireTest = new FireAbilityTest(ability, quirk, id, slot);
                abilities.add(fireTest);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, fireTest.getItem());
                return fireTest;
            case ICE_WALL:
                IceWallAbility wall = new IceWallAbility(ability, quirk, id, slot);
                abilities.add(wall);
                Bukkit.getPlayer(quirk.getUUID()).getInventory().setItem(slot, wall.getItem());
                return wall;
            default: return null;
        }
    }
}
