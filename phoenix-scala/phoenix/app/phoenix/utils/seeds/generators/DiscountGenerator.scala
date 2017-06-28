package phoenix.utils.seeds.generators

import core.db._
import objectframework.ObjectUtils
import objectframework.ObjectUtils._
import objectframework.models._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import phoenix.models.discount.Discount
import phoenix.models.product.SimpleContext
import phoenix.payloads.DiscountPayloads._
import phoenix.responses.DiscountResponses.DiscountResponse
import phoenix.services.discount.DiscountManager
import phoenix.utils.aliases._
import phoenix.utils.seeds.generators.SimpleDiscount._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

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

  val shadow: JValue = ObjectUtils.newShadow(
    parse("""
        {
          "title" : {"type": "string", "ref": "title"},
          "description" : {"type": "richText", "ref": "description"},
          "tags" : {"type": "tags", "ref": "tags"},
          "qualifier" : {"type": "qualifier", "ref": "qualifier"},
          "offer" : {"type": "offer", "ref": "offer"}
        }"""),
    f.keyMap
  )
}

trait DiscountGenerator {

  def generateDiscount: SimpleDiscount = {
    val percent     = Random.nextInt(90)
    val totalAmount = Random.nextInt(10)
    SimpleDiscount(percentOff = percent, totalAmount = totalAmount)
  }

  def generateDiscounts(sourceData: Seq[SimpleDiscount])(implicit db: DB,
                                                         au: AU): DbResultT[Seq[DiscountResponse]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      discounts ← * <~ sourceData.map(source ⇒ {
                   val discountForm   = SimpleDiscountForm(source.percentOff, source.totalAmount)
                   val discountShadow = SimpleDiscountShadow(discountForm)
                   def discountFS: FormAndShadow =
                     (ObjectForm(kind = Discount.kind, attributes = discountForm.form),
                      ObjectShadow(attributes = discountShadow.shadow))
                   val payload = CreateDiscount(attributes = discountFS.toPayload)
                   DiscountManager.create(payload, context.name)
                 })
    } yield discounts
}
