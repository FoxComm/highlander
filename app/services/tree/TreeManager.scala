package services.tree

import cats.data._
import com.github.tminglei.slickpg.LTree
import failures.DatabaseFailure
import failures.TreeFailures._
import models.objects._
import models.tree._
import payloads.GenericTreePayloads._
import responses.GenericTreeResponses.FullTreeResponse._
import responses.GenericTreeResponses._
import services.Result
import services.objects.ObjectManager
import utils.aliases._
import utils.db.DbResultT._
import utils.db.ExPostgresDriver.api._
import utils.db._

object TreeManager {

  def getFullTree(treeName: String, contextName: String)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      context  ← * <~ ObjectManager.mustFindByName404(contextName)
      response ← * <~ getFullTree(treeName, context)
    } yield response).run()

  def updateTree(treeName: String,
                 contextName: String,
                 newTree: NodePayload,
                 path: Option[String] = None)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      tree    ← * <~ getOrCreateDbTree(treeName, context, path.isEmpty)
      ltreePath = path.map(LTree.apply)
      deleted ← * <~ GenericTreeNodes.findNodes(tree.id, ltreePath).delete
      //subtree should exist
      _ ← * <~ (if (ltreePath.isDefined && deleted == 0)
                  DbResult.failure(TreeNotFound(treeName, contextName, path.get))
                else DbResult.unit)
      usedIndexes ← * <~ GenericTreeNodes.getUsedIndexes(tree.id).result
      (dbTree, _) = payloadToDbTree(tree.id,
                                    newTree,
                                    Stream.from(1).filter(!usedIndexes.contains(_)),
                                    ltreePath.map(_.value.init).getOrElse(List()))
      _          ← * <~ GenericTreeNodes.createAllReturningIds(dbTree)
      resultTree ← * <~ getFullTree(treeName, context)
    } yield resultTree).runTxn()

  def moveNode(treeName: String, contextName: String, moveSpec: MoveNodePayload)(
      implicit ec: EC,
      db: DB): Result[Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      tree    ← * <~ getOrCreateDbTree(treeName, context)
      newChildNode ← * <~ GenericTreeNodes
                      .findNodesByIndex(tree.id, moveSpec.child)
                      .mustFindOneOr(TreeNodeNotFound(treeName, contextName, moveSpec.child))
      shouldBeDeleted = moveSpec.index.isEmpty
      _ ← * <~ (if (shouldBeDeleted)
                  DbResultT.right(
                      GenericTreeNodes.deleteById(
                          newChildNode.id,
                          DbResult.unit,
                          _ ⇒
                            DatabaseFailure(
                                s"cannot delete node: index=${newChildNode.index}, tree=$treeName, context=$contextName")))
                else moveNode(tree.id, moveSpec.index.get, newChildNode))
      resultTree ← * <~ getFullTree(treeName, context)
    } yield resultTree).runTxn()

  def editNode(treeName: String, contextName: String, path: String, newValues: NodeValuesPayload)(
      implicit ec: EC,
      db: DB) =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      tree    ← * <~ getByNameAndContext(treeName, context)
      node ← * <~ GenericTreeNodes
              .findNodesByPath(tree.id, LTree(path))
              .mustFindOneOr(TreeNodeNotFound(treeName, contextName, path))
      _ ← * <~ GenericTreeNodes
           .update(node, node.copy(kind = newValues.kind, objectId = newValues.objectId))
      result ← * <~ getFullTree(treeName, context)
    } yield result).runTxn()

  def getByNameAndContext(name: String, context: ObjectContext)(implicit ec: EC) =
    GenericTrees
      .filterByNameAndContext(name, context.id)
      .mustFindOneOr(TreeNotFound(name, context.name))

  private def getFullTree(treeName: String, context: ObjectContext)(implicit ec: EC,
                                                                    db: DB): DbResultT[Root] =
    for {
      tree          ← * <~ getByNameAndContext(treeName, context)
      nodes         ← * <~ GenericTreeNodes.findNodes(tree.id).result
      maybeResponse ← * <~ TreeResponse.build(tree, nodes)
      response      ← * <~ Xor.fromOption(maybeResponse, TreeNotFound(treeName, context.name).single)
      fullTreeResponse = build(
          response,
          /*TODO: replace this with values returned by objectService when object service be implemented*/ List(
              ))
    } yield fullTreeResponse

  private def getOrCreateDbTree(
      treeName: String,
      context: ObjectContext,
      createIfNotFound: Boolean = false)(implicit ec: EC, db: DB): DbResultT[GenericTree] =
    for {
      maybeTree ← * <~ GenericTrees.filterByNameAndContext(treeName, context.id).result
      // TODO @anna: #longlivedbresultt
      ifEmptyAction = if (createIfNotFound)
        GenericTrees.create(GenericTree(0, treeName, context.id)).value
      else DbResult.failure(TreeNotFound(treeName, context.name))
      tree ← * <~ (if (maybeTree.isEmpty) ifEmptyAction else DbResult.good(maybeTree.head))
    } yield tree

  private def moveNode(treeId: Int, parentIndex: Int, childNode: GenericTreeNode)(
      implicit ec: EC,
      db: DB): DbResultT[Int] =
    for {
      parentNode ← * <~ GenericTreeNodes
                    .findNodesByIndex(treeId, parentIndex)
                    .mustFindOneOr(TreeNodeNotFound(treeId, parentIndex))
      _ ← * <~ (if (parentNode.path.value.contains(childNode.index.toString))
                  DbResult.failure(ParentChildSwapFailure(parentNode.index, childNode.index))
                else DbResult.none)

      parentPath    = parentNode.path.toString()
      patternLength = childNode.path.value.size - 1
      childPath     = childNode.path.toString()
      updated ← * <~ sqlu"""update generic_tree_nodes as t
               set path = (text2ltree($parentPath) || subpath(t.path, $patternLength))
               where t.tree_id=$treeId and t.path <@ text2ltree($childPath)"""
    } yield updated

  private def payloadToDbTree(treeId: Int,
                              tree: NodePayload,
                              indexes: Stream[Int],
                              parentPath: List[String]): (List[GenericTreeNode], Stream[Int]) = {

    val nodeIndex: Int = indexes.head
    val nodePath       = parentPath ::: List(nodeIndex.toString)

    var children: List[GenericTreeNode] = List()
    var childrenIndex                   = indexes.tail

    for (child ← tree.children) {
      val (newChild: List[GenericTreeNode], indexes: Stream[Int]) =
        payloadToDbTree(treeId, child, childrenIndex, nodePath)
      children = newChild ++ children
      childrenIndex = indexes
    }
    (GenericTreeNode(0, treeId, nodeIndex, LTree(nodePath), tree.kind, tree.objectId) :: children,
     childrenIndex)
  }
}
