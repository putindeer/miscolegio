package me.putindeer.miscolegio.zone;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Cuboid {
    private World world;
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    public Cuboid(Location pos1, Location pos2) {
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            throw new IllegalArgumentException("Las ubicaciones deben estar en el mismo mundo");
        }

        this.world = pos1.getWorld();
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public boolean contains(Location location) {
        if (!location.getWorld().equals(world)) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public Location getCenter() {
        double centerX = (minX + maxX) / 2.0;
        double centerY = minY + 1;
        double centerZ = (minZ + maxZ) / 2.0;
        return new Location(world, centerX, centerY, centerZ);
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    public int getVolume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("world", world.getName());
        section.set("pos1", minX + "," + minY + "," + minZ);
        section.set("pos2", maxX + "," + maxY + "," + maxZ);
    }

    public static Cuboid fromConfig(ConfigurationSection section, World world) {
        if (section == null) return null;

        String[] pos1Parts = section.getString("pos1", "").split(",");
        String[] pos2Parts = section.getString("pos2", "").split(",");

        if (pos1Parts.length != 3 || pos2Parts.length != 3) return null;

        try {
            int x1 = Integer.parseInt(pos1Parts[0]);
            int y1 = Integer.parseInt(pos1Parts[1]);
            int z1 = Integer.parseInt(pos1Parts[2]);

            int x2 = Integer.parseInt(pos2Parts[0]);
            int y2 = Integer.parseInt(pos2Parts[1]);
            int z2 = Integer.parseInt(pos2Parts[2]);

            return new Cuboid(world, x1, y1, z1, x2, y2, z2);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("Cuboid[%s: (%d,%d,%d) to (%d,%d,%d)]",
                world.getName(), minX, minY, minZ, maxX, maxY, maxZ);
    }
}