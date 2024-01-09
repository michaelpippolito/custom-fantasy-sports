package com.michaelpippolito.fantasy.mlb.repository;

import com.michaelpippolito.fantasy.mlb.def.MlbPositionGroup;
import com.michaelpippolito.fantasy.mlb.def.MlbTeam;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "MLB_PLAYER",
        indexes = {@Index(name = "idx_player_details", columnList = "NAME, TEAM, POSITION")}
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlbPlayer {
    @Id
    @Column(name = "ID", columnDefinition = "NVARCHAR2(140)")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "NAME", columnDefinition = "NVARCHAR2(140)")
    private String name;

    @Column(name = "TEAM", columnDefinition = "NVARCHAR(140)")
    @Convert(converter = MlbTeam.MlbTeamConverter.class)
    private MlbTeam team;

    @Column(name = "POSITION", columnDefinition = "NVARCHAR(140)")
    @Convert(converter = MlbPositionGroup.MlbPositionGroupConverter.class)
    private MlbPositionGroup position;

    @Column(name = "WAR", precision = 2)
    @Setter
    private Double war;

    @Column(name = "OVERVIEW_URL", columnDefinition = "NVARCHAR2(140)")
    private String overviewUrl;

    @Column(name = "SPLITS_URL", columnDefinition = "NVARCHAR2(140)")
    private String splitsUrl;

    @Column(name = "GAME_LOG_URL", columnDefinition = "NVARCHAR2(140)")
    private String gameLogUrl;

    @Transient
    private double totalInningsPitched;

    @Transient
    private double relevantInningsPitched;

    @Transient
    private int totalPlateAppearances;

    @Transient
    private int relevantPlateAppearances;

    /*
        For the Innings Pitched stat in baseball, the decimal value represents the number of outs out of 3 achieved in
        the next inning. For example, 6.2 Innings Pitched means that the pitcher pitched 6 innings and the first 2
        outs of the 7th inning. Therefore, 6.3 Innings Pitched == 7 Innings Pitched. We must account for this
        when summing the total innings of any pitcher.
     */
    public void updateInningsPitched(double inningsPitched, boolean relevant) {
        String inningsPitchedString = String.format("%.1f", this.totalInningsPitched + inningsPitched);

        double wholeInningsPitched = Double.parseDouble(inningsPitchedString.substring(0, inningsPitchedString.indexOf(".")));
        double decimalInningsPitched = Double.parseDouble(inningsPitchedString.substring(inningsPitchedString.indexOf(".")));

        while (decimalInningsPitched >= 0.3) {
            decimalInningsPitched -= 0.3;
            wholeInningsPitched += 1;
        }

        this.totalInningsPitched = wholeInningsPitched + decimalInningsPitched;

        if (relevant) {
            String relevantInningsPitchedString = String.format("%.1f", this.relevantInningsPitched + inningsPitched);

            wholeInningsPitched = Double.parseDouble(relevantInningsPitchedString.substring(0, relevantInningsPitchedString.indexOf(".")));
            decimalInningsPitched = Double.parseDouble(relevantInningsPitchedString.substring(relevantInningsPitchedString.indexOf(".")));

            while (decimalInningsPitched >= 0.3) {
                decimalInningsPitched -= 0.3;
                wholeInningsPitched += 1;
            }
            this.relevantInningsPitched = wholeInningsPitched + decimalInningsPitched;
        }
    }

    public void updatePlateAppearances(int plateAppearances, boolean relevant) {
        this.totalPlateAppearances += plateAppearances;
        if (relevant) { this.relevantPlateAppearances += plateAppearances; }
    }

    public void setAdjustedWar(double totalWar) {
        if (this.position.isPitcher()) {
            if (this.relevantInningsPitched == this.totalInningsPitched) {
                this.war = totalWar;
            } else {
                this.war = (this.relevantInningsPitched / this.totalInningsPitched) * totalWar;
            }
        } else {
            if (this.relevantInningsPitched == this.totalPlateAppearances) {
                this.war = totalWar;
            } else {
                this.war = ((double) this.relevantPlateAppearances / this.totalPlateAppearances) * totalWar;
            }
        }
    }
}