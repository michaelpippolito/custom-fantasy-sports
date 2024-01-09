package com.michaelpippolito.fantasy.mlb.def;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum MlbTeam {
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
    HOU("Houston Astros", "HOU"),
    KCR("Kansas City Royals", "KCR"),
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

    public static MlbTeam fromName(String name) {
        return Arrays.stream(MlbTeam.values()).filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    @Converter
    public static class MlbTeamConverter implements AttributeConverter<MlbTeam, String> {
        @Override
        public String convertToDatabaseColumn(MlbTeam mlbTeam) {
            return mlbTeam.getName();
        }

        @Override
        public MlbTeam convertToEntityAttribute(String databaseColumn) {
            return MlbTeam.fromName(databaseColumn);
        }
    }
}
