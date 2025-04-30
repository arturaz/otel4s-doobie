## otel4s-doobie

### Installation

This library is currently available for Scala binary versions 2.13 and 3.3+.

To use the latest version, include the following in your `build.sbt`:

```scala
libraryDependencies += "io.github.arturaz" %% "otel4s-doobie" % "@VERSION@"
javaOptions += "-Dcats.effect.trackFiberContext=true"
```

Or `build.mill` if you are using [mill](https://mill-build.com):

```scala
override def ivyDeps = Agg(
  ivy"io.github.arturaz::otel4s-doobie:@VERSION@"
)
```

and `.mill-jvm-opts`:
```scala
-Dcats.effect.trackFiberContext=true
```

The code from `main` branch can be obtained with:
```scala
resolvers ++= Resolver.sonatypeOssRepos("snapshots")
libraryDependencies += "io.github.arturaz" %% "otel4s-doobie" % "@SNAPSHOT_VERSION@"
```

For [mill](https://mill-build.com):
```scala
  override def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(
      coursier.Repositories.sonatype("snapshots")
    )
  }

override def ivyDeps = Agg(
  ivy"io.github.arturaz::otel4s-doobie:@SNAPSHOT_VERSION@"
)
```

You can see all the published artifacts on 
[MVN Repository](https://mvnrepository.com/artifact/io.github.arturaz/otel4s-doobie).

#### Versions table

Due to the usage of RC versions, binary compatibility is finicky. Consult this table to know which versions are compatible.

| Library Version | Doobie Version | Otel4s Version | Cats Effect Version |
|-----------------|----------------|----------------|---------------------|
| 0.5.0           | 1.0.0-RC9      | 0.12.0         | 3.6.0               |
| 0.4.0           | 1.0.0-RC8      | 0.12.0         | 3.6.0               |
| 0.3.0           | 1.0.0-RC8      | 0.12.0-RC3     | 3.5.7               | 
| 0.2.0           | 1.0.0-RC8      | 0.12.0-RC2     | 3.5.7               |
| 0.1.0           | 1.0.0-RC5      | 0.12.0-RC2     | 3.5.7               |

### Usage

```scala mdoc
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.MonadCancelThrow
import doobie.Transactor
import doobie.implicits.toSqlInterpolator
import doobie.otel4s.tracing.TraceTransactor
import doobie.syntax.connectionio.toConnectionIOOps
import doobie.util.ExecutionContexts
import org.h2.jdbcx.JdbcDataSource
import org.typelevel.otel4s.context.LocalProvider
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.oteljava.context.IOLocalContextStorage

import javax.sql.DataSource

object App extends IOApp.Simple {

  def program[F[_]: MonadCancelThrow](xa: Transactor[F]): F[Int] = {
    sql"""SELECT 1""".query[Int].unique.transact(xa)
  }

  override def run: IO[Unit] = {
    // don't forget to add
    // javaOptions += "-Dcats.effect.trackFiberContext=true"
    implicit val provider: LocalProvider[IO, Context] =
      IOLocalContextStorage.localProvider[IO]

    OtelJava.autoConfigured[IO]().use { otel4s =>
      // initialize your transactor the way you want
      val xa: Transactor.Aux[IO, DataSource] = Transactor.fromDataSource[IO](
        {
          val ds = new JdbcDataSource()
          ds.setURL("jdbc:h2:mem:test_database")
          ds
        },
        ExecutionContexts.synchronous
      )
      // wrap it with a TraceTransactor
      val traceTransactor =
        TraceTransactor.fromDataSource(otel4s.underlying, xa)

      program[IO](traceTransactor).void
    }
  }
}
```

This is how it looks after instrumentation:

@:image(example.png) {
  intrinsicWidth = 1024
  intrinsicHeight = 368
  alt = Example
}
[(open image in full size)](example.png)

## Credits

This library was created by [Artūras Šlajus](https://arturaz.net). You can find me as `arturaz` on the
[Typelevel Discord Server](https://discord.gg/XF3CXcMzqD) in the `#doobie` channel.

## Changelog

### v0.5.0

- Upgraded Doobie from `1.0.0-RC8` to `1.0.0-RC9`.
- Use opentelemetry-jdbc instead of custom transactor (https://github.com/arturaz/otel4s-doobie/pull/5).

### v0.4.0

- Upgraded Otel4s from `0.12.0-RC3` to `0.12.0`, which also bumps the cats-effect version from `3.5.7` 
  to `3.6.0`.

### v0.3.0

- Upgraded Otel4s from `0.12.0-RC2` to `0.12.0-RC3`.

### v0.2.0

- Upgraded Doobie from `1.0.0-RC5` to `1.0.0-RC8`.

### v0.1.0

- Initial release.