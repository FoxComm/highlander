package objectframework.payloads

import org.json4s.JsonAST.JValue

object ObjectSchemaPayloads {

  case class UpdateObjectSchema(schema: JValue, dependencies: Option[List[String]])
}
