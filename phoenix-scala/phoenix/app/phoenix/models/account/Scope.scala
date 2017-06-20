package phoenix.models.account

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.failures._
import phoenix.failures.ScopeFailures._
import phoenix.failures.UserFailures.OrganizationNotFoundByName
import phoenix.utils.aliases._
import shapeless._
import slick.jdbc.PostgresProfile.api._
import core.db._

case class Scope(id: Int = 0, source: String, parentPath: Option[String]) extends FoxModel[Scope] {
  lazy val path: String = parentPath match {
    case Some(pp) ⇒ if (pp.isEmpty) id.toString else s"$pp.$id"
    case None     ⇒ id.toString
  }
  lazy val ltree: LTree = LTree(path)
}

object Scope {

  def current(implicit au: AU): LTree = LTree(au.token.scope)

  def resolveOverride(maybeSubscope: Option[String] = None)(implicit ec: EC, au: AU): DbResultT[LTree] =
    overwrite(au.token.scope, maybeSubscope)

  def overwrite(scope: String, maybeSubscope: Option[String])(implicit ec: EC): DbResultT[LTree] =
    DbResultT.fromEither(scopeOrSubscope(scope, maybeSubscope)).map(LTree(_))

  private def scopeOrSubscope(scope: String, maybeSubscope: Option[String]): Either[Failures, String] =
    maybeSubscope match {
      case _ if scope.isEmpty ⇒ Either.left(EmptyScope.single)
      case Some(subscope)     ⇒ validateSubscope(scope, subscope)
      case None               ⇒ Either.right(scope)
    }

  // A subscope is a child if the scope is a prefix match and where it matches
  // is a '.' since scopes are separated by period characters.
  private def validateSubscope(scope: String, maybeSubscope: String): Either[Failures, String] =
    if (scope.equals(maybeSubscope) || maybeSubscope.startsWith(scope + "."))
      Either.right(maybeSubscope)
    else
      Either.left(InvalidSubscope(scope, maybeSubscope).single)
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

  def forOrganization(org: String)(implicit ec: EC, db: DB): DbResultT[LTree] =
    for {
      organization ← * <~ Organizations.findByName(org).mustFindOr(OrganizationNotFoundByName(org))
      scope        ← * <~ Scopes.mustFindById400(organization.scopeId)
    } yield LTree(scope.path)
}
