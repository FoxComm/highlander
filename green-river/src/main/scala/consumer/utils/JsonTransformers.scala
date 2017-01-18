package consumer.utils

import org.json4s.JsonAST.{JArray, JValue, JField, JString, JInt}

/**
  * Helper functions to transform JSON.
  */
object JsonTransformers {

  def underscoreToCamel(s: String): String = "_([a-z])".r.replaceAllIn(s, _.group(1).toUpperCase)

  def camelCase(input: JValue): JValue = {
    input.transformField {
      case JField(name, anything) ⇒ (underscoreToCamel(name), anything)
    }
  }

  def dateTimeToDateString(obj: JValue): JValue = {
    val date = for {
      JInt(year)   ← obj \ "year"
      JInt(month)  ← obj \ "month"
      JInt(day)    ← obj \ "day"
      JInt(hour)   ← obj \ "hour"
      JInt(minute) ← obj \ "minute"
      JInt(second) ← obj \ "second"
      JInt(micro)  ← obj \ "micro"
      milliseconds = micro / 1000
    } yield
      JString(
        f"$year%04d-$month%02d-$day%02dT$hour%02d:$minute%02d:$second%02d.$milliseconds%03dZ")

    if (date.isEmpty) obj else date.head
  }

  def extractStringSeq(data: JValue, fieldName: String): Seq[String] = {
    data \ fieldName match {
      case JArray(list: List[JValue]) ⇒ list.collect { case JString(value) ⇒ value }
      case _                          ⇒ Seq.empty
    }
  }

  def extractBigIntSeq(data: JValue, fieldName: String): Seq[String] = {
    data \ fieldName match {
      case JArray(list: List[JValue]) ⇒ list.collect { case JInt(value) ⇒ value.toString }
      case _                          ⇒ Seq.empty
    }
  }
}
