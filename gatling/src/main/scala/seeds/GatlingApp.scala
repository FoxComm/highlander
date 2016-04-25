package seeds

import com.typesafe.config.Config
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import seeds.Scenarios._
import slick.driver.PostgresDriver.api._
import utils.FoxConfig

object GatlingApp extends App {

  // Load DB config for JDBC feeder
  implicit val env = FoxConfig.environment
  val appConfig: Config = FoxConfig.loadWithEnv()
  val dbUrl = appConfig.getString("db.baseUrl")
  val dbUser = appConfig.getString("db.user")
  val dbPassword = ""

  // Loads the driver...
  Database.forConfig("db", appConfig).close()

  def dbFeeder(sql: String) = jdbcFeeder(dbUrl, dbUser, dbPassword, sql)

  Gatling.main(Array())
}

object Conf {

  val defaultAssertion = global.failedRequests.count.is(0)

  val httpConf = http
    .baseURL("http://localhost:9090")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableWarmUp

}

class GatlingSeeds extends Simulation {
  setUp(pacificNwVips, randomCustomerActivity)
    .assertions(Conf.defaultAssertion)
    .protocols(Conf.httpConf)
}
