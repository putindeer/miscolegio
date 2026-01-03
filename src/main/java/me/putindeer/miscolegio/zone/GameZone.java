package me.putindeer.miscolegio.zone;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Data
public class GameZone {
    private Cuboid answerA;
    private Cuboid answerB;
    private Cuboid answerC;
    private Cuboid answerD;
    private Cuboid playArea;

    public boolean isComplete() {
        return answerA != null && answerB != null && answerC != null
                && answerD != null && playArea != null;
    }

    public ZoneLocation getPlayerZone(Player player) {
        Location loc = player.getLocation();

        if (playArea == null || !playArea.contains(loc)) {
            return ZoneLocation.OUT_OF_BOUNDS;
        }

        if (answerA != null && answerA.contains(loc)) return ZoneLocation.A;
        if (answerB != null && answerB.contains(loc)) return ZoneLocation.B;
        if (answerC != null && answerC.contains(loc)) return ZoneLocation.C;
        if (answerD != null && answerD.contains(loc)) return ZoneLocation.D;

        return ZoneLocation.NONE;
    }
}