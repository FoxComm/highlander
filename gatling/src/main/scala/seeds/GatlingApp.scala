package seeds

import com.typesafe.config.Config
import io.gatling.app.Gatling
import io.gatling.core.scenario.Simulation
import io.gatling.jdbc.Predef._
import seeds.Simulations._
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

  // Gatling API is weird...
  val pingExitCode = Gatling.fromArgs(Array(), Some(classOf[PhoenixPing].asInstanceOf[Class[Simulation]]))
  if (pingExitCode != 0) {
    println(s"Phoenix did not respond in ${Conf.phoenixStartupTimeout}, exiting now!")
    System.exit(1)
  }
  Gatling.fromArgs(Array(), Some(classOf[GatlingSeeds].asInstanceOf[Class[Simulation]]))
}
