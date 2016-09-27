import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.UserFailures.UserEmailNotUnique
import failures.{NotFoundFailure404, StateTransitionNotAllowed}
import models.account._
import models.admin.StoreAdminUser
import payloads.StoreAdminPayloads._
import responses.StoreAdminResponse
import util.IntegrationTestBase
import util.fixtures.BakedFixtures

class StoreAdminIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures {

  "POST /v1/store-admins" - {
    "create successfully" in new Fixture {
      val payload = CreateStoreAdminPayload(name = "Admin Donkey",
                                            email = "donkey.admin@donkeys.com",
                                            password = Some("123456"),
                                            phoneNumber = Some("1231231234"),
                                            roles = List("admin"),
                                            org = "tenant")
      val response = POST("v1/store-admins", payload)

      response.status must === (StatusCodes.OK)

      val admin = response.as[StoreAdminResponse.Root]
      admin.name.value must === (payload.name)
      admin.email.value must === (payload.email)
      admin.state must === (StoreAdminUser.Invited)
    }

    "don't create with duplicated email" in new Fixture {
      val payload = CreateStoreAdminPayload(name = authedUser.name.getOrElse(""),
                                            email = authedUser.email.getOrElse(""),
                                            roles = List("admin"),
                                            org = "tenant")
      val response = POST("v1/store-admins", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (UserEmailNotUnique.description)
    }
  }

  "GET /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      val response = GET(s"v1/store-admins/${storeAdmin.accountId}")

      response.status must === (StatusCodes.OK)

      val admin = response.as[StoreAdminResponse.Root]
      admin.id must === (storeAdmin.accountId)
      admin.name must === (storeAdmin.name)
      admin.email must === (storeAdmin.email)
      admin.phoneNumber must === (storeAdmin.phoneNumber)
      admin.state must === (storeAdminUser.state)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val id       = 666
      val response = GET(s"v1/store-admins/$id")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(User, 666).description)
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

      val response = PATCH(s"v1/store-admins/${storeAdmin.accountId}", payload)

      response.status must === (StatusCodes.OK)

      val updated = response.as[StoreAdminResponse.Root]

      updated.id must === (storeAdmin.accountId)
      updated.state must === (storeAdminUser.state)
      updated.email.value must === (newEmail)
      updated.name.value must === (newName)
      updated.phoneNumber must === (newPhone)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val id       = 666
      val payload  = UpdateStoreAdminPayload(email = "superdonkey@email.com", name = "SuperDonkey")
      val response = PATCH(s"v1/store-admins/$id", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(User, id).description)
    }

  }

  "PATCH /v1/store-admins/:id/state" - {
    "change state successfully" in new Fixture {
      val payload  = StateChangeStoreAdminPayload(state = StoreAdminUser.Inactive)
      val response = PATCH(s"v1/store-admins/${storeAdmin.accountId}/state", payload)

      response.status must === (StatusCodes.OK)

      val updated = response.as[StoreAdminResponse.Root]
      updated.state must === (StoreAdminUser.Inactive)
    }

    "respond with 400 when cannot apply new state" in new Fixture {
      val payload  = StateChangeStoreAdminPayload(state = StoreAdminUser.Invited)
      val response = PATCH(s"v1/store-admins/${storeAdmin.accountId}/state", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          StateTransitionNotAllowed(StoreAdminUser,
                                    storeAdminUser.state.toString,
                                    StoreAdminUser.Invited.toString,
                                    storeAdmin.accountId).description)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val id       = 666
      val payload  = StateChangeStoreAdminPayload(state = StoreAdminUser.Inactive)
      val response = PATCH(s"v1/store-admins/$id/state", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(User, id).description)
    }

  }

  "DELETE /v1/store-admins/:id" - {
    "delete store admin when id points to valid admin" in new Fixture {
      val response = DELETE(s"v1/store-admins/${storeAdmin.accountId}")
      response.status must === (StatusCodes.NoContent)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val id       = 666
      val response = DELETE(s"v1/store-admins/$id")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(User, id).description)
    }
  }

  trait Fixture extends StoreAdmin_Seed
}
