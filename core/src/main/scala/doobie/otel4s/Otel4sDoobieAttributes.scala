package doobie.otel4s

import org.typelevel.otel4s.AttributeKey

object Otel4sDoobieAttributes {
  val SpanType: AttributeKey[String] = AttributeKey("span.type")
}
