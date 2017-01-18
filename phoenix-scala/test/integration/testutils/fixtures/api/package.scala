package testutils.fixtures

import cats.implicits._
import models.cord.lineitems.{GiftCardLineItemAttributes, LineItemAttributes}
import models.objects.ObjectForm

package object api {

  def giftCardLineItemAttributes: Option[LineItemAttributes] =
    LineItemAttributes(
        GiftCardLineItemAttributes(recipientName = faker.Name.name,
                                   recipientEmail = faker.Internet.email,
                                   senderName = faker.Name.name,
                                   message = faker.Lorem.sentence()).some).some

  // TODO
  def TEMPORARY_skuCodeToVariantFormId(skuCode: String): ObjectForm#Id = ???

}
