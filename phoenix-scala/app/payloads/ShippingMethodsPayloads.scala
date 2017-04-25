package payloads

object ShippingMethodsPayloads {
  case class RegionSearchPayload(countryId: Option[Int] = None, regionId: Option[Int] = None)
}
