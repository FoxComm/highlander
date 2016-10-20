package models.account

import com.github.tminglei.slickpg.LTree
import failures.ScopeFailures._
import failures._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class Scope(id: Int = 0, source: String, parentPath: Option[String]) extends FoxModel[Scope] {
  def path = parentPath match {
    case Some(pp) ⇒ if (pp.isEmpty) s"$id" else s"$pp.$id"
    case None     ⇒ s"$id"
  }
}

object Scope {

  def current(implicit au: AU): LTree = LTree(au.token.scope)

  def getScopeOrSubscope(potentialSubscope: Option[String])(implicit ec: EC,
                                                            au: AU): DbResultT[LTree] = {
    val scope = au.token.scope
    scopeOrSubscope(scope, potentialSubscope) match {
      case Some(scope) ⇒ DbResultT.good(LTree(scope))
      case None        ⇒ DbResultT.failures(ImproperScope.single)
    }
  }

  //Will return None if there is a problem, otherwise you will get the subscope if
  //the subscope is specified and it is a proper subscope of the scope specified,
  //or if the subscope is not specified then the scope is returned.
  def scopeOrSubscope(scope: String, potentialSubscope: Option[String]): Option[String] =
    if (scope.isEmpty) None
    else
      potentialSubscope match {
        case Some(subscope) ⇒ subscopeIfScopeOrChild(scope, subscope)
        case None           ⇒ Some(scope)
      }

  //Return subscope if subscope is really a child of the scope specified.
  def subscopeIfScopeOrChild(scope: String, potentialSubscope: String): Option[String] =
    if (isScopeOrChild(scope, potentialSubscope)) Some(potentialSubscope) else None

  //A subscope is a child if the scope is a prefix match and where it matches
  //is a '.' since scopes are seperated by period characters.
  def isScopeOrChild(scope: String, possibleSubscope: String): Boolean =
    scope.equals(possibleSubscope) ||
      (possibleSubscope.startsWith(scope) && possibleSubscope.charAt(scope.length) == '.')
}

class Scopes(tag: Tag) extends FoxTable[Scope](tag, "scopes") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def source     = column[String]("source")
  def parentPath = column[Option[String]]("parent_path")

  def * =
    (id, source, parentPath) <> ((Scope.apply _).tupled, Scope.unapply)
}

object Scopes extends FoxTableQuery[Scope, Scopes](new Scopes(_)) with ReturningId[Scope, Scopes] {

  val returningLens: Lens[Scope, Int] = lens[Scope].id
}
