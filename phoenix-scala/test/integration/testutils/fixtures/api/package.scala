package testutils.fixtures

import cats.implicits._
import models.cord.lineitems.{GiftCardLineItemAttributes, LineItemAttributes}

package object api {

  def giftCardLineItemAttributes: Option[LineItemAttributes] =
    LineItemAttributes(
        GiftCardLineItemAttributes(recipientName = faker.Name.name,
                                   recipientEmail = faker.Internet.email,
                                   senderName = faker.Name.name,
                                   message = faker.Lorem.sentence()).some).some

}
