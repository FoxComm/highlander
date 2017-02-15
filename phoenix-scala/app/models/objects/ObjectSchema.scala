package models.objects

import java.time.Instant

import org.json4s.JsonAST.JObject
import shapeless._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.{JsonFormatters, Validation}

/**
  Represent json-schema for views: ObjectForm applied to ObjectShadow.
  */
case class ObjectSchema(id: Int = 0,
                        contextId: Int,
                        kind: String,
                        name: String,
                        dependencies: List[String],
                        schema: Json,
                        createdAt: Instant = Instant.now)
    extends FoxModel[ObjectSchema]
    with Validation[ObjectSchema]

class ObjectSchemas(tag: Tag) extends FoxTable[ObjectSchema](tag, "object_schemas") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def contextId    = column[Int]("context_id")
  def name         = column[String]("name")
  def kind         = column[String]("kind")
  def dependencies = column[List[String]]("dependencies")
  def schema       = column[Json]("schema")
  def createdAt    = column[Instant]("created_at")

  def * =
    (id, contextId, name, kind, dependencies, schema, createdAt) <> ((ObjectSchema.apply _).tupled, ObjectSchema.unapply)
}

object ObjectSchemas
    extends FoxTableQuery[ObjectSchema, ObjectSchemas](new ObjectSchemas(_))
    with ReturningId[ObjectSchema, ObjectSchemas] {

  val returningLens: Lens[ObjectSchema, Int] = lens[ObjectSchema].id

  implicit val formats = JsonFormatters.phoenixFormats
}

/**
  ObjectFullSchema represent ObjectSchema which embed all dependencies schemas to definitions
  See more about definitions and references at http://json-schema.org/latest/json-schema-core.html#anchor26
  ObjectFullSchemas automatically generates from ObjectSchemas by postgres triggers.
  */
case class ObjectFullSchema(id: Int = 0,
                            contextId: Int,
                            name: String,
                            kind: String,
                            schema: Json,
                            createdAt: Instant = Instant.now)
    extends FoxModel[ObjectFullSchema]
    with Validation[ObjectFullSchema]

object ObjectFullSchema {
  def emptySchema =
    ObjectFullSchema(contextId = 0, name = "empty", kind = "empty", schema = JObject())
}

class ObjectFullSchemas(tag: Tag) extends FoxTable[ObjectFullSchema](tag, "object_full_schemas") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def contextId = column[Int]("context_id")
  def name      = column[String]("name")
  def kind      = column[String]("kind")
  def schema    = column[Json]("schema")
  def createdAt = column[Instant]("created_at")

  def * =
    (id, contextId, name, kind, schema, createdAt) <> ((ObjectFullSchema.apply _).tupled, ObjectFullSchema.unapply)
}

object ObjectFullSchemas
    extends FoxTableQuery[ObjectFullSchema, ObjectFullSchemas](new ObjectFullSchemas(_))
    with ReturningId[ObjectFullSchema, ObjectFullSchemas] {

  val returningLens: Lens[ObjectFullSchema, Int] = lens[ObjectFullSchema].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByKind(kind: String): QuerySeq =
    filter(_.kind === kind)

  def mustFindByName404(name: String)(implicit ec: EC): DbResultT[ObjectFullSchema] =
    filter(_.name === name)
      .mustFindOneOr(failures.NotFoundFailure404(ObjectFullSchemas, "name", name))

  def findOneByName(name: String): DBIO[Option[ObjectFullSchema]] =
    filter(_.name === name).one

  // test method
  def getDefaultOrEmptySchemaForForm(form: ObjectForm)(implicit ec: EC) = {
    findOneByName(form.kind).map { o ⇒
      o.getOrElse(ObjectFullSchema.emptySchema)
    }
  }
}
