package doobie.otel4s.hikari

import com.zaxxer.hikari.HikariConfig
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.hikaricp.v3_0.HikariTelemetry

object MetricHikariConfig {

  /** Modifies a HikariConfig with open telemetry metrics
    */
  def fromConfig(otel: OpenTelemetry, config: HikariConfig): HikariConfig = {
    config.setMetricRegistry(null);
    val telemetry = HikariTelemetry.create(otel)

    Option(config.getMetricsTrackerFactory) match {
      case Some(value) =>
        config.setMetricsTrackerFactory(
          telemetry.createMetricsTrackerFactory(value)
        )
      case None =>
        config.setMetricsTrackerFactory(telemetry.createMetricsTrackerFactory())
    }

    config
  }

}
