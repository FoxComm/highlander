package testutils.fixtures

import cats.implicits._
import faker.Lorem
import models.cord.lineitems._
import payloads.AddressPayloads.CreateAddressPayload

package object api {

  def giftCardLineItemAttributes: Option[LineItemAttributes] =
    LineItemAttributes(
        GiftCardLineItemAttributes(recipientName = faker.Name.name,
                                   recipientEmail = faker.Internet.email,
                                   senderName = faker.Name.name,
                                   message = faker.Lorem.sentence()).some).some

  def randomAddress(regionId: Int): CreateAddressPayload =
    CreateAddressPayload(regionId = regionId,
                         name = Lorem.letterify("???"),
                         address1 = Lorem.letterify("???"),
                         city = Lorem.letterify("???"),
                         zip = Lorem.numerify("#####"))
}
