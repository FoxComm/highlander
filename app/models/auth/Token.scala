package models.auth

import responses.ResponseItem
import utils.Strings.EnrichedString
import models.{StoreAdmin, BaseStoreAdmin}

sealed trait Token {
  val id: Int
  val admin: Boolean
  val name: String
  val email: String
  val scopes: Seq[String]
}

final case class AdminToken(id: Int,
  admin: Boolean = true,
  name: String,
  email: String,
  scopes: Seq[String],
  department: Option[String] = None
) extends Token with BaseStoreAdmin

object AdminToken {
  def fromAdmin(admin: StoreAdmin): AdminToken = {
    AdminToken(id = admin.id, name = admin.name, email = admin.email,
      scopes = Array("admin"),
      department = admin.department)
  }
}


final case class CustomerToken(id: Int,
  admin: Boolean = false,
  name: String,
  email: String,
  scopes: Seq[String]
  ) extends Token