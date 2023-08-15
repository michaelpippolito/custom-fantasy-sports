package com.michaelpippolito.fantasy;

import com.michaelpippolito.fantasy.mlb.MLBPlayer;
import com.michaelpippolito.fantasy.mlb.MLBPositionGroup;
import com.michaelpippolito.fantasy.mlb.MLBTeam;

import java.util.List;

public class FantasyApplication {
    public static void main(String[] args) {
        FantasyPlayer mike = FantasyPlayer.builder()
                .name("Mike")
                .draftPicks(List.of(
                        new FantasyDraftPick(MLBTeam.HOU, MLBPositionGroup.ROTATION),
                        new FantasyDraftPick(MLBTeam.PHI, MLBPositionGroup.INFIELD),
                        new FantasyDraftPick(MLBTeam.NYY, MLBPositionGroup.OUTFIELD_DH),
                        new FantasyDraftPick(MLBTeam.SEA, MLBPositionGroup.BULLPEN)
                ))
                .wildcardPick(
                        MLBPlayer.builder()
                                .name("Julio Rodríguez")
                                .team(MLBTeam.SEA)
                                .positionGroup(MLBPositionGroup.OUTFIELD_DH)
                                .build()
                )
                .build();

        FantasyPlayer nick = FantasyPlayer.builder()
                .name("Nick")
                .draftPicks(List.of(
                        new FantasyDraftPick(MLBTeam.ATL, MLBPositionGroup.ROTATION),
                        new FantasyDraftPick(MLBTeam.SDP, MLBPositionGroup.INFIELD),
                        new FantasyDraftPick(MLBTeam.LAA, MLBPositionGroup.OUTFIELD_DH),
                        new FantasyDraftPick(MLBTeam.HOU, MLBPositionGroup.BULLPEN)
                ))
                .wildcardPick(
                        MLBPlayer.builder()
                                .name("Mookie Betts")
                                .team(MLBTeam.LAD)
                                .positionGroup(MLBPositionGroup.OUTFIELD_DH)
                                .build()
                )
                .build();

        FantasyPlayer jon = FantasyPlayer.builder()
                .name("Jon")
                .draftPicks(List.of(
                        new FantasyDraftPick(MLBTeam.NYM, MLBPositionGroup.ROTATION),
                        new FantasyDraftPick(MLBTeam.TOR, MLBPositionGroup.INFIELD),
                        new FantasyDraftPick(MLBTeam.SDP, MLBPositionGroup.OUTFIELD_DH),
                        new FantasyDraftPick(MLBTeam.LAD, MLBPositionGroup.BULLPEN)
                ))
                .wildcardPick(
                        MLBPlayer.builder()
                                .name("Nolan Arenado")
                                .team(MLBTeam.STL)
                                .positionGroup(MLBPositionGroup.INFIELD)
                                .build()
                )
                .build();

        FantasyPlayer dan = FantasyPlayer.builder()
                .name("Dan")
                .draftPicks(List.of(
                        new FantasyDraftPick(MLBTeam.SDP, MLBPositionGroup.ROTATION),
                        new FantasyDraftPick(MLBTeam.NYM, MLBPositionGroup.INFIELD),
                        new FantasyDraftPick(MLBTeam.ATL, MLBPositionGroup.OUTFIELD_DH),
                        new FantasyDraftPick(MLBTeam.TBR, MLBPositionGroup.BULLPEN)
                ))
                .wildcardPick(
                        MLBPlayer.builder()
                                .name("Sandy Alcantara")
                                .team(MLBTeam.MIA)
                                .positionGroup(MLBPositionGroup.ROTATION)
                                .build()
                )
                .build();

        FantasyGame game = new FantasyGame(List.of(mike, nick, jon, dan), 2023);
        game.play();
    }
}
