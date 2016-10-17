package models.objects

import java.time.Instant

//import org.json4s._
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import shapeless._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.{JsonFormatters, Validation}

/**
  * A ObjectShadow is what you get when a context illuminates a Object.
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
    val attributesJson = attributes.foldLeft(JNothing: JValue) {
      case (acc, (key, value)) ⇒
        // TODO: Clean this up and make a case class to represent the shadow ref.
        val shadowJson: JValue = key → (("type" → (value \ "t")) ~ ("ref" → key))
        acc.merge(shadowJson)
    }

    ObjectShadow(attributes = attributesJson)
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

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByForm(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def filterByAttributes(key: String, value: String): QuerySeq =
    filter(_.attributes +>> key === value)
}
