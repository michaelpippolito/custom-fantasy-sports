package com.michaelpippolito.fantasy.mlb.repository;

import com.michaelpippolito.fantasy.mlb.def.MlbPositionGroup;
import com.michaelpippolito.fantasy.mlb.def.MlbTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MlbPlayerRepository extends JpaRepository<MlbPlayer, UUID> {
    MlbPlayer findByNameAndTeamAndPosition(String name, MlbTeam team, MlbPositionGroup position);
    @Query(value = """
            SELECT TEAM, POSITION, SUM(WAR) AS WAR
            FROM MLB_PLAYER
            GROUP BY TEAM, POSITION
            ORDER BY POSITION ASC, WAR DESC
            """, nativeQuery = true)
    List<Object[]> totalStatsByTeam();
}
