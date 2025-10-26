package doobie.otel4s.testkit

import cats.effect.IO
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
    for {
      logExporter <- IO.delay(InMemoryLogRecordExporter.create()).toResource
      metricReader <- IO.delay(InMemoryMetricReader.create()).toResource
      spanExporter <- IO.delay(InMemorySpanExporter.create()).toResource

      logProcessor <- Resource.fromAutoCloseable(
        IO.delay(SimpleLogRecordProcessor.create(logExporter))
      )
      spanProcessor <- Resource.fromAutoCloseable(
        IO.delay(SimpleSpanProcessor.create(spanExporter))
      )

      // not fromAutoCloseable because OpenTelemetrySdk.shutdown already does it
      loggerProvider <- Resource.eval(
        IO.delay(
          SdkLoggerProvider
            .builder()
            .addLogRecordProcessor(logProcessor)
            .build()
        )
      )
      meterProvider <- Resource.eval(
        IO.delay(
          SdkMeterProvider.builder().registerMetricReader(metricReader).build()
        )
      )
      tracerProvider <- Resource.eval(
        IO.delay(
          SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build()
        )
      )
      openTelemetry <- Resource.fromAutoCloseable(
        IO.delay(
          OpenTelemetrySdk
            .builder()
            .setLoggerProvider(loggerProvider)
            .setMeterProvider(meterProvider)
            .setTracerProvider(tracerProvider)
            .setPropagators(
              ContextPropagators.create(W3CTraceContextPropagator.getInstance())
            )
            .build()
        )
      )
    } yield InMemoryJOpenTelemetry(
      openTelemetry,
      logExporter,
      metricReader,
      spanExporter
    )

}
