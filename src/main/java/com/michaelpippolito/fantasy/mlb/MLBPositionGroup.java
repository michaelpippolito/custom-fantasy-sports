package com.michaelpippolito.fantasy.mlb;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MLBPositionGroup {
    ROTATION("Rotation"),
    BULLPEN("Bullpen"),
    INFIELD("Infield"),
    OUTFIELD_DH("Outfield/DH");

    private final String value;
}
