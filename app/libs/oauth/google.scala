package libs.oauth


final case class GoogleOauthOptions(
  clientId: String,
  clientSecret: String,
  redirectUri: String,
  accessType: String = "offlien",
  hostedDomain: Option[String] = None
) extends OauthClientOptions {

  override def buildExtraAuthParams: Map[String, String] = {
    Map.empty[String, String]
        .+? ("hd", hostedDomain)
        .+  ("access_type" → accessType)
  }
}

trait GoogleProvider extends OauthProvider {
  val oauthAuthorizationUrl = "https://accounts.google.com/o/oauth2/auth"
  val oauthAccessTokenUrl = "https://accounts.google.com/o/oauth2/token"
  val oauthInfoUrl = "https://www.googleapis.com/oauth2/v1/userinfo"
}
