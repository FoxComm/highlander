package models.objects

import java.time.Instant

import shapeless._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.{JsonFormatters, Validation}

/**
  Represent json-schema for views: ObjectForm applied to ObjectShadow.
  */
case class ObjectSchema(id: Int = 0,
                        name: String,
                        dependencies: List[String],
                        schema: Json,
                        createdAt: Instant = Instant.now)
    extends FoxModel[ObjectSchema]
    with Validation[ObjectSchema]

class ObjectSchemas(tag: Tag) extends FoxTable[ObjectSchema](tag, "object_schemas") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name         = column[String]("name")
  def dependencies = column[List[String]]("dependencies")
  def schema       = column[Json]("schema")
  def createdAt    = column[Instant]("created_at")

  def * =
    (id, name, dependencies, schema, createdAt) <> ((ObjectSchema.apply _).tupled, ObjectSchema.unapply)
}

object ObjectSchemas
    extends FoxTableQuery[ObjectSchema, ObjectSchemas](new ObjectSchemas(_))
    with ReturningId[ObjectSchema, ObjectSchemas] {

  val returningLens: Lens[ObjectSchema, Int] = lens[ObjectSchema].id

  implicit val formats = JsonFormatters.phoenixFormats
}

case class ObjectFullSchema(id: Int = 0,
                            name: String,
                            schema: Json,
                            createdAt: Instant = Instant.now)
    extends FoxModel[ObjectFullSchema]
    with Validation[ObjectFullSchema]

class ObjectFullSchemas(tag: Tag) extends FoxTable[ObjectFullSchema](tag, "object_full_schemas") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name      = column[String]("name")
  def schema    = column[Json]("schema")
  def createdAt = column[Instant]("created_at")

  def * =
    (id, name, schema, createdAt) <> ((ObjectFullSchema.apply _).tupled, ObjectFullSchema.unapply)
}

object ObjectFullSchemas
    extends FoxTableQuery[ObjectFullSchema, ObjectFullSchemas](new ObjectFullSchemas(_))
    with ReturningId[ObjectFullSchema, ObjectFullSchemas] {

  val returningLens: Lens[ObjectFullSchema, Int] = lens[ObjectFullSchema].id

  implicit val formats = JsonFormatters.phoenixFormats

  def findByName(name: String): DBIO[Option[ObjectFullSchema]] =
    filter(_.name === name).one
}
