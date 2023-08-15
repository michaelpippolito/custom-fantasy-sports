package com.michaelpippolito.fantasy.results;

import com.michaelpippolito.fantasy.FantasyPlayer;
import com.michaelpippolito.fantasy.mlb.MLBPlayer;
import com.michaelpippolito.fantasy.mlb.MLBPositionGroup;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultsWriter {
    static final DecimalFormat decimalFormat = new DecimalFormat("#.00");

    public static void writeResults(Map<FantasyPlayer, Collection<MLBPlayer>> results) {
        Workbook resultsWorkbook = new XSSFWorkbook();

        Sheet generalStandingsSheet = resultsWorkbook.createSheet("General_Standings");
        Row generalStandingsHeader = generalStandingsSheet.createRow(0);
        generalStandingsHeader.createCell(0).setCellValue("Name");
        generalStandingsHeader.createCell(1).setCellValue(MLBPositionGroup.ROTATION.getValue() + " WAR");
        generalStandingsHeader.createCell(2).setCellValue(MLBPositionGroup.INFIELD.getValue() + " WAR");
        generalStandingsHeader.createCell(3).setCellValue(MLBPositionGroup.OUTFIELD_DH.getValue() + " WAR");
        generalStandingsHeader.createCell(4).setCellValue(MLBPositionGroup.BULLPEN.getValue() + " WAR");
        generalStandingsHeader.createCell(5).setCellValue("Wildcard WAR");
        generalStandingsHeader.createCell(6).setCellValue("Total Score");

        int generalStandingsRow = 1;
        for (FantasyPlayer fantasyPlayer : results.keySet()) {
            Sheet fantasyPlayerSheet = resultsWorkbook.createSheet(fantasyPlayer.getName());

            Row positionsHeader = fantasyPlayerSheet.createRow(0);
            positionsHeader.createCell(0).setCellValue(MLBPositionGroup.ROTATION.getValue());
            positionsHeader.createCell(6).setCellValue(MLBPositionGroup.INFIELD.getValue());
            positionsHeader.createCell(12).setCellValue(MLBPositionGroup.OUTFIELD_DH.getValue());
            positionsHeader.createCell(18).setCellValue(MLBPositionGroup.BULLPEN.getValue());
            positionsHeader.createCell(24).setCellValue("Wildcard");

            Row statHeader = fantasyPlayerSheet.createRow(1);
            statHeader.createCell(0).setCellValue("Name");
            statHeader.createCell(1).setCellValue("Total IP");
            statHeader.createCell(2).setCellValue(MLBPositionGroup.ROTATION.getValue() + " IP");
            statHeader.createCell(3).setCellValue("Total WAR");
            statHeader.createCell(4).setCellValue(MLBPositionGroup.ROTATION.getValue() + " WAR");
            statHeader.createCell(6).setCellValue("Name");
            statHeader.createCell(7).setCellValue("Total PA");
            statHeader.createCell(8).setCellValue(MLBPositionGroup.INFIELD.getValue() + " PA");
            statHeader.createCell(9).setCellValue("Total WAR");
            statHeader.createCell(10).setCellValue(MLBPositionGroup.INFIELD.getValue() + " WAR");
            statHeader.createCell(12).setCellValue("Name");
            statHeader.createCell(13).setCellValue("Total PA");
            statHeader.createCell(14).setCellValue(MLBPositionGroup.OUTFIELD_DH.getValue() + " PA");
            statHeader.createCell(15).setCellValue("Total WAR");
            statHeader.createCell(16).setCellValue(MLBPositionGroup.OUTFIELD_DH.getValue() + " WAR");
            statHeader.createCell(18).setCellValue("Name");
            statHeader.createCell(19).setCellValue("Total IP");
            statHeader.createCell(20).setCellValue(MLBPositionGroup.BULLPEN + " IP");
            statHeader.createCell(21).setCellValue("Total WAR");
            statHeader.createCell(22).setCellValue(MLBPositionGroup.BULLPEN + " WAR");
            statHeader.createCell(24).setCellValue("Name");
            statHeader.createCell(25).setCellValue("WAR");

            double totalRotationWAR = 0.0;
            double totalInfieldWAR = 0.0;
            double totalOutfieldWAR = 0.0;
            double totalBullpenWAR = 0.0;

            int startRow = 2;
            for (MLBPlayer mlbPlayer : results.get(fantasyPlayer).stream().filter(mlbPlayer -> mlbPlayer.getPositionGroup().equals(MLBPositionGroup.ROTATION)).collect(Collectors.toList())) {
                Row playerRow;
                if (startRow <= fantasyPlayerSheet.getPhysicalNumberOfRows()-1) {
                    playerRow = fantasyPlayerSheet.getRow(startRow++);
                } else {
                    playerRow = fantasyPlayerSheet.createRow(startRow++);
                }
                playerRow.createCell(0).setCellValue(mlbPlayer.getName());
                playerRow.createCell(1).setCellValue(mlbPlayer.getTotalInningsPitched());
                playerRow.createCell(2).setCellValue(mlbPlayer.getInningsPitched());
                playerRow.createCell(3).setCellValue(mlbPlayer.getTotalWAR());

                String earnedWAR = decimalFormat.format(mlbPlayer.getAdjustedWAR());
                playerRow.createCell(4).setCellValue(earnedWAR);
                totalRotationWAR += Double.parseDouble(earnedWAR);
            }

            startRow = 2;
            for (MLBPlayer mlbPlayer : results.get(fantasyPlayer).stream().filter(mlbPlayer -> mlbPlayer.getPositionGroup().equals(MLBPositionGroup.INFIELD)).collect(Collectors.toList())) {
                Row playerRow;
                if (startRow <= fantasyPlayerSheet.getPhysicalNumberOfRows()-1) {
                    playerRow = fantasyPlayerSheet.getRow(startRow++);
                } else {
                    playerRow = fantasyPlayerSheet.createRow(startRow++);
                }
                playerRow.createCell(6).setCellValue(mlbPlayer.getName());
                playerRow.createCell(7).setCellValue(mlbPlayer.getTotalPlateAppearances());
                playerRow.createCell(8).setCellValue(mlbPlayer.getPlateAppearances());
                playerRow.createCell(9).setCellValue(mlbPlayer.getTotalWAR());

                String earnedWAR = decimalFormat.format(mlbPlayer.getAdjustedWAR());
                playerRow.createCell(10).setCellValue(earnedWAR);
                totalInfieldWAR += Double.parseDouble(earnedWAR);
            }

            startRow = 2;
            for (MLBPlayer mlbPlayer : results.get(fantasyPlayer).stream().filter(mlbPlayer -> mlbPlayer.getPositionGroup().equals(MLBPositionGroup.OUTFIELD_DH)).collect(Collectors.toList())) {
                Row playerRow;
                if (startRow <= fantasyPlayerSheet.getPhysicalNumberOfRows()-1) {
                    playerRow = fantasyPlayerSheet.getRow(startRow++);
                } else {
                    playerRow = fantasyPlayerSheet.createRow(startRow++);
                }
                playerRow.createCell(12).setCellValue(mlbPlayer.getName());
                playerRow.createCell(13).setCellValue(mlbPlayer.getTotalPlateAppearances());
                playerRow.createCell(14).setCellValue(mlbPlayer.getPlateAppearances());
                playerRow.createCell(15).setCellValue(mlbPlayer.getTotalWAR());

                String earnedWAR = decimalFormat.format(mlbPlayer.getAdjustedWAR());
                playerRow.createCell(16).setCellValue(earnedWAR);
                totalOutfieldWAR += Double.parseDouble(earnedWAR);
            }

            startRow = 2;
            for (MLBPlayer mlbPlayer : results.get(fantasyPlayer).stream().filter(mlbPlayer -> mlbPlayer.getPositionGroup().equals(MLBPositionGroup.BULLPEN)).collect(Collectors.toList())) {
                Row playerRow;
                if (startRow <= fantasyPlayerSheet.getPhysicalNumberOfRows()-1) {
                    playerRow = fantasyPlayerSheet.getRow(startRow++);
                } else {
                    playerRow = fantasyPlayerSheet.createRow(startRow++);
                }
                playerRow.createCell(18).setCellValue(mlbPlayer.getName());
                playerRow.createCell(19).setCellValue(mlbPlayer.getTotalInningsPitched());
                playerRow.createCell(20).setCellValue(mlbPlayer.getInningsPitched());
                playerRow.createCell(21).setCellValue(mlbPlayer.getTotalWAR());

                String earnedWAR = decimalFormat.format(mlbPlayer.getAdjustedWAR());
                playerRow.createCell(22).setCellValue(earnedWAR);
                totalBullpenWAR += Double.parseDouble(earnedWAR);
            }

            Row playerRow;
            if (fantasyPlayerSheet.getPhysicalNumberOfRows() >= 3) {
                playerRow = fantasyPlayerSheet.getRow(2);
            } else {
                playerRow = fantasyPlayerSheet.createRow(2);
            }

            playerRow.createCell(24).setCellValue(fantasyPlayer.getWildcardPick().getName());
            playerRow.createCell(25).setCellValue(fantasyPlayer.getWildcardPick().getTotalWAR());

            Row playerGeneralStandingsRow = generalStandingsSheet.createRow(generalStandingsRow++);
            playerGeneralStandingsRow.createCell(0).setCellValue(fantasyPlayer.getName());
            playerGeneralStandingsRow.createCell(1).setCellValue(totalRotationWAR);
            playerGeneralStandingsRow.createCell(2).setCellValue(totalInfieldWAR);
            playerGeneralStandingsRow.createCell(3).setCellValue(totalOutfieldWAR);
            playerGeneralStandingsRow.createCell(4).setCellValue(totalBullpenWAR);
            playerGeneralStandingsRow.createCell(5).setCellValue(fantasyPlayer.getWildcardPick().getTotalWAR());
            playerGeneralStandingsRow.createCell(6).setCellValue(totalRotationWAR + totalInfieldWAR + totalOutfieldWAR + totalBullpenWAR + fantasyPlayer.getWildcardPick().getTotalWAR());
        }

        try (FileOutputStream outputStream = new FileOutputStream("results.xlsx")) {
            resultsWorkbook.write(outputStream);
            System.out.println("Workbook saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the workbook
            try {
                resultsWorkbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
