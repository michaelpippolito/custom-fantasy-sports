CREATE TABLE IF NOT EXISTS MLB_GAME (
    URL NVARCHAR2(140) PRIMARY KEY,
    AWAY_TEAM NVARCHAR2(140),
    HOME_TEAM NVARCHAR2(140),
    HTML BLOB
);

CREATE INDEX IDX_TEAM_NAMES
ON MLB_GAME (AWAY_TEAM, HOME_TEAM);

CREATE TABLE IF NOT EXISTS MLB_PLAYER (
    ID NVARCHAR2(140) PRIMARY KEY,
    NAME NVARCHAR2(140),
    TEAM NVARCHAR2(140),
    POSITION NVARCHAR2(140),
    WAR DECIMAL(10, 2),
    OVERVIEW_URL NVARCHAR2(140),
    SPLITS_URL NVARCHAR2(140),
    GAME_LOG_URL NVARCHAR2(140)
);

CREATE INDEX IDX_PLAYER_DETAILS
ON MLB_PLAYER (NAME, TEAM, POSITION);