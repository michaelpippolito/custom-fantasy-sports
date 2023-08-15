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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BaseballReferenceHelper {
    private static final String BASEBALL_REFERENCE_BASE_URL = "https://www.baseball-reference.com";

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

    public static Collection<MLBPlayer> getTeamStats(MLBTeam team, MLBPositionGroup positionGroup, int year) {
        Map<String, MLBPlayer> players = new LinkedHashMap<>();

        String teamUrl = BASEBALL_REFERENCE_BASE_URL + "/teams/" + team.getAbbreviation() + "/" + year + ".shtml";
        Document teamDocument = getHtmlDocument(teamUrl);
        List<String> gameUrls = getAllGameUrls(teamDocument);

        for (int i = 0; i < gameUrls.size(); i++) {

            String gameUrl = gameUrls.get(i);

            Document gameDocument = BaseballReferenceHelper.getHtmlDocument(gameUrl);
            System.out.print("\tDownloading data from game " + (i + 1) + "/" + gameUrls.size() + " - " + getGameInfo(gameDocument) + "...");
            switch (positionGroup) {
                case ROTATION:
                    MLBPlayer startingPitcher = getStartingPitcher(team, gameDocument);

                    if (players.containsKey(startingPitcher.getName())) {
                        players.get(startingPitcher.getName()).updateInningsPitched(startingPitcher.getInningsPitched());
                    } else {
                        Document playerDocument = getHtmlDocument(startingPitcher.getUrl());
                        startingPitcher.setTotalWAR(getTotalWAR(playerDocument, positionGroup, team, year));
                        startingPitcher.setTotalInningsPitched(getTotalInningsPitched(playerDocument, team, year));
                        players.put(startingPitcher.getName(), startingPitcher);
                    }
                    break;
                case INFIELD:
                    Set<MLBPlayer> infielders = getInfielders(team, gameDocument);

                    for (MLBPlayer infielder : infielders) {
                        if (players.containsKey(infielder.getName())) {
                            players.get(infielder.getName()).updatePlateAppearances(infielder.getPlateAppearances());
                        } else {
                            Document infielderDocument = getHtmlDocument(infielder.getUrl());
                            infielder.setTotalWAR(getTotalWAR(infielderDocument, positionGroup, team, year));
                            infielder.setTotalPlateAppearances(getTotalPlateAppearances(infielderDocument, team, year));
                            players.put(infielder.getName(), infielder);
                        }
                    }
                    break;
                case OUTFIELD_DH:
                    Set<MLBPlayer> outfielders = getOutfielders(team, gameDocument);

                    for (MLBPlayer outfielder : outfielders) {
                        if (players.containsKey(outfielder.getName())) {
                            players.get(outfielder.getName()).updatePlateAppearances(outfielder.getPlateAppearances());
                        } else {
                            Document outfielderDocument = getHtmlDocument(outfielder.getUrl());
                            outfielder.setTotalWAR(getTotalWAR(outfielderDocument, positionGroup, team, year));
                            outfielder.setTotalPlateAppearances(getTotalPlateAppearances(outfielderDocument, team, year));
                            players.put(outfielder.getName(), outfielder);
                        }
                    }
                    break;
                case BULLPEN:
                    Set<MLBPlayer> reliefPitchers = getReliefPitchers(team, gameDocument);

                    for (MLBPlayer reliefPitcher : reliefPitchers) {
                        if (players.containsKey(reliefPitcher.getName())) {
                            players.get(reliefPitcher.getName()).updateInningsPitched(reliefPitcher.getInningsPitched());
                        } else {
                            Document playerDocument = getHtmlDocument(reliefPitcher.getUrl());
                            reliefPitcher.setTotalWAR(getTotalWAR(playerDocument, positionGroup, team, year));
                            reliefPitcher.setTotalInningsPitched(getTotalInningsPitched(playerDocument, team, year));
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

    public static void populatePlayerStats(MLBPlayer player, int year) {
        setPlayerUrl(player, year);
        Document playerDocument = getHtmlDocument(player.getUrl());
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
            player.setUrl(BASEBALL_REFERENCE_BASE_URL + players.get(player.getName()));
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

    private static MLBPlayer getStartingPitcher(MLBTeam team, Document gameDocument) {
        Element pitchingTableElement = getPitchingTableElement(team, gameDocument);
        Element startingPitcherElement = pitchingTableElement.select("th[csk=\"0\"]").first().parent();

        String pitcherName = getPitcherName(startingPitcherElement);

        return MLBPlayer.builder()
                .name(pitcherName)
                .team(team)
                .positionGroup(MLBPositionGroup.ROTATION)
                .inningsPitched(getInningsPitched(startingPitcherElement))
                .url(getPitcherUrl(startingPitcherElement))
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

    private static Double getInningsPitched(Element pitcherElement) {
        return Double.parseDouble(pitcherElement.select("td[data-stat=\"IP\"]").first().firstChild().toString());
    }

    private static int getPlateAppearances(Element positionPlayerElement) {
        return Integer.parseInt(Optional.of(positionPlayerElement.select("td[data-stat=\"PA\"]")).map(Elements::first).map(Node::firstChild).map(Node::toString).orElse("0"));
    }

    private static String getPitcherUrl(Element pitcherElement) {
        return BASEBALL_REFERENCE_BASE_URL + pitcherElement.select("th").first().firstElementChild().attributes().get("href");
    }

    private static String getPositionPlayerUrl(Element positionPlayerElement) {
        Element identifierElement = positionPlayerElement.select("th").first();
        if (identifierElement.firstChild().toString().trim().contains("&nbsp;")) {
            return BASEBALL_REFERENCE_BASE_URL + identifierElement.childNode(1).attributes().get("href");
        }
        return BASEBALL_REFERENCE_BASE_URL + positionPlayerElement.select("th").first().childNode(0).attributes().get("href");
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

    private static int getTotalPlateAppearances(Document playerDocument, MLBTeam team, int year) {
        Node playerValueComment = playerDocument.select("div[id~=all_batting_value]").stream().filter(element ->
                element.childNodes().stream().anyMatch(node ->
                        node instanceof Element && ((Element) node).select(
                                "span[data-label=\"Player Value--Batting\"]"
                        ).size() > 0
                )
        ).findFirst().orElse(null).childNodes().stream().filter(node -> node instanceof Comment).findFirst().orElse(null);

        Document playerValueDocument = Jsoup.parse(removeHtmlComments(playerValueComment.toString()));

        AtomicInteger plateAppearances = new AtomicInteger();
        playerValueDocument.getElementsByAttributeValue("id", "batting_value." + year).stream().filter(element ->
                team.getAbbreviation().equals(element.select("td[data-stat=\"team_ID\"]").first().firstChild().firstChild().toString())
        ).forEach(element -> {
            Element plateAppearancesElement = element.select("td[data-stat=\"PA\"]").first();
            String plateAppearancesString = plateAppearancesElement.firstChild().toString();

            if (plateAppearancesString.contains("<strong>")) {
                plateAppearances.getAndAdd(Integer.parseInt(plateAppearancesElement.firstChild().firstChild().toString()));
            } else {
                plateAppearances.getAndAdd(Integer.parseInt(plateAppearancesElement.firstChild().toString()));
            }
        });
        return plateAppearances.get();
    }

    private static Set<MLBPlayer> getInfielders(MLBTeam team, Document gameDocument) {
        Set<MLBPlayer> infielders = new LinkedHashSet<>();

        Element battingTableElement = getBattingTableElement(team, gameDocument);
        List<Element> positionPlayers = battingTableElement.select("th[csk~=[0-9][0-8]]").stream().map(Element::parent).collect(Collectors.toList());

        for (Element positionPlayerElement : positionPlayers) {
            List<BaseballReferencePosition> positionsPlayed = getPositionPlayerPositionsPlayed(positionPlayerElement);
            if (positionsPlayed.stream().anyMatch(BaseballReferencePosition::isInfield)) {
                MLBPlayer infielder = MLBPlayer.builder()
                        .name(getPositionPlayerName(positionPlayerElement))
                        .team(team)
                        .positionGroup(MLBPositionGroup.INFIELD)
                        .plateAppearances(getPlateAppearances(positionPlayerElement))
                        .url(getPositionPlayerUrl(positionPlayerElement))
                        .build();
                infielders.add(infielder);
            }
        }

        return infielders;
    }

    private static Set<MLBPlayer> getOutfielders(MLBTeam team, Document gameDocument) {
        Set<MLBPlayer> outfielders = new LinkedHashSet<>();

        Element battingTableElement = getBattingTableElement(team, gameDocument);
        List<Element> positionPlayers = battingTableElement.getElementById(team.getName().replaceAll(" ", "") + "batting").getElementsByTag("tbody").first().children().stream().filter(element -> !element.hasClass("spacer")).collect(Collectors.toList());

        for (Element positionPlayerElement : positionPlayers) {
            List<BaseballReferencePosition> positionsPlayed = getPositionPlayerPositionsPlayed(positionPlayerElement);
            if (positionsPlayed.stream().anyMatch(BaseballReferencePosition::isOutfield) || positionsPlayed.stream().allMatch(BaseballReferencePosition::isPinch)) {
                MLBPlayer infielder = MLBPlayer.builder()
                        .name(getPositionPlayerName(positionPlayerElement))
                        .team(team)
                        .positionGroup(MLBPositionGroup.OUTFIELD_DH)
                        .plateAppearances(getPlateAppearances(positionPlayerElement))
                        .url(getPositionPlayerUrl(positionPlayerElement))
                        .build();
                outfielders.add(infielder);
            }

        }

        return outfielders;
    }

    private static Set<MLBPlayer> getReliefPitchers(MLBTeam team, Document gameDocument) {
        Set<MLBPlayer> reliefPitchers = new LinkedHashSet<>();

        Element pitchingTableElement = getPitchingTableElement(team, gameDocument);
        List<Element> reliefPitcherElements = pitchingTableElement.select("th[csk~=[1-9]+]").stream().map(Element::parent).collect(Collectors.toList());

        for (Element reliefPitcherElement : reliefPitcherElements) {
            MLBPlayer reliefPitcher = MLBPlayer.builder()
                    .name(getPitcherName(reliefPitcherElement))
                    .team(team)
                    .positionGroup(MLBPositionGroup.BULLPEN)
                    .inningsPitched(getInningsPitched(reliefPitcherElement))
                    .url(getPitcherUrl(reliefPitcherElement))
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
