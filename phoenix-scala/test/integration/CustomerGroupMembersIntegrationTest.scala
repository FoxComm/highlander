import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import com.github.tminglei.slickpg.LTree
import failures.NotFoundFailure404
import models.account._
import models.customer._
import org.scalatest.mockito.MockitoSugar
import payloads.CustomerGroupPayloads._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories
import cats.implicits._
import failures.CustomerGroupFailures._
import models.customer.CustomerGroup._
import responses.CustomerResponse.Root
import utils.db.ExPostgresDriver.api._

class CustomerGroupMembersIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockitoSugar
    with BakedFixtures {

  val scope = LTree("1")

  "POST /v1/service/customer-groups/users" - {

    "handles users" in new Fixture {
      val payload = CustomerGroupMemberServiceSyncPayload(Seq(account2.id, account3.id))

      customerGroupsMembersServiceApi(group.id)
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
      customerGroupsMembersServiceApi(666)
        .syncCustomers(CustomerGroupMemberServiceSyncPayload(Seq.empty))
        .mustFailWith404(NotFoundFailure404(CustomerGroup, 666))
    }

    "400 if group is manual" in new Fixture {
      customerGroupsMembersServiceApi(manualGroup.id)
        .syncCustomers(CustomerGroupMemberServiceSyncPayload(Seq(account3.id)))
        .mustFailWith400(CustomerGroupTypeIsWrong(manualGroup.id, Manual, Set(Dynamic, Template)))
    }
  }

  "POST /v1/admin/customers/:id/customer-groups" - {

    "adds user to manual groups" in new FixtureForCustomerGroups {
      val payload = AddCustomerToGroups(Seq(group2.id, group3.id))

      val response = customersApi(account.id).groups.syncGroups(payload).as[Root]

      response.groups.length must === (3)

      val updatedMemberships =
        CustomerGroupMembers.findByCustomerDataId(custData.id).gimme.map(_.groupId)

      withClue(s"Group ${group1.id} was not deleted from user group member list: ") {
        (updatedMemberships.contains(group1.id)) must === (false)
      }

      withClue(s"Group ${group2.id} was deleted from user group member list: ") {
        (updatedMemberships.contains(group2.id)) must === (true)
      }

      withClue(s"Group ${group3.id} was not added to user group member list: ") {
        (updatedMemberships.contains(group3.id)) must === (true)
      }

      withClue(
          s"Group ${groupDynamic.id} is dynamic and must not be deleted from group member list: ") {
        (updatedMemberships.contains(groupDynamic.id)) must === (true)
      }
    }

    "404 if customer not found" in new FixtureForCustomerGroups {
      customersApi(666).groups
        .syncGroups(AddCustomerToGroups(Seq.empty))
        .mustFailWith404(NotFoundFailure404(User, 666))
    }

    "400 if group is dynamic" in new FixtureForCustomerGroups {
      customersApi(account.id).groups
        .syncGroups(AddCustomerToGroups(Seq(groupDynamic2.id)))
        .mustFailWith400(CustomerGroupTypeIsWrong(groupDynamic2.id, Dynamic, Set(Manual)))
    }
  }

  "POST /v1/admin/customer-groups/:id/customers" - {
    "syncs users in manual groups" in new FixtureForCustomerGroups {
      val payload = CustomerGroupMemberSyncPayload(Seq(account2.id, account3.id), Seq(account.id))
      customerGroupsMembersApi(group1.id)
        .syncCustomers(payload)
        .mustHaveStatus(StatusCodes.NoContent)

      val updatedMemberships =
        CustomerGroupMembers.findByGroupId(group1.id).gimme.map(_.customerDataId)

      withClue(s"Customers ${account2.id}, ${account3.id} were not added to group member list: ") {
        (updatedMemberships.contains(custData2.id)) must === (true)
        (updatedMemberships.contains(custData3.id)) must === (true)
      }

      withClue(s"Customer ${account.id} was not deleted from group member list: ") {
        (updatedMemberships.contains(custData.id)) must === (false)
      }
    }

    "404 if customer group not found" in new FixtureForCustomerGroups {
      customerGroupsMembersApi(666)
        .syncCustomers(CustomerGroupMemberSyncPayload(Seq.empty, Seq.empty))
        .mustFailWith404(NotFoundFailure404(CustomerGroup, 666))
    }

    "400 if customer group is dynamic" in new FixtureForCustomerGroups {
      customerGroupsMembersApi(groupDynamic2.id)
        .syncCustomers(CustomerGroupMemberSyncPayload(Seq.empty, Seq.empty))
        .mustFailWith400(CustomerGroupTypeIsWrong(groupDynamic2.id, Dynamic, Set(Manual)))
    }

    "400 if payload contains same ids for addition and deletion" in new FixtureForCustomerGroups {
      customerGroupsMembersApi(group1.id)
        .syncCustomers(CustomerGroupMemberSyncPayload(Seq(account2.id), Seq(account2.id)))
        .mustFailWith400(CustomerGroupMemberPayloadContainsSameIdsInBothSections(group1.id,
                                                                                 Set(account2.id),
                                                                                 Set(account2.id)))
    }
  }

  trait Fixture extends StoreAdmin_Seed {

    val (group, account1, custData1, account2, custData2, account3, custData3, manualGroup) =
      (for {
        group       ← * <~ CustomerGroups.create(Factories.group(scope))
        manualGroup ← * <~ CustomerGroups.create(Factories.group(scope).copy(groupType = Manual))

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

      } yield
        (group, account1, custData1, account2, custData2, account3, custData3, manualGroup)).gimmeTxn
  }

  trait FixtureForCustomerGroups extends StoreAdmin_Seed {

    val (group1,
         group2,
         group3,
         groupDynamic,
         groupDynamic2,
         account,
         custData,
         account2,
         custData2,
         account3,
         custData3) = (for {
      group1        ← * <~ CustomerGroups.create(Factories.group(scope).copy(groupType = Manual))
      group2        ← * <~ CustomerGroups.create(Factories.group(scope).copy(groupType = Manual))
      group3        ← * <~ CustomerGroups.create(Factories.group(scope).copy(groupType = Manual))
      groupDynamic  ← * <~ CustomerGroups.create(Factories.group(scope).copy(groupType = Dynamic))
      groupDynamic2 ← * <~ CustomerGroups.create(Factories.group(scope).copy(groupType = Dynamic))
      account       ← * <~ Accounts.create(Account())
      user          ← * <~ Users.create(Factories.customer.copy(accountId = account.id))
      custData ← * <~ CustomersData.create(
                    CustomerData(userId = user.id, accountId = account.id, scope = scope))

      account2 ← * <~ Accounts.create(Account())
      user2    ← * <~ Users.create(Factories.customer.copy(accountId = account2.id))
      custData2 ← * <~ CustomersData.create(
                     CustomerData(userId = user2.id, accountId = account2.id, scope = scope))

      account3 ← * <~ Accounts.create(Account())
      user3    ← * <~ Users.create(Factories.customer.copy(accountId = account3.id))
      custData3 ← * <~ CustomersData.create(
                     CustomerData(userId = user3.id, accountId = account3.id, scope = scope))

      _ ← * <~ CustomerGroupMembers.create(
             CustomerGroupMember(groupId = group1.id, customerDataId = custData.id))
      _ ← * <~ CustomerGroupMembers.create(
             CustomerGroupMember(groupId = group2.id, customerDataId = custData.id))
      _ ← * <~ CustomerGroupMembers.create(
             CustomerGroupMember(groupId = groupDynamic.id, customerDataId = custData.id))
    } yield
      (group1,
       group2,
       group3,
       groupDynamic,
       groupDynamic2,
       account,
       custData,
       account2,
       custData2,
       account3,
       custData3)).gimmeTxn
  }

}
