package utils

import slick.ast.{Apply, Filter, LiteralNode, Node, Path}
import slick.lifted.Query
import utils.Strings._

case class QueryErrorInfo(modelType: String, searchKey: Any, searchTerm: String)

object QueryErrorInfo {

  def build(tableName: String, searchKey: Any, searchTerm: String): QueryErrorInfo =
    QueryErrorInfo(
      modelType = tableName.underscoreToCamel.dropRight(1),
      searchKey = searchKey,
      searchTerm = searchTerm)

  def searchKeyForQuery[M, U, C[_]](query: Query[M, U, C], primarySearchTerm: String): Option[Any] = {
    findSearchKey(query.toNode, primarySearchTerm)
  }

  private def findSearchKey(node: Node, searchTerm: String): Option[Any] = node match {
    case Filter(_, from, where) ⇒
      val key = findSearchKey(where, searchTerm)
      if (key.isDefined) key else findSearchKey(from, searchTerm)
    case a @ Apply(_, children) ⇒ children.toList match {
      case (Path(term :: _)) :: (l: LiteralNode) :: Nil if term.name.underscoreToCamel == searchTerm ⇒ Some(l.value)
      case _ ⇒ None
    }
  }
}
