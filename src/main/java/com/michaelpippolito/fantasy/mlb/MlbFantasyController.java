package com.michaelpippolito.fantasy.mlb;

import com.michaelpippolito.fantasy.mlb.def.MlbPositionGroup;
import com.michaelpippolito.fantasy.mlb.def.MlbTeam;
import com.michaelpippolito.fantasy.mlb.repository.MlbPlayer;
import com.michaelpippolito.fantasy.mlb.repository.MlbPlayerRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@RestController
public class MlbFantasyController {
    @Autowired
    private MlbPlayerRepository mlbPlayerRepository;

    @GetMapping("/mlb/all-stats")
    public ResponseEntity getAllTeamStats() {
        return ResponseEntity.ok(mlbPlayerRepository.totalStatsByTeam());
    }

    @GetMapping("/mlb/new-game")
    public void getGameResults(@RequestBody NewGameRequest request, HttpServletResponse response) throws IOException {
        Workbook resultsWorkbook = new XSSFWorkbook();

        CellStyle genericHeaderStyle = resultsWorkbook.createCellStyle();
        Font genericHeaderFont = resultsWorkbook.createFont();
        genericHeaderFont.setColor(IndexedColors.BLUE_GREY.getIndex());
        genericHeaderFont.setBold(true);
        genericHeaderFont.setFontHeightInPoints((short) 15);
        genericHeaderStyle.setFont(genericHeaderFont);

        CellStyle blueTableHeaderStyle = resultsWorkbook.createCellStyle();
        blueTableHeaderStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        blueTableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        blueTableHeaderStyle.setBottomBorderColor(IndexedColors.ROYAL_BLUE.getIndex());
        blueTableHeaderStyle.setBorderBottom(BorderStyle.THIN);
        Font blueTableHeaderFont = resultsWorkbook.createFont();
        blueTableHeaderFont.setBold(true);
        blueTableHeaderFont.setColor(IndexedColors.WHITE.getIndex());
        blueTableHeaderStyle.setFont(blueTableHeaderFont);

        CellStyle blueTableFilledRowStyle = resultsWorkbook.createCellStyle();
        blueTableFilledRowStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        blueTableFilledRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        blueTableFilledRowStyle.setBottomBorderColor(IndexedColors.ROYAL_BLUE.getIndex());
        blueTableFilledRowStyle.setBorderBottom(BorderStyle.THIN);
        blueTableFilledRowStyle.setBorderLeft(BorderStyle.NONE);
        blueTableFilledRowStyle.setBorderRight(BorderStyle.NONE);

        CellStyle blueTableWhiteRowStyle = resultsWorkbook.createCellStyle();
        blueTableWhiteRowStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        blueTableWhiteRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        blueTableWhiteRowStyle.setBottomBorderColor(IndexedColors.ROYAL_BLUE.getIndex());
        blueTableWhiteRowStyle.setBorderBottom(BorderStyle.THIN);
        blueTableWhiteRowStyle.setBorderLeft(BorderStyle.NONE);
        blueTableWhiteRowStyle.setBorderRight(BorderStyle.NONE);

        Sheet generalStandingsSheet = resultsWorkbook.createSheet("General Standings");
        Row headerRow = generalStandingsSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Rotation WAR");
        headerRow.createCell(2).setCellValue("Infield WAR");
        headerRow.createCell(3).setCellValue("Outfield/DH WAR");
        headerRow.createCell(4).setCellValue("Bullpen WAR");
        headerRow.createCell(5).setCellValue("Wildcard WAR");
        headerRow.createCell(6).setCellValue("Total Score");
        applyCellStyle(headerRow, blueTableHeaderStyle);

        int currentRow = 1;
        for (Player player : request.getPlayers()) {
            MlbTeam rotation = MlbTeam.fromName(player.getRotation());
            MlbTeam infield = MlbTeam.fromName(player.getInfield());
            MlbTeam outfield = MlbTeam.fromName(player.getOutfield());
            MlbTeam bullpen = MlbTeam.fromName(player.getBullpen());

            double rotationWar = mlbPlayerRepository.calculateTotalWarForTeamAndPosition(rotation, MlbPositionGroup.ROTATION);
            double infieldWar = mlbPlayerRepository.calculateTotalWarForTeamAndPosition(infield, MlbPositionGroup.INFIELD);
            double outfieldWar = mlbPlayerRepository.calculateTotalWarForTeamAndPosition(outfield, MlbPositionGroup.OUTFIELD_DH);
            double bullpenWar = mlbPlayerRepository.calculateTotalWarForTeamAndPosition(bullpen, MlbPositionGroup.BULLPEN);
            double wildcardWar = StringUtils.isNotEmpty(player.getWildcard()) ? mlbPlayerRepository.calculateTotalWarForPlayer(player.getWildcard()) : 0.0;
            Row playerRow = generalStandingsSheet.createRow(currentRow);
            playerRow.createCell(0).setCellValue(player.getName());
            playerRow.createCell(1).setCellValue(rotationWar);
            playerRow.createCell(2).setCellValue(infieldWar);
            playerRow.createCell(3).setCellValue(outfieldWar);
            playerRow.createCell(4).setCellValue(bullpenWar);
            playerRow.createCell(5).setCellValue(wildcardWar);
            playerRow.createCell(6).setCellValue(rotationWar + infieldWar + outfieldWar + bullpenWar + wildcardWar);
            applyCellStyle(playerRow, currentRow % 2 == 0 ? blueTableWhiteRowStyle : blueTableFilledRowStyle);
            currentRow++;

            Sheet playerDetailSheet = resultsWorkbook.createSheet(player.getName());
            Row playerHeaderRow = playerDetailSheet.createRow(0);
            playerHeaderRow.createCell(0).setCellValue("Rotation");
            playerHeaderRow.createCell(3).setCellValue("Infield");
            playerHeaderRow.createCell(6).setCellValue("Outfield/DH");
            playerHeaderRow.createCell(9).setCellValue("Bullpen");
            playerHeaderRow.createCell(12).setCellValue("Wildcard");
            applyCellStyle(playerHeaderRow, genericHeaderStyle);

            Row columnHeaderRow = playerDetailSheet.createRow(1);
            columnHeaderRow.createCell(0).setCellValue("Name");
            columnHeaderRow.createCell(1).setCellValue("WAR");
            columnHeaderRow.createCell(3).setCellValue("Name");
            columnHeaderRow.createCell(4).setCellValue("WAR");
            columnHeaderRow.createCell(6).setCellValue("Name");
            columnHeaderRow.createCell(7).setCellValue("WAR");
            columnHeaderRow.createCell(9).setCellValue("Name");
            columnHeaderRow.createCell(10).setCellValue("WAR");
            columnHeaderRow.createCell(12).setCellValue("Name");
            columnHeaderRow.createCell(13).setCellValue("WAR");
            applyCellStyle(columnHeaderRow, blueTableHeaderStyle);

            List<MlbPlayer> rotationDraftedPlayers = mlbPlayerRepository.findByTeamAndPosition(rotation, MlbPositionGroup.ROTATION);
            List<MlbPlayer> infieldDraftedPlayers = mlbPlayerRepository.findByTeamAndPosition(infield, MlbPositionGroup.INFIELD);
            List<MlbPlayer> outfieldDraftedPlayers = mlbPlayerRepository.findByTeamAndPosition(outfield, MlbPositionGroup.OUTFIELD_DH);
            List<MlbPlayer> bullpenDraftedPlayers = mlbPlayerRepository.findByTeamAndPosition(bullpen, MlbPositionGroup.BULLPEN);
            int numDraftedPlayers = Collections.max(List.of(rotationDraftedPlayers.size(), infieldDraftedPlayers.size(), outfieldDraftedPlayers.size(), bullpenDraftedPlayers.size()));

            int draftedPlayerIndex = 0;
            for (int rowNum = 2; rowNum <= numDraftedPlayers + 1; rowNum++) {
                Row draftedPlayerRow = playerDetailSheet.getRow(rowNum) == null ?
                        playerDetailSheet.createRow(rowNum) : playerDetailSheet.getRow(rowNum);

                if (draftedPlayerIndex < rotationDraftedPlayers.size()) {
                    draftedPlayerRow.createCell(0).setCellValue(rotationDraftedPlayers.get(draftedPlayerIndex).getName());
                    draftedPlayerRow.createCell(1).setCellValue(rotationDraftedPlayers.get(draftedPlayerIndex).getWar());
                }
                if (draftedPlayerIndex < infieldDraftedPlayers.size()) {
                    draftedPlayerRow.createCell(3).setCellValue(infieldDraftedPlayers.get(draftedPlayerIndex).getName());
                    draftedPlayerRow.createCell(4).setCellValue(infieldDraftedPlayers.get(draftedPlayerIndex).getWar());
                }
                if (draftedPlayerIndex < outfieldDraftedPlayers.size()) {
                    draftedPlayerRow.createCell(6).setCellValue(outfieldDraftedPlayers.get(draftedPlayerIndex).getName());
                    draftedPlayerRow.createCell(7).setCellValue(outfieldDraftedPlayers.get(draftedPlayerIndex).getWar());
                }
                if (draftedPlayerIndex < bullpenDraftedPlayers.size()) {
                    draftedPlayerRow.createCell(9).setCellValue(bullpenDraftedPlayers.get(draftedPlayerIndex).getName());
                    draftedPlayerRow.createCell(10).setCellValue(bullpenDraftedPlayers.get(draftedPlayerIndex).getWar());
                }
                applyCellStyle(draftedPlayerRow, rowNum % 2 == 0 ? blueTableFilledRowStyle : blueTableWhiteRowStyle);

                draftedPlayerIndex++;
            }

            playerDetailSheet.getRow(2).createCell(12).setCellValue(player.getWildcard());
            playerDetailSheet.getRow(2).createCell(13).setCellValue(wildcardWar);
            playerDetailSheet.getRow(2).getCell(12).setCellStyle(blueTableFilledRowStyle);
            playerDetailSheet.getRow(2).getCell(13).setCellStyle(blueTableFilledRowStyle);

            playerDetailSheet.autoSizeColumn(0);
            playerDetailSheet.autoSizeColumn(3);
            playerDetailSheet.autoSizeColumn(6);
            playerDetailSheet.autoSizeColumn(9);
            playerDetailSheet.autoSizeColumn(12);
        }

        generalStandingsSheet.autoSizeColumn(0);
        generalStandingsSheet.autoSizeColumn(1);
        generalStandingsSheet.autoSizeColumn(2);
        generalStandingsSheet.autoSizeColumn(3);
        generalStandingsSheet.autoSizeColumn(4);
        generalStandingsSheet.autoSizeColumn(5);
        generalStandingsSheet.autoSizeColumn(6);

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=results" + Instant.now().toEpochMilli() + ".xlsx");
        resultsWorkbook.write(response.getOutputStream());
        resultsWorkbook.close();
    }

    private void applyCellStyle(Row row, CellStyle style) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                cell.setCellStyle(style);
            }
        }
    }

    @Data
    public static class NewGameRequest {
        private List<Player> players;
    }

    @Data
    public static class Player {
        private String name;
        private String rotation;
        private String infield;
        private String outfield;
        private String bullpen;
        private String wildcard;
    }
}
