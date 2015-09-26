package responses

object ShippingMethods {
  final case class Root(id: Int, name: String, price: Int, isEnabled: Boolean)

  def build(record: models.ShippingMethod, isEnabled: Boolean = true): Root =
    Root(id = record.id, name = record.adminDisplayName, price = record.defaultPrice, isEnabled = isEnabled)
}