## otel4s-doobie

### Installation

This library is currently available for Scala binary versions 2.13 and 3.3+.

To use the latest version, include the following in your `build.sbt`:

```scala
libraryDependencies += "io.github.arturaz" %% "otel4s-doobie" % "@VERSION@"
```

Or `build.mill` if you are using [mill](https://mill-build.com):

```scala
override def ivyDeps = Agg(
  ivy"io.github.arturaz::otel4s-doobie:@VERSION@"
)
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
|-----------------| -------------- |----------------|---------------------|
| 0.4.0           | 1.0.0-RC8      | 0.12.0         | 3.6.0               |
| 0.3.0           | 1.0.0-RC8      | 0.12.0-RC3     | 3.5.7               | 
| 0.2.0           | 1.0.0-RC8      | 0.12.0-RC2     | 3.5.7               |
| 0.1.0           | 1.0.0-RC5      | 0.12.0-RC2     | 3.5.7               |

### Usage

```scala mdoc
import doobie._
import doobie.otel4s._
import cats.effect.Async
import org.typelevel.otel4s.trace.Tracer

def makeTraced[F[_] : Async : Tracer](transactor: Transactor[F]): Transactor[F] = {
  /** Also see `TracedTransactor.Config` for various configuration options. */
  TracedTransactor[F](transactor, LogHandler.noop)
}
```

## Credits

This library was created by [Artūras Šlajus](https://arturaz.net). You can find me as `arturaz` on the
[Typelevel Discord Server](https://discord.gg/XF3CXcMzqD) in the `#doobie` channel.

## Changelog

### v0.4.0

- Upgraded Otel4s from `0.12.0-RC3` to `0.12.0`, which also bumps the cats-effect version from `3.5.7` 
  to `3.6.0`.

### v0.3.0

- Upgraded Otel4s from `0.12.0-RC2` to `0.12.0-RC3`.

### v0.2.0

- Upgraded Doobie from `1.0.0-RC5` to `1.0.0-RC8`.

### v0.1.0

- Initial release.