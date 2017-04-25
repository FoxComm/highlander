package models.tree

import shapeless._
import slick.lifted.Tag
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

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

  def filterByNameAndContext(name: String, contextId: Int): QuerySeq =
    filter(tree ⇒ tree.name === name && tree.contextId === contextId)
}
