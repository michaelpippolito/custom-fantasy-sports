package com.michaelpippolito.fantasy.mlb.repository;

import com.michaelpippolito.fantasy.mlb.def.MlbTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MlbGameRepository extends JpaRepository<MlbGame, String> {
    int countByAwayTeamOrHomeTeam(MlbTeam awayTeam, MlbTeam homeTeam);
    List<MlbGame> findByAwayTeamOrHomeTeam(MlbTeam awayTeam, MlbTeam homeTeam);
}