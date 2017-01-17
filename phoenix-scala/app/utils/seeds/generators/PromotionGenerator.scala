package utils.seeds.generators

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import models.objects._
import models.objects.ObjectUtils._
import models.product.SimpleContext
import models.promotion._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import payloads.DiscountPayloads._
import payloads.PromotionPayloads._
import services.promotion.PromotionManager
import utils.aliases._
import utils.db._
import utils.seeds.generators.SimplePromotion._

object SimplePromotion {
  type Percent = Int
}

case class SimplePromotion(promotionId: Int = 0,
                           formId: Int = 0,
                           shadowId: Int = 0,
                           percentOff: Percent,
                           totalAmount: Int,
                           applyType: Promotion.ApplyType = Promotion.Auto)

case class SimplePromotionForm(percentOff: Percent, totalAmount: Int) {

  val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "name" : "$percentOff% off after spending $totalAmount dollars",
      "storefrontName" : "$percentOff% off after spending $totalAmount dollars",
      "description" : "$percentOff% off full order after spending $totalAmount dollars",
      "details" : "This offer applies after you spend over $totalAmount dollars",
      "activeFrom" : "${Instant.now}",
      "activeTo" : null,
      "tags" : []
      }
    }"""))
}

case class SimplePromotionShadow(f: SimplePromotionForm) {

  val shadow = ObjectUtils.newShadow(
    parse("""
      {
        "name" : {"type": "string", "ref": "name"},
        "storefrontName" : {"type": "richText", "ref": "storefrontName"},
        "description" : {"type": "text", "ref": "description"},
        "details" : {"type": "richText", "ref": "details"},
        "activeFrom" : {"type": "date", "ref": "activeFrom"},
        "activeTo" : {"type": "date", "ref": "activeTo"},
        "tags" : {"type": "tags", "ref": "tags"}
      }"""),
    f.keyMap)
}

trait PromotionGenerator {

  val percents = Seq(10, 15, 20, 25, 30, 45)
  val amounts  = Seq(10, 15, 20, 25, 30, 45, 50)

  def generatePromotion(applyType: Promotion.ApplyType = Promotion.Auto): SimplePromotion = {
    val percent     = percents(Random.nextInt(percents.length))
    val totalAmount = amounts(Random.nextInt(amounts.length))
    SimplePromotion(applyType = applyType, percentOff = percent, totalAmount = totalAmount)
  }

  def generatePromotions(sourceData: Seq[SimplePromotion])(
      implicit db: DB,
      ac: AC,
      au: AU): DbResultT[Seq[SimplePromotion]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      promotions ← * <~ sourceData.map(source ⇒ {
        val promotionForm   = SimplePromotionForm(source.percentOff, source.totalAmount)
        val promotionShadow = SimplePromotionShadow(promotionForm)
        val discountForm    = SimpleDiscountForm(source.percentOff, source.totalAmount)
        val discountShadow  = SimpleDiscountShadow(discountForm)

        def discountFS: FormAndShadow = {
          (ObjectForm(kind = Promotion.kind, attributes = discountForm.form),
           ObjectShadow(attributes = discountShadow.shadow))
        }
        val promotionFS: FormAndShadow = {
          (ObjectForm(kind = Promotion.kind, attributes = promotionForm.form),
           ObjectShadow(attributes = promotionShadow.shadow))
        }

        val payload =
          CreatePromotion(applyType = source.applyType,
                          attributes = promotionFS.toPayload,
                          discounts = Seq(CreateDiscount(attributes = discountFS.toPayload)))

        PromotionManager.create(payload, context.name, None).map { newPromo ⇒
          source.copy(promotionId = newPromo.id)
        }
      })
    } yield promotions
}
