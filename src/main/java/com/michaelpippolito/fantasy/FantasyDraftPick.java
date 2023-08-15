package com.michaelpippolito.fantasy;

import com.michaelpippolito.fantasy.mlb.MLBPositionGroup;
import com.michaelpippolito.fantasy.mlb.MLBTeam;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FantasyDraftPick {
    private final MLBTeam team;
    private final MLBPositionGroup position;
}
