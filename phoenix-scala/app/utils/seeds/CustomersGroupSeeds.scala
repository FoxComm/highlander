package utils.seeds

import com.github.tminglei.slickpg.LTree
import io.circe.Json
import models.account.Scopes
import models.customer.CustomerGroup.Dynamic
import models.customer._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.aliases._
import utils.db._

trait CustomersGroupSeeds {

  type Groups = (CustomerGroup#Id, CustomerGroup#Id)

  def fakeJson: Json = Json.obj()

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
