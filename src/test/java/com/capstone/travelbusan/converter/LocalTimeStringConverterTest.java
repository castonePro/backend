package com.capstone.travelbusan.converter;

import com.capstone.travelbusan.global.converter.LocalTimeStringConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class LocalTimeStringConverterTest {

    private LocalTimeStringConverter converter;

    @BeforeEach
    void setUp() {
        converter = new LocalTimeStringConverter();
    }

    @Test
    @DisplayName("LocalTime을 DB 컬럼(VARCHAR2) 문자열로 변환할 때 HH:mm:ss 포맷으로 변환된다")
    void convertToDatabaseColumn_Success() {
        LocalTime time = LocalTime.of(9, 30, 0);
        String result = converter.convertToDatabaseColumn(time);
        assertThat(result).isEqualTo("09:30:00");
    }

    @Test
    @DisplayName("LocalTime이 null이면 DB 컬럼 값도 null을 반환한다")
    void convertToDatabaseColumn_Null() {
        String result = converter.convertToDatabaseColumn(null);
        assertThat(result).isNull();
    }

    @ParameterizedTest(name = "dbData: {0} -> LocalTime: {1}")
    @CsvSource({
            "'09:00', 09:00:00",
            "'09:00:00', 09:00:00",
            "'14:30:45', 14:30:45",
            "'9:00', 09:00:00",
            "' 10:15 ', 10:15:00",
            "'01-JAN-70 10:30:00', 10:30:00",
            "'1970-01-01 14:00:00', 14:00:00",
            "'2026-09-15T16:20:00', 16:20:00",
            "'01-JAN-70 02:30:00 PM', 14:30:00",
            "'01-JAN-70', 09:00:00",
            "'01-JAN-1970', 09:00:00",
            "'1970-01-01', 09:00:00"
    })
    @DisplayName("DB의 다양한 시간/날짜 포맷 및 레거시 데이터(01-JAN-70 등)를 올바르게 LocalTime으로 변환한다")
    void convertToEntityAttribute_VariousFormats(String dbData, String expectedTime) {
        LocalTime result = converter.convertToEntityAttribute(dbData);
        assertThat(result).isEqualTo(LocalTime.parse(expectedTime));
    }

    @Test
    @DisplayName("DB 데이터가 null이거나 빈 문자열이면 null을 반환한다")
    void convertToEntityAttribute_NullOrBlank() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }
}
