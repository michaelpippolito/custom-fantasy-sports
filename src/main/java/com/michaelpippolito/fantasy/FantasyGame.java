package com.michaelpippolito.fantasy;

import com.michaelpippolito.fantasy.mlb.MLBPlayer;
import com.michaelpippolito.fantasy.mlb.MLBTeam;
import com.michaelpippolito.fantasy.results.ResultsWriter;
import com.michaelpippolito.fantasy.stats.BaseballReferenceHelper;
import lombok.AllArgsConstructor;
import org.jsoup.nodes.Document;

import java.util.*;

@AllArgsConstructor
public class FantasyGame {
    private final List<FantasyPlayer> players;
    private final int year;

    public void play() {

        Map<FantasyPlayer, Collection<MLBPlayer>> results = new LinkedHashMap<>();
        Map<MLBTeam, List<Document>> gameDocumentsCache = new LinkedHashMap<>();

        for (FantasyPlayer player : players) {
            System.out.println("Calculating score for " + player.getName() + "...");

            Collection<MLBPlayer> draftedPlayersResults = new LinkedHashSet<>();
            for (FantasyDraftPick draftPick : player.getDraftPicks()) {
                System.out.println("\t" + player.getName() + " has drafted the " + draftPick.getTeam().getName() + " " + draftPick.getPosition().getValue());

                Collection<MLBPlayer> draftedPlayers = BaseballReferenceHelper.getTeamStats(draftPick.getTeam(), draftPick.getPosition(), year, gameDocumentsCache);
                draftedPlayersResults.addAll(draftedPlayers);

                System.out.println("\t\t" + draftedPlayers.size() + " players in the " + draftPick.getTeam().getName() + " " + draftPick.getPosition().getValue());
            }

            System.out.println("\t" + player.getName() + " has drafted " + player.getWildcardPick().getName() + " as a Wildcard");
            BaseballReferenceHelper.populatePlayerStats(player.getWildcardPick(), year);

            results.put(player, draftedPlayersResults);
        }

        ResultsWriter.writeResults(results);

    }
}
