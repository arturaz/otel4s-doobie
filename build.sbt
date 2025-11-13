// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.9" // your current series x.y

ThisBuild / organization := "io.github.arturaz"
ThisBuild / organizationName := "arturaz"
ThisBuild / startYear := Some(2025)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("arturaz", "Artūras Šlajus")
)
ThisBuild / scalacOptions ++= Seq(
  "-Werror"
)

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("master")

// Disable the checks, I don't want to deal with them right now.
ThisBuild / tlCiHeaderCheck := false

val Scala213 = "2.13.17"
ThisBuild / crossScalaVersions := Seq(Scala213, "3.3.4")
ThisBuild / scalaVersion := Scala213 // the default Scala

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "otel4s-doobie",
    description := "Otel4s tracing integration with Doobie",
    libraryDependencies ++= Seq(
      // https://mvnrepository.com/artifact/org.typelevel/cats-core
      "org.typelevel" %%% "cats-core" % "2.13.0",
      // https://mvnrepository.com/artifact/org.typelevel/cats-effect
      "org.typelevel" %%% "cats-effect" % "3.6.3",
      // https://mvnrepository.com/artifact/org.typelevel/otel4s-java
      "org.typelevel" %%% "otel4s-oteljava" % "0.14.0",
      // https://mvnrepository.com/artifact/org.tpolecat/doobie-core
      "org.tpolecat" %%% "doobie-core" % "1.0.0-RC11",
      // https://mvnrepository.com/artifact/org.tpolecat/doobie-hikari
      "org.tpolecat" %%% "doobie-hikari" % "1.0.0-RC11" % Provided,
      // https://mvnrepository.com/artifact/io.opentelemetry.instrumentation/opentelemetry-jdbc
      "io.opentelemetry.instrumentation" % "opentelemetry-jdbc" % "2.21.0-alpha",
      // https://mvnrepository.com/artifact/io.opentelemetry.instrumentation/opentelemetry-hikaricp-3.0
      "io.opentelemetry.instrumentation" % "opentelemetry-hikaricp-3.0" % "2.21.0-alpha" % Provided,
      // https://mvnrepository.com/artifact/org.typelevel/otel4s-oteljava-context-storage
      "org.typelevel" %% "otel4s-oteljava-context-storage" % "0.14.0" % Test,
      // https://mvnrepository.com/artifact/org.scalameta/munit
      "org.scalameta" %%% "munit" % "1.2.1" % Test,
      "org.typelevel" %%% "munit-cats-effect" % "2.1.0" % Test,
      // https://mvnrepository.com/artifact/org.typelevel/otel4s-oteljava-testkit
      "org.typelevel" %% "otel4s-oteljava-testkit" % "0.14.0" % Test,
      "org.typelevel" %% "otel4s-oteljava-context-storage-testkit" % "0.14.0" % Test,
      // https://mvnrepository.com/artifact/com.h2database/h2
      "com.h2database" % "h2" % "2.4.240" % Test
    ),
    addCommandAlias(
      "prepareCi",
      "scalafmtAll;scalafmtSbt;scalafixAll;+ test;docs/tlSite;mimaReportBinaryIssues"
    ),
    Test / fork := true,
    Test / javaOptions += "-Dcats.effect.trackFiberContext=true"
  )

lazy val docs = project
  .in(file("site"))
  .dependsOn(core)
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    scalacOptions --= Seq(
      // Disable unused import warnings for the docs as they report false positives.
      "-Wunused:imports"
    )
  )
