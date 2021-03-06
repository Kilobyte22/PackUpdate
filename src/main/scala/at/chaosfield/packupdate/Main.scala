package at.chaosfield.packupdate

import java.io.File
import java.net.URL
import java.util.jar.Manifest

import at.chaosfield.packupdate.common.{MainConfig, MainLogic, PackSide}
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import net.sourceforge.argparse4j.inf.{ArgumentAction, ArgumentParser, Namespace, Subparser}

import scala.collection.JavaConverters._


object Main {

  val ProjectName = "PackUpdate"

  lazy val Version: String = Manifest
    .map(_.getMainAttributes.getValue("Implementation-Version"))
    .getOrElse("Unknown")

  lazy val Manifest: Option[Manifest] = {
    getClass
      .getClassLoader
      .getResources("META-INF/MANIFEST.MF")
      .asScala
      .map(res => new Manifest(res.openStream()))
      .find(man => man.getMainAttributes.getValue("Implementation-Title") == ProjectName)
  }

  def createServerParser(parser: Subparser) = {

    parser
      .addArgument("--init")
      .dest("init")
      .action(Arguments.storeConst())
      .help(s"Initialize the directory for use with ${Main.ProjectName}")

    parser
        .addArgument("--update")
        .dest("update")
        .action(Arguments.storeTrue())
        .help("Updates the Server")

    parser
      .addArgument("--run")
      .dest("run")
      .action(Arguments.storeTrue())
      .help("Run the Server in foreground. Implies --update")
  }

  def createClientParser(parser: Subparser) = {
    parser
      .addArgument("--bootstrap")
      .dest("bootstrap")
      .help(s"Initialize the directory for use with ${Main.ProjectName}")
  }

  def createParser(): ArgumentParser = {
    val parser = ArgumentParsers
      .newFor(Main.ProjectName)
      .build()
      .description("Tool for maintaining Minecraft Mod Packs")

    val sub = parser.addSubparsers().help("Sub Commands").metavar("COMMAND")

    createServerParser(sub.addParser("server").help("Operate in Server mode"))
    createClientParser(sub.addParser("client").help("Operate in Client mode"))

    parser
  }

  def getConfig(side: PackSide, options: Namespace) = {
    val mcDir = side match {
      case PackSide.Client => new File(Option(System.getenv("INST_MC_DIR")).get)
      case PackSide.Server => new File(".")
    }

    val remoteUrl = Option(options.getString("bootstrap"))

    ??? // new MainConfig(mcDir, )
  }

  def mainClient(namespace: Namespace) = {
    val config = getConfig(PackSide.Client, namespace)

    if (namespace.getString("bootstrap") != null) {
      // TODO: Write Bootstrap config
    }
  }

  def mainServer(namespace: Namespace) = {
    ???
  }

  def main(args: Array[String]): Unit = {
    val options = createParser().parseArgs(args)
    options.getString("COMMAND") match {
      case "server" => mainServer(options)
      case "client" => mainClient(options)
    }
  }
}
