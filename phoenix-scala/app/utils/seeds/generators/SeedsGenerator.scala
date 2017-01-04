package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import cats.implicits._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineMV
import faker.Faker
import models.account._
import models.cord._
import models.coupon._
import models.customer._
import models.inventory._
import models.location.{Address, Addresses}
import models.objects.{ObjectContext, ObjectContexts}
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.product.SimpleContext
import models.promotion._
import org.json4s.JObject
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.seeds.Seeds.Factories

object SeedsGenerator
    extends CustomerGenerator
    with AddressGenerator
    with CreditCardGenerator
    with OrderGenerator
    with GiftCardGenerator
    with ProductGenerator
    with PromotionGenerator
    with CouponGenerator {

  def generateAddresses(customers: Seq[User]): Seq[Address] = {
    customers.flatMap { c ⇒
      generateAddress(customer = c, isDefault = true) +:
      ((0 to Random.nextInt(2)) map { i ⇒
            generateAddress(customer = c, isDefault = false)
          })
    }
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

  def makeCouponCodes(promotions: Seq[SimpleCoupon]) =
    promotions.flatMap { c ⇒
      val prefix     = refineMV[NonEmpty]("CP")
      val codeLength = refineMV[Positive](12)
      val quantity   = refineMV[Positive](3)
      CouponCodes.generateCodes(prefix, codeLength, quantity).map { d ⇒
        CouponCode(couponFormId = c.formId, code = d)
      }
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
      _ ← * <~ Addresses.createAll(generateAddresses(customers))
      _ ← * <~ CreditCards.createAll(generateCreditCards(customers))
      orderedGcs ← * <~ randomSubset(customerIds).map { id ⇒
                    generateGiftCardPurchase(id, context)
                  }
      appeasements ← * <~ (1 to appeasementCount).map(i ⇒ generateGiftCardAppeasement)

      giftCards ← * <~ orderedGcs ++ appeasements
      unsavedPromotions = makePromotions(1)
      promotions     ← * <~ generatePromotions(unsavedPromotions)
      unsavedCoupons ← * <~ makeCoupons(promotions.filter(_.applyType == Promotion.Coupon))
      coupons        ← * <~ generateCoupons(unsavedCoupons)
      unsavedCodes   ← * <~ makeCouponCodes(coupons)
      _              ← * <~ CouponCodes.createAll(unsavedCodes)
      _ ← * <~ randomSubset(customerIds, customerIds.length).map { id ⇒
           generateOrders(id, context, skuIds, pickOne(giftCards))
         }
    } yield {}
  }
}
