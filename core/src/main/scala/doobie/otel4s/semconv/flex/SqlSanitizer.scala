package doobie.otel4s.semconv.flex

import scala.util.Try

object SqlSanitizer {

  def sanitize(statement: String): Try[SqlStatementInfo] =
    Try(AutoSqlSanitizer.sanitize(statement, SqlDialect.DEFAULT))

}
