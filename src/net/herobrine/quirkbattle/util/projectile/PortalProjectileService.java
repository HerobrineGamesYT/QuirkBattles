package net.herobrine.quirkbattle.util.projectile;

import net.herobrine.quirkbattle.util.WarpGateManager;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Service class for handling projectiles that need to work with portals
 */
public class PortalProjectileService {
    private final List<WarpGateManager> portalManagers;

    public PortalProjectileService(List<WarpGateManager> portalManagers) {
        this.portalManagers = portalManagers;
    }

    /**
     * Interface for projectile step callbacks
     */
    public interface ProjectileStepCallback {
        void onStep(Location location, Vector direction, boolean wentThroughPortal);
    }

    /**
     * Interface for particle spawn callbacks
     */
    public interface ParticleCallback {
        void spawnParticle(Location location);
    }

    /**
     * Sequential projectile that moves step by step (like BlueflameWall)
     */
    public class SequentialProjectile {
        private Location currentLoc;
        private Vector currentDir;
        private final double stepSize;
        private final ProjectileStepCallback callback;

        public SequentialProjectile(Location start, Vector direction, double stepSize,
                                    ProjectileStepCallback callback) {
            this.currentLoc = start.clone();
            this.currentDir = direction.clone().normalize();
            this.stepSize = stepSize;
            this.callback = callback;
        }

        public boolean step() {
            Location prevLoc = currentLoc.clone();
            Location nextLoc = currentLoc.clone().add(currentDir.clone().multiply(stepSize));

            boolean wentThroughPortal = false;

            // Check portal transformation
            for (WarpGateManager manager : portalManagers) {
                if (manager.getAbility() == null || !manager.getAbility().hasActivePortals()) {
                    continue;
                }

                PortalTransformResult res = manager.transformLocationWithPath(prevLoc, nextLoc, currentDir);

                if (res.passedThrough()) {
                    currentLoc = res.getLocation().clone();
                    currentDir = res.getDirection().clone().normalize();

                    // Move one step forward from portal exit
                    currentLoc.add(currentDir.clone().multiply(stepSize));
                    wentThroughPortal = true;
                    break;
                }
            }

            if (!wentThroughPortal) {
                currentLoc = nextLoc;
            }

            callback.onStep(currentLoc.clone(), currentDir.clone(), wentThroughPortal);
            return true;
        }

        public Location getCurrentLocation() { return currentLoc.clone(); }
        public Vector getCurrentDirection() { return currentDir.clone(); }
    }

    /**
     * Particle-based cone attack (like Air Cannon)
     * FIXED VERSION - properly generates spreading cone
     */
    public class ConeParticleAttack {
        private final Location origin;
        private final Vector direction;
        private final double maxRange;
        private final double maxRadius;

        public ConeParticleAttack(Location origin, Vector direction, double maxRange, double maxRadius) {
            this.origin = origin.clone();
            this.direction = direction.clone().normalize();
            this.maxRange = maxRange;
            this.maxRadius = maxRadius;
        }

        /**
         * Generate particles for a given progress (0.0 to 1.0)
         * FIXED: Properly calculates particle positions in expanding cone
         */
        public List<Location> generateParticles(double progress, int particleCount, ParticleCallback particleCallback) {
            // IMPORTANT: Calculate the current maximum distance based on progress
            double currentMaxDistance = maxRange * progress;

            Set<Location> portalExits = new HashSet<>();
            List<Location> particleLocations = new ArrayList<>();

            for (int i = 0; i < particleCount; i++) {
                // Generate random distance from 0 to current max
                double distance = Math.random() * currentMaxDistance;

                // Calculate radius at this distance (cone expands with distance)
                double radiusAtDistance = (distance / maxRange) * maxRadius;

                // Generate the cone point offset
                Vector particleOffset = generateConePoint(direction, distance, radiusAtDistance);

                // Calculate raw location
                Location rawLoc = origin.clone().add(particleOffset);

                // Transform through portals if needed
                Location finalLoc = transformParticleWithCone(origin, rawLoc, direction, particleOffset, portalExits);
                particleLocations.add(finalLoc);

                // Call the callback to spawn particles
                if (particleCallback != null) {
                    particleCallback.spawnParticle(finalLoc);
                }
            }

            // Fill portal exit gaps
            for (Location portalExit : portalExits) {
                fillPortalExit(portalExit, particleCallback);
            }

            return particleLocations;
        }

