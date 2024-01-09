package com.michaelpippolito.fantasy.mlb.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Result {
    private String team;
    private String position;
    private double totalWar;
}
