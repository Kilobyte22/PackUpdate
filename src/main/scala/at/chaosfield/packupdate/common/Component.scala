package at.chaosfield.packupdate.common

import java.io.FileInputStream
import java.net.{URI, URL}

import org.apache.commons.codec.digest.DigestUtils

class Component(val name: String, val version: String, val downloadUrl: Option[URI], val componentType: ComponentType, val hash: Option[String], val flags: Array[ComponentFlag]) {
  def toCSV = {
    Array(
      name,
      version,
      downloadUrl.map(_.toString).getOrElse(""),
      componentType.stringValue,
      hash.getOrElse(""),
      flags.map(_.internalName).mkString(";")
    ).mkString(",")
  }

  def verifyChecksum(config: MainConfig): Boolean = {
    componentType match {
      case ComponentType.Mod | ComponentType.Forge =>
        val files = List(
          Util.fileForComponent(this, config.minecraftDir),
          Util.fileForComponent(this, config.minecraftDir, disabled = true)
        )
        files.find(_.exists()) match {
          case Some(file) =>
            val disabled = file.getName.endsWith(".disabled")
            if (!flags.contains(ComponentFlag.Optional) && flags.contains(ComponentFlag.Disabled) != disabled) {
              // The mod is in wrong disabled state and should be repaired
              false
            } else {
              hash match {
                case Some(h) =>
                  val d = DigestUtils.sha256Hex(new FileInputStream(file))
                  //println(s"Hash of $name: $d <=> $h")
                  d == h
                case _ =>
                  println(s"Warning: Could not validate integrity of Component $name, because no hash was provided")
                  true
              }
            }
          case None => false // Even if no checksum is provided, we know something is wrong if the file is missing
        }

      case _ =>
        println(s"Warning: Checking integrity of component type ${componentType.stringValue} not supported yet, assuming working")
        true
    }
  }

  def neededOnSide(packSide: PackSide): Boolean = {
    packSide match {
      case PackSide.Client => !flags.contains(ComponentFlag.ServerOnly)
      case PackSide.Server => !flags.contains(ComponentFlag.ClientOnly)
    }
  }

  def display = s"$name $version"
}

object Component {
  def fromCSV(data: Array[String]) = {
    new Component(
      data(0), // name
      data(1), // version
      if (data(2).isEmpty) None else Some(new URI(data(2))), // downloadUrl
      ComponentType.fromString(data(3)).getOrElse(ComponentType.Unknown), // componentType
      data.lift(4).filter(_ != ""), // hash
      data.lift(5).map(_.split(';')).getOrElse(Array.empty[String]).flatMap(ComponentFlag.fromString) // flags
    )
  }
}
