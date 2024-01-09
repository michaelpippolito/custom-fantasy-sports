package com.michaelpippolito.fantasy.mlb;

import com.michaelpippolito.fantasy.common.HtmlHelper;
import com.michaelpippolito.fantasy.mlb.def.BaseballReferencePosition;
import com.michaelpippolito.fantasy.mlb.def.MlbPositionGroup;
import com.michaelpippolito.fantasy.mlb.def.MlbTeam;
import com.michaelpippolito.fantasy.mlb.repository.MlbPlayer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.stream.Collectors;

import static com.michaelpippolito.fantasy.mlb.def.Constants.*;
import static com.michaelpippolito.fantasy.mlb.def.MlbPositionGroup.*;

public class BaseballReferenceHelper {
    public static List<String> getAllGameUrls(Document teamDocument) {
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

    public static MlbPlayer getStartingPitcher(MlbTeam team, Document gameDocument, int year) {
        Element pitchingTableElement = getPitchingTableElement(team, gameDocument);
        Element startingPitcherElement = pitchingTableElement.select("th[csk=\"0\"]").first().parent();
        String playerId = getPlayerId(startingPitcherElement);

        return MlbPlayer.builder()
                .name(getPitcherName(startingPitcherElement))
                .team(team)
                .position(ROTATION)
                .overviewUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_OVERVIEW_PAGE, playerId.charAt(0), playerId))
                .splitsUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_SPLITS_PAGE, playerId, year, "p"))
                .gameLogUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_GAME_LOG_PAGE, playerId, year, "p"))
                .totalInningsPitched(0.0)
                .relevantInningsPitched(0.0)
                .build();
    }

    public static Set<MlbPlayer> getInfielders(MlbTeam team, Document gameDocument, int year) {
        Set<MlbPlayer> infielders = new LinkedHashSet<>();

        Element battingTableElement = getBattingTableElement(team, gameDocument);
        List<Element> positionPlayers = battingTableElement.select("th[csk~=[0-9][0-8]]").stream().map(Element::parent).collect(Collectors.toList());

        for (Element positionPlayerElement : positionPlayers) {
            List<BaseballReferencePosition> positionsPlayed = getPositionPlayerPositionsPlayed(positionPlayerElement);
            if (positionsPlayed.stream().anyMatch(BaseballReferencePosition::isInfield)) {
                String playerId = getPlayerId(positionPlayerElement);

                MlbPlayer infielder = MlbPlayer.builder()
                        .name(getPositionPlayerName(positionPlayerElement))
                        .team(team)
                        .position(INFIELD)
                        .overviewUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_OVERVIEW_PAGE, playerId.charAt(0), playerId))
                        .splitsUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_SPLITS_PAGE, playerId, year, "b"))
                        .gameLogUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_GAME_LOG_PAGE, playerId, year, "b"))
                        .build();
                infielders.add(infielder);
            }
        }

        return infielders;
    }

    public static Set<MlbPlayer> getOutfielders(MlbTeam team, Document gameDocument, int year) {
        Set<MlbPlayer> outfielders = new LinkedHashSet<>();

        Element battingTableElement = getBattingTableElement(team, gameDocument);
        List<Element> positionPlayers = battingTableElement.getElementById(team.getName().replaceAll(" ", "").replaceAll("\\.", "") + "batting").getElementsByTag("tbody").first().children().stream().filter(element -> !element.hasClass("spacer")).collect(Collectors.toList());

        for (Element positionPlayerElement : positionPlayers) {
            List<BaseballReferencePosition> positionsPlayed = getPositionPlayerPositionsPlayed(positionPlayerElement);
            if (positionsPlayed.stream().anyMatch(BaseballReferencePosition::isOutfield) || positionsPlayed.stream().allMatch(BaseballReferencePosition::isPinch)) {
                String playerId = getPlayerId(positionPlayerElement);

                MlbPlayer outfielder = MlbPlayer.builder()
                        .name(getPositionPlayerName(positionPlayerElement))
                        .team(team)
                        .position(MlbPositionGroup.OUTFIELD_DH)
                        .overviewUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_OVERVIEW_PAGE, playerId.charAt(0), playerId))
                        .splitsUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_SPLITS_PAGE, playerId, year, "b"))
                        .gameLogUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_GAME_LOG_PAGE, playerId, year, "b"))
                        .build();
                outfielders.add(outfielder);
            }

        }

        return outfielders;
    }

    public static Set<MlbPlayer> getReliefPitchers(MlbTeam team, Document gameDocument, int year) {
        Set<MlbPlayer> reliefPitchers = new LinkedHashSet<>();

        Element pitchingTableElement = getPitchingTableElement(team, gameDocument);
        List<Element> reliefPitcherElements = pitchingTableElement.select("th[csk~=[1-9]+]").stream().map(Element::parent).collect(Collectors.toList());

        for (Element reliefPitcherElement : reliefPitcherElements) {
            String playerId = getPlayerId(reliefPitcherElement);

            MlbPlayer reliefPitcher = MlbPlayer.builder()
                    .name(getPitcherName(reliefPitcherElement))
                    .team(team)
                    .position(BULLPEN)
                    .overviewUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_OVERVIEW_PAGE, playerId.charAt(0), playerId))
                    .splitsUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_SPLITS_PAGE, playerId, year, "p"))
                    .gameLogUrl(BASEBALL_REFERENCE_BASE_URL + String.format(BASEBALL_REFERENCE_GAME_LOG_PAGE, playerId, year, "p"))
                    .build();
            reliefPitchers.add(reliefPitcher);
        }

        return reliefPitchers;
    }

    public static void setPitcherWar(MlbPlayer player, Document pitcherOverviewDocument, int year, HtmlHelper htmlHelper) throws Exception {
        Document gameLogDocument = htmlHelper.getHtmlDocument(player.getGameLogUrl());
        setInningsPitchedFromGameLog(gameLogDocument, player);

        Element pitchingValueTableElement = getPitchingValueTableElement(pitcherOverviewDocument, year, player.getTeam());
        Element pitchingValueRow = pitchingValueTableElement.getElementsByTag("tr").stream().filter(row -> row.childNodes().stream().anyMatch(column ->
                column.hasAttr("data-stat") && "team_ID".equals(column.attributes().get("data-stat")) && column.firstChild() != null && column.firstChild().firstChild() != null && column.firstChild().firstChild().toString().equals(player.getTeam().getAbbreviation())
        )).filter(row -> row.childNodes().stream().anyMatch(column ->
                column.hasAttr("data-stat") && "year_ID".equals(column.attributes().get("data-stat")) && column.firstChild() != null && column.firstChild().toString().equals(String.valueOf(year))
        )).findFirst().orElse(null);

        double totalWar = Double.parseDouble(
                stripAdditionalHtmlFromStat(pitchingValueRow.childNodes().stream().filter(it -> "WAR_pitch".equals(it.attributes().get("data-stat"))).findFirst().orElse(null)
                    .firstChild().toString())
        );
        player.setAdjustedWar(totalWar);
    }

    public static void setPositionPlayerWar(MlbPlayer player, Document playerOverviewDocument, int year, HtmlHelper htmlHelper) throws Exception {
        Document gameLogDocument = htmlHelper.getHtmlDocument(player.getGameLogUrl());
        setPlateAppearancesFromGameLog(gameLogDocument, player);

        Element battingValueTableElement = getBattingValueTableElement(playerOverviewDocument, year, player.getTeam());
        Element battingValueRow = battingValueTableElement.getElementsByTag("tr").stream().filter(row -> row.childNodes().stream().anyMatch(column ->
                column.hasAttr("data-stat") && "team_ID".equals(column.attributes().get("data-stat")) && column.firstChild() != null && column.firstChild().firstChild() != null && column.firstChild().firstChild().toString().equals(player.getTeam().getAbbreviation())
        )).filter(row -> row.childNodes().stream().anyMatch(column ->
                column.hasAttr("data-stat") && "year_ID".equals(column.attributes().get("data-stat")) && column.firstChild() != null && column.firstChild().toString().equals(String.valueOf(year))
        )).findFirst().orElse(null);

        double totalWar = Double.parseDouble(
                stripAdditionalHtmlFromStat(Optional.ofNullable(battingValueRow.childNodes().stream().filter(it -> "WAR".equals(it.attributes().get("data-stat"))).findFirst().orElse(null)
                        ).map(Node::firstChild).map(Node::toString).orElse("0.0"))
        );
        player.setAdjustedWar(totalWar);
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

    private static String removeHtmlComments(String html) {
        return html.replace("<!--", "").replace("-->", "");
    }

    private static Element getPitchingTableElement(MlbTeam team, Document gameDocument) {
        String teamNameNoSpaces = team.getName().replaceAll(" ", "").replaceAll("\\.", "");
        Document pitchingTableDocument = gameDocument.getElementsByAttributeValueMatching("id", "all_\\d")
                .stream().map(section -> section.childNodes().stream().filter(child -> child instanceof Comment).findFirst().orElse(null))
                .map(commentNode -> commentNode == null ? null : Jsoup.parse(removeHtmlComments(commentNode.toString())))
                .filter(document -> document.getElementById("all_" + teamNameNoSpaces + "pitching") != null)
                .findFirst().orElse(null);

        return pitchingTableDocument.getElementById(teamNameNoSpaces + "pitching");
    }

    private static Element getPitchingValueTableElement(Document playerOverviewDocument, int year, MlbTeam team) {
        Node pitchingValueTableComment = playerOverviewDocument.getElementById("all_pitching_value").childNodes()
                .stream().filter(node -> node instanceof Comment).findFirst().orElse(null);

        Document pitchingValueTableDocument = Jsoup.parse(removeHtmlComments(pitchingValueTableComment.toString()));

        return pitchingValueTableDocument.getElementsByTag("tr")
                .stream().filter(row -> row.hasAttr("id") && row.attributes().get("id").equals("pitching_value."+year))
                .filter(node -> node.childNodes().stream().anyMatch(column ->
                        column.hasAttr("data-stat")
                            && column.attributes().get("data-stat").equals("team_ID")
                            && column.firstChild() != null && column.firstChild().firstChild() != null && column.firstChild().firstChild().toString().equals(team.getAbbreviation())
                        ))
                .findFirst().orElse(null);
    }

    private static Element getBattingValueTableElement(Document playerOverviewDocument, int year, MlbTeam team) {
        Node battingValueTableComment = playerOverviewDocument.getElementById("all_batting_value").childNodes()
                .stream().filter(node -> node instanceof Comment).findFirst().orElse(null);

        Document battingValueTableDocument = Jsoup.parse(removeHtmlComments(battingValueTableComment.toString()));

        return battingValueTableDocument.getElementsByTag("tr")
                .stream().filter(row -> row.hasAttr("id") && row.attributes().get("id").equals("batting_value."+year))
                .filter(node -> node.childNodes().stream().anyMatch(column ->
                        column.hasAttr("data-stat")
                                && column.attributes().get("data-stat").equals("team_ID")
                                && column.firstChild() != null && column.firstChild().firstChild() != null && column.firstChild().firstChild().toString().equals(team.getAbbreviation())
                ))
                .findFirst().orElse(null);
    }

    private static Element getBattingTableElement(MlbTeam team, Document gameDocument) {
        String battingTableDivId = team.getName().replaceAll(" ", "").replaceAll("\\.", "") + "batting";

        Element battingTableElement = gameDocument.getElementById("all_" + battingTableDivId);

        Document battingTableDocument = battingTableElement.childNodes().stream()
                .filter(childNode -> childNode instanceof Comment)
                .findFirst()
                .map(battingTableComment -> Jsoup.parse(removeHtmlComments(battingTableComment.toString())))
                .orElse(null);

        return battingTableDocument.getElementById(battingTableDivId);
    }

    private static String getPitcherName(Element pitcherElement) {
        return pitcherElement.select("th").first().firstElementChild().firstChild().toString();
    }

    private static String getPositionPlayerName(Element positionPlayerElement) {
        Element identifierElement = positionPlayerElement.select("th").first();
        if (identifierElement.firstChild().toString().trim().contains("&nbsp;")) {
            return identifierElement.childNode(1).firstChild().toString();
        }
        return identifierElement.childNode(0).firstChild().toString();
    }

    private static List<BaseballReferencePosition> getPositionPlayerPositionsPlayed(Element positionPlayerElement) {
        Element identifierElement = positionPlayerElement.select("th").first();
        if (identifierElement.firstChild().toString().trim().contains("&nbsp;")) {
            return Arrays.stream(positionPlayerElement.select("th").first().childNode(2).toString().trim().split("-")).map(BaseballReferencePosition::fromString).collect(Collectors.toList());
        }
        return Arrays.stream(positionPlayerElement.select("th").first().childNode(1).toString().trim().split("-")).map(BaseballReferencePosition::fromString).collect(Collectors.toList());
    }

    private static boolean getPitcherIsTraded(Document pitcherOverviewDocument, int year) {
        return pitcherOverviewDocument.getElementById("pitching_standard." + year).childNodes().stream()
                .filter(it -> it.hasAttr("data-stat") && "team_ID".equals(it.attributes().get("data-stat")))
                .findFirst().map(Node::firstChild)
                .map(Node::childNodeSize).orElse(1) == 0;
    }

    private static void setInningsPitchedFromGameLog(Document gameLogDocument, MlbPlayer player) {
        // this gets all pitching logs from games where this player appeared for the provided team
        List<List<Node>> gameLogRows = gameLogDocument.getElementById("pitching_gamelogs").getElementsByTag("tbody").first()
                .childNodes().stream().filter(it -> it.hasAttr("id")).toList()
                .stream().filter(
                        row -> row.childNodes().stream()
                                .anyMatch(column ->
                                                column.hasAttr("data-stat")
                                                && "team_ID".equals(column.attributes().get("data-stat"))
                                                && player.getTeam().getAbbreviation().equals(column.firstChild().firstChild().toString())
                                )
                )
                .map(row ->
                        row.childNodes().stream().filter(node ->
                                        node.hasAttr("data-stat")
                                        && (
                                                "player_game_span".equals(node.attributes().get("data-stat"))
                                                || "IP".equals(node.attributes().get("data-stat"))
                                        )
                        ).collect(Collectors.toList())
                ).toList();

        for (List<Node> gameLogRow : gameLogRows) {
            Node gameSpanNode = gameLogRow.stream().filter(row -> "player_game_span".equals(row.attributes().get("data-stat"))).findFirst().orElse(null);
            Node inningsPitchedNode = gameLogRow.stream().filter(row -> "IP".equals(row.attributes().get("data-stat"))).findFirst().orElse(null);

            boolean starter = playerStartedGame(gameSpanNode);
            if (player.getPosition().equals(ROTATION)) {
                player.updateInningsPitched(Double.parseDouble(inningsPitchedNode.firstChild().toString()), starter);
            } else {
                player.updateInningsPitched(Double.parseDouble(inningsPitchedNode.firstChild().toString()), !starter);
            }
        }
    }

    private static void setPlateAppearancesFromGameLog(Document gameLogDocument, MlbPlayer player) {
        List<Node> gamesPlayedForTeam = gameLogDocument.getElementById("batting_gamelogs").getElementsByTag("tbody").first()
                .childNodes().stream().filter(it -> it.hasAttr("id")).toList()
                .stream().filter(
                        row -> row.childNodes().stream()
                                .anyMatch(column ->
                                        column.hasAttr("data-stat")
                                                && "team_ID".equals(column.attributes().get("data-stat"))
                                                && player.getTeam().getAbbreviation().equals(column.firstChild().firstChild().toString())
                                )
                ).toList();


        for (Node gameLogRow : gamesPlayedForTeam) {
            int plateAppearances = Integer.parseInt(gameLogRow.childNodes().stream().filter(column -> column.hasAttr("data-stat") && "PA".equals(column.attributes().get("data-stat"))).findFirst().orElse(null).firstChild().toString());
            boolean relevant = Arrays.stream(gameLogRow.childNodes().stream().filter(column -> column.hasAttr("data-stat") && "pos_game".equals(column.attributes().get("data-stat"))).findFirst().orElse(null).firstChild().toString().split(" ")).anyMatch(position -> BaseballReferencePosition.fromString(position).isPositionGroup(player.getPosition()));

            player.updatePlateAppearances(plateAppearances, relevant);
        }
    }

    private static boolean playerStartedGame(Node gameSpanNode) {
        String gameSpan = gameSpanNode.firstChild().toString();
        return gameSpan.contains("GS")
                || gameSpan.equals("CG")
                || gameSpan.equals("SHO");
    }

    private static String stripAdditionalHtmlFromStat(String stat) {
        return stat
                .replaceAll("<strong>", "")
                .replaceAll("</strong>", "")
                .replaceAll("<em>", "")
                .replaceAll("</em>", "");
    }
}
