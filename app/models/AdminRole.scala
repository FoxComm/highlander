package models

import utils.ModelWithIdParameter

final case class AdminRole (id: Int, name: String, description: String) extends ModelWithIdParameter[AdminRole]

class AdminRoles

object AdminRoles
