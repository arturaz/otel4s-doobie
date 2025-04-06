package doobie.otel4s.semconv.flex

import munit.FunSuite

import scala.util.Success

class SqlSanitizerSuite extends FunSuite {

  test("basic SQL parsing") {
    val result = SqlSanitizer.sanitize("""
        SELECT c1, c2, c3 
        FROM schema.table t
        WHERE c1 = 'c1'
    """)

    assertEquals(
      result,
      Success(
        SqlStatementInfo(
          "SELECT c1, c2, c3 FROM schema.table t WHERE c1 = ?",
          "SELECT",
          "schema.table"
        )
      )
    )
  }

}
