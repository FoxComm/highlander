package utils

import slick.ast.{TableExpansion, Filter, TableNode, Path, LiteralNode, Node}
import slick.lifted.Query
import utils.Strings._

case class QueryErrorInfo(modelType: String, searchKey: Any, searchTerm: String)

object QueryErrorInfo {

  def build(tableName: String, searchKey: Any, searchTerm: String): QueryErrorInfo =
    QueryErrorInfo(
      modelType = tableName.underscoreToCamel.dropRight(1),
      searchKey = searchKey,
      searchTerm = searchTerm.underscoreToCamel)

  def forQuery[M, U, C[_]](query: Query[M, U, C]): QueryErrorInfo = {
    val node = query.toNode

    val firstFilterChildren = findFirstFilter(node).nodeChildren
    val applyNode = firstFilterChildren.tail.head.nodeChildren
    val searchKey = applyNode.collect { case ln @ LiteralNode(_) ⇒ ln.value }.head
    val searchTerm = applyNode.collect { case Path(term :: _) ⇒ term.name }.head

    val tableExpansion = firstFilterChildren.head.nodeChildren
    val tableName = tableExpansion.collect { case TableNode(_, name, _, _, _) ⇒ name }.head

    QueryErrorInfo.build(
      tableName = tableName,
      searchKey = searchKey,
      searchTerm = searchTerm)
  }

  private def findFirstFilter(node: Node): Node = node.nodeChildren.head match {
    case filter @ Filter(_, _, _) ⇒ findFirstFilter(filter)
    case TableExpansion(_, _, _)  ⇒ node
  }
}
