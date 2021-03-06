import sbt._
import Keys._
import scala.util.Properties
import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
import scala.xml.transform._

object BuildSettings {
  val buildVersion = "0.8.0-SNAPSHOT"
  val buildScalaVersion = "2.10.2-SNAPSHOT"
  val buildScalaOrganization = "org.scala-lang.macro-paradise"

  val useLocalBuildOfParadise = false
  // path to a build of https://github.com/scalamacros/kepler/tree/paradise/macros219
  val localBuildOfParadise210 = Properties.envOrElse("MACRO_PARADISE210", "/Users/xeno_by/Projects/Paradise210/build/pack")

  val buildSettings = Defaults.defaultSettings ++ Seq(
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    scalaOrganization := buildScalaOrganization
  ) ++ (if (useLocalBuildOfParadise) Seq(
    scalaHome := Some(file(localBuildOfParadise210)),
    unmanagedBase := file(localBuildOfParadise210 + "/lib")
  ) else Nil) ++ Seq(
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq("-feature")
  )
}

object MyBuild extends Build {
  import BuildSettings._

  // http://www.scala-sbt.org/release/docs/Extending/Input-Tasks
  def benchTask(benchClass: String, config: Traversable[Int]) = inputTask((args: TaskKey[Seq[String]]) =>
    (dependencyClasspath in Runtime in benchmark) map { (wrappedProjectCP) => {
      val projectCP = wrappedProjectCP.map(_.data).mkString(java.io.File.pathSeparatorChar.toString)
      val toolCP = projectCP // TODO: segregate compiler jars from the rest of dependencies
      val libraryCP = projectCP

      for (len <- config) {
        import scala.sys.process._
        var shellCommand = Seq(
          "java", "-Dsize=" + len, "-cp", toolCP,
          "-Xms1536M", "-Xmx4096M", "-Xss2M", "-XX:MaxPermSize=512M", "-XX:+UseParallelGC",
          "scala.tools.nsc.MainGenericRunner", "-cp", libraryCP,
          benchClass, "10")
        // println(shellCommand)
        shellCommand.!
      }
    }
  })

