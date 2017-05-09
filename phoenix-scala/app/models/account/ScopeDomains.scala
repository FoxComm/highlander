package models.account

import shapeless._
import slick.jdbc.PostgresProfile.api._
import utils.db._

case class ScopeDomain(id: Int = 0, scopeId: Int, domain: String) extends FoxModel[ScopeDomain]

class ScopeDomains(tag: Tag) extends FoxTable[ScopeDomain](tag, "scope_domains") {
  def id      = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scopeId = column[Int]("scope_id")
  def domain  = column[String]("domain")

  def * =
    (id, scopeId, domain) <> ((ScopeDomain.apply _).tupled, ScopeDomain.unapply)

  def scope = foreignKey(Scopes.tableName, scopeId, Scopes)(_.id)
}

object ScopeDomains
    extends FoxTableQuery[ScopeDomain, ScopeDomains](new ScopeDomains(_))
    with ReturningId[ScopeDomain, ScopeDomains] {

  val returningLens: Lens[ScopeDomain, Int] = lens[ScopeDomain].id

  def findByScopeId(scopeId: Int): QuerySeq =
    filter(_.scopeId === scopeId)

  def findByDomain(domain: String): QuerySeq =
    filter(_.domain === domain)
}
