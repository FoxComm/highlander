package phoenix.responses.cord.base

import phoenix.models.shipping.ShippingMethods
import phoenix.responses.{AddressResponse, ShippingMethodsResponse}
import slick.dbio.DBIO
import utils.db._

object CordResponseShipping {

  def shippingMethod(cordRef: String)(
      implicit ec: EC): DBIO[Option[ShippingMethodsResponse.Root]] =
    ShippingMethods.forCordRef(cordRef).one.map(_.map(ShippingMethodsResponse.build(_)))

  def shippingAddress(cordRef: String)(implicit ec: EC): DbResultT[AddressResponse] =
    AddressResponse.forCordRef(cordRef)
}
