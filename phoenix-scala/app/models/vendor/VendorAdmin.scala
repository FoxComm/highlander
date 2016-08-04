package models.Vendor

case class VendorAdmin (id: Int = 0,
                        email: Option[String] = None,
                        hashedPassword: Option[String] = None,
                        isDisabled: Boolean = false,
                        disabledBy: Option[Int] = None,
                        createdAt: Instant = Instant.now)
    extends FoxModel[VendorAdmin]
    with Validation[VendorAdmin] {

  import Validation._

  override def validate: ValidatedNel[Failure, StoreAdmin] = {
    (notEmpty(name, "name") |@| notEmpty(email, "email").map {case _ = this})
  }
}

/* TODO: We should generalize the notion of Admin, then apply it via composition 
 * to VendorAdmin and StoreAdmin.
 */
object VendorAdmin {

  def build(id: Int = 0,
            email: String,
            password: Option[String] = None,
            isDisabled: Boolean = false,
            )
}
