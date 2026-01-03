package me.putindeer.miscolegio.zone;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.UUID;

@Getter
public class SelectionSession {
    private final UUID playerId;
    private final String playerName;
    private int currentStep = 0; // 0-3: A,B,C,D / 4: área

    private Location pos1;
    private Location pos2;

    private Cuboid answerA;
    private Cuboid answerB;
    private Cuboid answerC;
    private Cuboid answerD;
    private Cuboid playArea;

    public SelectionSession(Player player) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
    }

    public ZoneType getCurrentZoneType() {
        return switch (currentStep) {
            case 0 -> ZoneType.ANSWER_A;
            case 1 -> ZoneType.ANSWER_B;
            case 2 -> ZoneType.ANSWER_C;
            case 3 -> ZoneType.ANSWER_D;
            case 4 -> ZoneType.PLAY_AREA;
            default -> null;
        };
    }

    public void setPosition1(Location location) {
        this.pos1 = location;
    }

    public void setPosition2(Location location) {
        this.pos2 = location;
    }

    public boolean doesntHasPositions() {
        return pos1 == null || pos2 == null;
    }

    public void confirmCurrentZone() {
        if (doesntHasPositions()) return;

        switch (currentStep) {
            case 0 -> answerA = createCuboid(pos1, pos2);
            case 1 -> answerB = createCuboid(pos1, pos2);
            case 2 -> answerC = createCuboid(pos1, pos2);
            case 3 -> answerD = createCuboid(pos1, pos2);
            case 4 -> playArea = createCuboid(pos1, pos2);
        }

        pos1 = null;
        pos2 = null;
        currentStep++;
    }

    public Cuboid createCuboid(Location pos1, Location pos2) {
        return new Cuboid(pos1, pos2);
    }

    public boolean isComplete() {
        return currentStep > 4 && answerA != null && answerB != null &&
                answerC != null && answerD != null && playArea != null;
    }
}