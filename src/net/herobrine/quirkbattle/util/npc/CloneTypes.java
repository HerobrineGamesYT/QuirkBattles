package net.herobrine.quirkbattle.util.npc;

public enum CloneTypes {

    // Configuration for different "clone types" or npcs. -1 lifetime ticks = permanent unless despawned manually.

    TWICE_CLONE(true, true, 200, 5, 6, 3, 2, 20),
    KUROGIRI_DOUBLE(false, false, -1, 0,0,0,0,0);



    boolean hasAI;
    boolean isAggressive;
    int lifetimeTicks;
    double speedPerTick;
    double chaseRadius;
    double attackRange;
    double damagePerHit;
    int attackCooldownTicks;

    private CloneTypes(boolean hasAi, boolean isAggressive, int lifetimeTicks, double speedPerTick, double chaseRadius, double attackRange, double damagePerHit, int attackCooldownTicks) {
        this.hasAI = hasAi;
        this.isAggressive = isAggressive;
        this.lifetimeTicks= lifetimeTicks;
        this.speedPerTick = speedPerTick;
        this.chaseRadius = chaseRadius;
        this.attackRange = attackRange;
        this.damagePerHit = damagePerHit;
        this.attackCooldownTicks = attackCooldownTicks;
    }

    public boolean hasAI() {return hasAI;}
    public boolean isAggressive() {return  isAggressive;}
    public int getLifetimeTicks() {return lifetimeTicks;}
    public double getSpeedPerTick() {return speedPerTick;}
    public double getChaseRadius() {return chaseRadius;}
    public double getAttackRange() {return attackRange;}
    public double getDamage() {return damagePerHit;}
    public int getAttackCooldownTicks() {return attackCooldownTicks;}
}
