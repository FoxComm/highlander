package anthill.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import anthill.payloads._
import anthill.responses._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val pingResponseFormat: RootJsonFormat[PingResponse] = jsonFormat1(PingResponse)

  // training payload from orders-anthill
  implicit val purchaseEventPayloadFormat: RootJsonFormat[PurchaseEventPayload] = jsonFormat3(
    PurchaseEventPayload)

  // product response from es
  implicit val taxonomyFormat: RootJsonFormat[Taxonomy]               = jsonFormat2(Taxonomy)
  implicit val imageFormat: RootJsonFormat[Image]                     = jsonFormat4(Image)
  implicit val albumFormat: RootJsonFormat[Album]                     = jsonFormat3(Album)
  implicit val productResponseFormat: RootJsonFormat[ProductResponse] = jsonFormat14(ProductResponse)
}
