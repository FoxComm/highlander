package utils.seeds

import com.github.tminglei.slickpg.LTree
import models.account.Scopes

import scala.concurrent.ExecutionContext.Implicits.global
import models.customer._
import org.json4s.JObject
import utils.aliases._
import utils.db._

trait CustomersGroupSeeds {

  type Groups = (CustomerDynamicGroup#Id, CustomerDynamicGroup#Id)

  def fakeJson = JObject()

  def createGroups(scopeId: Int)(implicit db: DB, ac: AC): DbResultT[Groups] =
    for {
      scope  ← * <~ Scopes.mustFindById400(scopeId)
      groups ← * <~ CustomerDynamicGroups.createAllReturningIds(groups(scope.ltree))
    } yield
      groups.toList match {
        case c1 :: c2 :: Nil ⇒ (c1, c2)
        case _               ⇒ ???
      }

  def group1(scope: LTree) =
    CustomerDynamicGroup(name = "Super awesome group",
                         scope = scope,
                         clientState = fakeJson,
                         createdBy = 1,
                         elasticRequest = fakeJson,
                         customersCount = 500)

  def group2(scope: LTree) =
    CustomerDynamicGroup(name = "Top 10%",
                         scope = scope,
                         clientState = fakeJson,
                         createdBy = 1,
                         elasticRequest = fakeJson,
                         customersCount = 200)

  def groups(scope: LTree): Seq[CustomerDynamicGroup] = Seq(group1(scope), group2(scope))

  def group(scope: LTree): CustomerDynamicGroup = group1(scope)
}
