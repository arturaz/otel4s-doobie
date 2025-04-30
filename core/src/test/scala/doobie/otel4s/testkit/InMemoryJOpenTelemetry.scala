package doobie.otel4s.testkit

import cats.effect.IO
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.`export`.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor

case class InMemoryJOpenTelemetry(
    otel: OpenTelemetry,
    logs: InMemoryLogRecordExporter,
    metrics: InMemoryMetricReader,
    traces: InMemorySpanExporter
)

object InMemoryJOpenTelemetry {

  def forIO: Resource[IO, InMemoryJOpenTelemetry] =
    forF[IO]

  def forF[F[_]: Async]: Resource[F, InMemoryJOpenTelemetry] = {
    val logs = InMemoryLogRecordExporter.create()
    val metrics = InMemoryMetricReader.create()
    val traces = InMemorySpanExporter.create()

    Resource
      .make(
        Async[F].catchNonFatal(
          OpenTelemetrySdk
            .builder()
            .setLoggerProvider(
              SdkLoggerProvider
                .builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(logs))
                .build()
            )
            .setMeterProvider(
              SdkMeterProvider
                .builder()
                .registerMetricReader(metrics)
                .build()
            )
            .setTracerProvider(
              SdkTracerProvider
                .builder()
                .addSpanProcessor(SimpleSpanProcessor.create(traces))
                .build()
            )
            .setPropagators(
              ContextPropagators.create(W3CTraceContextPropagator.getInstance())
            )
            .build()
        )
      )(otel => Async[F].delay(otel.shutdown()))
      .map(otel => InMemoryJOpenTelemetry(otel, logs, metrics, traces))
  }

}
