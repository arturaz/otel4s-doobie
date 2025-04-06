package doobie.otel4s.semconv

import cats.syntax.all.catsSyntaxTuple2Semigroupal

object SpanName {

  def from(
      summary: Option[String],
      operationName: Option[String],
      target: Option[String],
      systemName: String
  ): String =
    summary
      .orElse((operationName, target).mapN((o, t) => s"$o $t"))
      .orElse(target)
      .getOrElse(systemName)

}
