import core.failures.NotFoundFailure404
import phoenix.failures.StateTransitionNotAllowed
import phoenix.failures.UserFailures.UserEmailNotUnique
import phoenix.models.account._
import phoenix.models.admin.AdminData
import phoenix.payloads.StoreAdminPayloads._
import phoenix.responses.users.StoreAdminResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

class StoreAdminIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with BakedFixtures {

  "POST /v1/store-admins" - {
    "create successfully" in new Fixture {
      val payload = CreateStoreAdminPayload(name = "Admin Donkey",
                                            email = "donkey.admin@donkeys.com",
                                            password = Some("123456"),
                                            phoneNumber = Some("1231231234"),
                                            roles = List("admin"),
                                            org = "tenant")
      val admin = storeAdminsApi.create(payload).as[StoreAdminResponse]

      admin.name.value must === (payload.name)
      admin.email.value must === (payload.email)
      admin.state must === (AdminData.Invited)
    }

    "don't create with duplicated email" in new Fixture {
      val payload = CreateStoreAdminPayload(name = storeAdmin.name.value,
                                            email = storeAdmin.email.value,
                                            roles = List("admin"),
                                            org = "tenant")

      storeAdminsApi.create(payload).mustFailWith400(UserEmailNotUnique)
    }
  }

  "GET /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      val admin = storeAdminsApi(storeAdmin.accountId).get().as[StoreAdminResponse]

      admin.id must === (storeAdmin.accountId)
      admin.name must === (storeAdmin.name)
      admin.email must === (storeAdmin.email)
      admin.phoneNumber must === (storeAdmin.phoneNumber)
      admin.state must === (storeAdminUser.state)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      storeAdminsApi(666).get().mustFailWith404(NotFoundFailure404(User, 666))
    }
  }

  "PATCH /v1/store-admins/:id" - {
    "update successfully" in new Fixture {
      val newName       = "SuperDonkey"
      val newEmail      = "superdonkey@donkeys.com"
      val newDepartment = Some("Overpowered Donkey Squad")
      val newPhone      = Some("1234512345")
      val payload =
        UpdateStoreAdminPayload(email = newEmail, name = newName, phoneNumber = newPhone)

      val updated = storeAdminsApi(storeAdmin.accountId).update(payload).as[StoreAdminResponse]

      updated.id must === (storeAdmin.accountId)
      updated.state must === (storeAdminUser.state)
      updated.email.value must === (newEmail)
      updated.name.value must === (newName)
      updated.phoneNumber must === (newPhone)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      storeAdminsApi(666)
        .update(UpdateStoreAdminPayload(email = "superdonkey@email.com", name = "SuperDonkey"))
        .mustFailWith404(NotFoundFailure404(User, 666))
    }

    "don't update with duplicated email" in new Fixture {
      val create_payload = CreateStoreAdminPayload(name = "Admin Donkey",
                                                   email = "donkey.admin@donkeys.com",
                                                   password = Some("123456"),
                                                   phoneNumber = Some("1231231234"),
                                                   roles = List("admin"),
                                                   org = "tenant")
      val admin = storeAdminsApi.create(create_payload).as[StoreAdminResponse]
      val payload =
        UpdateStoreAdminPayload(name = storeAdmin.name.value, email = storeAdmin.email.value)

      storeAdminsApi(admin.id).update(payload).mustFailWith400(UserEmailNotUnique)
    }
  }

  "PATCH /v1/store-admins/:id/state" - {
    "change state successfully" in new Fixture {
      storeAdminsApi(storeAdmin.accountId)
        .updateState(StateChangeStoreAdminPayload(state = AdminData.Inactive))
        .as[StoreAdminResponse]
        .state must === (AdminData.Inactive)
    }

    "respond with 400 when cannot apply new state" in new Fixture {
      storeAdminsApi(storeAdmin.accountId)
        .updateState(StateChangeStoreAdminPayload(state = AdminData.Invited))
        .mustFailWith400(
          StateTransitionNotAllowed(AdminData,
                                    storeAdminUser.state.toString,
                                    AdminData.Invited.toString,
                                    storeAdminUser.id))
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      storeAdminsApi(666)
        .updateState(StateChangeStoreAdminPayload(state = AdminData.Inactive))
        .mustFailWith404(NotFoundFailure404(User, 666))
    }

  }

  "DELETE /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      storeAdminsApi(storeAdmin.accountId).delete().mustBeEmpty()
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      storeAdminsApi(666).delete().mustFailWith404(NotFoundFailure404(User, 666))
    }
  }

  trait Fixture extends StoreAdmin_Seed
}
