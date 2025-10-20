package doobie.otel4s.tracing

import doobie.util.transactor.Transactor
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
import io.opentelemetry.instrumentation.jdbc.datasource.internal.Experimental
import io.opentelemetry.instrumentation.jdbc.internal.JdbcInstrumenterFactory
import io.opentelemetry.instrumentation.jdbc.internal.JdbcUtils
import io.opentelemetry.instrumentation.jdbc.internal.OpenTelemetryConnection

import java.sql.Connection
import javax.sql.DataSource

object TraceTransactor {

  /** Wraps an existing DataSource Transactor with open telemetry tracing
    */
  def fromDataSource[M[_]](
      otel: OpenTelemetry,
      transactor: Transactor.Aux[M, DataSource],
      statementInstrumenterEnabled: Boolean = true,
      statementSanitizationEnabled: Boolean = true,
      captureQueryParameters: Boolean = false,
      transactionInstrumenterEnabled: Boolean = false,
      sqlCommenterEnabled: Boolean = false
  ): Transactor.Aux[M, DataSource] =
    transactor.copy(
      kernel0 = {
        val builder = JdbcTelemetry
          .builder(otel)
          .setDataSourceInstrumenterEnabled(true)
          .setStatementInstrumenterEnabled(statementInstrumenterEnabled)
          .setStatementSanitizationEnabled(statementSanitizationEnabled)
          .setCaptureQueryParameters(captureQueryParameters)
          .setTransactionInstrumenterEnabled(transactionInstrumenterEnabled)
        Experimental.setEnableSqlCommenter(builder, sqlCommenterEnabled)
        builder.build()
      }
        .wrap(transactor.kernel)
    )

  /** Wraps an existing Connection Transactor with open telemetry tracing
    */
  def fromConnection[M[_]](
      otel: OpenTelemetry,
      transactor: Transactor.Aux[M, Connection],
      statementInstrumenterEnabled: Boolean = true,
      statementSanitizationEnabled: Boolean = true,
      captureQueryParameters: Boolean = false,
      transactionInstrumenterEnabled: Boolean = false,
      sqlCommenterEnabled: Boolean = false
  ): Transactor.Aux[M, Connection] =
    transactor.copy(
      kernel0 = OpenTelemetryConnection.create(
        transactor.kernel,
        JdbcUtils.extractDbInfo(transactor.kernel),
        JdbcInstrumenterFactory.createStatementInstrumenter(
          otel,
          statementInstrumenterEnabled,
          statementSanitizationEnabled,
          captureQueryParameters
        ),
        JdbcInstrumenterFactory.createTransactionInstrumenter(
          otel,
          transactionInstrumenterEnabled
        ),
        captureQueryParameters,
        sqlCommenterEnabled
      )
    )

}
