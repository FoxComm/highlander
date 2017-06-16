package phoenix.utils.seeds.generators

import core.db._
import core.failures.NotFoundFailure404
import faker.Faker
import objectframework.models.ObjectContexts
import phoenix.models.account._
import phoenix.models.admin.{AdminData, AdminsData}
import phoenix.models.coupon._
import phoenix.models.customer._
import phoenix.models.inventory._
import phoenix.models.location.{Address, Addresses}
import phoenix.models.payment.creditcard.CreditCards
import phoenix.models.product.SimpleContext
import phoenix.models.promotion._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

object SeedsGenerator
    extends CustomerGenerator
    with AddressGenerator
    with CreditCardGenerator
    with OrderGenerator
    with GiftCardGenerator
    with ProductGenerator
    with PromotionGenerator
    with CouponGenerator {

  def generateAddresses(customers: Seq[User]): Seq[Address] =
    customers.flatMap { c ⇒
      generateAddress(customer = c, isDefault = true) +:
      ((0 to Random.nextInt(2)) map { i ⇒
        generateAddress(customer = c, isDefault = false)
      })
    }

  def makePromotions(promotionCount: Int) =
    (1 to promotionCount).par.map { i ⇒
      generatePromotion(Random.nextInt(2) match {
        case 0 ⇒ Promotion.Auto
        case _ ⇒ Promotion.Coupon
      })
    }.toList

  def makeCoupons(promotions: Seq[SimplePromotion]) =
    promotions.par.map { p ⇒
      generateCoupon(p)
    }.toList

  def pickOne[T](vals: Seq[T]): T = vals(Random.nextInt(vals.length))

  def insertRandomizedSeeds(customersCount: Int,
                            appeasementCount: Int)(implicit ec: EC, db: DB, ac: AC, au: AU) = {
    Faker.locale("en")

    for {
      context     ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      shipMethods ← * <~ getShipmentRules
      skus        ← * <~ Skus.filter(_.contextId === context.id).result
      skuIds             = skus.map(_.id)
      generatedCustomers = generateCustomers(customersCount)
      accountIds ← * <~ Accounts.createAllReturningIds(generatedCustomers.map { _ ⇒
                    Account()
                  })
      accountCustomers = accountIds zip generatedCustomers
      customerIds ← * <~ Users.createAllReturningIds(accountCustomers.map {
                     case (accountId, customer) ⇒
                       customer.copy(accountId = accountId)
                   })
      customers ← * <~ Users.filter(_.id.inSet(customerIds)).result
      _ ← * <~ CustomersData.createAll(customers.map { c ⇒
           CustomerData(accountId = c.accountId, userId = c.id, scope = Scope.current)
         })
      _            ← * <~ Addresses.createAll(generateAddresses(customers))
      _            ← * <~ CreditCards.createAll(generateCreditCards(customers))
      admin        ← * <~ AdminsData.take(1).mustFindOneOr(NotFoundFailure404(AdminData, "first"))
      orderedGcs   ← * <~ (1 to appeasementCount).map(_ ⇒ generateGiftCard(admin.accountId, context))
      appeasements ← * <~ (1 to appeasementCount).map(_ ⇒ generateGiftCardAppeasement)
      giftCards    ← * <~ orderedGcs ++ appeasements
      unsavedPromotions = makePromotions(1)
      promotions     ← * <~ generatePromotions(unsavedPromotions)
      unsavedCoupons ← * <~ makeCoupons(promotions.filter(_.applyType == Promotion.Coupon))
      coupons        ← * <~ generateCoupons(unsavedCoupons)
      _ ← * <~ randomSubset(customerIds, customerIds.length).map { id ⇒
           generateOrders(id, context, skuIds, pickOne(giftCards))
         }
    } yield {}
  }
}
