package com.michaelpippolito.fantasy.mlb;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MLBTeam {
    ARI("Arizona Diamondbacks", "ARI"),
    ATL("Atlanta Braves", "ATL"),
    BAL("Baltimore Orioles", "BAL"),
    BOS("Boston Red Sox", "BOS"),
    CHC("Chicago Cubs", "CHC"),
    CHW("Chicago White Sox", "CHW"),
    CIN("Cincinnati Reds", "CIN"),
    CLE("Cleveland Guardians", "CLE"),
    COL("Colorado Rockies", "COL"),
    DET("Detroit Tigers", "DET"),
//    FLA("Miami Marlins", "FLA"),
    HOU("Houston Astros", "HOU"),
    KAN("Kansas City Royals", "KAN"),
    LAA("Los Angeles Angels", "LAA"),
    LAD("Los Angeles Dodgers", "LAD"),
    MIA("Miami Marlins", "MIA"),
    MIL("Milwaukee Brewers" ,"MIL"),
    MIN("Minnesota Twins", "MIN"),
    NYM("New York Mets", "NYM"),
    NYY("New York Yankees", "NYY"),
    OAK("Oakland Athletics", "OAK"),
    PHI("Philadelphia Phillies", "PHI"),
    PIT("Pittsburgh Pirates", "PIT"),
    SDP("San Diego Padres", "SDP"),
    SFG("San Francisco Giants", "SFG"),
    SEA("Seattle Mariners", "SEA"),
    STL("St. Louis Cardinals", "STL"),
    TBR("Tampa Bay Rays", "TBR"),
    TEX("Texas Rangers", "TEX"),
    TOR("Toronto Blue Jays", "TOR"),
    WSN("Washington Nationals", "WSN");

    private final String name;
    private final String abbreviation;
}
