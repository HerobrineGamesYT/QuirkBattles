package net.herobrine.quirkbattle.util.projectile;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class PortalTransformResult {
    private final Location location;
    private final Vector direction;
    private final boolean passedThrough;


    public PortalTransformResult(Location location, Vector direction, boolean passedThrough) {
        this.location = location;
        this.direction = direction;
        this.passedThrough = passedThrough;
    }

    public Location getLocation() { return location; }
    public Vector getDirection() { return direction; }
    public boolean passedThrough() { return passedThrough; }
}