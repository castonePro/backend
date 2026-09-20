package com.capstone.travelbusan.global.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LocalTime과 Oracle VARCHAR2 컬럼 간의 변환기.
 * Oracle DB에는 표준 ANSI TIME 타입이 없어 시간 정보가 VARCHAR2로 저장됩니다.
 * 
 * [주의사항 & 레거시 데이터 대응]
 * 과거 JPA 매핑이 LocalTime 기본 설정일 때 저장된 데이터는,
 * Oracle 드라이버가 java.sql.Time(1970-01-01 기준)을 Oracle NLS_DATE_FORMAT('DD-MON-RR')에 따라
 * 문자열로 변환하면서 '01-JAN-70'과 같이 날짜만 저장된 경우가 있습니다.
 * 이 컨버터는 다음을 지원합니다:
 * 1) "10:00:00", "09:00", "9:00" 등의 순수 시간 포맷
 * 2) "01-JAN-70 10:30:00", "1970-01-01 09:00:00" 등 날짜+시간 혼합 포맷에서의 시간 추출
 * 3) '01-JAN-70'처럼 시간 정보가 유실된 레거시 데이터는 09:00(기본 시작 시간)으로 안전하게 폴백
 */
@Slf4j
@Converter(autoApply = true)
public class LocalTimeStringConverter implements AttributeConverter<LocalTime, String> {

    private static final DateTimeFormatter FORMATTER_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    // 문자열 내 시간 부분 탐색: e.g. "10:00:00", "09:30", "10:00"
    private static final Pattern TIME_PORTION_PATTERN =
            Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");

    @Override
    public String convertToDatabaseColumn(LocalTime attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.format(FORMATTER_HH_MM_SS);
    }

    @Override
    public LocalTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String trimmed = dbData.trim();

        // 1. 순수 시간 형식 시도 (HH:mm:ss, HH:mm, H:m:s)
        try {
            return LocalTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }

        if (trimmed.length() == 5) {
            try {
                return LocalTime.parse(trimmed, FORMATTER_HH_MM);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:m[:s]"));
        } catch (DateTimeParseException ignored) {
        }

        // 2. 문자열 내에 시간(HH:mm 또는 HH:mm:ss)이 포함되어 있는지 정규식으로 추출
        // 예: "01-JAN-70 10:30:00", "1970-01-01 09:00:00", "2026-09-15T14:00:00"
        Matcher matcher = TIME_PORTION_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            int second = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60 && second >= 0 && second < 60) {
                // AM/PM 확인
                String upper = trimmed.toUpperCase(Locale.ROOT);
                if (upper.contains("PM") && hour < 12) {
                    hour += 12;
                } else if (upper.contains("AM") && hour == 12) {
                    hour = 0;
                }
                return LocalTime.of(hour, minute, second);
            }
        }

        // 3. Oracle DATE 포맷 ('01-JAN-70', '01-JAN-1970', '1970-01-01' 등 날짜만 있는 레거시 데이터)
        // 이전 저장 시 Oracle의 기본 NLS_DATE_FORMAT(DD-MON-RR)에 의해 시간 정보가 유실된 경우 안전하게 기본 시작 시간(09:00)으로 대체
        log.warn("DB의 시간 컬럼에서 날짜만 포함된 레거시 데이터가 감지되어 기본 시작 시간(09:00)으로 대체합니다: '{}'", trimmed);
        return LocalTime.of(9, 0);
    }
}
