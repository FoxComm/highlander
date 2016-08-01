package models.vendor

import java.time.Instant

case class VendorAddress(id: Int = 0,
                   vendorId: Int,
                   regionId: Int,
                   name: String,
                   address1: String,
                   address2: Option[String],
                   city: String,
                   zip: String,
                   isPrimary: Boolean = false,
                   deletedAt: Option[Instant] = None)
    extends FoxModel[VendorAddress]
    with Addressable[VendorAddress]
    with Validation[VendorAddress] {


  def instance: VendorAddress = { this }
  def zipLens = len[VendorAddress].zip
  override def sanitize = super.sanitize(this)
  override def validate = super.validate
}


