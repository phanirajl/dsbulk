/*
 * Copyright (C) 2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dsbulk.engine.internal.metrics;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.UniformReservoir;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Statement;
import com.datastax.dsbulk.connectors.api.Record;
import com.datastax.dsbulk.engine.WorkflowType;
import com.datastax.dsbulk.engine.internal.record.UnmappableRecord;
import com.datastax.dsbulk.engine.internal.statement.UnmappableStatement;
import com.datastax.dsbulk.executor.api.listener.MetricsCollectingExecutionListener;
import com.datastax.dsbulk.executor.api.listener.MetricsReportingExecutionListener;
import java.time.Duration;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import reactor.core.publisher.Flux;

/** */
public class MetricsManager implements AutoCloseable {

  private final MetricRegistry registry = new MetricRegistry();
  private final MetricsCollectingExecutionListener listener =
      new MetricsCollectingExecutionListener(registry);

  private final WorkflowType workflowType;
  private final String executionId;
  private final ScheduledExecutorService scheduler;
  private final TimeUnit rateUnit;
  private final TimeUnit durationUnit;
  private final long expectedWrites;
  private final long expectedReads;
  private final boolean jmx;
  private final Duration reportInterval;

  private Meter records;
  private Meter failedRecords;
  private Meter successfulRecords;
  private Meter mappings;
  private Meter failedMappings;
  private Meter successfulMappings;
  private Histogram batchSize;
  private RecordReporter resultMappingsReporter;
  private MappingReporter recordMappingsReporter;
  private BatchReporter batchesReporter;
  private MetricsReportingExecutionListener writesReporter;
  private MetricsReportingExecutionListener readsReporter;
  private JmxReporter jmxReporter;

  public MetricsManager(
      WorkflowType workflowType,
      String executionId,
      ScheduledExecutorService scheduler,
      TimeUnit rateUnit,
      TimeUnit durationUnit,
      long expectedWrites,
      long expectedReads,
      boolean jmx,
      Duration reportInterval) {
    this.workflowType = workflowType;
    this.executionId = executionId;
    this.scheduler = scheduler;
    this.rateUnit = rateUnit;
    this.durationUnit = durationUnit;
    this.expectedWrites = expectedWrites;
    this.expectedReads = expectedReads;
    this.jmx = jmx;
    this.reportInterval = reportInterval;
  }

  public void init() {
    records = registry.meter("records/total");
    successfulRecords = registry.meter("records/successful");
    failedRecords = registry.meter("records/failed");
    mappings = registry.meter("mappings/total");
    failedMappings = registry.meter("mappings/failed");
    successfulMappings = registry.meter("mappings/successful");
    batchSize = registry.histogram("batches/size", () -> new Histogram(new UniformReservoir()));
    if (jmx) {
      startJMXReporter();
    }
    switch (workflowType) {
      case LOAD:
        startRecordMappingsReporter();
        startBatchesReporter();
        startWritesReporter();
        break;

      case UNLOAD:
        startReadsReporter();
        startResultMappingsReporter();
        break;
    }
  }

  private void startJMXReporter() {
    jmxReporter =
        JmxReporter.forRegistry(registry)
            .convertDurationsTo(durationUnit)
            .convertRatesTo(rateUnit)
            .createsObjectNamesWith(
                (type, domain, name) -> {
                  try {
                    StringBuilder sb =
                        new StringBuilder("com.datastax.dsbulk:0=").append(executionId).append(',');
                    StringTokenizer tokenizer = new StringTokenizer(name, "/");
                    int i = 1;
                    while (tokenizer.hasMoreTokens()) {
                      String token = tokenizer.nextToken();
                      if (tokenizer.hasMoreTokens()) {
                        sb.append(i++).append('=').append(token).append(',');
                      } else {
                        sb.append("name=").append(token);
                      }
                    }
                    return new ObjectName(sb.toString());
                  } catch (MalformedObjectNameException e) {
                    throw new RuntimeException(e);
                  }
                })
            .build();
    jmxReporter.start();
  }

