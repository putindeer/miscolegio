package me.putindeer.miscolegio.zone;

import lombok.Getter;

@Getter
public enum ZoneType {
    ANSWER_A("a", "Respuesta A"),
    ANSWER_B("b", "Respuesta B"),
    ANSWER_C("c", "Respuesta C"),
    ANSWER_D("d", "Respuesta D"),
    PLAY_AREA("area", "Área de Juego");

    private final String key;
    private final String displayName;

    ZoneType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }
}
