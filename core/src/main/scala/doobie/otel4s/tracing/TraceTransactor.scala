package doobie.otel4s.tracing

import doobie.util.transactor.Transactor
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
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
      transactor: Transactor.Aux[M, DataSource]
  ): Transactor.Aux[M, DataSource] =
    transactor.copy(
      kernel0 = JdbcTelemetry.create(otel).wrap(transactor.kernel)
    )

  /** Wraps an existing Connection Transactor with open telemetry tracing
    */
  def fromConnection[M[_]](
      otel: OpenTelemetry,
      transactor: Transactor.Aux[M, Connection]
  ): Transactor.Aux[M, Connection] =
    transactor.copy(
      kernel0 = OpenTelemetryConnection.create(
        transactor.kernel,
        JdbcUtils.extractDbInfo(transactor.kernel),
        JdbcInstrumenterFactory.createStatementInstrumenter(otel, true, true)
      )
    )

}
