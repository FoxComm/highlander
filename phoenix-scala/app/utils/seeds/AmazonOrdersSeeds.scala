package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.cord.AmazonOrder
import com.github.tminglei.slickpg.LTree
import utils.aliases._
import utils.db._
import java.time.Instant
import utils.Money.Currency

trait AmazonOrdersSeeds {
  def amazonOrder =
    AmazonOrder(id = 0,
                amazonOrderId = "112233",
                orderTotal = 4500,
                paymentMethodDetail = "CreditCard",
                orderType = "StandardOrder",
                currency = Currency.USD,
                orderStatus = "Shipped",
                purchaseDate = Instant.now,
                scope = LTree("1"),
                accountId = 0,
                createdAt = Instant.now,
                updatedAt = Instant.now)
}
