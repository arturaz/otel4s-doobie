package doobie.otel4s.semconv

import cats.FlatMap
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import doobie.util.invariant.InvariantViolation
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.trace.Span
import org.typelevel.otel4s.trace.StatusCode
import org.typelevel.otel4s.trace.Tracer

import java.sql.SQLException

object SpanExceptionHandler {

  def recordException[F[_]: Tracer: FlatMap](throwable: Throwable): F[Span[F]] =
    Tracer[F].currentSpanOrNoop.flatMap(span =>
      for {
        _ <- span.setStatus(StatusCode.Error, throwable.getMessage())
        _ <- span.recordException(
          throwable,
          (throwable match {
            case exception: SQLException =>
              Attributes(
                Attribute(
                  "error.type",
                  exception.getClass().getCanonicalName()
                ),
                Attribute("db.response.status_code", exception.getSQLState())
              )
            case violation: InvariantViolation =>
              Attributes(
                Attribute("error.type", violation.getClass().getCanonicalName())
              )
            case _ =>
              Attributes(
                Attribute("error.type", throwable.getClass().getCanonicalName())
              )
          })
        )
      } yield span
    )

}
