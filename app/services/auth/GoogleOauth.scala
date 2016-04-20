package services.auth

import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth, UserInfo}
import models.auth.{AdminToken, CustomerToken, Identity, Token}
import models.customer.{Customer, Customers}
import models.{StoreAdmin, StoreAdmins}
import utils.Config._
import utils.aliases._
import utils.db._

class GoogleOauthStoreAdmin(options: GoogleOauthOptions)
  extends Oauth(options)
    with OauthService[StoreAdmin]
    with GoogleProvider {

    def createByUserInfo(userInfo: UserInfo)(implicit ec: EC): DbResult[StoreAdmin] = {
      StoreAdmins.create(StoreAdmin(email = userInfo.email, name = userInfo.name) )
    }

    def findByEmail(email: String)(implicit ec: EC, db: DB) = StoreAdmins.findByEmail(email)

    def createToken(admin: StoreAdmin): Token = {
      AdminToken.fromAdmin(admin)
    }
}

class GoogleOauthCustomer(options: GoogleOauthOptions)
  extends Oauth(options)
    with OauthService[Customer]
    with GoogleProvider {

  def createByUserInfo(userInfo: UserInfo)(implicit ec: EC): DbResult[Customer] = {
    Customers.create(Customer(email = userInfo.email, name = Some(userInfo.name)))
  }

  def findByEmail(email: String)(implicit ec: EC, db: DB) = Customers.findByEmail(email)

  def createToken(customer: Customer): Token = {
    CustomerToken.fromCustomer(customer)
  }
}

object GoogleOauth {

  def oauthServiceFromConfig(identity: Identity.IdentityKind) = {
    val configPrefix = identity.toString.toLowerCase

    val opts = GoogleOauthOptions(
      clientId = config.getString(s"oauth.$configPrefix.google.client_id"),
      clientSecret = config.getString(s"oauth.$configPrefix.google.client_secret"),
      redirectUri = config.getString(s"oauth.$configPrefix.google.redirect_uri"),
      hostedDomain = config.getOptString(s"oauth.$configPrefix.google.hosted_domain"))

    identity match {
      case Identity.Customer ⇒ new GoogleOauthCustomer(opts)
      case Identity.Admin ⇒ new GoogleOauthStoreAdmin(opts)
      case _ ⇒ throw new RuntimeException(s"Identity $configPrefix not supported for google oauth.")
    }

  }

}

