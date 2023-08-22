package com.michaelpippolito.fantasy.stats;

import com.google.common.util.concurrent.AtomicDouble;
import com.michaelpippolito.fantasy.mlb.MLBPlayer;
import com.michaelpippolito.fantasy.mlb.MLBPositionGroup;
import com.michaelpippolito.fantasy.mlb.MLBTeam;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseballReferenceHelper {
    private static final String BASEBALL_REFERENCE_BASE_URL = "https://www.baseball-reference.com";
    private static final String BASEBALL_REFERENCE_OVERVIEW_PAGE = "/players/%s/%s.shtml";
    private static final String BASEBALL_REFERENCE_SPLITS_PAGE = "/players/split.fcgi?id=%s&year=%d&t=%s";

    @SneakyThrows
    private static Document getHtmlDocument(String url) {
        try {
            return getHtmlDocument(url, 2000);
        } catch (org.jsoup.HttpStatusException e) {
            if (e.getStatusCode() == 429) {
                System.out.println(e.getUrl());
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                Map<String, java.util.List<String>> headers = connection.getHeaderFields();
                int retryAfter = Integer.parseInt(headers.get("Retry-After").get(0));
                System.out.println("Uh-oh! Too many requests - banned from baseball-reference for " + retryAfter + " seconds");
                return getHtmlDocument(url, retryAfter * 1000);
            } else {
                throw e;
            }
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("Socket time out!");
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            Map<String, java.util.List<String>> headers = connection.getHeaderFields();
            int retryAfter = Integer.parseInt(headers.get("Retry-After").get(0));
            return getHtmlDocument(url, retryAfter * 1000);
        }
    }

    private static Document getHtmlDocument(String url, int sleep) throws InterruptedException, IOException {
        Thread.sleep(sleep);
        return Jsoup.connect(url).get();
    }

    public static Collection<MLBPlayer> getTeamStats(MLBTeam team, MLBPositionGroup positionGroup, int year, Map<MLBTeam, List<Document>> gameDocumentsCache) {
        Map<String, MLBPlayer> players = new LinkedHashMap<>();

        String teamUrl = BASEBALL_REFERENCE_BASE_URL + "/teams/" + team.getAbbreviation() + "/" + year + ".shtml";

        Document teamDocument;
        List<String> gameUrls = new ArrayList<>();
        int size;
        boolean teamCached = false;

        if (!gameDocumentsCache.containsKey(team)) {
            gameDocumentsCache.put(team, new LinkedList<Document>());
            teamDocument = getHtmlDocument(teamUrl);
            gameUrls = getAllGameUrls(teamDocument);
            size = gameUrls.size();
        } else {
            size = gameDocumentsCache.get(team).size();
            teamCached = true;
        }

        for (int i = 0; i < size; i++) {

            String gameUrl;

            Document gameDocument;
            if (teamCached) {
                gameDocument = gameDocumentsCache.get(team).get(i);
                System.out.print("\tRetrieving cached data from game " + (i + 1) + "/" + size + " - " + getGameInfo(gameDocument) + "...");
            } else {
                gameUrl = gameUrls.get(i);
                gameDocument = BaseballReferenceHelper.getHtmlDocument(gameUrl);
                gameDocumentsCache.get(team).add(gameDocument);
                System.out.print("\tDownloading data from game " + (i + 1) + "/" + size + " - " + getGameInfo(gameDocument) + "...");
            }

            switch (positionGroup) {
                case ROTATION:
                    MLBPlayer startingPitcher = getStartingPitcher(team, gameDocument, year);

                    if (!players.containsKey(startingPitcher.getName())) {
                        Document startingPitcherOverviewDocument = getHtmlDocument(startingPitcher.getOverviewUrl());
                        Document startingPitcherSplitsDocument = getHtmlDocument(startingPitcher.getSplitsUrl());
                        setInningsPitched(startingPitcher, startingPitcherSplitsDocument, MLBPositionGroup.ROTATION);
                        startingPitcher.setTotalInningsPitched(getTotalInningsPitched(startingPitcherOverviewDocument, team, year));
                        startingPitcher.setTotalWAR(getTotalWAR(startingPitcherOverviewDocument, MLBPositionGroup.ROTATION, team, year));
                        players.put(startingPitcher.getName(), startingPitcher);
                    }
                    break;
                case INFIELD:
                    Set<MLBPlayer> infielders = getInfielders(team, gameDocument, year);

                    for (MLBPlayer infielder : infielders) {
                        if (!players.containsKey(infielder.getName())) {
                            Document infielderOverviewDocument = getHtmlDocument(infielder.getOverviewUrl());
                            Document infielderSplitsDocument = getHtmlDocument(infielder.getSplitsUrl());
                            setPlateAppearances(infielder, infielderSplitsDocument, MLBPositionGroup.INFIELD);
                            infielder.setTotalWAR(getTotalWAR(infielderOverviewDocument, positionGroup, team, year));
                            players.put(infielder.getName(), infielder);
                        }
                    }
                    break;
                case OUTFIELD_DH:
                    Set<MLBPlayer> outfielders = getOutfielders(team, gameDocument, year);

                    for (MLBPlayer outfielder : outfielders) {
                        if (!players.containsKey(outfielder.getName())) {
                            Document outfielderOverviewDocument = getHtmlDocument(outfielder.getOverviewUrl());
                            Document outfielderSplitsDocument = getHtmlDocument(outfielder.getSplitsUrl());
                            setPlateAppearances(outfielder, outfielderSplitsDocument, MLBPositionGroup.OUTFIELD_DH);
                            outfielder.setTotalWAR(getTotalWAR(outfielderOverviewDocument, positionGroup, team, year));
                            players.put(outfielder.getName(), outfielder);
                        }
                    }
                    break;
                case BULLPEN:
                    Set<MLBPlayer> reliefPitchers = getReliefPitchers(team, gameDocument, year);

                    for (MLBPlayer reliefPitcher : reliefPitchers) {
                        if (!players.containsKey(reliefPitcher.getName())) {
                            Document reliefPitcherOverviewDocument = getHtmlDocument(reliefPitcher.getOverviewUrl());
                            Document reliefPitcherSplitsDocument = getHtmlDocument(reliefPitcher.getSplitsUrl());
                            setInningsPitched(reliefPitcher, reliefPitcherSplitsDocument, MLBPositionGroup.BULLPEN);
                            reliefPitcher.setTotalInningsPitched(getTotalInningsPitched(reliefPitcherOverviewDocument, team, year));
                            reliefPitcher.setTotalWAR(getTotalWAR(reliefPitcherOverviewDocument, MLBPositionGroup.BULLPEN, team, year));
                            players.put(reliefPitcher.getName(), reliefPitcher);
                        }
                    }
                    break;
            }
            System.out.print("\r");
        }
        System.out.println("\tSuccessfully looked up stats for " + team.getName() + " " + positionGroup.getValue());
        return players.values();
    }

    private static void setPlateAppearances(MLBPlayer player, Document positionPlayerDocument, MLBPositionGroup positionGroup) {
        Element defensiveSplitsElement = positionPlayerDocument.select("div[id~=all_defp]").stream().filter(element ->
                element.childNodes().stream().anyMatch(node ->
                        node instanceof Element && ((Element) node).select(
                                "span[data-label=\"Defensive Positions\"]"
                        ).size() > 0
                )
        ).findFirst().orElse(null);

        if (defensiveSplitsElement != null) {
            Node defensiveSplitsComment = defensiveSplitsElement.childNodes().stream().filter(node -> node instanceof Comment).findFirst().orElse(null);

            Document defensiveTableDocument = Jsoup.parse(removeHtmlComments(defensiveSplitsComment.toString()));


            Element defensivePositionsTable = defensiveTableDocument.getElementById("defp");
            Element defensivePositionsTableBody = defensivePositionsTable.getElementsByTag("tbody").first();

            int totalPlateAppearances = 0;
            player.setPlateAppearances(0);
            for (Element defensivePositionsRow : defensivePositionsTableBody.getElementsByTag("tr")) {
                BaseballReferencePosition position = BaseballReferencePosition.fromString(getPositionString(defensivePositionsRow));
                Node plateAppearancesNode = defensivePositionsRow.select("td[data-stat=\"PA\"]").first().firstChild();
                if (plateAppearancesNode != null) {
                    int plateAppearances = Integer.parseInt(plateAppearancesNode.toString());
                    totalPlateAppearances += plateAppearances;

                    switch (positionGroup) {
                        case INFIELD:
                            if (position.isInfield()) {
                                player.updatePlateAppearances(plateAppearances);
                            }
                            break;
                        case OUTFIELD_DH:
                            if (position.isOutfield()) {
                                player.updatePlateAppearances(plateAppearances);
                            }
                            break;
                    }
                }
            }
            player.setTotalPlateAppearances(totalPlateAppearances);
        } else {
            player.setPlateAppearances(0);
            player.setTotalPlateAppearances(0);
        }
    }

    private static void setInningsPitched(MLBPlayer player, Document pitcherDocument, MLBPositionGroup positionGroup) {
        Node pitchingRoleComment = pitcherDocument.select("span[data-label=Pitching Role]").first().parent().parent().childNodes().stream().filter(node -> node instanceof Comment).findFirst().orElse(null);
        Document pitchingRoleDocument = Jsoup.parse(removeHtmlComments(pitchingRoleComment.toString()));

        Element pitchingRoleTable = pitchingRoleDocument.select("div[id=\"all_sprel_extra\"]").first();
        Element pitchingRoleTableBody = pitchingRoleTable.getElementsByTag("tbody").first();

        player.setInningsPitched(0);
        for (Element pitchingPositionRow : pitchingRoleTableBody.getElementsByTag("tr")) {
            BaseballReferencePosition position = BaseballReferencePosition.fromString(getPositionString(pitchingPositionRow));
            Node inningsPitchedNode = pitchingPositionRow.select("td[data-stat=\"IP\"]").first().firstChild();
            if (inningsPitchedNode != null) {
                double inningsPitched = Double.parseDouble(inningsPitchedNode.toString());

                switch (positionGroup) {
                    case ROTATION:
                        if (BaseballReferencePosition.STARTER.equals(position)) {
                            player.updateInningsPitched(inningsPitched);
                        }
                        break;
                    case BULLPEN:
                        if (BaseballReferencePosition.RELIEVER.equals(position)) {
                            player.updateInningsPitched(inningsPitched);
                        }
                        break;
                }
            }
        }
    }

    private static String getPositionString(Element defensivePositionsRow) {
        String positionString = defensivePositionsRow.select("th[data-stat=\"split_name\"]").first().firstChild().toString();
        if (positionString.equals(BaseballReferencePosition.OTHER.getWebValue())) {
            return positionString;
        } else {
            positionString = positionString.substring(3);
        }

        if (positionString.matches("^(?:[A-Z]{1,2}|[1-3][A-Z]) for (?:[A-Z]{1,2}|[1-3][A-Z])$")) {
            return positionString.substring(0, positionString.indexOf("for")).trim();
        } else {
            return positionString;
        }
    }

    public static void populatePlayerStats(MLBPlayer player, int year) {
        setPlayerUrl(player, year);
        Document playerDocument = getHtmlDocument(player.getOverviewUrl());
        player.setTotalWAR(getTotalWAR(playerDocument, player.getPositionGroup(), player.getTeam(), year));
    }

    private static void setPlayerUrl(MLBPlayer player, int year) {
        String teamUrl = BASEBALL_REFERENCE_BASE_URL + "/teams/" + player.getTeam().getAbbreviation() + "/" + year + ".shtml";
        Document teamDocument = getHtmlDocument(teamUrl);

        Map<String, String> players;

        switch (player.getPositionGroup()) {
            case ROTATION:
            case BULLPEN:
                players = teamDocument.getElementById("team_pitching").getElementsByTag("a").stream()
                        .collect(Collectors.toMap(
                                element -> element.firstChild().toString(),
                                element -> element.attr("href")
                        ));
                break;
            case INFIELD:
            case OUTFIELD_DH:
                players = teamDocument.getElementById("team_batting").getElementsByTag("a").stream()
                        .collect(Collectors.toMap(
                                element -> element.firstChild().toString(),
                                element -> element.attr("href")
                        ));
                break;
            default:
                players = Collections.emptyMap();
        }

        if (players.keySet().contains(player.getName())) {
            player.setOverviewUrl(BASEBALL_REFERENCE_BASE_URL + players.get(player.getName()));
        }

    }

    private static List<String> getAllGameUrls(Document teamDocument) {
        List<String> gameUrls = new LinkedList<>();
        Elements timelineResults = teamDocument.getElementsByClass("timeline");
        for (Element timelineResult : timelineResults) {
            for (Node timelineNode : timelineResult.childNodes().stream().filter(node -> node.hasAttr("class")).collect(Collectors.toList())) {
                Element timelineNodeElement = (Element) timelineNode;
                if (timelineNodeElement.attributes().get("class").equals("result")) {
                    for (Node gameNode : timelineNodeElement.childNodes().stream().filter(node -> node.hasAttr("class")).collect(Collectors.toList())) {
                        Element gameElement = (Element) gameNode;
                        if (gameElement.hasAttr("tip")) {
                            if (!gameElement.attributes().get("tip").equals("Off Day")) {
                                String gameDate = gameElement.attributes().get("tip");

                                if (Character.isDigit(gameDate.charAt(0))) {
                                    for (Node gameDataNode : gameElement.childNodes().stream().filter(node -> node.hasAttr("href")).collect(Collectors.toList())) {
                                        String gameEndpoint = gameDataNode.attributes().get("href");
                                        gameUrls.add(BASEBALL_REFERENCE_BASE_URL + "/" + gameEndpoint);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        return gameUrls;
    }

    private static MLBPlayer getStartingPitcher(MLBTeam team, Document gameDocument, int year) {
        Element pitchingTableElement = getPitchingTableElement(team, gameDocument);
        Element startingPitcherElement = pitchingTableElement.select("th[csk=\"0\"]").first().parent();
        String playerId = getPlayerId(startingPitcherElement);

        return MLBPlayer.builder()
                .name(getPitcherName(startingPitcherElement))
                .team(team)
                .positionGroup(MLBPositionGroup.ROTATION)
                .overviewUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_OVERVIEW_PAGE, playerId.charAt(0), playerId))
                .splitsUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_SPLITS_PAGE, playerId, year, "p"))
                .build();
    }

    private static Element getPitchingTableElement(MLBTeam team, Document gameDocument) {
        Node pitchingTableComment = gameDocument.select("div[id~=all_\\d+]").stream().filter(element ->
                element.childNodes().stream().anyMatch(node ->
                        node instanceof Element && ((Element) node).select(
                                "span[data-label=\"Pitching Lines and Info\"]"
                        ).size() > 0
                )
        ).findFirst().orElse(null).childNodes().stream().filter(node -> node instanceof Comment).findFirst().orElse(null);

        Document pitchingTableDocument = Jsoup.parse(removeHtmlComments(pitchingTableComment.toString()));
        return pitchingTableDocument.getElementById(team.getName().replaceAll(" ", "") + "pitching");
    }

    private static String getGameInfo(Document gameDocument) {
        return gameDocument.select("h1").first().firstChild().toString().replace(" Box Score", "");
    }

    private static String getPitcherName(Element pitcherElement) {
        return pitcherElement.select("th").first().firstElementChild().firstChild().toString();
    }

    private static String getPlayerId(Element playerElement) {
        Element identifierElement = playerElement.select("th").first();
        String playerHref;
        if (identifierElement.firstChild().toString().trim().contains("&nbsp;")) {
            playerHref = identifierElement.childNode(1).attributes().get("href");
        } else {
            playerHref = playerElement.select("th").first().childNode(0).attributes().get("href");
        }
        return playerHref.substring(playerHref.lastIndexOf("/") + 1, playerHref.lastIndexOf("."));
    }

    private static double getTotalWAR(Document playerDocument, MLBPositionGroup positionGroup, MLBTeam team, int year) {
        switch (positionGroup) {
            case ROTATION:
            case BULLPEN: {
                Node playerValueComment = playerDocument.select("div[id~=all_pitching_value]").stream().filter(element ->
                        element.childNodes().stream().anyMatch(node ->
                                node instanceof Element && ((Element) node).select(
                                        "span[data-label=\"Player Value--Pitching\"]"
                                ).size() > 0
                        )
                ).findFirst().orElse(null).childNodes().stream().filter(node -> node instanceof Comment).findFirst().orElse(null);

                Document playerValueDocument = Jsoup.parse(removeHtmlComments(playerValueComment.toString()));

                AtomicDouble totalWAR = new AtomicDouble();
                playerValueDocument.getElementsByAttributeValue("id", "pitching_value." + year).stream().filter(element ->
                        team.getAbbreviation().equals(element.select("td[data-stat=\"team_ID\"]").first().firstChild().firstChild().toString())
                ).forEach(element -> {
                    Element warElement = element.select("td[data-stat=\"WAR_pitch\"]").first();
                    String warString = Optional.of(warElement).map(Node::firstChild).map(Node::toString).orElse("0.0")
                            .replace("<em>", "")
                            .replace("</em>", "")
                            .replace("<strong>", "")
                            .replace("</strong>", "");

                    totalWAR.getAndAdd(Double.parseDouble(warString));
                });
                return totalWAR.get();
            }
            case INFIELD:
            case OUTFIELD_DH:
                Node playerValueComment = playerDocument.select("div[id~=all_batting_value]").stream().filter(element ->
                        element.childNodes().stream().anyMatch(node ->
                                node instanceof Element && ((Element) node).select(
                                        "span[data-label=\"Player Value--Batting\"]"
                                ).size() > 0
                        )
                ).findFirst().orElse(null).childNodes().stream().filter(node -> node instanceof Comment).findFirst().orElse(null);

                Document playerValueDocument = Jsoup.parse(removeHtmlComments(playerValueComment.toString()));

                AtomicDouble totalWAR = new AtomicDouble();
                playerValueDocument.getElementsByAttributeValue("id", "batting_value." + year).stream().filter(element ->
                        team.getAbbreviation().equals(element.select("td[data-stat=\"team_ID\"]").first().firstChild().firstChild().toString())
                ).forEach(element -> {
                    Element warElement = element.select("td[data-stat=\"WAR\"]").first();
                    String warString = Optional.of(warElement).map(Node::firstChild).map(Node::toString).orElse("0.0")
                            .replace("<em>", "")
                            .replace("</em>", "")
                            .replace("<strong>", "")
                            .replace("</strong>", "");

                    totalWAR.getAndAdd(Double.parseDouble(warString));
                });
                return totalWAR.get();
            default:
                return 0;
        }
    }

    private static double getTotalInningsPitched(Document playerDocument, MLBTeam team, int year) {
        Node playerValueComment = playerDocument.select("div[id~=all_pitching_value]").stream().filter(element ->
                element.childNodes().stream().anyMatch(node ->
                        node instanceof Element && ((Element) node).select(
                                "span[data-label=\"Player Value--Pitching\"]"
                        ).size() > 0
                )
        ).findFirst().orElse(null).childNodes().stream().filter(node -> node instanceof Comment).findFirst().orElse(null);

        Document playerValueDocument = Jsoup.parse(removeHtmlComments(playerValueComment.toString()));

        AtomicDouble inningsPitched = new AtomicDouble();
        playerValueDocument.getElementsByAttributeValue("id", "pitching_value." + year).stream()
                .filter(element ->
                        team.getAbbreviation().equals(element.select("td[data-stat=\"team_ID\"]").first().firstChild().firstChild().toString())
                ).forEach(element -> {
                    Element inningsPitchedElement = element.select("td[data-stat=\"IP\"]").first();
                    String inningsPitchedString = inningsPitchedElement.firstChild().toString();

                    if (inningsPitchedString.contains("<strong>")) {
                        inningsPitched.getAndAdd(Double.parseDouble(inningsPitchedElement.firstChild().firstChild().toString()));
                    } else {
                        inningsPitched.getAndAdd(Double.parseDouble(inningsPitchedElement.firstChild().toString()));
                    }
                });
        return inningsPitched.get();
    }

    private static Set<MLBPlayer> getInfielders(MLBTeam team, Document gameDocument, int year) {
        Set<MLBPlayer> infielders = new LinkedHashSet<>();

        Element battingTableElement = getBattingTableElement(team, gameDocument);
        List<Element> positionPlayers = battingTableElement.select("th[csk~=[0-9][0-8]]").stream().map(Element::parent).collect(Collectors.toList());

        for (Element positionPlayerElement : positionPlayers) {
            List<BaseballReferencePosition> positionsPlayed = getPositionPlayerPositionsPlayed(positionPlayerElement);
            if (positionsPlayed.stream().anyMatch(BaseballReferencePosition::isInfield)) {
                String playerId = getPlayerId(positionPlayerElement);

                MLBPlayer infielder = MLBPlayer.builder()
                        .name(getPositionPlayerName(positionPlayerElement))
                        .team(team)
                        .positionGroup(MLBPositionGroup.INFIELD)
                        .overviewUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_OVERVIEW_PAGE, playerId.charAt(0), playerId))
                        .splitsUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_SPLITS_PAGE, playerId, year, "b"))
                        .build();
                infielders.add(infielder);
            }
        }

        return infielders;
    }

    private static Set<MLBPlayer> getOutfielders(MLBTeam team, Document gameDocument, int year) {
        Set<MLBPlayer> outfielders = new LinkedHashSet<>();

        Element battingTableElement = getBattingTableElement(team, gameDocument);
        List<Element> positionPlayers = battingTableElement.getElementById(team.getName().replaceAll(" ", "") + "batting").getElementsByTag("tbody").first().children().stream().filter(element -> !element.hasClass("spacer")).collect(Collectors.toList());

        for (Element positionPlayerElement : positionPlayers) {
            List<BaseballReferencePosition> positionsPlayed = getPositionPlayerPositionsPlayed(positionPlayerElement);
            if (positionsPlayed.stream().anyMatch(BaseballReferencePosition::isOutfield) || positionsPlayed.stream().allMatch(BaseballReferencePosition::isPinch)) {
                String playerId = getPlayerId(positionPlayerElement);

                MLBPlayer outfielder = MLBPlayer.builder()
                        .name(getPositionPlayerName(positionPlayerElement))
                        .team(team)
                        .positionGroup(MLBPositionGroup.OUTFIELD_DH)
                        .overviewUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_OVERVIEW_PAGE, playerId.charAt(0), playerId))
                        .splitsUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_SPLITS_PAGE, playerId, year, "b"))
                        .build();
                outfielders.add(outfielder);
            }

        }

        return outfielders;
    }

    private static Set<MLBPlayer> getReliefPitchers(MLBTeam team, Document gameDocument, int year) {
        Set<MLBPlayer> reliefPitchers = new LinkedHashSet<>();

        Element pitchingTableElement = getPitchingTableElement(team, gameDocument);
        List<Element> reliefPitcherElements = pitchingTableElement.select("th[csk~=[1-9]+]").stream().map(Element::parent).collect(Collectors.toList());

        for (Element reliefPitcherElement : reliefPitcherElements) {
            String playerId = getPlayerId(reliefPitcherElement);

            MLBPlayer reliefPitcher = MLBPlayer.builder()
                    .name(getPitcherName(reliefPitcherElement))
                    .team(team)
                    .positionGroup(MLBPositionGroup.BULLPEN)
                    .overviewUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_OVERVIEW_PAGE, playerId.charAt(0), playerId))
                    .splitsUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_SPLITS_PAGE, playerId, year, "p"))
                    .build();
            reliefPitchers.add(reliefPitcher);
        }

        return reliefPitchers;
    }

    private static Element getBattingTableElement(MLBTeam team, Document gameDocument) {
        String battingTableDivId = team.getName().replaceAll(" ", "") + "batting";

        Element battingTableElement = gameDocument.getElementById("all_" + battingTableDivId);

        Document battingTableDocument = battingTableElement.childNodes().stream()
                .filter(childNode -> childNode instanceof Comment)
                .findFirst()
                .map(battingTableComment -> Jsoup.parse(removeHtmlComments(battingTableComment.toString())))
                .orElse(null);

        return battingTableDocument.getElementById(battingTableDivId);
    }

    private static List<BaseballReferencePosition> getPositionPlayerPositionsPlayed(Element positionPlayerElement) {
        Element identifierElement = positionPlayerElement.select("th").first();
        if (identifierElement.firstChild().toString().trim().contains("&nbsp;")) {
            return Arrays.stream(positionPlayerElement.select("th").first().childNode(2).toString().trim().split("-")).map(BaseballReferencePosition::fromString).collect(Collectors.toList());
        }
        return Arrays.stream(positionPlayerElement.select("th").first().childNode(1).toString().trim().split("-")).map(BaseballReferencePosition::fromString).collect(Collectors.toList());
    }

    private static String getPositionPlayerName(Element positionPlayerElement) {
        Element identifierElement = positionPlayerElement.select("th").first();
        if (identifierElement.firstChild().toString().trim().contains("&nbsp;")) {
            return identifierElement.childNode(1).firstChild().toString();
        }
        return identifierElement.childNode(0).firstChild().toString();
    }

    private static String removeHtmlComments(String html) {
        return html.replace("<!--", "").replace("-->", "");
    }

}
