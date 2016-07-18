package models.auth

import com.pellucid.sealerate
import utils.ADT

object Identity {
  sealed trait IdentityKind
  case object Admin    extends IdentityKind
  case object Customer extends IdentityKind

  object IdentityKind extends ADT[IdentityKind] {
    def types = sealerate.values[IdentityKind]
  }
}
