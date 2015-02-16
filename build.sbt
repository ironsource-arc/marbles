name := "marbles"

organization := "com.supersonicads"

version := "0.1"

scalaVersion := "2.10.4"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:higherKinds"
)

// Testing framework
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.2" % "test"