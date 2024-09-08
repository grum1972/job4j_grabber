package ru.job4j.grabber.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;


public class HabrCareerDateTimeParserTest {
    @Test
    public void whenParse() {
        HabrCareerDateTimeParser parser = new HabrCareerDateTimeParser();
        String date = "2024-08-15T11:09:45+03:00";
        assertThat(parser.parse(date)).isEqualTo("2024-08-15T11:09:45");
    }
}