package libs.oauth


trait GoogleProvider extends OauthProvider {

  val oauthAuthorizationUrl = "https://accounts.google.com/o/oauth2/auth"
  val oauthAccessTokenUrl = "https://accounts.google.com/o/oauth2/token"
  val oauthInfoUrl = "https://www.googleapis.com/oauth2/v1/userinfo"
}
