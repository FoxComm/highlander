import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Seconds, Span, Milliseconds}
import org.scalatest.{Outcome, OneInstancePerTest, FreeSpec}

class AddressIntegrationTest extends FreeSpec with OneInstancePerTest
  with ScalaFutures {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = Span(5, Seconds),
    interval = Span(20, Milliseconds)
  )

  private val ActorSystemNameChars = ('a' to 'z').toSet | ('A' to 'Z').toSet | ('0' to '9').toSet | Set('-', '_')
  implicit var system: ActorSystem = _

  override protected def withFixture(test: NoArgTest): Outcome = {
    system = ActorSystem(test.name.filter(ActorSystemNameChars.contains))

    try { super.withFixture(test) } finally {
      system.shutdown()
      system.awaitTermination()

      println("It did shut down.")
    }
  }

  "works" in {
    val port    = getFreePorts(1).head
    val server  = new Service(systemOverride = Some(system))
    val binding = server.bind(ConfigFactory.parseString(
      s"""
        |http.interface = 127.0.0.1
        |http.port      = ${ port }
      """.stripMargin))

    info(binding.futureValue.toString)

    implicit val fm: FlowMaterializer = ActorFlowMaterializer()

    val response = Http().singleRequest(
      HttpRequest(
        HttpMethods.POST,
        uri    = s"http://127.0.0.1:${ port }/v1/addresses",
        entity = HttpEntity(jsonMap(Map(
          "name"    → "Ferdinand",
          "stateId" → 1,
          "street1" → "Hauptstrasse",
          "city"    → "Achau",
          "zip"     → 2481
        ))
      )).futureValue

    info((response.status, response.headers, response.entity.getClass.toString).toString)
  }

  def jsonMap(in: Map[String, Any]): String = {
    import org.json4s.{ DefaultFormats, Extraction }
    import org.json4s.jackson.JsonMethods.pretty

    implicit val formats = DefaultFormats

    pretty(Extraction.decompose(in))
  }

  /**
   * Returns an ephemeral port that is free, that is not currently used by the
   * OS and not likely to be used.
   *
   * Ephemeral ports are part of a pool that is managed by the kernel.
   * We get a port and immediately release it.
   *
   * Those ports are assigned in ascending order from the kernel until it wraps over,
   * so this must be quite safe.
   */
  def getFreePorts(count: Int): Seq[Int] = {
    val sockets = for(i <- 0 until count) yield new ServerSocket(0)
    val ports   = sockets.map(_.getLocalPort)
    sockets.foreach(_.close())

    ports
  }
}