  private void startResultMappingsReporter() {
    resultMappingsReporter = new RecordReporter(registry, rateUnit, scheduler, expectedWrites);
    resultMappingsReporter.start(reportInterval.getSeconds(), SECONDS);
  }

  private void startRecordMappingsReporter() {
    recordMappingsReporter = new MappingReporter(registry, rateUnit, scheduler, expectedWrites);
    recordMappingsReporter.start(reportInterval.getSeconds(), SECONDS);
  }

  private void startBatchesReporter() {
    batchesReporter = new BatchReporter(registry, scheduler);
    batchesReporter.start(reportInterval.getSeconds(), SECONDS);
  }

  private void startWritesReporter() {
    MetricsReportingExecutionListener.Builder builder =
        MetricsReportingExecutionListener.builder()
            .reportingWrites()
            .withScheduler(scheduler)
            .convertRatesTo(rateUnit)
            .convertDurationsTo(durationUnit)
            .extractingMetricsFrom(listener);
    if (expectedWrites > 0) {
      builder.expectingTotalEvents(expectedWrites);
    }
    writesReporter = builder.build();
    writesReporter.start(reportInterval.getSeconds(), SECONDS);
  }

  private void startReadsReporter() {
    MetricsReportingExecutionListener.Builder builder =
        MetricsReportingExecutionListener.builder()
            .reportingReads()
            .withScheduler(scheduler)
            .convertRatesTo(rateUnit)
            .convertDurationsTo(durationUnit)
            .extractingMetricsFrom(listener);
    if (expectedReads > 0) {
      builder.expectingTotalEvents(expectedWrites);
    }
    readsReporter = builder.build();
    readsReporter.start(reportInterval.getSeconds(), SECONDS);
  }

  @Override
  public void close() throws Exception {
    closeReporters();
    reportFinalMetrics();
    scheduler.shutdown();
    scheduler.awaitTermination(1, MINUTES);
    scheduler.shutdownNow();
  }

  private void closeReporters() {
    if (jmxReporter != null) {
      jmxReporter.close();
    }
    if (resultMappingsReporter != null) {
      resultMappingsReporter.close();
    }
    if (recordMappingsReporter != null) {
      recordMappingsReporter.close();
    }
    if (batchesReporter != null) {
      batchesReporter.close();
    }
    if (writesReporter != null) {
      writesReporter.close();
    }
    if (readsReporter != null) {
      readsReporter.close();
    }
  }

  private void reportFinalMetrics() {
    if (resultMappingsReporter != null) {
      resultMappingsReporter.report();
    }
    if (recordMappingsReporter != null) {
      recordMappingsReporter.report();
    }
    if (batchesReporter != null) {
      batchesReporter.report();
    }
    if (writesReporter != null) {
      writesReporter.report();
    }
    if (readsReporter != null) {
      readsReporter.report();
    }
  }

  public Function<Flux<Record>, Flux<Record>> newResultMapperMonitor() {
    return upstream ->
        upstream.doOnNext(
            r -> {
              records.mark();
              if (r instanceof UnmappableRecord) {
                failedRecords.mark();
              } else {
                successfulRecords.mark();
              }
            });
  }

  public Function<Flux<Statement>, Flux<Statement>> newRecordMapperMonitor() {
    return upstream ->
        upstream.doOnNext(
            stmt -> {
              mappings.mark();
              if (stmt instanceof UnmappableStatement) {
                failedMappings.mark();
              } else {
                successfulMappings.mark();
              }
            });
  }

  public Function<Flux<Statement>, Flux<Statement>> newBatcherMonitor() {
    return upstream ->
        upstream.doOnNext(
            stmt -> {
              if (stmt instanceof BatchStatement) {
                batchSize.update(((BatchStatement) stmt).size());
              } else {
                batchSize.update(1);
              }
            });
  }

  public MetricsCollectingExecutionListener getExecutionListener() {
    return listener;
  }
}