package models.objects

import io.circe._
import java.time.Instant
import shapeless._
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

/**
  * An ObjectShadow is what you get when a context illuminates an Object.
  * The ObjectShadow, when applied to a Object is what is displayed on the
  * storefront.
  */
case class ObjectShadow(id: Int = 0,
                        formId: Int = 0,
                        jsonSchema: Option[String] = None,
                        attributes: Json,
                        createdAt: Instant = Instant.now)
    extends FoxModel[ObjectShadow]
    with Validation[ObjectShadow]

object ObjectShadow {
  def fromPayload(attributes: Map[String, Json]): ObjectShadow = {
    val attributesJson = attributes.foldLeft(JsonObject.empty) {
      case (acc, (key, value)) ⇒
        // TODO: Clean this up and make a case class to represent the shadow ref.

        // FIXME KJ: yolo is not justified case here,
        // but I had really no idea what should be default value when "t" is not found
        // let it reproduce previous behaviour and default to json array then ;-)

        val shadowJson = Json.obj("type" → (value \ "t"), "ref" → Json.fromString(key))
        acc.add(key, shadowJson)
    }

    ObjectShadow(attributes = Json.fromJsonObject(attributesJson))
  }
}

class ObjectShadows(tag: Tag) extends FoxTable[ObjectShadow](tag, "object_shadows") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def formId     = column[Int]("form_id")
  def jsonSchema = column[Option[String]]("json_schema")
  def attributes = column[Json]("attributes")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, formId, jsonSchema, attributes, createdAt) <> ((ObjectShadow.apply _).tupled, ObjectShadow.unapply)

  def form   = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
  def schema = foreignKey(ObjectFullSchemas.tableName, jsonSchema, ObjectFullSchemas)(_.name.?)
}

object ObjectShadows
    extends FoxTableQuery[ObjectShadow, ObjectShadows](new ObjectShadows(_))
    with ReturningId[ObjectShadow, ObjectShadows] {

  val returningLens: Lens[ObjectShadow, Int] = lens[ObjectShadow].id

  def filterByForm(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def filterByAttributes(key: String, value: String): QuerySeq =
    filter(_.attributes +>> key === value)
}