  def loadCredentials(): List[Credentials] = {
    val mavenSettingsFile = System.getProperty("maven.settings.file")
    if (mavenSettingsFile != null) {
      println("Loading Sonatype credentials from " + mavenSettingsFile)
      try {
        import scala.xml._
        val settings = XML.loadFile(mavenSettingsFile)
        def readServerConfig(key: String) = (settings \\ "settings" \\ "servers" \\ "server" \\ key).head.text
        List(Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          readServerConfig("username"),
          readServerConfig("password")
        ))
      } catch {
        case ex: Exception =>
          println("Failed to load Maven settings from " + mavenSettingsFile + ": " + ex)
          Nil
      }
    } else {
      println("Sonatype credentials cannot be loaded: -Dmaven.settings.file is not specified.")
      Nil
    }
  }

  lazy val core: Project = Project(
    "scala-pickling",
    file("core"),
    settings = buildSettings ++ (if (useLocalBuildOfParadise) Nil else Seq(
      libraryDependencies <+= (scalaVersion)(buildScalaOrganization % "scala-reflect" % _)
    )) ++ Seq(
      scalacOptions ++= Seq("-optimise"),
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
      conflictWarning in ThisBuild := ConflictWarning.disable,
      parallelExecution in Test := false, // hello, reflection sync!!
      run <<= run in Compile in sandbox, // http://www.scala-sbt.org/release/docs/Detailed-Topics/Tasks
      InputKey[Unit]("travInt") <<= InputKey[Unit]("travInt") in Compile in benchmark,
      InputKey[Unit]("travIntFreeMem") <<= InputKey[Unit]("travIntFreeMem") in Compile in benchmark,
      InputKey[Unit]("travIntSize") <<= InputKey[Unit]("travIntSize") in Compile in benchmark,
      InputKey[Unit]("geoTrellis") <<= InputKey[Unit]("geoTrellis") in Compile in benchmark,
      InputKey[Unit]("evactor1") <<= InputKey[Unit]("evactor1") in Compile in benchmark,
      InputKey[Unit]("evactor2") <<= InputKey[Unit]("evactor2") in Compile in benchmark,
      organization := "org.scala-lang",
      publishMavenStyle := true,
      publishArtifact in Test := false,
      publishTo <<= version { v: String =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      },
      pomIncludeRepository := { x => false },
      pomExtra := (
        <url>https://github.com/scala/pickling</url>
        <inceptionYear>2013</inceptionYear>
        <organization>
          <name>LAMP/EPFL</name>
          <url>http://lamp.epfl.ch/</url>
        </organization>
        <licenses>
          <license>
            <name>BSD-like</name>
            <url>http://www.scala-lang.org/downloads/license.html
            </url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git://github.com/scala/pickling.git</url>
          <connection>scm:git:git://github.com/scala/pickling.git</connection>
        </scm>
        <developers>
          <developer>
            <id>lamp</id>
            <name>EPFL LAMP</name>
          </developer>
        </developers>
      ),
      pomPostProcess := { (node: XmlNode) =>
        val hardcodeDeps = new RewriteRule {
          override def transform(n: XmlNode): XmlNodeSeq = n match {
            case e: Elem if e != null && e.label == "dependencies" =>
              // NOTE: this is necessary to unbind from paradise 210
              // we need to be compiled with paradise 210, because it's the only way to get quasiquotes in 210
              // however we don't need to be run with paradise 210, because all quasiquotes expand at compile-time
              // http://docs.scala-lang.org/overviews/macros/paradise.html#macro_paradise_for_210x
              <dependencies>
                <dependency>
                    <groupId>org.scala-lang</groupId>
                    <artifactId>scala-library</artifactId>
                    <version>2.10.2</version>
                </dependency>
                <dependency>
                    <groupId>org.scala-lang</groupId>
                    <artifactId>scala-reflect</artifactId>
                    <version>2.10.2</version>
                </dependency>
              </dependencies>
            case _ => n
          }
        }
        new RuleTransformer(hardcodeDeps).transform(node).head
      },
      credentials ++= loadCredentials()
    )
  )

  lazy val sandbox: Project = Project(
    "sandbox",
    file("sandbox"),
    settings = buildSettings ++ Seq(
      sourceDirectory in Compile <<= baseDirectory(root => root),
      sourceDirectory in Test <<= baseDirectory(root => root),
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1",
      parallelExecution in Test := false,
      scalacOptions ++= Seq()
      // scalacOptions ++= Seq("-Xprint:typer")
    )
  ) dependsOn(core)

  lazy val runtime: Project = Project(
    "runtime",
    file("runtime"),
    settings = buildSettings ++ (if (useLocalBuildOfParadise) Nil else Seq(
      libraryDependencies <+= (scalaVersion)(buildScalaOrganization % "scala-reflect" % _),
      libraryDependencies <+= (scalaVersion)(buildScalaOrganization % "scala-compiler" % _)
    ))
  ) dependsOn(core)

  lazy val benchmark: Project = Project(
    "benchmark",
    file("benchmark"),
    settings = buildSettings ++ Seq(
      sourceDirectory in Compile <<= baseDirectory(root => root),
      sourceDirectory in Test <<= baseDirectory(root => root),
      scalacOptions ++= Seq("-optimise"),
      InputKey[Unit]("travInt") <<= benchTask("TraversableIntBench", 100000 to 1000000 by 100000),
      InputKey[Unit]("travIntFreeMem") <<= benchTask("TraversableIntBenchFreeMem", 100000 to 1000000 by 100000),
      InputKey[Unit]("travIntSize") <<= benchTask("TraversableIntBenchSize", 100000 to 1000000 by 100000),
      InputKey[Unit]("geoTrellis") <<= benchTask("GeoTrellisBench", 100000 to 1000000 by 100000),
      InputKey[Unit]("evactor1") <<= benchTask("EvactorBench", 1000 to 10000 by 1000),
      InputKey[Unit]("evactor2") <<= benchTask("EvactorBench", 20000 to 40000 by 2000)
    )
  ) dependsOn(core, runtime)
}
