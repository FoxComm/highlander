package services.activity

import models.activity.OpaqueActivity
import payloads.UpdateCustomerPayload

import org.json4s._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._

import scala.language.implicitConversions

/**
 * This file contains activities and their mappings to JSON.
 * They implicitly convert to an OpaqueActivity which means you can use like so
 *
 *   Activities.log(<Tailored Activity>)
 *
 * For now the convertion is one way as the Ashes interface is what really cares about
 * parsing these.
 */


final case class CustomerInfoChanged(
  customerId: Int, 
  oldInfo: UpdateCustomerPayload,
  newInfo: UpdateCustomerPayload)

object CustomerInfoChanged {
  val typeName = "customer_contact_changed"

  private def asJson(j: UpdateCustomerPayload) = {
    ("name" → j.name) ~
    ("email" → j.email) ~
    ("phone" → j.phoneNumber)
  }

  implicit def typed2opaque(a: CustomerInfoChanged) : OpaqueActivity = {
    val t = typeName
    val d = (
      ("customer_id" → a.customerId) ~
      ("old" → asJson(a.oldInfo)) ~
      ("new" → asJson(a.newInfo)))
    OpaqueActivity(t, d)
  }
}


