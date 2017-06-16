package phoenix.utils.seeds

import com.github.tminglei.slickpg.LTree
import core.db._
import org.json4s.JObject
import phoenix.models.account.Scopes
import phoenix.models.customer.CustomerGroup.Dynamic
import phoenix.models.customer._
import phoenix.utils.aliases._

import scala.concurrent.ExecutionContext.Implicits.global

trait CustomersGroupSeeds {

  type Groups = (CustomerGroup#Id, CustomerGroup#Id)

  def fakeJson = JObject()

  def createGroups(scopeId: Int)(implicit db: DB, ac: AC): DbResultT[Groups] =
    for {
      scope  ← * <~ Scopes.mustFindById400(scopeId)
      groups ← * <~ CustomerGroups.createAllReturningIds(groups(scope.ltree))
    } yield
      groups.toList match {
        case c1 :: c2 :: Nil ⇒ (c1, c2)
        case _               ⇒ ???
      }

  def group1(scope: LTree) =
    CustomerGroup(name = "Super awesome group",
                  scope = scope,
                  clientState = fakeJson,
                  createdBy = 1,
                  elasticRequest = fakeJson,
                  customersCount = 500,
                  groupType = Dynamic)

  def group2(scope: LTree) =
    CustomerGroup(name = "Top 10%",
                  scope = scope,
                  clientState = fakeJson,
                  createdBy = 1,
                  elasticRequest = fakeJson,
                  customersCount = 200,
                  groupType = Dynamic)

  def groups(scope: LTree): Seq[CustomerGroup] = Seq(group1(scope), group2(scope))

  def group(scope: LTree): CustomerGroup = group1(scope)
}
