package phoenix.responses.cord.base

import core.db._
import phoenix.models.shipping.ShippingMethods
import phoenix.responses.{AddressResponse, ShippingMethodsResponse}
import slick.dbio.DBIO

object CordResponseShipping {

  def shippingMethod(cordRef: String)(implicit ec: EC): DBIO[Option[ShippingMethodsResponse]] =
    ShippingMethods.forCordRef(cordRef).one.map(_.map(ShippingMethodsResponse.build(_)))

  def shippingAddress(cordRef: String)(implicit ec: EC): DbResultT[AddressResponse] =
    AddressResponse.forCordRef(cordRef)
}
