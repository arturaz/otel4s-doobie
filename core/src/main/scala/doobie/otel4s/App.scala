package doobie.otel4s

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.MonadCancelThrow
import com.zaxxer.hikari.HikariConfig
import doobie.Transactor
import doobie.implicits.toSqlInterpolator
import doobie.otel4s.hikari.TelemetryHikariTransactor
import doobie.syntax.connectionio.toConnectionIOOps
import org.typelevel.otel4s.context.LocalProvider
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.oteljava.context.IOLocalContextStorage
import org.typelevel.otel4s.trace.Tracer

object App extends IOApp.Simple {

  def program[F[_]: MonadCancelThrow: Tracer](xa: Transactor[F]): F[Int] = {
    Tracer[F]
      .span("program")
      .surround(sql"""SELECT 1""".query[Int].unique.transact(xa))
  }

  override def run: IO[Unit] = {
    // don't forget to add
    // javaOptions += "-Dcats.effect.trackFiberContext=true"
    implicit val provider: LocalProvider[IO, Context] =
      IOLocalContextStorage.localProvider[IO]

    (for {
      otel4s <- OtelJava.autoConfigured[IO]()
      tracer <- otel4s.tracerProvider.get("tracer").toResource
      xa <- TelemetryHikariTransactor.fromHikariConfig[IO](
        otel = otel4s.underlying,
        config = {
          val conf = new HikariConfig()
          // set the right properties
          conf
        }
      )
    } yield (tracer, xa))
      .use { case (tracer, xa) =>
        implicit val _tracer: Tracer[IO] = tracer
        program[IO](xa).void
      }
  }
}
