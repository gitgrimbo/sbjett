name := """techtest-play-service"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"

resolvers += "Local Maven Repository" at "file:///c:/.m3/repository"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)

libraryDependencies += "techtest" % "techtest-original-service-api" % "1.0.0"
libraryDependencies += "org.projectlombok" % "lombok" % "1.16.6"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
