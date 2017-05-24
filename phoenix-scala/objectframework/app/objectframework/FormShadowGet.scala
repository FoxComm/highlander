package objectframework

import cats.implicits._
import objectframework.models.{ObjectForm, ObjectShadow}
import org.json4s._
import utils.Money.Currency

object FormShadowGet {

  def priceFromJson(p: JValue): Option[(Int, Currency)] = {
    val price = for {
      JInt(value)       ← p \ "value"
      JString(currency) ← p \ "currency"
    } yield (value.toInt, Currency(currency))
    if (price.isEmpty) None else price.headOption
  }

  def price(f: ObjectForm, s: ObjectShadow): Option[(Int, Currency)] = {
    ObjectUtils.get("salePrice", f, s) match {
      case JNothing ⇒ None
      case v        ⇒ priceFromJson(v)
    }
  }

  def priceAsInt(f: ObjectForm, s: ObjectShadow): Int =
    price(f, s).map { case (value, _) ⇒ value }.getOrElse(0)

  def title(f: ObjectForm, s: ObjectShadow): Option[String] = {
    ObjectUtils.get("title", f, s) match {
      case JString(title) ⇒ title.some
      case _              ⇒ None
    }
  }

  def externalId(f: ObjectForm, s: ObjectShadow): Option[String] = {
    ObjectUtils.get("externalId", f, s) match {
      case JString(externalId) ⇒ externalId.some
      case _                   ⇒ None
    }
  }

  def trackInventory(f: ObjectForm, s: ObjectShadow): Boolean = {
    ObjectUtils.get("trackInventory", f, s) match {
      case JBool(trackInventory) ⇒ trackInventory
      case _                     ⇒ true
    }
  }
}
