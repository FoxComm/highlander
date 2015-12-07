package services.activity

import models.activity.OpaqueActivity
import payloads.UpdateCustomerPayload

import org.json4s._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.json4s.DefaultFormats
import org.json4s.Extraction

import scala.language.implicitConversions

/**
 * This file contains activities and their mappings to JSON.
 * They implicitly convert to an OpaqueActivity which means you can use like so
 *
 *   Activities.log(<Tailored Activity>)
 */


final case class CustomerInfoChanged(
  customerId: Int, 
  oldInfo: UpdateCustomerPayload,
  newInfo: UpdateCustomerPayload)

object CustomerInfoChanged {
  val typeName = "customer_contact_changed"

  implicit val formats: DefaultFormats.type = DefaultFormats

  implicit def typed2opaque(a: CustomerInfoChanged) : OpaqueActivity = {
    val t = typeName
    OpaqueActivity(t, Extraction.decompose(a))
  }
}


