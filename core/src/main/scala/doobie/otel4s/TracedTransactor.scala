package doobie.otel4s

import cats.data.Kleisli
import cats.effect.kernel.Async
import doobie.WeakAsync
import doobie.free.KleisliInterpreter
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.SpanBuilder
import org.typelevel.otel4s.trace.SpanKind
import org.typelevel.otel4s.trace.Tracer

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

object TracedTransactor {

  /** Configuration for the tracing.
    *
    * @param makeSpanName
    *   Makes the span name from the query.
    * @param spanKind
    *   The kind of the span.
    * @param configureSpan
    *   Configures the span builder.
    */
  case class Config[F[_]](
      makeSpanName: String => String,
      spanKind: SpanKind,
      configureSpan: SpanBuilder[F] => SpanBuilder[F]
  )
  object Config {

    /** Default configuration with `SpanKind.Server` and spans prefixed with
      * `SQL: `.
      */
    def default[F[_]]: Config[F] = Config(
      makeSpanName = sql => s"SQL: $sql",
      // Usually databases are accessed from a server.
      SpanKind.Server,
      identity
    )
  }

  def apply[F[_]: Tracer: Async](
      transactor: Transactor[F],
      logHandler: LogHandler[F],
      config: Config[F] = Config.default[F]
  ): Transactor[F] =
    transactor.copy(interpret0 =
      createInterpreter(config, Async[F], logHandler).ConnectionInterpreter
    )

  private val commentNamedQueryRegEx = """--\s*Name:\s*(\w+)""".r

  private def extractQueryNameOrSql(sql: String): String =
    commentNamedQueryRegEx
      .findFirstMatchIn(sql)
      .flatMap(m => Option(m.group(1)))
      .getOrElse(sql)

  /** Replace the newlines and multiple spaces with single spaces. */
  private def formatQuery(q: String): String =
    q.replace("\n", " ").replaceAll("\\s+", " ").trim()

  private def createInterpreter[F[_]: Tracer](
      config: Config[F],
      F: Async[F],
      logHandler: LogHandler[F]
  ): KleisliInterpreter[F] = {
    val tracer = implicitly[Tracer[F]]

    new KleisliInterpreter[F](logHandler)(
      WeakAsync.doobieWeakAsyncForAsync(F)
    ) {
      override lazy val PreparedStatementInterpreter
          : PreparedStatementInterpreter =
        new PreparedStatementInterpreter {

          type TracedOp[A] =
            Kleisli[F, PreparedStatement, A] // PreparedStatement => F[A]

          def runTraced[A](makeF: TracedOp[A]): TracedOp[A] =
            Kleisli {
              case TracedStatement(preparedStatement, sql) =>
                val spanName =
                  config.makeSpanName(formatQuery(extractQueryNameOrSql(sql)))
                val fa = makeF(preparedStatement)
                config
                  .configureSpan(
                    tracer
                      .spanBuilder(spanName)
                      .withSpanKind(config.spanKind)
                      .addAttribute(
                        Attribute(Otel4sDoobieAttributes.SpanType, "db")
                      )
                  )
                  .build
                  .surround(fa)
              case a =>
                makeF(a)
            }

          override val executeBatch: TracedOp[Array[Int]] =
            runTraced(super.executeBatch)

          override val executeLargeBatch: TracedOp[Array[Long]] =
            runTraced(super.executeLargeBatch)

          override val execute: TracedOp[Boolean] =
            runTraced(super.execute)

          override val executeUpdate: TracedOp[Int] =
            runTraced(super.executeUpdate)

          override val executeQuery: TracedOp[ResultSet] =
            runTraced(super.executeQuery)
        }

      override lazy val ConnectionInterpreter: ConnectionInterpreter =
        new ConnectionInterpreter {
          override def prepareStatement(
              a: String
          ): Kleisli[F, Connection, PreparedStatement] =
            super
              .prepareStatement(a)
              .map(TracedStatement(_, a): PreparedStatement)

          override def prepareStatement(
              a: String,
              b: Array[String]
          ): Kleisli[F, Connection, PreparedStatement] =
            super
              .prepareStatement(a, b)
              .map(TracedStatement(_, a): PreparedStatement)

          override def prepareStatement(
              a: String,
              b: Array[Int]
          ): Kleisli[F, Connection, PreparedStatement] =
            super
              .prepareStatement(a, b)
              .map(TracedStatement(_, a): PreparedStatement)

          override def prepareStatement(a: String, b: Int) =
            super
              .prepareStatement(a, b)
              .map(TracedStatement(_, a): PreparedStatement)

          override def prepareStatement(a: String, b: Int, c: Int) =
            super
              .prepareStatement(a, b, c)
              .map(TracedStatement(_, a): PreparedStatement)

          override def prepareStatement(a: String, b: Int, c: Int, d: Int) =
            super
              .prepareStatement(a, b, c, d)
              .map(TracedStatement(_, a): PreparedStatement)

          override def getTypeMap: Nothing =
            // See: https://github.com/tpolecat/doobie/blob/v1.0.0-RC4/modules/core/src/test/scala/doobie/util/StrategySuite.scala#L47
            super.getTypeMap.asInstanceOf[Nothing]
        }
    }
  }
}
