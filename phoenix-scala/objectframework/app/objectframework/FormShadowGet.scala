package objectframework

import cats.implicits._
import core.utils.Money._
import objectframework.models.{ObjectForm, ObjectShadow}
import org.json4s._

object FormShadowGet {

  def priceFromJson(p: JValue): Option[Price] = {
    val price: List[Price] = for {
      JInt(value)       ← p \ "value"
      JString(currency) ← p \ "currency"
    } yield (value.toLong, Currency(currency))
    if (price.isEmpty) None else price.headOption
  }

  def price(f: ObjectForm, s: ObjectShadow): Option[Price] =
    ObjectUtils.get("salePrice", f, s) match {
      case JNothing ⇒ None
      case v        ⇒ priceFromJson(v)
    }

  def priceAsLong(f: ObjectForm, s: ObjectShadow): Long =
    price(f, s).map { case (value, _) ⇒ value }.getOrElse(0)

  def title(f: ObjectForm, s: ObjectShadow): Option[String] =
    ObjectUtils.get("title", f, s) match {
      case JString(title) ⇒ title.some
      case _              ⇒ None
    }

  def externalId(f: ObjectForm, s: ObjectShadow): Option[String] =
    ObjectUtils.get("externalId", f, s) match {
      case JString(externalId) ⇒ externalId.some
      case _                   ⇒ None
    }

  def trackInventory(f: ObjectForm, s: ObjectShadow): Boolean =
    ObjectUtils.get("trackInventory", f, s) match {
      case JBool(trackInventory) ⇒ trackInventory
      case _                     ⇒ true
    }
}
