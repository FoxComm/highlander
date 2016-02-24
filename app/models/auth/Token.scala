package models.auth

import models.{StoreAdmin, BaseStoreAdmin}
import com.pellucid.sealerate
import utils.ADT


object Identity {
  sealed trait IdentityKind
  case object Admin extends IdentityKind
  case object Customer extends IdentityKind

  object IdentityKind extends ADT[IdentityKind] {
    def types = sealerate.values[IdentityKind]
  }
}

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