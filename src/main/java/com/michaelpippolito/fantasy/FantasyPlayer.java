package com.michaelpippolito.fantasy;

import com.michaelpippolito.fantasy.mlb.MLBPlayer;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class FantasyPlayer {
    private final String name;
    private final List<FantasyDraftPick> draftPicks;
    private final MLBPlayer wildcardPick;
}
