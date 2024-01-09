package com.michaelpippolito.fantasy.mlb.repository;

import com.michaelpippolito.fantasy.mlb.def.MlbTeam;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "MLB_GAME",
        indexes = {@Index(name = "idx_team_names", columnList = "HOME_TEAM, AWAY_TEAM")})
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class MlbGame {
    @Id
    @Column(name = "URL", columnDefinition = "NVARCHAR2(140)")
    private String url;

    @Column(name = "AWAY_TEAM", columnDefinition = "NVARCHAR(140)")
    @Convert(converter = MlbTeam.MlbTeamConverter.class)
    private MlbTeam awayTeam;

    @Column(name = "HOME_TEAM", columnDefinition = "NVARCHAR(140)")
    @Convert(converter = MlbTeam.MlbTeamConverter.class)
    private MlbTeam homeTeam;

    @Column(name = "HTML", columnDefinition = "BLOB")
    @Lob
    private byte[] html;
}
