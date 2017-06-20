package gatling.seeds

import com.typesafe.config.Config
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import phoenix.utils.FoxConfig
import slick.jdbc.PostgresProfile.api._
import core.utils.Money.Currency._

import scala.concurrent.duration._

object Conf {

  val (appConfig, dbUrl, dbUser, dbPassword) = {
    val appConfig: Config = FoxConfig.unsafe
    (appConfig, appConfig.getString("db.baseUrl"), appConfig.getString("db.user"), "")
  }

  // Loads the driver...
  Database.forConfig("db", appConfig).close()

  val defaultAssertion = global.failedRequests.count.is(0)

  val httpConf = http
    .baseURL(appConfig.getString("app.baseUrl"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableWarmUp

  val phoenixPingPause      = 10.seconds
  val phoenixStartupTimeout = 1.minute

  val contexts = Seq(("default", USD), ("ru", RUB))
}
