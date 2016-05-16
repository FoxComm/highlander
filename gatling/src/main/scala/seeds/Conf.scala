package seeds

import scala.concurrent.duration._

import com.typesafe.config.Config
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import slick.driver.PostgresDriver.api._
import utils.FoxConfig
import utils.Money.Currency._

object Conf {

  implicit val env = FoxConfig.environment

  val (appConfig, dbUrl, dbUser, dbPassword) = {
    val appConfig: Config = FoxConfig.loadWithEnv()
    (appConfig, appConfig.getString("db.baseUrl"), appConfig.getString("db.user"), "")
  }

  // Loads the driver...
  Database.forConfig("db", appConfig).close()

  val defaultAssertion = global.failedRequests.count.is(0)

  val httpConf = http
    .baseURL("http://localhost:9090")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableWarmUp

  val phoenixPingPause = 10.seconds
  val phoenixStartupTimeout = 1.minute

  val contexts = Seq(("default", USD), ("ru", RUB))

}
