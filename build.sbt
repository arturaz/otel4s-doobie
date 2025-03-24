// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.2" // your current series x.y

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

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("master")

// Disable the checks, I don't want to deal with them right now.
ThisBuild / tlCiHeaderCheck := false

val Scala213 = "2.13.16"
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
      "org.typelevel" %%% "cats-effect" % "3.5.7",
      // https://mvnrepository.com/artifact/org.typelevel/otel4s-core-trace
      "org.typelevel" %%% "otel4s-core-trace" % "0.12.0-RC3",
      // https://mvnrepository.com/artifact/org.tpolecat/doobie-core
      "org.tpolecat" %%% "doobie-core" % "1.0.0-RC8",
      // https://mvnrepository.com/artifact/org.scalameta/munit
      "org.scalameta" %%% "munit" % "1.1.0" % Test,
      "org.typelevel" %%% "munit-cats-effect" % "2.0.0" % Test
    ),
    addCommandAlias(
      "prepareCi",
      "scalafmtAll;scalafmtSbt;scalafixAll;+ test;docs/tlSite;mimaReportBinaryIssues"
    )
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
