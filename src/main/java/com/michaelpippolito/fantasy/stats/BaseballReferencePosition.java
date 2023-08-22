package com.michaelpippolito.fantasy.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum BaseballReferencePosition {
    FIRST_BASE("1B"),
    SECOND_BASE("2B"),
    SHORT_STOP("SS"),
    THIRD_BASE("3B"),
    CATCHER("C"),
    LEFT_FIELD("LF"),
    CENTER_FIELD("CF"),
    RIGHT_FIELD("RF"),
    DESIGNATED_HITTER("DH"),
    PINCH_HITTER("PH"),
    PINCH_RUNNER("PR"),
    PITCHER("P"),
    STARTER("Starter"),
    RELIEVER("Reliever"),
    OTHER("Other");

    @Getter
    private final String webValue;

    public boolean isInfield() {
        switch (this) {
            case FIRST_BASE:
            case SECOND_BASE:
            case SHORT_STOP:
            case THIRD_BASE:
            case CATCHER:
                return true;
            default:
                return false;
        }
    }

    public boolean isOutfield() {
        switch (this) {
            case LEFT_FIELD:
            case CENTER_FIELD:
            case RIGHT_FIELD:
            case DESIGNATED_HITTER: // DH is treated as outfield for this fantasy game
                return true;
            default:
                return false;
        }
    }

    public boolean isPinch() {
        switch (this) {
            case PINCH_HITTER:
            case PINCH_RUNNER:
                return true;
            default:
                return false;
        }
    }

    public static BaseballReferencePosition fromString(String text) {
        for (BaseballReferencePosition position : BaseballReferencePosition.values()) {
            if (position.getWebValue().equals(text)) {
                return position;
            }
        }
        return null;
    }
}
