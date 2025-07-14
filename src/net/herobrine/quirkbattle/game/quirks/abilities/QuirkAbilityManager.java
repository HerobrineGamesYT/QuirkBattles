package net.herobrine.quirkbattle.game.quirks.abilities;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.engine.EngineBoostAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.engine.RapidKickAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.engine.ReciproBurstAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.erasure.CaptureAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.erasure.EyeDropsAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.erasure.SharpenedKnifeAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion.ExplosionDashAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion.ExplosionPunchAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion.HowitzerImpactAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening.SharpClawAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening.StoneChargeAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening.UnbreakableAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire.FireAbilityTest;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire.FireWallAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire.FlashfireFistAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire.OverdriveAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice.GlacierAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice.IceAbilityTest;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice.IceShardAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice.IceWallAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.AirPropulsionAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.DetroitSmashAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.ShootStyleAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.SwitchAbilitySetTest;
import net.herobrine.quirkbattle.game.quirks.abilities.villain.afo.AirCannonAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.villain.afo.StealAbility;
import net.herobrine.quirkbattle.game.quirks.abilities.villain.afo.TendrilAbility;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class QuirkAbilityManager {
    List<Ability> abilities = new ArrayList<>();
    private final int id;

    public QuirkAbilityManager(int id) {
        this.id = id;
    }

    public void unregisterAbilities() {
        for (Ability ability : abilities) {
            ability.setActive(false);
            ability.getQuirk().getAbilities().clear();
            ability.remove();
        }
        abilities.clear();
    }

    public Ability getAbilityFromQuirk(Class quirk, Abilities desiredAbility) {
        for (Ability ability : abilities) {
            if (ability.getQuirk() == quirk && ability.getAbility() == desiredAbility) return ability;
        }
        return null;
    }

    public Ability registerAbility(Abilities ability, Quirk quirk, int slot) {
        switch (ability) {
            case DETROIT_SMASH:
                DetroitSmashAbility smash = new DetroitSmashAbility(ability, quirk, id, slot);
                abilities.add(smash);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, smash.getItem());
                return smash;
            case SHOOT_STYLE:
                ShootStyleAbility shoot = new ShootStyleAbility(ability, quirk, id, slot);
                abilities.add(shoot);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, shoot.getItem());
                return shoot;
            case AIR_PROPULSION:
                AirPropulsionAbility air = new AirPropulsionAbility(ability, quirk, id, slot);
                abilities.add(air);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, air.getItem());
                return air;
            case EXPLOSION_DASH:
                ExplosionDashAbility dash = new ExplosionDashAbility(ability, quirk, id, slot);
                abilities.add(dash);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, dash.getItem());
                return dash;
            case EXPLOSION_PUNCH:
                ExplosionPunchAbility punch = new ExplosionPunchAbility(ability, quirk, id, slot);
                abilities.add(punch);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, punch.getItem());
                return punch;
            case HOWITZER_IMPACT:
                HowitzerImpactAbility howitzer = new HowitzerImpactAbility(ability, quirk, id, slot);
                abilities.add(howitzer);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, howitzer.getItem());
                return howitzer;
            case SHARP_CLAW:
                SharpClawAbility claw = new SharpClawAbility(ability, quirk, id, slot);
                abilities.add(claw);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, claw.getItem());
                return claw;
            case STONE_CHARGE:
                StoneChargeAbility stone = new StoneChargeAbility(ability, quirk, id, slot);
                abilities.add(stone);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, stone.getItem());
                return stone;
            case UNBREAKABLE:
                UnbreakableAbility unbreakable = new UnbreakableAbility(ability, quirk, id, slot);
                abilities.add(unbreakable);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, unbreakable.getItem());
                return unbreakable;
            case OFA_ABILITY_SWITCH_TEST:
                SwitchAbilitySetTest switchTest = new SwitchAbilitySetTest(ability, quirk, id, slot);
                abilities.add(switchTest);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, switchTest.getItem());
                return switchTest;
            case ICE_ABILITY_TEST:
                IceAbilityTest iceTest = new IceAbilityTest(ability, quirk, id, slot);
                abilities.add(iceTest);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, iceTest.getItem());
                return iceTest;
            case FIRE_ABILITY_TEST:
                FireAbilityTest fireTest = new FireAbilityTest(ability, quirk, id, slot);
                abilities.add(fireTest);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, fireTest.getItem());
                return fireTest;
            case ICE_WALL:
                IceWallAbility wall = new IceWallAbility(ability, quirk, id, slot);
                abilities.add(wall);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, wall.getItem());
                return wall;
            case FIRE_WALL:
                FireWallAbility fireWall = new FireWallAbility(ability, quirk, id, slot);
                abilities.add(fireWall);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, fireWall.getItem());
                return fireWall;
            case ICE_SHARD:
                IceShardAbility iceShard = new IceShardAbility(ability, quirk, id, slot);
                abilities.add(iceShard);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, iceShard.getItem());
                return iceShard;
            case FLASHFIRE_FIST:
                FlashfireFistAbility fist = new FlashfireFistAbility(ability, quirk, id, slot);
                abilities.add(fist);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, fist.getItem());
                return fist;
            case GLACIER:
                GlacierAbility glacier = new GlacierAbility(ability, quirk, id, slot);
                abilities.add(glacier);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, glacier.getItem());
                return glacier;
            case FLAME_OVERDRIVE:
                OverdriveAbility overdriveAbility = new OverdriveAbility(ability, quirk, id, slot);
                abilities.add(overdriveAbility);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, overdriveAbility.getItem());
                return overdriveAbility;
            case CAPTURE:
                CaptureAbility capture = new CaptureAbility(ability, quirk, id, slot);
                abilities.add(capture);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, capture.getItem());
                return capture;
            case SHARPENED_KNIFE:
                SharpenedKnifeAbility knife = new SharpenedKnifeAbility(ability, quirk, id, slot);
                abilities.add(knife);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, knife.getItem());
                return knife;
            case EYEDROPS:
                EyeDropsAbility drops = new EyeDropsAbility(ability, quirk, id, slot);
                abilities.add(drops);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, drops.getItem());
                return drops;
            case ENGINE_BOOST:
                EngineBoostAbility boost = new EngineBoostAbility(ability, quirk, id, slot);
                abilities.add(boost);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, boost.getItem());
                return boost;
            case RECIPRO_BURST:
                ReciproBurstAbility reciproBurst = new ReciproBurstAbility(ability, quirk, id, slot);
                abilities.add(reciproBurst);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, reciproBurst.getItem());
                return reciproBurst;
            case RAPID_KICK:
                RapidKickAbility rapidKick = new RapidKickAbility(ability, quirk, id, slot);
                abilities.add(rapidKick);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, rapidKick.getItem());
                return rapidKick;
            case STEAL:
                StealAbility steal = new StealAbility(ability, quirk, id, slot);
                abilities.add(steal);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, steal.getItem());
                return steal;
            case TENDRIL:
                TendrilAbility tendrils = new TendrilAbility(ability, quirk, id, slot);
                abilities.add(tendrils);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, tendrils.getItem());
                return tendrils;
            case AIR_CANNON:
                AirCannonAbility airCannon = new AirCannonAbility(ability, quirk, id, slot);
                abilities.add(airCannon);
                Bukkit.getPlayer(quirk.getUniqueId()).getInventory().setItem(slot, airCannon.getItem());
                return airCannon;
            default:
                return null;
        }
    }

    public void unregisterAbility(Ability ability) {
        ability.setActive(false);
        abilities.remove(ability);
    }
}
