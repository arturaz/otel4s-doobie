package doobie.otel4s.hikari

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.otel4s.tracing.TraceTransactor
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import io.opentelemetry.api.OpenTelemetry

import javax.sql.DataSource

object TelemetryHikariTransactor {

  /** Creates a Transactor from a HikariConfig with tracing and metrics
    */
  def fromHikariConfig[M[_]: Async](
      otel: OpenTelemetry,
      config: HikariConfig,
      logHandler: Option[LogHandler[M]] = None
  ): Resource[M, Aux[M, DataSource]] =
    fromHikariConfig(
      otel = otel,
      config = config,
      logHandler = logHandler,
      statementInstrumenterEnabled = true,
      statementSanitizationEnabled = true,
      captureQueryParameters = false,
      transactionInstrumenterEnabled = false
    )

  /** Creates a Transactor from a HikariConfig with tracing and metrics
    */
  def fromHikariConfig[M[_]: Async](
      otel: OpenTelemetry,
      config: HikariConfig,
      logHandler: Option[LogHandler[M]],
      statementInstrumenterEnabled: Boolean,
      statementSanitizationEnabled: Boolean,
      captureQueryParameters: Boolean,
      transactionInstrumenterEnabled: Boolean
  ): Resource[M, Aux[M, DataSource]] =
    HikariTransactor
      .fromHikariConfig(MetricHikariConfig.fromConfig(otel, config), logHandler)
      // not super safe but works as long as Doobie isn't using Hikari specific
      // methods on the connect method, which is the case as of this commit
      .map(_.asInstanceOf[Transactor.Aux[M, DataSource]])
      .map(transactor =>
        TraceTransactor.fromDataSource(
          otel = otel,
          transactor = transactor,
          statementInstrumenterEnabled = statementInstrumenterEnabled,
          statementSanitizationEnabled = statementSanitizationEnabled,
          captureQueryParameters = captureQueryParameters,
          transactionInstrumenterEnabled = transactionInstrumenterEnabled
        )
      )

}
