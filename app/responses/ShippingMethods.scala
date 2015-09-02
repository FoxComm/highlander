package responses

object ShippingMethods {
  final case class Root(id: Int, name: String, price: Int, isEnabled: Boolean)

  def build(record: models.ShippingMethod): Root =
    Root(id = record.id, name = record.adminDisplayName, price = record.defaultPrice, isEnabled = true)

  def build(records: Seq[models.ShippingMethod]): Seq[Root] =
    records.map(build)
}