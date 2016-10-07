package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import cats.implicits._
import faker.{Faker, Lorem}
import models.cord._
import models.coupon._
import models.customer._
import models.account._
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

object RankingSeedsGenerator {
  def fakeJson = JObject()

  def generateCustomer: User =
    User(accountId = 0,
         email = s"${randomString(10)}@email.com".some,
         name = Some(randomString(10)))

  def generateOrderPayment[A <: PaymentMethod with FoxModel[A]](
      orderRef: String,
      paymentMethod: A,
      amount: Int = 100): OrderPayment = {
    Factories.orderPayment
      .copy(cordRef = orderRef, amount = Some(amount), paymentMethodId = paymentMethod.id)
  }

  def generateAddress: Address =
    Address(accountId = 0,
            regionId = 4177,
            name = faker.Name.name,
            address1 = randomString(30),
            address2 = None,
            city = "Seattle",
            zip = "12345",
            isDefaultShipping = false,
            phoneNumber = None)

  def generateGroup(ownerId: Int): CustomerDynamicGroup =
    CustomerDynamicGroup(name = s"${randomString(10)}Group",
                         createdBy = ownerId,
                         clientState = fakeJson,
                         elasticRequest = fakeJson,
                         customersCount = Some(Random.nextInt))

  def insertRankingSeeds(customersCount: Int)(implicit db: Database) = {

    val location = "Arkham"

    def makeOrders(c: User, context: ObjectContext) = {
      (1 to 5 + Random.nextInt(20)).map { i ⇒
        for {
          cart  ← * <~ Carts.create(Cart(accountId = c.accountId))
          order ← * <~ Orders.createFromCart(cart, context.id)
          order ← * <~ Orders.update(order, order.copy(state = Order.FulfillmentStarted))
          order ← * <~ Orders.update(order, order.copy(state = Order.Shipped))
        } yield order
      }
    }

    def makePayment(o: Order, pm: CreditCard) = {
      generateOrderPayment(o.refNum, pm, Random.nextInt(20000) + 100)
    }

    def insertAccounts =
      Accounts.createAllReturningIds((1 to customersCount).map { _ ⇒
        Account()
      })

    def insertCustomers(accountIds: Seq[Int]) =
      Users.createAll(accountIds.map { accountId ⇒
        val s = randomString(15)
        User(accountId = accountId, name = s.some, email = s"$s-$accountId@email.com".some)
      })

    def insertOrders() =
      for {
        context   ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
        customers ← * <~ Users.result
        newCreditCards ← * <~ customers.map { c ⇒
                          Factories.creditCard.copy(accountId = c.accountId,
                                                    holderName = c.name.getOrElse(""))
                        }
        _ ← * <~ CreditCards.createAll(newCreditCards)
        _ ← * <~ customers.flatMap(c ⇒ makeOrders(c, context))
      } yield {}

    def insertPayments() = {
      val action = (for {
        (o, cc) ← Orders.join(CreditCards).on(_.accountId === _.accountId)
      } yield (o, cc)).result

      for {
        ordersWithCc ← * <~ action
        _ ← * <~ OrderPayments.createAll(ordersWithCc.map {
             case (order, cc) ⇒ makePayment(order, cc)
           })
      } yield {}
    }

    for {
      accountIds ← * <~ insertAccounts
      _          ← * <~ insertCustomers(accountIds)
      _          ← * <~ insertOrders
      _          ← * <~ insertPayments
    } yield {}
  }

  def randomOrderState: Order.State = {
    val types = Order.State.types
    val index = Random.nextInt(types.size)
    types.drop(index).head
  }

  def randomString(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase
}

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
      CouponCodes.generateCodes("CP", 12, 1 + Random.nextInt(5)).map { d ⇒
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
           CustomerData(accountId = c.accountId, userId = c.id)
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
