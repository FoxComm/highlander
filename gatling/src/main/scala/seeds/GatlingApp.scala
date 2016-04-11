package seeds

import scala.collection.mutable

import com.typesafe.config.Config
import io.gatling.app.Gatling
import io.gatling.core.ConfigKeys
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import slick.driver.PostgresDriver.api._

object GatlingApp extends App {

  // Load DB config for JDBC feeder
  implicit val env = utils.Config.environment
  val appConfig: Config = utils.Config.loadWithEnv()
  val dbUrl = appConfig.getString("db.baseUrl")
  val dbUser = appConfig.getString("db.user")
  val dbPassword = ""

  implicit val db = Database.forConfig("db", appConfig)

  def dbFeeder(sql: String) = jdbcFeeder(dbUrl, dbUser, dbPassword, sql)

  // Gatling config
  val gatlingConfig = mutable.Map(
    ConfigKeys.core.directory.Binaries → "./gatling/target/scala-2.11/gatling-classes",
    ConfigKeys.core.Mute → "true"
  )
  println(Gatling.fromMap(gatlingConfig))
  db.close()
}

object Conf {

  val httpConf = http
    .baseURL("http://localhost:9090")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableWarmUp

}
