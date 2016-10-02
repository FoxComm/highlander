import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.StoreAdminFailures.AlreadyExistsWithEmail
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.StoreAdmin
import payloads.StoreAdminPayloads._
import responses.StoreAdminResponse
import util.IntegrationTestBase
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
      val response = storeAdminsApi.create(payload)

      response.status must === (StatusCodes.OK)

      val admin = response.as[StoreAdminResponse.Root]
      admin.name.value must === (payload.name)
      admin.email.value must === (payload.email)
      admin.department must === (payload.department)
      admin.state must === (StoreAdmin.Invited)
    }

    "don't create with duplicated email" in new Fixture {
      val payload =
        CreateStoreAdminPayload(name = authedStoreAdmin.name, email = authedStoreAdmin.email)
      val response = storeAdminsApi.create(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (AlreadyExistsWithEmail(authedStoreAdmin.email).description)
    }
  }

  "GET /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      val response = storeAdminsApi(storeAdmin.id).get()

      response.status must === (StatusCodes.OK)

      val admin = response.as[StoreAdminResponse.Root]
      admin.id must === (storeAdmin.id)
      admin.name.value must === (storeAdmin.name)
      admin.email.value must === (storeAdmin.email)
      admin.department must === (storeAdmin.department)
      admin.phoneNumber must === (storeAdmin.phoneNumber)
      admin.state must === (storeAdmin.state)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val response = storeAdminsApi(666).get()

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 666).description)
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

      val response = storeAdminsApi(storeAdmin.id).update(payload)

      response.status must === (StatusCodes.OK)

      val updated = response.as[StoreAdminResponse.Root]

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
      val response = storeAdminsApi(666).update(payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 666).description)
    }

  }

  "PATCH /v1/store-admins/:id/state" - {
    "change state successfully" in new Fixture {
      val payload  = StateChangeStoreAdminPayload(state = StoreAdmin.Inactive)
      val response = storeAdminsApi(storeAdmin.id).updateState(payload)

      response.status must === (StatusCodes.OK)

      val updated = response.as[StoreAdminResponse.Root]
      updated.state must === (StoreAdmin.Inactive)
    }

    "respond with 400 when cannot apply new state" in new Fixture {
      val payload  = StateChangeStoreAdminPayload(state = StoreAdmin.Invited)
      val response = storeAdminsApi(storeAdmin.id).updateState(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          StateTransitionNotAllowed(StoreAdmin,
                                    storeAdmin.state.toString,
                                    StoreAdmin.Invited.toString,
                                    storeAdmin.id).description)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val payload  = StateChangeStoreAdminPayload(state = StoreAdmin.Inactive)
      val response = storeAdminsApi(666).updateState(payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 666).description)
    }

  }

  "DELETE /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      val response = storeAdminsApi(storeAdmin.id).delete()
      response.status must === (StatusCodes.NoContent)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val response = storeAdminsApi(666).delete()

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 666).description)
    }
  }

  trait Fixture extends StoreAdmin_Seed
}
