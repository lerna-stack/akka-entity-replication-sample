import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "1.0.0"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

val AkkaVersion = "2.6.12"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "sample-akka-entity-replication",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
        "com.lerna-stack"   %% "akka-entity-replication"    % "1.0.0+142-165d1ced-SNAPSHOT",
        "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.4",
        "com.typesafe.akka" %% "akka-http"                  % "10.2.1",
        "com.typesafe.akka" %% "akka-cluster"               % AkkaVersion,
        "com.typesafe.akka" %% "akka-slf4j"                 % AkkaVersion,
        "ch.qos.logback"     % "logback-classic"            % "1.2.3",
        scalaTest            % Test,
        "com.typesafe.akka" %% "akka-testkit"               % AkkaVersion % Test,
        "com.typesafe.akka" %% "akka-actor-testkit-typed"   % AkkaVersion % Test,
      ),
    dockerBaseImage := "openjdk:11-slim",
    dockerExposedPorts := Seq(2551, 8080),
    fork in runNode1 := true,
    javaOptions in runNode1 ++= Seq(
        "-Dhttp.port=8080",
        "-Dakka.remote.artery.canonical.port=2551",
        "-Dakka.cluster.roles.0=replica-group-1",
        "-Dakka-entity-replication.raft.persistence.cassandra.journal.keyspace-autocreate=true",
        "-Dakka-entity-replication.raft.persistence.cassandra.journal.tables-autocreate=true",
        "-Dakka-entity-replication.raft.persistence.cassandra.snapshot.keyspace-autocreate=true",
        "-Dakka-entity-replication.raft.persistence.cassandra.snapshot.tables-autocreate=true",
        "-Dakka-entity-replication.eventsourced.persistence.cassandra.journal.keyspace-autocreate=true",
        "-Dakka-entity-replication.eventsourced.persistence.cassandra.journal.tables-autocreate=true",
      ),
    fork in runNode2 := true,
    javaOptions in runNode2 ++= Seq(
        "-Dhttp.port=18081",
        "-Dakka.remote.artery.canonical.port=2552",
        "-Dakka.cluster.roles.0=replica-group-2",
      ),
    fork in runNode3 := true,
    javaOptions in runNode3 ++= Seq(
        "-Dhttp.port=18082",
        "-Dakka.remote.artery.canonical.port=2553",
        "-Dakka.cluster.roles.0=replica-group-3",
      ),
    fullRunTask(runNode1, Compile, "example.Main"),
    fullRunTask(runNode2, Compile, "example.Main"),
    fullRunTask(runNode3, Compile, "example.Main"),
  )

lazy val runNode1 = taskKey[Unit]("run node1")
lazy val runNode2 = taskKey[Unit]("run node2")
lazy val runNode3 = taskKey[Unit]("run node3")

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
