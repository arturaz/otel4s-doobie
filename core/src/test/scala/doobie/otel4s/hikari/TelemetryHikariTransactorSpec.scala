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
import org.typelevel.otel4s.oteljava.testkit.context.IOLocalTestContextStorage

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.CollectionConverters.ListHasAsScala

class TelemetryHikariTransactorSpec extends CatsEffectSuite {

  test("should init a HikariDataSource with metrics and spans") {
    implicit val provider: LocalProvider[IO, Context] =
      IOLocalTestContextStorage.localProvider[IO]

    val config = new HikariConfig()
    config.setPoolName("TelemetryHikariTransactorSpec")
    config.setJdbcUrl("jdbc:h2:mem:test_database")

    (for {
      jotel <- InMemoryJOpenTelemetry.forIO
      otel <- OtelJava.fromJOpenTelemetry[IO](jotel.otel).toResource
      xa <- TelemetryHikariTransactor.fromHikariConfig[IO](jotel.otel, config)
    } yield (jotel, otel, xa))
      .use { case (jotel, otel, xa) =>
        otel.tracerProvider
          .get("otel4s-doobie")
          .flatMap {
            _.rootSpan("ROOT_SPAN").use { root =>
              sql"""SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES"""
                .query[String]
                .to[Vector]
                .transact(xa)
                .as(root.context.traceIdHex)
            }
          }
          .flatMap(traceId =>
            for {
              spans <- IO(jotel.traces.getFinishedSpanItems().asScala.toVector)
              metrics <- IO(jotel.metrics.collectAllMetrics.asScala.toVector)
            } yield (traceId, spans, metrics)
          )
          .map { case (traceId, spans, metrics) =>
            // spans checks
            assertEquals(spans.size, 3)
            assertEquals(spans.map(_.getTraceId).distinct, Vector(traceId))

            List(
              "ROOT_SPAN",
              "HikariDataSource.getConnection",
              "SELECT INFORMATION_SCHEMA.TABLES"
            ).foreach(name => spans.exists(_.getName == name))

            // metrics checks
            List(
              "db.client.connections.max",
              "db.client.connections.wait_time",
              "db.client.connections.use_time",
              "db.client.connections.idle.min",
              "db.client.connections.pending_requests",
              "db.client.connections.usage"
            ).foreach(name => metrics.exists(_.getName == name))
          }
      }
  }

}
