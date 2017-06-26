package objectframework

import java.time.Instant

import objectframework.models._
import org.json4s.JValue

object ObjectResponses {

  object ObjectContextResponse {

    case class Root(name: String, attributes: JValue)

    def build(c: ObjectContext): Root =
      Root(name = c.name, attributes = c.attributes)

    def build(c: IlluminatedContext): Root =
      Root(name = c.name, attributes = c.attributes)
  }

  object ObjectFormResponse {

    case class Root(id: Int, attributes: JValue, createdAt: Instant)

    def build(f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object ObjectShadowResponse {

    case class Root(id: Int, formId: Int, attributes: JValue, createdAt: Instant)

    def build(s: ObjectShadow): Root =
      Root(id = s.id, formId = s.formId, attributes = s.attributes, createdAt = s.createdAt)
  }

  object IlluminatedObjectResponse {

    case class Root(id: Int, attributes: JValue)

    def build(s: IlluminatedObject): Root =
      Root(id = s.id, attributes = s.attributes)
  }

  object ObjectSchemaResponse {
    case class Root(name: String, kind: String, schema: JValue)

    def build(s: ObjectFullSchema): Root =
      Root(name = s.name, kind = s.kind, schema = s.schema)

  }
}
