import java.io.PrintWriter
import java.util.Base64

import com.typesafe.config.ConfigFactory

import scala.io.Source
import scalaj.http.Http

object Main extends App {
  val conf = ConfigFactory.load()
  val url = conf.getString("gfwUrl")
  val proxyHost = conf.getString("proxyHost")
  val proxyPort = conf.getInt("proxyPort")
  val gfwBase64 = Http(url).proxy(proxyHost, proxyPort, java.net.Proxy.Type.SOCKS).asString.body
  val gfw = new String(Base64.getMimeDecoder.decode(gfwBase64))
  println(gfw)
  val removeComment = gfw.split("\n").drop(1).takeWhile(!_.contains("Whitelist")).toList.filter(!_.startsWith("!"))
    .map(_.dropWhile(x => x == '|' || x == '@' || x == '.'))
    .filter(!_.trim.isEmpty)

  val additionalUrl = Source.fromInputStream(getClass.getResourceAsStream("additional-proxy-urls.txt")).getLines()

  val pacTemplate = Source.fromInputStream(getClass.getResourceAsStream("pacFileTemplate.txt")).getLines().mkString("\n")

  val out = new PrintWriter("/home/weili/work/myproxy.pac")
  out.print("var domains = {")
  removeComment.foreach{ x =>
    out.println(s""" "$x": 1,""")
  }
  additionalUrl.foreach{ x =>
    out.println(s""" "$x": 1,""")
  }
  out.println("}")
  out.print(pacTemplate)
  out.close()
}
