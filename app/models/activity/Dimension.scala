package models.activity

import cats.data.ValidatedNel
import monocle.macros.GenLens
import services.Failure
import utils.ExPostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.Validation._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import utils.aliases._

/**
 * An activity dimension has a set of activity trails. It is used as a logical grouping
 * of trails by some 'kind' of activity. A particular activity can be in multiple dimensions 
 * at a time.
 */
final case class Dimension(
  id: Int = 0, 
  name: String, 
  description: String)
  extends ModelWithIdParameter[Dimension]
  with Validation[Dimension] {

    val nameRegex = """([a-zA-Z0-9-_]*)""".r

    override def validate: ValidatedNel[Failure, Dimension] = {
      matches(name, nameRegex, "name").map{case _ ⇒  this}
    }

  }

object Dimension {
  val order         = "order"
  val customer      = "customer"
  val admin         = "admin"
  val notification  = "notification"
  val giftCard      = "giftCard"
  val rma           = "rma"
}

class Dimensions(tag: Tag) extends GenericTable.TableWithId[Dimension](tag, "activity_dimensions")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")

  def * = (id, name, description) <> ((Dimension.apply _).tupled, Dimension.unapply)
}

object Dimensions extends TableQueryWithId[Dimension, Dimensions](
  idLens = GenLens[Dimension](_.id))(new Dimensions(_)) {

    def findByName(name: String) : QuerySeq = filter(_.name === name) 

  def findOrCreateByName(name: String)(implicit ec: EC): DbResult[Dimension] =
    findByName(name).one.flatMap {
      case Some(dimension) ⇒ DbResult.good(dimension)
      case None ⇒ create(Dimension(name = name, description = name.capitalize))
    }
  }
