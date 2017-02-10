package responses

import java.time.Instant

import models.account.Users
import models.admin.AdminsData
import models.cord.Orders
import models.customer.CustomersData
import models.inventory.Sku
import models.objects._
import models.payment.PaymentMethod
import models.payment.giftcard.GiftCard
import models.product.Mvp
import models.returns._
import models.shipping.Shipment
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse.{Root ⇒ User}
import responses.cord.OrderResponse
import services.returns.ReturnTotaler
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.aliases._
import utils.db._

object ReturnReasonsResponse {}
