package models.activity

import cats.data.ValidatedNel
import failures.Failure
import shapeless._
import slick.lifted.Tag
import utils.Validation
import utils.Validation._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

/**
  * An activity dimension has a set of activity trails. It is used as a logical grouping
  * of trails by some 'kind' of activity. A particular activity can be in multiple dimensions
  * at a time.
  */
case class Dimension(id: Int = 0, name: String, description: String)
    extends FoxModel[Dimension]
    with Validation[Dimension] {

  val nameRegex = """([a-zA-Z0-9-_]*)""".r

  override def validate: ValidatedNel[Failure, Dimension] = {
    matches(name, nameRegex, "name").map { case _ ⇒ this }
  }
}

object Dimension {
  val admin        = "admin"
  val cart         = "cart"
  val coupon       = "coupon"
  val customer     = "customer"
  val giftCard     = "giftCard"
  val notification = "notification"
  val order        = "order"
  val product      = "product"
  val promotion    = "promotion"
  val rma          = "return"
  val sku          = "sku"
}

class Dimensions(tag: Tag) extends FoxTable[Dimension](tag, "activity_dimensions") {
  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name        = column[String]("name")
  def description = column[String]("description")

  def * = (id, name, description) <> ((Dimension.apply _).tupled, Dimension.unapply)
}

object Dimensions
    extends FoxTableQuery[Dimension, Dimensions](new Dimensions(_))
    with ReturningId[Dimension, Dimensions] {

  val returningLens: Lens[Dimension, Int] = lens[Dimension].id

  def findByName(name: String): QuerySeq = filter(_.name === name)

  def findOrCreateByName(name: String)(implicit ec: EC): DbResultT[Dimension] =
    findByName(name).one.dbresult.flatMap {
      case Some(dimension) ⇒ DbResultT.good(dimension)
      case None            ⇒ create(Dimension(name = name, description = name.capitalize))
    }
}
