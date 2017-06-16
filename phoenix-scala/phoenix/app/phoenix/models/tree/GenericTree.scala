package phoenix.models.tree

import core.db.ExPostgresDriver.api._
import core.db._
import core.utils.Validation
import phoenix.utils.JsonFormatters
import shapeless._
import slick.lifted.Tag

case class GenericTree(id: Int, name: String, contextId: Int)
    extends FoxModel[GenericTree]
    with Validation[GenericTree]

class GenericTrees(tag: Tag) extends FoxTable[GenericTree](tag, "generic_tree") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name      = column[String]("name")
  def contextId = column[Int]("context_id")

  def * =
    (id, name, contextId) <>
      ((GenericTree.apply _).tupled, GenericTree.unapply)
}

object GenericTrees
    extends FoxTableQuery[GenericTree, GenericTrees](new GenericTrees(_))
    with ReturningId[GenericTree, GenericTrees] {
  val returningLens: Lens[GenericTree, Int] = lens[GenericTree].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByNameAndContext(name: String, contextId: Int): QuerySeq =
    filter(tree ⇒ tree.name === name && tree.contextId === contextId)
}
