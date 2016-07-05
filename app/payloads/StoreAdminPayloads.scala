package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import utils.Validation
import utils.Validation._

object StoreAdminPayloads {

  case class CreateStoreAdminPayload(email: String,
                                     name: String,
                                     phoneNumber: Option[String] = None,
                                     password: Option[String] = None,
                                     department: Option[String] = None)
      extends Validation[CreateStoreAdminPayload] {

    def validate: ValidatedNel[Failure, CreateStoreAdminPayload] = {
      (notEmpty(name, "name") |@| notEmpty(email, "email") |@|
            nullOrNotEmpty(phoneNumber, "phoneNumber") |@| nullOrNotEmpty(password, "password") |@|
            nullOrNotEmpty(department, "department")).map {
        case _ ⇒ this
      }
    }
  }

  case class UpdateStoreAdminPayload(email: String,
                                     name: String,
                                     phoneNumber: Option[String] = None,
                                     department: Option[String] = None)
      extends Validation[UpdateStoreAdminPayload] {

    def validate: ValidatedNel[Failure, UpdateStoreAdminPayload] = {
      (notEmpty(name, "name") |@| notEmpty(email, "email") |@|
            nullOrNotEmpty(phoneNumber, "phoneNumber") |@| nullOrNotEmpty(department,
                                                                          "department")).map {
        case _ ⇒ this
      }
    }
  }
}
