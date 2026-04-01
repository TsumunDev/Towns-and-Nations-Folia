package org.leralix.tan.monitoring;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.leralix.tan.TownsAndNations;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PERFORMANCE MONITORING: Database metrics for 1000+ player servers.
 *
 * Tracks:
 * - Active connections vs pool size
 * - Idle connections
 * - Waiting threads (queue size)
 * - Query execution times
 * - Connection latency
 *
 * Use /tan admin dbstats to view metrics in-game.
 */
public class DatabaseMetrics {

    private static volatile DatabaseMetrics instance;
    private final HikariDataSource dataSource;
    private HikariPoolMXBean poolProxy;
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong slowQueries = new AtomicLong(0);
    private final AtomicLong totalQueryTimeMs = new AtomicLong(0);
    private final ScheduledExecutorService scheduler;

    private static final long SLOW_QUERY_THRESHOLD_MS = 100;

    private DatabaseMetrics(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TaN-DB-Metrics");
            t.setDaemon(true);
            return t;
        });

        // Register MXBean proxy
        try {
            ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + dataSource.getPoolName() + ")");
            poolProxy = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
                    poolName, HikariPoolMXBean.class);
        } catch (MalformedObjectNameException e) {
            TownsAndNations.getPlugin().getLogger().warning("Could not register HikariMXBean: " + e.getMessage());
        }

        // Log metrics every 5 minutes
        scheduler.scheduleAtFixedRate(this::logMetrics, 5, 5, TimeUnit.MINUTES);
    }

    public static void initialize(HikariDataSource dataSource) {
        if (instance == null) {
            synchronized (DatabaseMetrics.class) {
                if (instance == null) {
                    instance = new DatabaseMetrics(dataSource);
                }
            }
        }
    }

    public static DatabaseMetrics getInstance() {
        return instance;
    }

    /**
     * Record a query execution.
     * Call this after each query with execution time.
     */
    public static void recordQuery(long executionTimeMs) {
        DatabaseMetrics metrics = getInstance();
        if (metrics != null) {
            metrics.totalQueries.incrementAndGet();
            metrics.totalQueryTimeMs.addAndGet(executionTimeMs);
            if (executionTimeMs > SLOW_QUERY_THRESHOLD_MS) {
                metrics.slowQueries.incrementAndGet();
            }
        }
    }

    /**
     * Get current pool statistics as a formatted string.
     */
    public String getStats() {
        if (poolProxy == null) {
            return "Pool metrics not available";
        }

        int activeConnections = poolProxy.getActiveConnections();
        int totalConnections = poolProxy.getTotalConnections();
        int idleConnections = poolProxy.getIdleConnections();
        int threadsAwaitingConnection = poolProxy.getThreadsAwaitingConnection();
        int maxPoolSize = dataSource.getMaximumPoolSize();

        double avgQueryTime = totalQueries.get() > 0
            ? (double) totalQueryTimeMs.get() / totalQueries.get()
            : 0;
        double slowQueryRate = totalQueries.get() > 0
            ? (double) slowQueries.get() / totalQueries.get() * 100
            : 0;

        return String.format(
            """
            ══════════════════════════════════════════════════════
            📊 DATABASE POOL METRICS
            ══════════════════════════════════════════════════════
            Active Connections:  %d / %d (%.1f%%)
            Idle Connections:    %d
            Waiting Threads:     %d
            ══════════════════════════════════════════════════════
            Total Queries:       %d
            Slow Queries (>%dms): %d (%.2f%%)
            Avg Query Time:      %.2fms
            ══════════════════════════════════════════════════════
            """,
            activeConnections, maxPoolSize, (activeConnections * 100.0 / maxPoolSize),
            idleConnections,
            threadsAwaitingConnection,
            totalQueries.get(), SLOW_QUERY_THRESHOLD_MS, slowQueries.get(), slowQueryRate,
            avgQueryTime
        );
    }

    /**
     * Get health status for monitoring/alerting.
     */
    public HealthStatus getHealthStatus() {
        if (poolProxy == null) {
            return HealthStatus.UNKNOWN;
        }

        int activeConnections = poolProxy.getActiveConnections();
        int maxPoolSize = dataSource.getMaximumPoolSize();
        int threadsAwaitingConnection = poolProxy.getThreadsAwaitingConnection();
        double utilization = (double) activeConnections / maxPoolSize;

        if (threadsAwaitingConnection > 10 || utilization > 0.9) {
            return HealthStatus.CRITICAL;
        } else if (threadsAwaitingConnection > 5 || utilization > 0.75) {
            return HealthStatus.WARNING;
        } else {
            return HealthStatus.HEALTHY;
        }
    }

    public int getActiveConnections() {
        return poolProxy != null ? poolProxy.getActiveConnections() : -1;
    }

    public int getWaitingThreads() {
        return poolProxy != null ? poolProxy.getThreadsAwaitingConnection() : -1;
    }

    private void logMetrics() {
        if (poolProxy == null) {
            return;
        }

        int activeConnections = poolProxy.getActiveConnections();
        int maxPoolSize = dataSource.getMaximumPoolSize();
        int threadsAwaitingConnection = poolProxy.getThreadsAwaitingConnection();
        HealthStatus status = getHealthStatus();

        String logLevel = switch (status) {
            case CRITICAL -> "SEVERE";
            case WARNING -> "WARNING";
            case HEALTHY -> "INFO";
            case UNKNOWN -> "FINE";
        };

        TownsAndNations.getPlugin().getLogger()
            .log(java.util.logging.Level.parse(logLevel),
                "[TaN-DB-Metrics] " + getStats().replace("\n", " | "));
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public enum HealthStatus {
        HEALTHY,     // < 75% utilization, < 5 waiting threads
        WARNING,     // 75-90% utilization or 5-10 waiting threads
        CRITICAL,    // > 90% utilization or > 10 waiting threads
        UNKNOWN      // Metrics not available
    }
}