        private Location transformParticleWithCone(Location origin, Location particleLoc, Vector originalDir,
                                                   Vector originalOffset, Set<Location> portalExits) {
            for (WarpGateManager manager : portalManagers) {
                if (manager.getAbility() == null || !manager.getAbility().hasActivePortals()) {
                    continue;
                }

                PortalTransformResult res = manager.transformLocationWithPath(origin, particleLoc, originalDir);

                if (res.passedThrough()) {
                    portalExits.add(res.getLocation().clone());

                    // Transform the offset vector through the portal
                    Vector transformedOffset = manager.transformDirection(originalOffset,
                            manager.getAbility().getPortalA(),
                            manager.getAbility().getPortalB());

                    return res.getLocation().clone().add(transformedOffset);
                }
            }
            return particleLoc;
        }

        private void fillPortalExit(Location portalExit, ParticleCallback callback) {
            Vector exitDir = portalExit.getDirection().normalize();

            for (int i = 0; i < 8; i++) {
                double distance = 0.5 + Math.random() * 2.0;
                double radius = 0.2 + Math.random() * 0.5;

                Vector offset = generateConePoint(exitDir, distance, radius);
                Location fillLoc = portalExit.clone().add(offset);

                if (callback != null) {
                    callback.spawnParticle(fillLoc);
                }
            }
        }

        /**
         * Generate a random point within a cone shape
         * FIXED: Properly generates cone point with correct perpendicular vectors
         */
        private Vector generateConePoint(Vector direction, double distance, double radius) {
            // Create perpendicular vectors for the cone
            Vector up = new Vector(0, 1, 0);

            // If direction is too parallel to up vector, use a different vector
            if (Math.abs(direction.dot(up)) > 0.9) {
                up = new Vector(1, 0, 0);
            }

            // Calculate right and up vectors perpendicular to direction
            Vector right = direction.clone().crossProduct(up).normalize();
            up = right.clone().crossProduct(direction).normalize();

            // Generate random angle and radius for circular distribution
            double angle = Math.random() * 2 * Math.PI;
            double r = Math.sqrt(Math.random()) * radius; // sqrt for uniform distribution

            // Calculate the radial offset
            Vector radialOffset = right.clone().multiply(Math.cos(angle) * r)
                    .add(up.clone().multiply(Math.sin(angle) * r));

            // Combine forward distance with radial offset
            return direction.clone().multiply(distance).add(radialOffset);
        }
    }

    /**
     * Factory methods for easy creation
     */
    public SequentialProjectile createSequentialProjectile(Location start, Vector direction,
                                                           double stepSize, ProjectileStepCallback callback) {
        return new SequentialProjectile(start, direction, stepSize, callback);
    }

    public ConeParticleAttack createConeAttack(Location start, Vector direction,
                                               double maxRange, double maxRadius) {
        return new ConeParticleAttack(start, direction, maxRange, maxRadius);
    }

    /**
     * Utility methods
     */
    public boolean hasAnyActivePortals() {
        return portalManagers.stream().anyMatch(manager ->
                manager != null && manager.getAbility() != null && manager.getAbility().hasActivePortals());
    }

    public Location transformThroughAnyPortal(Location loc, Vector direction) {
        for (WarpGateManager manager : portalManagers) {
            if (manager == null || manager.getAbility() == null || !manager.getAbility().hasActivePortals()) {
                continue;
            }

            Location transformed = manager.transformLocation(loc, direction);
            if (!transformed.equals(loc)) {
                return transformed;
            }
        }
        return loc;
    }
}