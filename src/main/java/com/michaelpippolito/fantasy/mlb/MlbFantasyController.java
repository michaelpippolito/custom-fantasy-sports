package com.michaelpippolito.fantasy.mlb;

import com.michaelpippolito.fantasy.mlb.repository.MlbPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MlbFantasyController {
    @Autowired
    private MlbPlayerRepository mlbPlayerRepository;

    @GetMapping("/mlb/all-stats")
    public ResponseEntity getAllTeamStats() {
        return ResponseEntity.ok(mlbPlayerRepository.totalStatsByTeam());
    }
}
