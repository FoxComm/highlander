package testutils.fixtures

import cats.implicits._
import faker.Lorem
import phoenix.models.cord.lineitems.{GiftCardLineItemAttributes, LineItemAttributes}
import phoenix.models.location.Region
import phoenix.payloads.AddressPayloads.CreateAddressPayload

package object api {

  // Good enough for test. I queried for this number
  val TOTAL_REGION_COUNT = 4367

  def randomAddress(regionId: Int = Region.californiaId) =
    CreateAddressPayload(regionId = regionId,
                         city = faker.Lorem.sentence(),
                         address1 = faker.Lorem.sentence(),
                         name = faker.Name.name,
                         zip = Lorem.numerify("#####"))

  def randomGiftCardLineItemAttributes(): Option[LineItemAttributes] =
    LineItemAttributes(
      GiftCardLineItemAttributes(recipientName = faker.Name.name,
                                 recipientEmail = faker.Internet.email,
                                 senderName = faker.Name.name,
                                 message = faker.Lorem.sentence().some).some).some

}
