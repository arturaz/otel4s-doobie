package doobie.otel4s.tracing

import cats.effect.IO
import doobie.Transactor
import doobie.implicits.toSqlInterpolator
import doobie.otel4s.testkit.InMemoryJOpenTelemetry
import doobie.syntax.connectionio.toConnectionIOOps
import doobie.util.ExecutionContexts
import munit.CatsEffectSuite
import org.h2.jdbcx.JdbcDataSource
import org.typelevel.otel4s.context.LocalProvider
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.oteljava.context.IOLocalContextStorage

import javax.sql.DataSource
import scala.jdk.CollectionConverters.CollectionHasAsScala

class TraceTransactorSuite extends CatsEffectSuite {

  test("should wrap with an open telemetry data source") {
    InMemoryJOpenTelemetry.forIO.use { otel =>
      IO {
        val xa = Transactor.fromDataSource[IO](
          dataSource = new JdbcDataSource(): DataSource,
          connectEC = ExecutionContexts.synchronous
        )

        val wrap = TraceTransactor.fromDataSource(otel.otel, xa)
        assertEquals(
          wrap.kernel.getClass.getName,
          "io.opentelemetry.instrumentation.jdbc.datasource.OpenTelemetryDataSource"
        )
      }
    }
  }

  test("should wrap with an open telemetry connection") {
    InMemoryJOpenTelemetry.forIO.use { otel =>
      IO {
        val conn = {
          val ds = new JdbcDataSource()
          ds.setURL("jdbc:h2:mem:test_database")
          val conn = ds.getConnection
          conn.close()
          conn
        }

        val xa =
          Transactor.fromConnection[IO](connection = conn, logHandler = None)

        val wrap = TraceTransactor.fromConnection(otel.otel, xa)
        assertEquals(
          wrap.kernel.getClass.getName,
          "io.opentelemetry.instrumentation.jdbc.internal.OpenTelemetryConnection$OpenTelemetryConnectionJdbc43"
        )
      }
    }
  }

  // ignored:
  // https://github.com/typelevel/otel4s/issues/959
  test("should trace from Scala to Java".ignore) {
    implicit val provider: LocalProvider[IO, Context] =
      IOLocalContextStorage.localProvider[IO]

    (for {
      jotel <- InMemoryJOpenTelemetry.forF[IO]
      otel <- OtelJava.fromJOpenTelemetry[IO](jotel.otel).toResource
      tracer <- otel.tracerProvider.get("io.swan.app").toResource
    } yield (jotel, tracer)).use { case (otel, tracer) =>
      val ds: DataSource = {
        val ds = new JdbcDataSource()
        ds.setURL("jdbc:h2:mem:test_database")
        ds
      }

      val xa = TraceTransactor.fromDataSource(
        otel = otel.otel,
        transactor = Transactor.fromDataSource[IO](
          dataSource = ds,
          connectEC = ExecutionContexts.synchronous
        )
      )

      tracer
        .rootSpan("ROOT_SPAN")
        .use { root =>
          sql"""SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES"""
            .query[String]
            .to[Vector]
            .transact(xa)
            .as(root)
        }
        .map { root =>
          val spans = otel.traces.getFinishedSpanItems.asScala.toVector
          assertEquals(spans.size, 3)
          assertEquals(
            spans.map(_.getTraceId).distinct,
            Vector(root.context.traceIdHex)
          )
          assert(spans.exists(_.getName == "ROOT_SPAN"))
          assert(spans.exists(_.getName == "JdbcDataSource.getConnection"))
          assert(spans.exists(_.getName == "SELECT INFORMATION_SCHEMA.TABLES"))
        }
    }
  }

}
