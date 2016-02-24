package payloads

import models.auth.Identity.IdentityKind

final case class LoginPayload(email: String, password: String, kind: IdentityKind)