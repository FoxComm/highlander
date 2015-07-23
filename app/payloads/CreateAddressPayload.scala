package payloads

import utils.Validation

import com.wix.accord.dsl.{validator => createValidator}
import com.wix.accord.dsl._

final case class CreateAddressPayload(name: String, stateId: Int, state: Option[String] = None,
  street1: String, street2: Option[String] = None, city: String, zip: String, isDefault: Boolean = false)
  extends Validation[CreateAddressPayload] {

  override def validator = createValidator[CreateAddressPayload] { address =>
    address.name is notEmpty
    address.street1 is notEmpty
    address.city is notEmpty
    address.zip should matchRegex("[0-9]{5}")
  }
}
