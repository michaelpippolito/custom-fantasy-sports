package com.michaelpippolito.fantasy.mlb.load;

import com.michaelpippolito.fantasy.common.HtmlHelper;
import com.michaelpippolito.fantasy.mlb.BaseballReferenceHelper;
import com.michaelpippolito.fantasy.mlb.def.MlbPositionGroup;
import com.michaelpippolito.fantasy.mlb.def.MlbTeam;
import com.michaelpippolito.fantasy.mlb.repository.MlbGame;
import com.michaelpippolito.fantasy.mlb.repository.MlbGameRepository;
import com.michaelpippolito.fantasy.mlb.repository.MlbPlayer;
import com.michaelpippolito.fantasy.mlb.repository.MlbPlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.michaelpippolito.fantasy.mlb.def.Constants.BASEBALL_REFERENCE_BASE_URL;

@Component
@Slf4j
public class MlbDataLoader {
    @Autowired
    private MlbGameRepository mlbGameRepository;

    @Autowired
    private MlbPlayerRepository mlbPlayerRepository;

    @Autowired
    private HtmlHelper htmlHelper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Value("${mlb-year}")
    private int year;

    @EventListener(classes = ContextRefreshedEvent.class)
    public void loadGameData() {
        log.info("Checking status of MLB game data...");
        for (MlbTeam team : MlbTeam.values()) {
            if (mlbGameRepository.countByAwayTeamOrHomeTeam(team, team) < 162) {
                log.info("Loading games for {}", team.getName());

                Document teamDocument = getDocument(BASEBALL_REFERENCE_BASE_URL + "/teams/" + team.getAbbreviation() + "/" + year + ".shtml");
                if (teamDocument != null) {
                    List<String> gameUrls = BaseballReferenceHelper.getAllGameUrls(teamDocument);
                    for (String gameUrl : gameUrls) {
                        if (mlbGameRepository.findById(gameUrl).isEmpty()) {
                            Document gameDocument = getDocument(gameUrl);

                            if (gameDocument != null) {
                                mlbGameRepository.save(new MlbGame(gameUrl, getAwayTeam(gameDocument), getHomeTeam(gameDocument), gameDocument.toString().getBytes(StandardCharsets.UTF_8)));
                            }
                        }
                    }
                }
            } else {
                log.debug("Game data already loaded for {}", team.getName());
            }
        }
        log.info("MLB game data loaded");
        applicationEventPublisher.publishEvent(new GameDataLoadedEvent(this));
    }

    @EventListener(classes = GameDataLoadedEvent.class)
    public void loadPlayerData() {
        log.info("Checking status of MLB player data...");
        for (MlbTeam team : MlbTeam.values()) {
            log.info("Checking status of {} player data...", team.getName());
            loadPlayerData(team, mlbGameRepository.findByAwayTeamOrHomeTeam(team, team).stream().map(it -> Jsoup.parse(new String(it.getHtml(), StandardCharsets.UTF_8))).collect(Collectors.toList()));
        }
        log.info("MLB player data loaded");
    }

    private void loadPlayerData(MlbTeam team, List<Document> gameDocuments) {
        Map<String, MlbPlayer> players = new LinkedHashMap<>();

        for (Document gameDocument : gameDocuments) {
            MlbPlayer startingPitcher = BaseballReferenceHelper.getStartingPitcher(team, gameDocument, year);
            Set<MlbPlayer> infielders = BaseballReferenceHelper.getInfielders(team, gameDocument, year);
            Set<MlbPlayer> outfielders = BaseballReferenceHelper.getOutfielders(team, gameDocument, year);
            Set<MlbPlayer> reliefPitchers = BaseballReferenceHelper.getReliefPitchers(team, gameDocument, year);

            savePlayer(startingPitcher);
            for (MlbPlayer reliefPitcher : reliefPitchers) {
                savePlayer(reliefPitcher);
            }
            for (MlbPlayer infielder : infielders) {
                savePlayer(infielder);
            }
            for (MlbPlayer outfielder : outfielders) {
                savePlayer(outfielder);
            }
        }
    }

    private Document getDocument(String url) {
        try {
            return htmlHelper.getHtmlDocument(url);
        } catch (Exception e) {
            log.error("Error downloading HTML for {}\n{}", url, ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    private MlbTeam getAwayTeam(Document gameDocument) {
        return MlbTeam.fromName(gameDocument.getElementsByClass("scorebox")
                .select("strong")
                .select("a")
                .get(0)
                .firstChild()
                .toString());
    }

    private MlbTeam getHomeTeam(Document gameDocument) {
        return MlbTeam.fromName(gameDocument.getElementsByClass("scorebox")
                .select("strong")
                .select("a")
                .get(1)
                .firstChild()
                .toString());
    }

    private void savePlayer(MlbPlayer player) {
        MlbPlayer existingPlayer = mlbPlayerRepository.findByNameAndTeamAndPosition(player.getName(), player.getTeam(), player.getPosition());
        if (existingPlayer == null) {
            try {
                log.debug("Downloading stats for {}", player.getName());
                if (player.getPosition().isPitcher()) {
                    BaseballReferenceHelper.setPitcherWar(player, getDocument(player.getOverviewUrl()), year, htmlHelper);
                } else {
                    BaseballReferenceHelper.setPositionPlayerWar(player, getDocument(player.getOverviewUrl()), year, htmlHelper);
                }
                mlbPlayerRepository.save(player);
            } catch (Exception e) {
                log.error("Failed to calculate WAR for {}", player.getName());
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    public static class GameDataLoadedEvent extends ApplicationEvent {
        public GameDataLoadedEvent(Object source) {
            super(source);
        }
    }
}
