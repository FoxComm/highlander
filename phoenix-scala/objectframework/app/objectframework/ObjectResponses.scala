package objectframework

import java.time.Instant

import objectframework.models._
import org.json4s.JValue

object ObjectResponses {

  case class ObjectContextResponse(name: String, attributes: JValue)

  object ObjectContextResponse {

    def build(c: ObjectContext): ObjectContextResponse =
      ObjectContextResponse(name = c.name, attributes = c.attributes)

    def build(c: IlluminatedContext): ObjectContextResponse =
      ObjectContextResponse(name = c.name, attributes = c.attributes)
  }

  case class ObjectFormResponse(id: Int, attributes: JValue, createdAt: Instant)

  object ObjectFormResponse {

    def build(f: ObjectForm): ObjectFormResponse =
      ObjectFormResponse(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  case class ObjectShadowResponse(id: Int, formId: Int, attributes: JValue, createdAt: Instant)

  object ObjectShadowResponse {

    def build(s: ObjectShadow): ObjectShadowResponse =
      ObjectShadowResponse(id = s.id, formId = s.formId, attributes = s.attributes, createdAt = s.createdAt)
  }

  case class IlluminatedObjectResponse(id: Int, attributes: JValue)

  object IlluminatedObjectResponse {

    def build(s: IlluminatedObject): IlluminatedObjectResponse =
      IlluminatedObjectResponse(id = s.id, attributes = s.attributes)
  }

  case class ObjectSchemaResponse(name: String, kind: String, schema: JValue)

  object ObjectSchemaResponse {

    def build(s: ObjectFullSchema): ObjectSchemaResponse =
      ObjectSchemaResponse(name = s.name, kind = s.kind, schema = s.schema)

  }
}
