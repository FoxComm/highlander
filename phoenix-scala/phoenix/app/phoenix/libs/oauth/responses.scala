package phoenix.libs.oauth

case class AccessTokenResponse(
    access_token: String,
    expires_in: Int,
    refresh_token: Option[String] = None,
    token_type: String = "Bearer"
)
