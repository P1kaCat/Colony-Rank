package com.colonyrank.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.colonyrank.mod.data.ColonyData;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper to access MineColonies API via reflection.
 * This avoids a hard compile-time dependency.
 */
public class MineColoniesAPIHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("ColonyRank");

    private static final String MINECOLONIES_API_CLASS = "com.minecolonies.api.IMinecoloniesAPI";
    private boolean minecoloniesAvailable = false;

    public MineColoniesAPIHelper() {
        this.minecoloniesAvailable = checkMineColoniesAvailable();
        if (minecoloniesAvailable) {
            LOGGER.info("API MineColonies detectee et disponible");
        } else {
            LOGGER.warn("MineColonies non detecte - le mod ne fonctionnera pas sans lui");
        }
    }

    private boolean checkMineColoniesAvailable() {
        try {
            Class.forName(MINECOLONIES_API_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private Object getColonyManager() throws Exception {
        Class<?> apiClass = Class.forName(MINECOLONIES_API_CLASS);
        Object api = apiClass.getMethod("getInstance").invoke(null);
        if (api == null) {
            return null;
        }
        return apiClass.getMethod("getColonyManager").invoke(api);
    }

    /**
     * Returns all colony IDs for the given level.
     */
    public Set<Integer> getAllColonyIds(ServerLevel level) {
        Set<Integer> colonyIds = new HashSet<>();

        if (!minecoloniesAvailable) {
            LOGGER.error("MineColonies n est pas disponible");
            return colonyIds;
        }

        try {
            Object colonyManager = getColonyManager();
            if (colonyManager == null) {
                LOGGER.warn("ColonyManager est null");
                return colonyIds;
            }

            List<?> colonies;
            try {
                colonies = (List<?>) colonyManager.getClass()
                    .getMethod("getColonies", Level.class)
                    .invoke(colonyManager, level);
            } catch (NoSuchMethodException e) {
                colonies = (List<?>) colonyManager.getClass()
                    .getMethod("getAllColonies")
                    .invoke(colonyManager);
            }

            if (colonies != null) {
                for (Object colony : colonies) {
                    try {
                        Integer colonyId = (Integer) colony.getClass()
                            .getMethod("getID")
                            .invoke(colony);
                        colonyIds.add(colonyId);
                    } catch (Exception ex) {
                        LOGGER.debug("Impossible de lire l ID de colonie", ex);
                    }
                }
            }

            LOGGER.debug("Recupere {} IDs de colonies", colonyIds.size());
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la recuperation des IDs de colonies", e);
        }

        return colonyIds;
    }

    /**
     * Returns colony data for a specific colony ID.
     */
    public ColonyData getColonyData(int colonyId, ServerLevel level) {
        if (!minecoloniesAvailable) {
            LOGGER.error("MineColonies n est pas disponible");
            return null;
        }

        try {
            Object colonyManager = getColonyManager();
            if (colonyManager == null) {
                return null;
            }

            Object colony = null;
            try {
                colony = colonyManager.getClass()
                    .getMethod("getColonyByWorld", int.class, Level.class)
                    .invoke(colonyManager, colonyId, level);
            } catch (NoSuchMethodException e) {
                try {
                    ResourceKey<Level> dimension = level.dimension();
                    colony = colonyManager.getClass()
                        .getMethod("getColonyByDimension", int.class, ResourceKey.class)
                        .invoke(colonyManager, colonyId, dimension);
                } catch (Exception ignored) {
                    // Leave colony as null
                }
            }

            if (colony == null) {
                LOGGER.debug("Colonie {} introuvable", colonyId);
                return null;
            }

            String colonyName = (String) colony.getClass()
                .getMethod("getName")
                .invoke(colony);

            ColonyData colonyData = new ColonyData(colonyId, colonyName);

            int population = getColonyPopulation(colony);
            colonyData.setPopulation(population);

            int colonyAgeDays = getColonyAgeDays(colony);
            colonyData.setColonyAgeDays(colonyAgeDays);

            int colonyAgeHours = getColonyAgeHours(colony, colonyAgeDays);
            colonyData.setColonyAgeHours(colonyAgeHours);

            int claimedChunks = getClaimedChunks(colony);
            colonyData.setClaimedChunks(claimedChunks);

            double overallHappiness = getOverallHappiness(colony);
            colonyData.setOverallHappiness(overallHappiness);

            setColonyBuildingData(colonyData, colony);

            LOGGER.debug("Retrieved colony data: {}", colonyData);
            return colonyData;

        } catch (Exception e) {
            LOGGER.error("Erreur lors de la recuperation des donnees pour l ID: {}", colonyId, e);
            return null;
        }
    }

    private int getColonyPopulation(Object colony) {
        try {
            Object citizenManager = colony.getClass()
                .getMethod("getCitizenManager")
                .invoke(colony);
            @SuppressWarnings("unchecked")
            List<Object> citizens = (List<Object>) citizenManager.getClass()
                .getMethod("getCitizens")
                .invoke(citizenManager);

            return citizens != null ? citizens.size() : 0;
        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer la population", e);
            return 0;
        }
    }

    private int getColonyAgeDays(Object colony) {
        try {
            Object value = colony.getClass()
                .getMethod("getDay")
                .invoke(colony);
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer l anciennete de la colonie", e);
        }
        return 0;
    }

    private int getColonyAgeHours(Object colony, int colonyAgeDays) {
        if (colonyAgeDays > 0) {
            return colonyAgeDays * 24;
        }
        try {
            Object world = colony.getClass()
                .getMethod("getWorld")
                .invoke(colony);
            if (world != null) {
                Object dayTimeObj = world.getClass()
                    .getMethod("getDayTime")
                    .invoke(world);
                if (dayTimeObj instanceof Long) {
                    long dayTime = (Long) dayTimeObj;
                    int hours = (int) ((dayTime % 24000L) / 1000L);
                    return Math.max(0, hours);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer l anciennete en heures", e);
        }
        return 0;
    }

    private int getClaimedChunks(Object colony) {
        int computedClaims = getClaimedChunksFromClaims(colony);
        if (computedClaims > 0) {
            return computedClaims;
        }
        try {
            Object ticketed = colony.getClass()
                .getMethod("getTicketedChunks")
                .invoke(colony);
            if (ticketed instanceof java.util.Set) {
                return ((java.util.Set<?>) ticketed).size();
            }
        } catch (Exception ignored) {
        }
        try {
            Object loaded = colony.getClass()
                .getMethod("getLoadedChunks")
                .invoke(colony);
            if (loaded instanceof java.util.Set) {
                return ((java.util.Set<?>) loaded).size();
            }
        } catch (Exception ignored) {
        }
        try {
            Object value = colony.getClass()
                .getMethod("getLoadedChunkCount")
                .invoke(colony);
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private int getClaimedChunksFromClaims(Object colony) {
        try {
            BlockPos colonyCenter = (BlockPos) colony.getClass()
                .getMethod("getCenter")
                .invoke(colony);
            if (colonyCenter == null) {
                return 0;
            }

            int initialRadius = getServerConfigInt("initialColonySize", 0);
            int maxRadius = getServerConfigInt("maxColonySize", 0);

            Set<Long> claimed = new HashSet<>();
            if (initialRadius > 0) {
                addChunksInRange(claimed, colonyCenter, initialRadius, maxRadius, colonyCenter);
            }

            Object buildingManager = colony.getClass()
                .getMethod("getServerBuildingManager")
                .invoke(colony);
            @SuppressWarnings("unchecked")
            Map<Object, Object> buildings = (Map<Object, Object>) buildingManager.getClass()
                .getMethod("getBuildings")
                .invoke(buildingManager);

            if (buildings != null && !buildings.isEmpty()) {
                for (Map.Entry<Object, Object> entry : buildings.entrySet()) {
                    Object building = entry.getValue();
                    if (building == null) {
                        continue;
                    }

                    BlockPos buildingPos = null;
                    if (entry.getKey() instanceof BlockPos) {
                        buildingPos = (BlockPos) entry.getKey();
                    } else {
                        buildingPos = getBuildingPosition(building);
                    }
                    if (buildingPos == null) {
                        continue;
                    }

                    int level = Math.max(0, getBuildingLevel(building));
                    int radius = Math.max(0, getBuildingClaimRadius(building, level));
                    addChunksInRange(claimed, buildingPos, radius, maxRadius, colonyCenter);
                }
            }

            if (claimed.isEmpty()) {
                claimed.add(ChunkPos.asLong(colonyCenter.getX() >> 4, colonyCenter.getZ() >> 4));
            }

            return claimed.size();
        } catch (Exception e) {
            LOGGER.debug("Impossible de calculer les chunks revendiques", e);
            return 0;
        }
    }

    private void addChunksInRange(Set<Long> claimed, BlockPos center, int radius, int maxRadius, BlockPos colonyCenter) {
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int colonyChunkX = colonyCenter.getX() >> 4;
        int colonyChunkZ = colonyCenter.getZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerChunkX + dx;
                int z = centerChunkZ + dz;
                if (maxRadius > 0) {
                    int ddx = x - colonyChunkX;
                    int ddz = z - colonyChunkZ;
                    if ((ddx * ddx) + (ddz * ddz) > (maxRadius * maxRadius)) {
                        continue;
                    }
                }
                claimed.add(ChunkPos.asLong(x, z));
            }
        }
    }

    private int getBuildingClaimRadius(Object building, int level) {
        try {
            Object value = building.getClass()
                .getMethod("getClaimRadius", int.class)
                .invoke(building, level);
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer le rayon de claim du batiment", e);
        }
        return 0;
    }

    private BlockPos getBuildingPosition(Object building) {
        try {
            Object value = building.getClass()
                .getMethod("getPosition")
                .invoke(building);
            if (value instanceof BlockPos) {
                return (BlockPos) value;
            }
        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer la position du batiment", e);
        }
        return null;
    }

    private int getServerConfigInt(String fieldName, int defaultValue) {
        try {
            Class<?> mineColoniesClass = Class.forName("com.minecolonies.core.MineColonies");
            Object config = mineColoniesClass.getMethod("getConfig").invoke(null);
            Object serverConfig = config.getClass().getMethod("getServer").invoke(config);

            Field field;
            try {
                field = serverConfig.getClass().getField(fieldName);
            } catch (NoSuchFieldException e) {
                field = serverConfig.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
            }

            Object valueHolder = field.get(serverConfig);
            if (valueHolder != null) {
                Object value = valueHolder.getClass().getMethod("get").invoke(valueHolder);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Impossible de lire la config MineColonies: {}", fieldName, e);
        }
        return defaultValue;
    }

    private double getOverallHappiness(Object colony) {
        try {
            Object value = colony.getClass()
                .getMethod("getOverallHappiness")
                .invoke(colony);
            if (value instanceof Double) {
                return (Double) value;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer le bonheur", e);
        }
        return 0.0;
    }

    private void setColonyBuildingData(ColonyData colonyData, Object colony) {
        try {
            Object buildingManager = colony.getClass()
                .getMethod("getServerBuildingManager")
                .invoke(colony);
            @SuppressWarnings("unchecked")
            Map<Object, Object> buildings = (Map<Object, Object>) buildingManager.getClass()
                .getMethod("getBuildings")
                .invoke(buildingManager);

            if (buildings == null || buildings.isEmpty()) {
                colonyData.setBuildingCount(0);
                colonyData.setAverageBuildingLevel(0.0);
                return;
            }

            double totalLevel = 0.0;
            int buildingCount = 0;

            for (Object building : buildings.values()) {
                int level = getBuildingLevel(building);
                if (level >= 0) {
                    int maxLevel = getBuildingMaxLevel(building);
                    int safeMax = maxLevel > 0 ? maxLevel : 5;
                    double normalized = (double) level / (double) safeMax * 5.0;
                    if (normalized < 0.0) {
                        normalized = 0.0;
                    } else if (normalized > 5.0) {
                        normalized = 5.0;
                    }
                    totalLevel += normalized;
                    buildingCount++;
                }
            }

            colonyData.setBuildingCount(buildingCount);
            if (buildingCount > 0) {
                colonyData.setAverageBuildingLevel(totalLevel / buildingCount);
            }

        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer les donnees des batiments", e);
        }
    }

    private int getBuildingLevel(Object building) {
        if (building == null) {
            return -1;
        }
        try {
            return (Integer) building.getClass()
                .getMethod("getBuildingLevel")
                .invoke(building);
        } catch (Exception ignored) {
            // Fallback to other common method names
        }
        try {
            return (Integer) building.getClass()
                .getMethod("getLevel")
                .invoke(building);
        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer le niveau du batiment", e);
            return -1;
        }
    }

    private int getBuildingMaxLevel(Object building) {
        if (building == null) {
            return -1;
        }
        try {
            return (Integer) building.getClass()
                .getMethod("getMaxBuildingLevel")
                .invoke(building);
        } catch (Exception ignored) {
            // Fallback to other common method names
        }
        try {
            return (Integer) building.getClass()
                .getMethod("getMaxLevel")
                .invoke(building);
        } catch (Exception e) {
            LOGGER.debug("Impossible de recuperer le niveau max du batiment", e);
            return -1;
        }
    }

    public boolean isColonyAccessible(int colonyId, ServerLevel level) {
        if (!minecoloniesAvailable) {
            return false;
        }
        try {
            Object colonyManager = getColonyManager();
            if (colonyManager == null) {
                return false;
            }
            Object colony = colonyManager.getClass()
                .getMethod("getColonyByWorld", int.class, Level.class)
                .invoke(colonyManager, colonyId, level);
            return colony != null;
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la verification d acces a la colonie", e);
            return false;
        }
    }
}