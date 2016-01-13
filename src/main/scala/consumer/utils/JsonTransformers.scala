package consumer.utils

import org.json4s.JsonAST.{JValue, JObject, JField, JString, JInt}
import org.json4s.jackson.JsonMethods.{render, compact, parse}

/**
 * Helper functions to transform JSON. 
 */
object JsonTransformer {

  def underscoreToCamel(s: String): String = "_([a-z])".r.replaceAllIn(s, _.group(1).toUpperCase)

  def camelCase(input: JValue): JValue = {
    input.transformField {
      case JField(name, anything) ⇒ (underscoreToCamel(name), anything)
    }
  }

  def dateTimeToDateString(obj: JValue) : JValue = {
    val date = for {
      JInt(year) ← obj \ "year"
      JInt(month) ← obj \ "month"
      JInt(day) ← obj \ "day"
      JInt(hour) ← obj \ "hour"
      JInt(minute) ← obj \ "minute"
      JInt(second) ← obj \ "second"
    } yield JString(f"$year%04d-$month%02d-$day%02d $hour%02d:$minute%02d:$second%02d")
    if(date.isEmpty) obj else date.head
  }

}
