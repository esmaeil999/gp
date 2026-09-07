package com.esmaeil.jforex;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Main {

    private static final String DEMO_JNLP =
            "http://platform.dukascopy.com/demo_3/jforex_3.jnlp";

    private Main() {}

    public static void main(String[] args) throws Exception {
        String username = requiredEnv("JFOREX_USERNAME");
        String password = requiredEnv("JFOREX_PASSWORD");

        ExportConfig config = ExportConfig.fromEnvironment();

        System.out.println("========================================");
        System.out.println("JForex Tick Exporter 1.0.1");
        System.out.println("Instrument : " + config.instrument);
        System.out.println("From (GMT) : " + config.from);
        System.out.println("To   (GMT) : " + config.to);
        System.out.println("Chunk      : " + config.chunkHours + " hour(s)");
        System.out.println("Output     : " + config.outputFile);
        System.out.println("========================================");

        final IClient client = ClientFactory.getDefaultInstance();
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicBoolean strategyStopped = new AtomicBoolean(false);

        client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {
                System.out.println("Strategy started. processId=" + processId);
            }

            @Override
            public void onStop(long processId) {
                System.out.println("Strategy stopped. processId=" + processId);
                strategyStopped.set(true);
                finished.countDown();
            }

            @Override
            public void onConnect() {
                System.out.println("Connected to Dukascopy.");
            }

            @Override
            public void onDisconnect() {
                System.out.println("Disconnected from Dukascopy.");
            }
        });

        System.out.println("Connecting to Dukascopy...");
        client.connect(DEMO_JNLP, username, password);

        waitForConnection(client, 180);

        Set<Instrument> instruments =
                Collections.singleton(config.instrument);

        System.out.println("Subscribing to " + config.instrument + "...");
        client.setSubscribedInstruments(instruments);

        Thread.sleep(5000);

        System.out.println("Starting exporter strategy...");
        client.startStrategy(new TickExporter(config));

        boolean completed = finished.await(
                config.maxRuntimeMinutes,
                TimeUnit.MINUTES
        );

        if (!completed) {
            System.err.println("ERROR: Maximum runtime exceeded.");
            try {
                client.disconnect();
            } catch (Exception ignored) {}
            System.exit(2);
        }

        try {
            client.disconnect();
        } catch (Exception ignored) {}

        if (!strategyStopped.get()) {
            System.exit(3);
        }

        System.out.println("Export completed successfully.");
    }

    private static void waitForConnection(
            IClient client,
            int timeoutSeconds) throws InterruptedException {

        long deadline =
                System.currentTimeMillis() + timeoutSeconds * 1000L;

        while (!client.isConnected()) {
            if (System.currentTimeMillis() >= deadline) {
                throw new IllegalStateException(
                        "Timed out while waiting for Dukascopy connection."
                );
            }

            Thread.sleep(1000);
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required environment variable is missing: " + name
            );
        }

        return value;
    }
}
