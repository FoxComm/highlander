package phoenix.models.tree

import com.github.tminglei.slickpg._
import core.db.ExPostgresDriver.api._
import core.db._
import core.utils.Validation
import phoenix.utils.JsonFormatters
import shapeless._
import slick.lifted.Tag

case class GenericTreeNode(id: Int, treeId: Int, index: Int, path: LTree, kind: String, objectId: Int)
    extends FoxModel[GenericTreeNode]
    with Validation[GenericTreeNode]

class GenericTreeNodes(tag: Tag) extends FoxTable[GenericTreeNode](tag, "generic_tree_nodes") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def treeId   = column[Int]("tree_id")
  def index    = column[Int]("index")
  def path     = column[LTree]("path")
  def kind     = column[String]("kind")
  def objectId = column[Int]("object_id")

  def * =
    (id, treeId, index, path, kind, objectId) <>
      ((GenericTreeNode.apply _).tupled, GenericTreeNode.unapply)
}

object GenericTreeNodes
    extends FoxTableQuery[GenericTreeNode, GenericTreeNodes](new GenericTreeNodes(_))
    with ReturningId[GenericTreeNode, GenericTreeNodes] {
  val returningLens: Lens[GenericTreeNode, Int] = lens[GenericTreeNode].id

  implicit val formats = JsonFormatters.phoenixFormats

  def findNodes(treeId: Int, path: Option[LTree] = None): QuerySeq = {
    val allNodesOfTree = filter(_.treeId === treeId)
    path.fold(allNodesOfTree)(path ⇒ allNodesOfTree.filter(_.path ~ (path.toString + ".*")))
  }

  def getUsedIndexes(treeId: Int)(implicit ec: EC): Query[Rep[Int], Int, scala.Seq] =
    findNodes(treeId).map(_.index)

  def findNodesByIndex(treeId: Int, index: Int)(implicit ec: EC): QuerySeq =
    filter(node ⇒ node.index === index && node.treeId === treeId)

  def findNodesByPath(treeId: Int, path: LTree)(implicit ec: EC): QuerySeq =
    filter(node ⇒ node.path === path && node.treeId === treeId)
}
