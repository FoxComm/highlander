package responses

import java.time.Instant

import models.objects._
import utils.aliases._

object ObjectResponses {

  object ObjectContextResponse {

    case class Root(name: String, attributes: Json)

    def build(c: ObjectContext): Root =
      Root(name = c.name, attributes = c.attributes)

    def build(c: IlluminatedContext): Root =
      Root(name = c.name, attributes = c.attributes)
  }

  object ObjectFormResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant)

    def build(f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object ObjectShadowResponse {

    case class Root(id: Int, formId: Int, attributes: Json, createdAt: Instant)

    def build(s: ObjectShadow): Root =
      Root(id = s.id, formId = s.formId, attributes = s.attributes, createdAt = s.createdAt)
  }

  object IlluminatedObjectResponse {

    case class Root(id: Int, kind: String, attributes: Json)

    def build(s: IlluminatedObject): Root =
      Root(id = s.id, kind = s.kind, attributes = s.attributes)
  }

  object ObjectSchemaResponse {
    case class Root(name: String, kind: String, schema: Json)

    def build(s: ObjectFullSchema): Root =
      Root(name = s.name, kind = s.kind, schema = s.schema)

  }
}
