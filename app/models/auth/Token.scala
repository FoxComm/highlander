package models.auth

import models.StoreAdmin
import models.customer.Customer

sealed trait Token {
  val id: Int
  val admin: Boolean
  val name: Option[String]
  val email: String
  val scopes: Seq[String]
}

final case class AdminToken(id: Int,
  admin: Boolean = true,
  name: Option[String],
  email: String,
  scopes: Seq[String],
  department: Option[String] = None
) extends Token

object AdminToken {
  def fromAdmin(admin: StoreAdmin): AdminToken = {
    AdminToken(id = admin.id, name = Some(admin.name), email = admin.email,
      scopes = Array("admin"),
      department = admin.department)
  }
}


final case class CustomerToken(id: Int,
  admin: Boolean = false,
  name: Option[String],
  email: String,
  scopes: Seq[String]
  ) extends Token

object CustomerToken {
  def fromCustomer(customer: Customer): CustomerToken = {
    CustomerToken(id = customer.id, name = customer.name, email = customer.email,
      scopes = Array[String]())
  }
}