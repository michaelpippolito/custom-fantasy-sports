package com.michaelpippolito.fantasy;

import com.michaelpippolito.fantasy.mlb.MLBPositionGroup;
import com.michaelpippolito.fantasy.mlb.MLBTeam;

import java.util.ArrayList;
import java.util.List;

public class FantasyApplication {
    public static void main(String[] args) {
        List<FantasyPlayer> players = new ArrayList<>();
        for (MLBTeam team : MLBTeam.values()) {
            players.add(FantasyPlayer.builder()
                    .name(team.getName())
                    .draftPicks(List.of(
                            new FantasyDraftPick(team, MLBPositionGroup.ROTATION),
                            new FantasyDraftPick(team, MLBPositionGroup.INFIELD),
                            new FantasyDraftPick(team, MLBPositionGroup.OUTFIELD_DH),
                            new FantasyDraftPick(team, MLBPositionGroup.BULLPEN)
                    ))
                    .build());
        }

        FantasyGame game = new FantasyGame(players, 2023, "all_team_results.xlsx");
        game.play();
    }
}
