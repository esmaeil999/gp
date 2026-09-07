package com.esmaeil.jforex;

import com.dukascopy.api.Instrument;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public final class ExportConfig {

    public final Instrument instrument;
    public final Instant from;
    public final Instant to;
    public final long chunkHours;
    public final long pauseMs;
    public final String outputFile;
    public final long maxRuntimeMinutes;

    private ExportConfig(
            Instrument instrument,
            Instant from,
            Instant to,
            long chunkHours,
            long pauseMs,
            String outputFile,
            long maxRuntimeMinutes) {

        this.instrument = instrument;
        this.from = from;
        this.to = to;
        this.chunkHours = chunkHours;
        this.pauseMs = pauseMs;
        this.outputFile = outputFile;
        this.maxRuntimeMinutes = maxRuntimeMinutes;
    }

    public static ExportConfig fromEnvironment() {
        String instrumentName = env("INSTRUMENT", "EURUSD").trim().toUpperCase();
        Instrument instrument;

        try {
            instrument = Instrument.valueOf(instrumentName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown Dukascopy Instrument: " + instrumentName, e);
        }

        Instant from = parseGmt("FROM", "2010-01-01T00:00:00Z");
        Instant to = parseGmt("TO", "2020-01-01T00:00:00Z");

        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("FROM must be earlier than TO.");
        }

        long chunkHours = positiveLong("CHUNK_HOURS", 6);
        long pauseMs = nonNegativeLong("PAUSE_MS", 500);
        long maxRuntimeMinutes = positiveLong("MAX_RUNTIME_MINUTES", 350);

        String outputFile = env(
                "OUTPUT_FILE",
                "output/" + instrumentName + "_ticks.csv"
        );

        return new ExportConfig(
                instrument,
                from,
                to,
                chunkHours,
                pauseMs,
                outputFile,
                maxRuntimeMinutes
        );
    }

    public long chunkMillis() {
        return Duration.ofHours(chunkHours).toMillis();
    }

    private static Instant parseGmt(String name, String defaultValue) {
        String value = env(name, defaultValue).trim();

        try {
            // Recommended format: 2020-01-01T00:00:00Z
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    name + " must use ISO-8601 UTC format, e.g. 2020-01-01T00:00:00Z",
                    e);
        }
    }

    private static long positiveLong(String name, long defaultValue) {
        long value = Long.parseLong(env(name, Long.toString(defaultValue)));
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be > 0.");
        }
        return value;
    }

    private static long nonNegativeLong(String name, long defaultValue) {
        long value = Long.parseLong(env(name, Long.toString(defaultValue)));
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0.");
        }
        return value;
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
