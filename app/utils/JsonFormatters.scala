package utils

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models.{GiftCardPaymentStatus, CreditCardPaymentStatus, Order}
import com.pellucid.sealerate
import org.json4s.JsonAST.JString
import org.json4s.{jackson, CustomSerializer, DefaultFormats}

object JsonFormatters {
  import Strings._
  implicit val serialization = jackson.Serialization

  /* Used to "easily" (de-)serialize ADTs. They can only be case objects due to sealerate limitations */
  object ADT {
    val orderStatuses = sealerate.values[Order.Status].foldLeft(Map[String, Order.Status]())(reducer)
    val ccStatuses = sealerate.values[CreditCardPaymentStatus].foldLeft(Map[String, CreditCardPaymentStatus]())(reducer)
    val gcStatuses = sealerate.values[GiftCardPaymentStatus].foldLeft(Map[String, GiftCardPaymentStatus]())(reducer)

    def reducer[A](map: Map[String, A], t: A): Map[String, A] =
      map.updated(t.toString.upperCaseFirstLetter, t)

    def render[A](a: A) = JString(a.toString.lowerCaseFirstLetter)

    def serializer[A: Manifest](adtMap: Map[String, A]) = new CustomSerializer[A](format => (
      { case JString(str) ⇒ adtMap.get(str.upperCaseFirstLetter).get },
      { case x: A ⇒ render(x) }))
  }

  implicit val phoenixFormats = DefaultFormats +
    ADT.serializer[Order.Status](ADT.orderStatuses) +
    ADT.serializer[GiftCardPaymentStatus](ADT.gcStatuses) +
    ADT.serializer[CreditCardPaymentStatus](ADT.ccStatuses)
}

