package doobie.otel4s.hikari

import cats.effect.IO
import com.zaxxer.hikari.HikariConfig
import doobie.implicits.toSqlInterpolator
import doobie.otel4s.testkit.InMemoryJOpenTelemetry
import doobie.syntax.connectionio.toConnectionIOOps
import munit.CatsEffectSuite
import org.typelevel.otel4s.context.LocalProvider
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.oteljava.context.IOLocalContextStorage

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.CollectionConverters.ListHasAsScala

class TelemetryHikariTransactorSpec extends CatsEffectSuite {

  // ignored:
  // https://github.com/typelevel/otel4s/issues/959
  test("should init a HikariDataSource with metrics and spans".ignore) {
    implicit val provider: LocalProvider[IO, Context] =
      IOLocalContextStorage.localProvider[IO]

    val config = new HikariConfig()
    config.setPoolName("TelemetryHikariTransactorSpec")
    config.setJdbcUrl("jdbc:h2:mem:test_database")

    (for {
      jotel <- InMemoryJOpenTelemetry.forF[IO]
      otel <- OtelJava.fromJOpenTelemetry[IO](jotel.otel).toResource
      tracer <- otel.tracerProvider.get("io.swan.app").toResource
      xa <- TelemetryHikariTransactor.fromHikariConfig[IO](jotel.otel, config)
    } yield (jotel, tracer, xa))
      .use { case (otel, tracer, xa) =>
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
            assert(spans.exists(_.getName == "HikariDataSource.getConnection"))
            assert(
              spans.exists(_.getName == "SELECT INFORMATION_SCHEMA.TABLES")
            )

            val metrics = otel.metrics.collectAllMetrics().asScala.toVector
            assert(metrics.exists(_.getName == "db.client.connections.max"))
            assert(
              metrics.exists(_.getName == "db.client.connections.wait_time")
            )
            assert(
              metrics.exists(_.getName == "db.client.connections.use_time")
            )
            assert(
              metrics.exists(_.getName == "db.client.connections.idle.min")
            )
            assert(
              metrics
                .exists(_.getName == "db.client.connections.pending_requests")
            )
            assert(metrics.exists(_.getName == "db.client.connections.usage"))

          }
      }
  }

}
