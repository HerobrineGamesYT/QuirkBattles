A PVP gamemode where players fight each other with custom classes with special abilities (or "Quirks") based on My Hero Academia. 

There are multiple different modes in planning, such as 1v1, 2v2, 3v3, 4v4, FFA, and Heroes VS Villains.

Currently, two quirks are fully developed and the 1v1 mode is fully playable.

Dependencies:

[Mine] HerobrinePVP-CORE (Main core. It's the backbone for all my custom plugins within this setup.)
[Mine] GameCore (Lots of minigame-related functions, configuration settings, and arena management. It's the backbone for all my minigame plugins within this setup.)
[Forked] NoteBlockAPI (HBPVP-Core Dependency for playing custom NoteBlock themes - i.e win/draw/lose jingles, and even custom soundtracks)

Design Doc: https://docs.google.com/document/d/1yxL2iu_Sn1XQJ-Y1-37pz-hk-nA9N_6OHKiqB0K0RHo/edit?usp=sharing

Quirk Battles uses GameCore's class system, which allow for you to develop custom classes that are individualized for each game. 
A quick version of the game has already been spun up which allows you to 1v1 with two quirks - One For All and Explosion.
A refactor is currently in progress which will make the abilities for Quirks work similar to [DeltaCraft](https://github.com/HerobrineGamesYT/DeltaCraft) Item Abilities.

A custom noteblock remix of You Say Run from the My Hero Academia OST has been created. You can hear it in the background of all of the gameplay clips.
Gameplay:
-  https://youtu.be/e2yh3xGOciY
-  https://youtu.be/35_uFmmSXLY

Currently Developed Quirks:

  One For All: 
  Basic Demonstration: https://streamable.com/77ix9v
  Base Damage:  4 + (4*power)
  Health: 250
  Defense: 50
  
  Hold Right Click to power up. Your movement speed increases while powering up.
  Your power will reset with every attack.

  Abilities:

  Detroit Smash: Create a vector to launch player up, then down towards the direction they are looking. Nearby players are damaged when you land on the ground.
    Damage: 15 + (15*power)
    Cooldown: 15s
  Shoot Style: Create a vector to launch player slightly up, and then forwards in direction they are looking. Any players within a small radius of the attack trajectory are damaged. When a player is directly hit by the shoot style attack, the vector is negated and the player is stopped in their tracks and falls down to continue fighting with basic melee attacks.
    Damage: 20 + (20*power)
    Cooldown: 25s
  Air Propulsion: Create a Vector to launch player in direction they are looking with x propulsion strength.
    Propulsion Strength: 2 + (2*power)
    Cooldown: 1.2s

  Every time a One For All player attacks, a random chance is rolled based on their current power-up percentage. 
  If it hits, then the player will take damage based on that. 
  OFA Damage Calculation: 10 + (10*Power)

  For example, if a player does a Shoot Style attack at 30%, and the game decides they should get hurt, the player will take 13 damage.
  Suppose the player decided to attack at 100%. In that case, the player will be GUARANTEED to take 20 damage, which could be devastating depending on how the fight is going- but it could be worth sacrificing your HP for a full-power attack in some situations.

  Note: The self-damage still goes under the damage reduction calculation from the player's defense.


  Explosion:
  Basic Demonstration: https://streamable.com/i0mj70
  Health: 200
  Base Damage: 5
  Defense: 40

  Attack players and use your abilities to gain stamina- which will fuel your abilities. 

  Stamina Gain Per Attack: 2%
  Stamina Gain Per Second: 1%

  Abilities:

  Explosion Dash: Propel yourself in the direction you are facing with an explosion- dealing damage to whoever’s around.
    Stamina Cost: 10%
    Cooldown: 1.2s
    Damage: 3
    Stamina Gain: 2%         
  Explosion Punch: Charge your next punch with explosive power - allowing you to gain increased area damage in exchange for stamina.
    Stamina Cost: 25%
    Damage Increase: +5
    Radius: 2
    Cooldown: 6s
    Stamina Gain: 5%
  Howitzer Impact: Launch yourself into the air and create a massive explosion all around you- dealing massive damage to any nearby targets.
    Stamina Cost: 40%
    Damage: 10 for small explosions, 30 for big explosion.
    Radius: 2, 5 for landing explosion.
    Cooldown: 20s
    Stamina Gain Per Explosion Hit: 5%

