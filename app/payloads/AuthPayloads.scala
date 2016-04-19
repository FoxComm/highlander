package payloads

import models.auth.Identity.IdentityKind

case class LoginPayload(email: String, password: String, kind: IdentityKind)
