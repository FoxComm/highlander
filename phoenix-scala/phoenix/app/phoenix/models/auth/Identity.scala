package phoenix.models.auth

import com.pellucid.sealerate
import core.ADT

object Identity {
  sealed trait IdentityKind
  case object User    extends IdentityKind
  case object Service extends IdentityKind

  object IdentityKind extends ADT[IdentityKind] {
    def types = sealerate.values[IdentityKind]
  }
}
