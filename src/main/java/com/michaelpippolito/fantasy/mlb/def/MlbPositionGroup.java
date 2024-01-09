package com.michaelpippolito.fantasy.mlb.def;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum MlbPositionGroup {
    ROTATION("Rotation"),
    BULLPEN("Bullpen"),
    INFIELD("Infield"),
    OUTFIELD_DH("Outfield/DH");

    private final String value;

    public static MlbPositionGroup fromValue(String value) {
        return Arrays.stream(MlbPositionGroup.values()).filter(it -> it.getValue().equals(value)).findFirst().orElse(null);
    }

    public boolean isPitcher() {
        return this.equals(ROTATION) || this.equals(BULLPEN);
    }

    @Converter
    public static class MlbPositionGroupConverter implements AttributeConverter<MlbPositionGroup, String> {
        @Override
        public String convertToDatabaseColumn(MlbPositionGroup mlbPositionGroup) {
            return mlbPositionGroup.getValue();
        }

        @Override
        public MlbPositionGroup convertToEntityAttribute(String databaseColumn) {
            return MlbPositionGroup.fromValue(databaseColumn);
        }
    }
}
