package objectframework.payloads

import org.json4s.JsonAST.JValue

object ContextPayloads {

  case class CreateObjectContext(name: String, attributes: JValue)

  case class UpdateObjectContext(name: String, attributes: JValue)
}
