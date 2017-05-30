package core

import core.utils.{Money, time}

object json {

  val DefaultFormats =
    org.json4s.DefaultFormats + time.JavaTimeJson4sSerializer.jsonFormat + Money.jsonFormat

}
