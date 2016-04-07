import scala.collection.mutable

import io.gatling.app.Gatling
import io.gatling.core.ConfigKeys._

object GatlingApp extends App {

  val config = mutable.Map(
    core.directory.Binaries → "./gatling/target/scala-2.11/gatling-classes",
    core.Mute → "true",
    charting.NoReports → "true"
  )
  println(Gatling.fromMap(config))

}
