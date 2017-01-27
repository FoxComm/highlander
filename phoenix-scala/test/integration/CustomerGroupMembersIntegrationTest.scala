import akka.http.scaladsl.model.StatusCodes
import com.github.tminglei.slickpg.LTree
import failures.NotFoundFailure404
import models.account._
import models.customer._
import org.scalatest.mockito.MockitoSugar
import payloads.CustomerGroupPayloads.CustomerGroupMemberSyncPayload
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories
import cats.implicits._
import utils.db.ExPostgresDriver.api._

class CustomerGroupMembersIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockitoSugar
    with BakedFixtures {

  "POST /v1/service/customer-groups/users" - {

    "handles users" in new Fixture {
      val payload = CustomerGroupMemberSyncPayload(Seq(account2.id, account3.id))

      customerGroupsMembersApi(group.id)
        .syncCustomers(payload)
        .mustHaveStatus(StatusCodes.NoContent)

      val updatedMemberships =
        CustomerGroupMembers.findByGroupId(group.id).gimme.map(_.customerDataId)

      withClue(s"Customer ${account1.id} was not deleted from group member list: ") {
        (updatedMemberships.contains(custData1.id)) must === (false)
      }

      withClue(s"Customer ${account2.id} was deleted from group member list: ") {
        (updatedMemberships.contains(custData2.id)) must === (true)
      }

      withClue(s"Customer ${account3.id} was not added to group member list: ") {
        (updatedMemberships.contains(custData3.id)) must === (true)
      }
    }

    "404 if group not found" in new Fixture {
      customerGroupsMembersApi(666)
        .syncCustomers(CustomerGroupMemberSyncPayload(Seq.empty))
        .mustFailWith404(NotFoundFailure404(CustomerDynamicGroup, 666))
    }
  }

  trait Fixture extends StoreAdmin_Seed {

    val scope = LTree("1")

    val (group, account1, custData1, account2, custData2, account3, custData3) = (for {
      group ← * <~ CustomerGroups.create(Factories.group(scope))

      account1 ← * <~ Accounts.create(Account())
      user1    ← * <~ Users.create(Factories.customer.copy(accountId = account1.id))
      custData1 ← * <~ CustomersData.create(
                     CustomerData(userId = user1.id, accountId = account1.id, scope = scope))

      account2 ← * <~ Accounts.create(Account())
      user2    ← * <~ Users.create(Factories.customer.copy(accountId = account2.id))
      custData2 ← * <~ CustomersData.create(
                     CustomerData(userId = user2.id, accountId = account2.id, scope = scope))

      account3 ← * <~ Accounts.create(Account())
      user3    ← * <~ Users.create(Factories.customer.copy(accountId = account3.id))
      custData3 ← * <~ CustomersData.create(
                     CustomerData(userId = user3.id, accountId = account3.id, scope = scope))

      _ ← * <~ CustomerGroupMembers.create(
             CustomerGroupMember(groupId = group.id, customerDataId = custData1.id))
      _ ← * <~ CustomerGroupMembers.create(
             CustomerGroupMember(groupId = group.id, customerDataId = custData2.id))

    } yield (group, account1, custData1, account2, custData2, account3, custData3)).gimmeTxn
  }

}
