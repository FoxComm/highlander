package objectframework.models

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.NotFoundFailure404
import core.utils.Validation
import org.json4s.{JObject, JValue}
import shapeless._

/**
  Represent json-schema for views: ObjectForm applied to ObjectShadow.
  */
case class ObjectSchema(id: Int = 0,
                        kind: String,
                        name: String,
                        dependencies: List[String],
                        schema: JValue,
                        createdAt: Instant = Instant.now)
    extends FoxModel[ObjectSchema]
    with Validation[ObjectSchema]

class ObjectSchemas(tag: Tag) extends FoxTable[ObjectSchema](tag, "object_schemas") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name         = column[String]("name")
  def kind         = column[String]("kind")
  def dependencies = column[List[String]]("dependencies")
  def schema       = column[JValue]("schema")
  def createdAt    = column[Instant]("created_at")

  def * =
    (id, name, kind, dependencies, schema, createdAt) <> ((ObjectSchema.apply _).tupled, ObjectSchema.unapply)
}

object ObjectSchemas
    extends FoxTableQuery[ObjectSchema, ObjectSchemas](new ObjectSchemas(_))
    with ReturningId[ObjectSchema, ObjectSchemas] {

  val returningLens: Lens[ObjectSchema, Int] = lens[ObjectSchema].id

  def findOneByName(name: String): DBIO[Option[ObjectSchema]] =
    filter(_.name === name).one

}

/**
  ObjectFullSchema represent ObjectSchema which embed all dependencies schemas to definitions
  See more about definitions and references at http://json-schema.org/latest/json-schema-core.html#anchor26
  ObjectFullSchemas automatically generates from ObjectSchemas by postgres triggers.
  */
case class ObjectFullSchema(id: Int = 0,
                            name: String,
                            kind: String,
                            schema: JValue,
                            createdAt: Instant = Instant.now)
    extends FoxModel[ObjectFullSchema]
    with Validation[ObjectFullSchema]

object ObjectFullSchema {
  def emptySchema = ObjectFullSchema(name = "empty", kind = "empty", schema = JObject())
}

class ObjectFullSchemas(tag: Tag) extends FoxTable[ObjectFullSchema](tag, "object_full_schemas") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name      = column[String]("name")
  def kind      = column[String]("kind")
  def schema    = column[JValue]("schema")
  def createdAt = column[Instant]("created_at")

  def * =
    (id, kind, name, schema, createdAt) <> ((ObjectFullSchema.apply _).tupled, ObjectFullSchema.unapply)
}

object ObjectFullSchemas
    extends FoxTableQuery[ObjectFullSchema, ObjectFullSchemas](new ObjectFullSchemas(_))
    with ReturningId[ObjectFullSchema, ObjectFullSchemas] {

  val returningLens: Lens[ObjectFullSchema, Int] = lens[ObjectFullSchema].id

  def filterByKind(kind: String): QuerySeq =
    filter(_.kind === kind)

  def mustFindByName404(name: String)(implicit ec: EC): DbResultT[ObjectFullSchema] =
    filter(_.name === name).mustFindOneOr(NotFoundFailure404(ObjectFullSchemas, "name", name))

  def findOneByName(name: String): DBIO[Option[ObjectFullSchema]] =
    filter(_.name === name).one

  // test method
  def getDefaultOrEmptySchemaForForm(form: ObjectForm)(implicit ec: EC): DBIO[ObjectFullSchema] =
    findOneByName(form.kind).map { o ⇒
      o.getOrElse(ObjectFullSchema.emptySchema)
    }
}
