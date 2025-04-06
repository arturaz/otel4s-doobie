package doobie.otel4s.semconv.flex;

// Had to do in Java, because used as `dialect == COUCHBASE`
// which is not working if SqlDialect declared as trait/object
public enum SqlDialect {
	DEFAULT, COUCHBASE
}
