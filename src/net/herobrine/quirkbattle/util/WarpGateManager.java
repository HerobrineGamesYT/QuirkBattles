package net.herobrine.quirkbattle.util;

import net.herobrine.quirkbattle.game.quirks.abilities.villain.warpgate.WarpGateAbility;
import net.herobrine.quirkbattle.util.projectile.PortalTransformResult;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class WarpGateManager {

    private WarpGateAbility ability;

    public WarpGateManager(WarpGateAbility ability) {
        this.ability = ability;
    }

    public void clear() {
        ability = null;
    }

    /**
     * Transform a location through portals without teleporting entities.
     * This is safer for projectile attacks that iterate through locations.
     */
    public Location transformLocation(Location loc, Vector direction) {
        if (!ability.hasActivePortals()) return loc;

        double radius = 2.0;
        Location portalA = ability.getPortalA();
        Location portalB = ability.getPortalB();

        // If near Portal A → transform to Portal B
        if (loc.distance(portalA) < radius) {
            // Calculate relative position from portal A
            Vector relativePos = loc.toVector().subtract(portalA.toVector());

            // Transform to portal B space
            Location exit = portalB.clone().add(relativePos);

            // Transform direction through portal (optional: you could add rotation here)
            Vector transformedDirection = transformDirection(direction, portalA, portalB);
            exit.setDirection(transformedDirection);

            return exit;
        }

        // If near Portal B → transform to Portal A
        if (loc.distance(portalB) < radius) {
            Vector relativePos = loc.toVector().subtract(portalB.toVector());
            Location exit = portalA.clone().add(relativePos);

            Vector transformedDirection = transformDirection(direction, portalB, portalA);
            exit.setDirection(transformedDirection);

            return exit;
        }

        return loc;
    }

    /**
     * Transform a location and teleport an entity through portals.
     * Use this for actual entities that should be teleported.
     */
    public Location transformLocation(Location loc, Vector direction, Entity ent) {
        if (!ability.hasActivePortals()) return loc;

        double radius = 2.0;
        Location portalA = ability.getPortalA();
        Location portalB = ability.getPortalB();

        Vector velocity = ent.getVelocity().clone();

        // If near Portal A → teleport to Portal B
        if (loc.distance(portalA) < radius) {
            Vector relativePos = loc.toVector().subtract(portalA.toVector());
            Location exit = portalB.clone().add(relativePos);

            Vector transformedDirection = transformDirection(direction, portalA, portalB);
            Vector transformedVelocity = transformDirection(velocity, portalA, portalB);

            exit.setDirection(transformedDirection);
            ent.teleport(exit);
            ent.setVelocity(transformedVelocity);

            return exit;
        }

        // If near Portal B → teleport to Portal A
        if (loc.distance(portalB) < radius) {
            Vector relativePos = loc.toVector().subtract(portalB.toVector());
            Location exit = portalA.clone().add(relativePos);

            Vector transformedDirection = transformDirection(direction, portalB, portalA);
            Vector transformedVelocity = transformDirection(velocity, portalB, portalA);

            exit.setDirection(transformedDirection);
            ent.teleport(exit);
            ent.setVelocity(transformedVelocity);

            return exit;
        }

        return loc;
    }

    /**
     * Transform a direction vector through portals.
     * This preserves the relative direction when going through portals.
     */
    private PortalTransformResult getExitResult(Location fromPortal, Location toPortal, Vector direction) {
        double exitOffset = 2.0; // push farther out of exit portal
        double forwardNudge = 0.5; // small movement forward in newDir, not old dir

        // Compute transformed direction first
        Vector newDir = transformDirection(direction, fromPortal, toPortal).normalize();

        // Exit point: slightly in front of the exit portal, in the *new* direction
        Location exit = toPortal.clone().add(newDir.clone().multiply(exitOffset));

        // Safety: nudge a little further forward
        exit.add(newDir.clone().multiply(forwardNudge));

        return new PortalTransformResult(exit, newDir, true);
    }

    public Vector transformDirection(Vector direction, Location fromPortal, Location toPortal) {
        Vector fromDir = fromPortal.getDirection().normalize();
        Vector toDir = toPortal.getDirection().normalize();

        // Rotation axis and angle
        Vector axis = fromDir.clone().crossProduct(toDir);
        double dot = Math.min(1.0, Math.max(-1.0, fromDir.dot(toDir))); // clamp to avoid NaN
        double angle = Math.acos(dot);

        if (axis.lengthSquared() < 1e-6) {
            // Parallel or anti-parallel case → snap to portal exit direction
            return toDir.clone().normalize().multiply(direction.length());
        }

        axis.normalize();
        return rotateAroundAxis(direction.clone(), axis, angle).normalize().multiply(direction.length());
    }

    private Vector rotateAroundAxis(Vector vec, Vector axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x = vec.getX(), y = vec.getY(), z = vec.getZ();
        double u = axis.getX(), v = axis.getY(), w = axis.getZ();

        // Rodrigues' rotation formula
        double nx = u*(u*x + v*y + w*z)*(1-cos) + x*cos + (-w*y + v*z)*sin;
        double ny = v*(u*x + v*y + w*z)*(1-cos) + y*cos + ( w*x - u*z)*sin;
        double nz = w*(u*x + v*y + w*z)*(1-cos) + z*cos + (-v*x + u*y)*sin;

        return new Vector(nx, ny, nz);
    }

    /**
     * Check if a line segment passes through any portal
     */
    public boolean linePassesThroughPortal(Location start, Location end) {
        if (!ability.hasActivePortals()) return false;

        double radius = 2.0;
        Location portalA = ability.getPortalA();
        Location portalB = ability.getPortalB();

        // Check if line passes near either portal
        return distanceFromLineToPoint(start, end, portalA) < radius ||
                distanceFromLineToPoint(start, end, portalB) < radius;
    }

    /**
     * Check if a line segment from prevLoc to nextLoc passes through a portal
     * and return the transformed location if it does
     */
    public PortalTransformResult transformLocationWithPath(Location prevLoc, Location nextLoc, Vector direction) {
        if (!ability.hasActivePortals()) return new PortalTransformResult(nextLoc, direction, false);

        double radius = 2.0;
        Location portalA = ability.getPortalA();
        Location portalB = ability.getPortalB();

        // Path through Portal A
        if (lineIntersectsSphere(prevLoc, nextLoc, portalA, radius)) {
            return getExitResult(portalA, portalB, direction);
        }

        // Path through Portal B
        if (lineIntersectsSphere(prevLoc, nextLoc, portalB, radius)) {
            return getExitResult(portalB, portalA, direction);
        }

        return new PortalTransformResult(nextLoc, direction, false);
    }

    /**
     * Check if a line segment intersects with a sphere (portal)
     */
    private boolean lineIntersectsSphere(Location lineStart, Location lineEnd, Location sphereCenter, double radius) {
        Vector d = lineEnd.toVector().subtract(lineStart.toVector());
        Vector f = lineStart.toVector().subtract(sphereCenter.toVector());

        double a = d.dot(d);
        double b = 2 * f.dot(d);
        double c = f.dot(f) - radius * radius;

        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            return false; // No intersection
        }

        discriminant = Math.sqrt(discriminant);
        double t1 = (-b - discriminant) / (2 * a);
        double t2 = (-b + discriminant) / (2 * a);

        // Check if intersection occurs within the line segment (t between 0 and 1)
        return (t1 >= 0 && t1 <= 1) || (t2 >= 0 && t2 <= 1) || (t1 < 0 && t2 > 1);
    }

    /**
     * Calculate distance from a line segment to a point
     */
    private double distanceFromLineToPoint(Location lineStart, Location lineEnd, Location point) {
        Vector line = lineEnd.toVector().subtract(lineStart.toVector());
        Vector toPoint = point.toVector().subtract(lineStart.toVector());

        double lineLength = line.length();
        if (lineLength == 0) return lineStart.distance(point);

        double t = Math.max(0, Math.min(1, toPoint.dot(line) / (lineLength * lineLength)));
        Vector projection = lineStart.toVector().add(line.multiply(t));

        return projection.distance(point.toVector());


    }


    public WarpGateAbility getAbility() {return ability;}
}