package utils.seeds.generators

import models.product.SimpleContext
import models.objects._
import scala.util.Random
import utils.db._
import utils.db.DbResultT._

import payloads.{CreateDiscount, CreateDiscountForm, CreateDiscountShadow}
import services.discount.DiscountManager

import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.PostgresDriver.api._

object SimpleDiscount {
  type Percent = Int
}
import SimpleDiscount._

case class SimpleDiscount(discountId: Int = 0, formId: Int = 0, shadowId: Int = 0,  
  percentOff: Percent, totalAmount: Int)

case class SimpleDiscountForm(percentOff: Percent, totalAmount: Int) {

    val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "title" : "Get ${percentOff}% off when you spend $totalAmount dollars",
      "description" : "${percentOff}% off when you spend over $totalAmount dollars",
      "tags" : [],
      "qualifier" : {
        "orderTotalAmount" : {
          "totalAmount" : ${totalAmount*100}
        }
      },
      "offer" : {
        "orderPercentOff": {
          "discount": $percentOff
        }
      }
    }"""))
}

case class SimpleDiscountShadow(f: SimpleDiscountForm) { 

    val shadow = ObjectUtils.newShadow(parse(
      """
        {
          "title" : {"type": "string", "ref": "title"},
          "description" : {"type": "richText", "ref": "description"},
          "tags" : {"type": "tags", "ref": "tags"},
          "qualifier" : {"type": "qualifier", "ref": "qualifier"},
          "offer" : {"type": "offer", "ref": "offer"}
        }"""), 
      f.keyMap)
}

trait DiscountGenerator {

  def generateDiscount: SimpleDiscount = {
    val percent = Random.nextInt(90)
    val totalAmount = Random.nextInt(10)
    SimpleDiscount(
      percentOff = percent,
      totalAmount = totalAmount)
  }

  def generateDiscounts(data: Seq[SimpleDiscount])(implicit db: Database) = for {
    context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
    discounts ← * <~ DbResultT.sequence(data.map( d ⇒  {
      val discountForm = SimpleDiscountForm(d.percentOff, d.totalAmount)
      val discountShadow = SimpleDiscountShadow(discountForm)
      val payload = CreateDiscount(
        form = CreateDiscountForm(attributes = discountForm.form),
        shadow = CreateDiscountShadow(attributes = discountShadow.shadow))
      DbResultT(DBIO.from(DiscountManager.create(payload, context.name)))
    }))
  } yield discounts

}
