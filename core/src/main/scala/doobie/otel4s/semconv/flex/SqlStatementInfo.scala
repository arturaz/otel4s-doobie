package doobie.otel4s.semconv.flex

final case class SqlStatementInfo(
    fullStatement: String,
    operation: String,
    mainIdentifier: String
)

object SqlStatementInfo {

  // called with (null, null, null) in AutoSqlSanitizer.java
  def create(fullStatement: String, operation: String, mainIdentifier: String) =
    SqlStatementInfo(
      Option(fullStatement).map(_.trim()).getOrElse(""),
      Option(operation).getOrElse(""),
      Option(mainIdentifier).getOrElse("")
    )

}
