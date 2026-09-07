package com.esmaeil.jforex;

import com.dukascopy.api.*;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public final class TickExporter implements IStrategy {

    private final ExportConfig config;

    private IHistory history;
    private IConsole console;

    public TickExporter(ExportConfig config) {
        this.config = config;
    }

    @Override
    public void onStart(IContext context) throws JFException {
        history = context.getHistory();
        console = context.getConsole();

        Path output =
                Paths.get(config.outputFile);

        try {
            Path parent = output.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            export(output);

        } catch (Exception e) {
            console.getErr().println(
                    "EXPORT ERROR: " + e
            );
            e.printStackTrace();

        } finally {
            context.stop();
        }
    }

    private void export(Path output)
            throws Exception {

        SimpleDateFormat fmt =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss.SSS");

        fmt.setTimeZone(
                TimeZone.getTimeZone("GMT"));

        long from =
                config.from.toEpochMilli();

        long to =
                config.to.toEpochMilli();

        long chunkMs =
                config.chunkMillis();

        long cursor = from;
        long lastTickTime = -1;
        long total = 0;

        System.out.println(
                "Writing: " +
                output.toAbsolutePath()
        );

        try (BufferedWriter out =
                     Files.newBufferedWriter(
                             output,
                             StandardCharsets.UTF_8)) {

            out.write(
                    "GmtTime,Bid,Ask,BidVolume,AskVolume"
            );
            out.newLine();

            while (cursor < to) {

                long chunkEnd =
                        Math.min(
                                cursor + chunkMs,
                                to
                        );

                System.out.printf(
                        "Requesting %s -> %s%n",
                        fmt.format(
                                new Date(cursor)),
                        fmt.format(
                                new Date(chunkEnd))
                );

                List<ITick> ticks =
                        history.getTicks(
                                config.instrument,
                                cursor,
                                chunkEnd
                        );

                long chunkWritten = 0;

                for (ITick tick : ticks) {

                    long time =
                            tick.getTime();

                    if (time <= lastTickTime) {
                        continue;
                    }

                    lastTickTime = time;

                    out.write(
                            fmt.format(
                                    new Date(time))
                    );
                    out.write(',');
                    out.write(
                            Double.toString(
                                    tick.getBid())
                    );
                    out.write(',');
                    out.write(
                            Double.toString(
                                    tick.getAsk())
                    );
                    out.write(',');
                    out.write(
                            Double.toString(
                                    tick.getBidVolume())
                    );
                    out.write(',');
                    out.write(
                            Double.toString(
                                    tick.getAskVolume())
                    );
                    out.newLine();

                    total++;
                    chunkWritten++;
                }

                out.flush();

                System.out.printf(
                        "chunk ticks=%d, written=%d, total=%d%n",
                        ticks.size(),
                        chunkWritten,
                        total
                );

                cursor = chunkEnd;

                if (config.pauseMs > 0) {
                    Thread.sleep(config.pauseMs);
                }
            }
        }

        System.out.println(
                "FINISHED. total ticks written = " +
                total
        );
    }

    @Override
    public void onTick(
            Instrument instrument,
            ITick tick)
            throws JFException {}

    @Override
    public void onBar(
            Instrument instrument,
            Period period,
            IBar askBar,
            IBar bidBar)
            throws JFException {}

    @Override
    public void onMessage(
            IMessage message)
            throws JFException {}

    @Override
    public void onAccount(
            IAccount account)
            throws JFException {}

    @Override
    public void onStop()
            throws JFException {

        System.out.println(
                "TickExporter onStop()"
        );
    }
}
