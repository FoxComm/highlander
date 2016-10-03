import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.StoreAdminFailures.AlreadyExistsWithEmail
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.StoreAdmin
import payloads.StoreAdminPayloads._
import responses.StoreAdminResponse
import responses.StoreAdminResponse.Root
import util.{IntegrationTestBase, PhoenixAdminApi}
import util.fixtures.BakedFixtures

class StoreAdminIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "POST /v1/store-admins" - {
    "create successfully" in new Fixture {
      val payload = CreateStoreAdminPayload(name = "Admin Donkey",
                                            email = "donkey.admin@donkeys.com",
                                            password = Some("123456"),
                                            department = Some("donkey team"),
                                            phoneNumber = Some("1231231234"))
      val admin = storeAdminsApi.create(payload).as[Root]

      admin.name.value must === (payload.name)
      admin.email.value must === (payload.email)
      admin.department must === (payload.department)
      admin.state must === (StoreAdmin.Invited)
    }

    "don't create with duplicated email" in new Fixture {
      val payload =
        CreateStoreAdminPayload(name = authedStoreAdmin.name, email = authedStoreAdmin.email)

      storeAdminsApi
        .create(payload)
        .mustFailWith400(AlreadyExistsWithEmail(authedStoreAdmin.email))
    }
  }

  "GET /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      val admin = storeAdminsApi(storeAdmin.id).get().as[Root]

      admin.id must === (storeAdmin.id)
      admin.name.value must === (storeAdmin.name)
      admin.email.value must === (storeAdmin.email)
      admin.department must === (storeAdmin.department)
      admin.phoneNumber must === (storeAdmin.phoneNumber)
      admin.state must === (storeAdmin.state)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      storeAdminsApi(666).get().mustFailWith404(NotFoundFailure404(StoreAdmin, 666))
    }
  }

  "PATCH /v1/store-admins/:id" - {
    "update successfully" in new Fixture {
      val newName       = "SuperDonkey"
      val newEmail      = "superdonkey@donkeys.com"
      val newDepartment = Some("Overpowered Donkey Squad")
      val newPhone      = Some("1234512345")
      val payload = UpdateStoreAdminPayload(email = newEmail,
                                            name = newName,
                                            department = newDepartment,
                                            phoneNumber = newPhone)

      val updated = storeAdminsApi(storeAdmin.id).update(payload).as[Root]

      updated.id must === (storeAdmin.id)
      updated.state must === (storeAdmin.state)
      updated.email.value must === (newEmail)
      updated.name.value must === (newName)
      updated.department must === (newDepartment)
      updated.phoneNumber must === (newPhone)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val payload = UpdateStoreAdminPayload(email = "superdonkey@email.com",
                                            name = "SuperDonkey",
                                            department = Some("Overpowered Donkey Squad"))
      storeAdminsApi(666).update(payload).mustFailWith404(NotFoundFailure404(StoreAdmin, 666))
    }

  }

  "PATCH /v1/store-admins/:id/state" - {
    "change state successfully" in new Fixture {
      val payload  = StateChangeStoreAdminPayload(state = StoreAdmin.Inactive)
      val response = storeAdminsApi(storeAdmin.id).updateState(payload).as[Root]

      response.state must === (StoreAdmin.Inactive)
    }

    "respond with 400 when cannot apply new state" in new Fixture {
      val payload = StateChangeStoreAdminPayload(state = StoreAdmin.Invited)
      val failure = StateTransitionNotAllowed(StoreAdmin,
                                              storeAdmin.state.toString,
                                              StoreAdmin.Invited.toString,
                                              storeAdmin.id)
      storeAdminsApi(storeAdmin.id).updateState(payload).mustFailWith400(failure)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      storeAdminsApi(666)
        .updateState(StateChangeStoreAdminPayload(state = StoreAdmin.Inactive))
        .mustFailWith404(NotFoundFailure404(StoreAdmin, 666))
    }

  }

  "DELETE /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      storeAdminsApi(storeAdmin.id).delete().mustBeEmpty()
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      storeAdminsApi(666).delete().mustFailWith404(NotFoundFailure404(StoreAdmin, 666))
    }
  }

  trait Fixture extends StoreAdmin_Seed
}
