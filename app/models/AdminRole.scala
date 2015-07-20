package models

import utils.Model

final case class AdminRole (id: Int, name: String, description: String) extends Model

class AdminRoles {}

object AdminRoles {}
