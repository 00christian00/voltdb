/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * This program exercises the Kafka import capability by inserting
 * <key, value> pairs into both a VoltDB table -- KAFKAMIRRORTABLE1, and
 * a export table -- KAFKAEXPORTTABLE1. The export table links to a
 * topic in a Kafka complex. The deployment file also add an import from
 * that same complex. The checking proceeds in parallel, checking
 * the rows in the KAFKAIMPORTTABLE1 with rows in the mirror table.
 * Matching rows are deleted from both tables. Separate threads check
 * statistics on both the export table and import table to determine when
 * both import and export activity have quiesced. At the end of a
 * successful run, both the import table and the mirror table are empty.
 * If there are rows left in the mirror table, then not all exported
 * rows have made the round trip back to the import table, or there might
 * be data corruption causing the match process to fail.
 */

package kafkaimporter.client.kafkaimporter;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.voltcore.logging.VoltLogger;
import org.voltdb.CLIConfig;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientStats;
import org.voltdb.client.ClientStatsContext;

import com.google_voltpatches.common.base.Splitter;
import com.google_voltpatches.common.net.HostAndPort;

public class KafkaImportBenchmark {

    static VoltLogger log = new VoltLogger("Benchmark");

    // handy, rather than typing this out several times
    static final String HORIZONTAL_RULE =
            "----------" + "----------" + "----------" + "----------" +
            "----------" + "----------" + "----------" + "----------";

    // Statistics manager objects from the client
    static ClientStatsContext periodicStatsContext;

    // validated command line configuration
    static Config config;
    // Timer for periodic stats printing
    static Timer statsTimer;
    static Timer checkTimer;
    // Benchmark start time
    long benchmarkStartTS;

    static final Map<HostAndPort, OutputStream> haplist = new HashMap<HostAndPort, OutputStream>();
    static Client client;
    // Some thread safe counters for reporting
    AtomicLong linesRead = new AtomicLong(0);
    // count of rows successfully exported
    static AtomicLong rowsAdded = new AtomicLong(0);
    // count of rows queued to export
    static final AtomicLong finalInsertCount = new AtomicLong(0);

    private static final int END_WAIT = 10; // wait at the end for import to settle after export completes

    static List<Integer> importProgress = new ArrayList<Integer>();

    static InsertExport exportProc;
    static TableChangeMonitor exportMon;
    static TableChangeMonitor importMon;
    static MatchChecks matchChecks;

    /**
     * Uses included {@link CLIConfig} class to
     * declaratively state command line options with defaults
     * and validation.
     */
    static class Config extends CLIConfig {
        @Option(desc = "Interval for performance feedback, in seconds.")
        long displayinterval = 5;

        @Option(desc = "Benchmark duration, in seconds.")
        int duration = 300;

        @Option(desc = "Maximum export TPS rate for benchmark.")
        int ratelimit = Integer.MAX_VALUE;

        @Option(desc = "Comma separated list of the form server[:port] to connect to for database queuries")
        String servers = "localhost";

        @Option(desc = "Number of rows to expect to import from the Kafka topic")
        long export_rows = 10_000_000;

        @Option(desc = "Report latency for kafka benchmark run.")
        boolean latencyreport = false;

        @Option(desc = "Test using all VoltDB datatypes (except varbin).")
        boolean allvalues = false;

        @Option(desc = "Filename to write raw summary statistics to.")
        String statsfile = "";

        @Override
        public void validate() {
            if (duration <= 0) exitWithMessageAndUsage("duration must be > 0");
            if (ratelimit <= 0) exitWithMessageAndUsage("ratelimit must be > 0");
            if (export_rows <= 0) exitWithMessageAndUsage("row number must be > 0");
            if (displayinterval <= 0) exitWithMessageAndUsage("displayinterval must be > 0");
            log.info("finished validating args");
        }
    }

