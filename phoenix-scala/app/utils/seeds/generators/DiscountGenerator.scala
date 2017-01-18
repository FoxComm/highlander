package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import models.objects._
import models.objects.ObjectUtils._
import models.product.SimpleContext
import org.json4s._
import org.json4s.jackson.JsonMethods._
import payloads.DiscountPayloads._
import responses.DiscountResponses.DiscountResponse
import services.discount.DiscountManager
import utils.aliases._
import utils.db._
import utils.seeds.generators.SimpleDiscount._

object SimpleDiscount {
  type Percent = Int
}

case class SimpleDiscount(discountId: Int = 0,
                          formId: Int = 0,
                          shadowId: Int = 0,
                          percentOff: Percent,
                          totalAmount: Int)

case class SimpleDiscountForm(percentOff: Percent, totalAmount: Int) {

  val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "title" : "Get $percentOff% off when you spend $totalAmount dollars",
      "description" : "$percentOff% off when you spend over $totalAmount dollars",
      "tags" : [],
      "qualifier" : {
        "orderTotalAmount" : {
          "totalAmount" : ${totalAmount * 100}
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

  val shadow = ObjectUtils.newShadow(
    parse("""
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
    val percent     = Random.nextInt(90)
    val totalAmount = Random.nextInt(10)
    SimpleDiscount(percentOff = percent, totalAmount = totalAmount)
  }

  def generateDiscounts(sourceData: Seq[SimpleDiscount])(
      implicit db: DB,
      au: AU): DbResultT[Seq[DiscountResponse.Root]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      discounts ← * <~ sourceData.map(source ⇒ {
        val discountForm   = SimpleDiscountForm(source.percentOff, source.totalAmount)
        val discountShadow = SimpleDiscountShadow(discountForm)
        def discountFS: FormAndShadow = {
          (ObjectForm(kind = models.discount.Discount.kind, attributes = discountForm.form),
           ObjectShadow(attributes = discountShadow.shadow))
        }
        val payload = CreateDiscount(attributes = discountFS.toPayload)
        DiscountManager.create(payload, context.name)
      })
    } yield discounts
}
