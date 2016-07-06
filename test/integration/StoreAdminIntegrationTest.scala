import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.NotFoundFailure404
import failures.StoreAdminFailures.AlreadyExistsWithEmail
import models.{StoreAdmin, StoreAdmins}
import payloads.StoreAdminPayloads._
import responses.StoreAdminResponse
import util.IntegrationTestBase

class StoreAdminIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST /v1/store-admins" - {
    "create successfully" in new Fixture {
      val payload = CreateStoreAdminPayload(name = "Admin Donkey",
                                            email = "donkey.admin@donkeys.com",
                                            password = Some("123456"),
                                            department = Some("donkey team"),
                                            phoneNumber = Some("1231231234"))
      val response = POST("v1/store-admins", payload)

      response.status must === (StatusCodes.OK)

      val admin = response.as[StoreAdminResponse.Root]
      admin.name must === (payload.name)
      admin.email must === (payload.email)
      admin.department must === (payload.department)
    }

    "don't create with duplicated email" in new Fixture {
      val payload =
        CreateStoreAdminPayload(name = authedStoreAdmin.name, email = authedStoreAdmin.email)
      val response = POST("v1/store-admins", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (AlreadyExistsWithEmail(authedStoreAdmin.email).description)
    }
  }

  "GET /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      val response = GET(s"v1/store-admins/${storeAdmin.id}")

      response.status must === (StatusCodes.OK)

      val admin = response.as[StoreAdminResponse.Root]
      admin.id must === (storeAdmin.id)
      admin.name must === (storeAdmin.name)
      admin.email must === (storeAdmin.email)
      admin.department must === (storeAdmin.department)
      admin.phoneNumber must === (storeAdmin.phoneNumber)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val response = GET(s"v1/store-admins/666")

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

      val response = PATCH(s"v1/store-admins/${storeAdmin.id}", payload)

      response.status must === (StatusCodes.OK)

      val updated = response.as[StoreAdminResponse.Root]

      updated.id must === (storeAdmin.id)
      updated.email must === (newEmail)
      updated.name must === (newName)
      updated.department must === (newDepartment)
      updated.phoneNumber must === (newPhone)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val id = 123456789
      val payload = UpdateStoreAdminPayload(email = "superdonkey@email.com",
                                            name = "SuperDonkey",
                                            department = Some("Overpowered Donkey Squad"))
      val response = PATCH(s"v1/store-admins/$id", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, id).description)
    }
  }

  "DELETE /v1/store-admins/:id" - {
    "display store admin when id points to valid admin" in new Fixture {
      val response = DELETE(s"v1/store-admins/${storeAdmin.id}")
      response.status must === (StatusCodes.NoContent)
    }

    "respond with 404 when id does not point to valid admin" in new Fixture {
      val id       = 123456789
      val response = DELETE(s"v1/store-admins/$id")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, id).description)
    }
  }

  trait Fixture {
    val storeAdmin = StoreAdmins.create(authedStoreAdmin).gimme
  }
}