    /**
     * Constructor for benchmark instance.
     * Configures VoltDB client and prints configuration.
     *
     * @param config Parsed & validated CLI options.
     */
    public KafkaImportBenchmark(Config config) {
        periodicStatsContext = client.createStatsContext();

        log.info(HORIZONTAL_RULE);
        log.info(" Command Line Configuration");
        log.info(HORIZONTAL_RULE);
        log.info(config.getConfigDumpString());
        if(config.latencyreport) {
            log.warn("Option latencyreport is ON for async run, please set a reasonable ratelimit.\n");
        }
    }

    /**
     * Connect to one or more VoltDB servers.
     *
     * @param servers A comma separated list of servers using the hostname:port
     * syntax (where :port is optional). Assumes 21212 if not specified otherwise.
     * @throws InterruptedException if anything bad happens with the threads.
     */
     static void dbconnect(String servers, int ratelimit) throws InterruptedException, Exception {
        final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

        log.info("Connecting to VoltDB Interface...");
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setMaxTransactionsPerSecond(ratelimit);
         clientConfig.setReconnectOnConnectionLoss(true);
        client = ClientFactory.createClient(clientConfig);

        for (String server: COMMA_SPLITTER.split(servers)) {
            log.info("..." + server);
            client.createConnection(server);
        }
    }

    /**
     * Create a Timer task to display performance data on the Vote procedure
     * It calls printStatistics() every displayInterval seconds
     */
    public static void schedulePeriodicStats() {
        statsTimer = new Timer("periodicStats", true);
        TimerTask statsPrinting = new TimerTask() {
            @Override
            public void run() { printStatistics(); }
        };
        statsTimer.scheduleAtFixedRate(statsPrinting,
          config.displayinterval * 1000,
          config.displayinterval * 1000);
    }

    /**
     * Prints a one line update on performance that can be printed
     * periodically during a benchmark.
     */
    public synchronized static void printStatistics() {
        try {
            ClientStats stats = periodicStatsContext.fetchAndResetBaseline().getStats();
            long thrup;

            thrup = stats.getTxnThroughput();
            long rows = MatchChecks.getExportRowCount(client);
            if (rows == VoltType.NULL_BIGINT)
                rows = 0;
            log.info(String.format("Export Throughput %d/s, Total Rows %d, Aborts/Failures %d/%d, Avg/95%% Latency %.2f/%.2fms",
                    thrup, rows, stats.getInvocationAborts(), stats.getInvocationErrors(),
                    stats.getAverageLatency(), stats.kPercentileLatencyAsDouble(0.95)));
        } catch (Exception e) {
            log.error("Exception in printStatistics", e);
        }
    }

