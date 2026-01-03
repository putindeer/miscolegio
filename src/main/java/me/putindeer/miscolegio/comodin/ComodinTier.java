package me.putindeer.miscolegio.comodin;

import lombok.Getter;

@Getter
public enum ComodinTier {
    C("<green>"),
    B("<blue>"),
    A("<dark_purple>"),
    S("<yellow>");

    private final String color;

    ComodinTier(String color) {
        this.color = color;
    }
}

