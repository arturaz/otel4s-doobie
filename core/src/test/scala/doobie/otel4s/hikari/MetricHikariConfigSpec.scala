package doobie.otel4s.hikari

import com.zaxxer.hikari.HikariConfig
import io.opentelemetry.api.OpenTelemetry
import munit.FunSuite

class MetricHikariConfigSpec extends FunSuite {

  test("should wrap with an open telemetry metrics tracker") {
    val config =
      MetricHikariConfig.fromConfig(OpenTelemetry.noop(), new HikariConfig())

    val metrics = config.getMetricsTrackerFactory
    assertEquals(
      metrics.getClass.getName,
      "io.opentelemetry.instrumentation.hikaricp.v3_0.OpenTelemetryMetricsTrackerFactory"
    )

  }

}