    protected static void scheduleCheckTimer() {

        final Timer timer = new Timer("checkTimer", true);
        final long period = config.displayinterval;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long count = MatchChecks.getImportRowCount(client);
                if (count == VoltType.NULL_BIGINT)
                    count = 0;
                importProgress.add((int) count);
                //log.info(importProgress.toString());

                if (importProgress.size() > 1) {
                    log.info("Import Throughput " + (count - importProgress.get(importProgress.size() - 2)) / period + "/s, Total Rows: " + count);
                }
            }
        },
            config.displayinterval * 1000,
            config.displayinterval * 1000);
    }

    /**
     * Core benchmark code.
     * Connect. Initialize. Run the loop. Cleanup. Print Results.
     *
     * @throws Exception if anything unexpected happens.
     */
    public void runBenchmark() throws Exception {
        log.info(HORIZONTAL_RULE);
        log.info(" Setup & Initialization");
        log.info(HORIZONTAL_RULE);

        log.info(HORIZONTAL_RULE);
        log.info("Starting Benchmark");
        log.info(HORIZONTAL_RULE);

        long icnt = 0;
        try {
            // print periodic statistics to the console
//          benchmarkStartTS = System.currentTimeMillis();
            schedulePeriodicStats();
            scheduleCheckTimer();

            // Run the benchmark loop for the requested duration
            // The throughput may be throttled depending on client configuration
            // Save the key/value pairs so they can be verified through the database
            log.info("Running benchmark...");
            final long benchmarkEndTime = System.currentTimeMillis() + (1000l * config.duration);
            while (benchmarkEndTime > System.currentTimeMillis()) {
                long value = System.currentTimeMillis();
                long key = icnt;
                exportProc.insertExport(key, value);
                icnt++;
            }
            // check for export completion
            //exportMon.waitForStreamedAllocatedMemoryZero();
        } catch (Exception ex) {
            log.error("Exception in Benchmark", ex);
            ex.printStackTrace();
        } finally {
            log.info("Benchmark ended, exported " + icnt + " rows.");
            // cancel periodic stats printing
            statsTimer.cancel();
            finalInsertCount.addAndGet(icnt);
        }
    }

    public static class BenchmarkRunner extends Thread {
        private final KafkaImportBenchmark benchmark;

        public BenchmarkRunner(KafkaImportBenchmark bm) {
            benchmark = bm;
        }

        @Override
        public void run() {
            try {
                schedulePeriodicStats();
                scheduleCheckTimer();
                benchmark.runBenchmark();
            } catch (Exception ex) {
                log.error("Exception in benchmark", ex);
                ex.printStackTrace();
                System.exit(-1);
            }
        }
    }

    /**
     * Main routine creates a benchmark instance and kicks off the run method.
     *
     * @param args Command line arguments.
     * @throws Exception if anything goes wrong.
     */
    public static void main(String[] args) throws Exception {
        VoltLogger log = new VoltLogger("Benchmark.main");
        // create a configuration from the arguments
        Config config = new Config();
        config.parse(KafkaImportBenchmark.class.getName(), args);

        // connect to one or more servers, loop until success
        dbconnect(config.servers, config.ratelimit);

        // instance handles inserts to Kafka export table and its mirror DB table
        exportProc = new InsertExport(config.allvalues, client, rowsAdded);

        // get instances to track track export completion using @Statistics
        exportMon = new TableChangeMonitor(client, "StreamedTable", "KAFKAEXPORTTABLE1");
        importMon = new TableChangeMonitor(client, "PersistentTable", "KAFKAIMPORTTABLE1");

        log.info("Starting KafkaImportBenchmark...");
        KafkaImportBenchmark benchmark = new KafkaImportBenchmark(config);
        BenchmarkRunner runner = new BenchmarkRunner(benchmark);
        runner.start();
        runner.join(); // writers are done

        long exportRowCount = MatchChecks.getExportRowCount(client);
        log.info("Export phase complete, " + exportRowCount + " rows exported, waiting for import to drain...");

        // final check time since the import and export tables have quiesced.
        // check that the mirror table is empty. If not, that indicates that
        // not all the rows got to Kafka or not all the rows got imported back.
        do {
            Thread.sleep(END_WAIT * 1000);
            // importProgress is an array of sampled counts of the importedcounts table, showing importProgressress of import
            // samples are recorded by the checkTimer thread
        } while (importProgress.size() < 4 || importProgress.get(importProgress.size()-1) > importProgress.get(importProgress.size()-2) ||
                    importProgress.get(importProgress.size()-1) > importProgress.get(importProgress.size()-3) ||
                    importProgress.get(importProgress.size()-1) > importProgress.get(importProgress.size()-4) );

        long mirrorRows = MatchChecks.getMirrorTableRowCount(config.allvalues, client);
        long importRows = MatchChecks.getImportTableRowCount(config.allvalues, client);
        long importRowCount = MatchChecks.getImportRowCount(client);

        log.info("Total rows exported: " + finalInsertCount);
        log.info("Unmatched Rows remaining in the export Mirror Table: " + mirrorRows);
        log.info("Unmatched Rows received from Kafka to Import Table (duplicate rows): " + importRows);

        boolean testResult = true;
        if (mirrorRows != 0) {
            log.error(mirrorRows + " Rows are missing from the import stream, failing test");
            testResult = false;
        }

        if (importRowCount < exportRowCount) {
            log.error("Export count '" + exportRowCount + "' does not match import row count '" + importRowCount + "' test fails.");
            testResult = false;
        }

        client.drain();
        client.close();

        if (testResult == true) {
            log.info("Test passed!");
            System.exit(0);
        } else {
            log.info("Test failed!");
            System.exit(1);
        }
    }
}
