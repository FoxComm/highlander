package utils.seeds.generators

import models.product.SimpleContext
import models.promotion._
import models.objects._
import services.Result
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import payloads.{CreatePromotion, CreatePromotionForm, CreatePromotionShadow,
  CreateDiscountForm, CreateDiscountShadow}
import services.promotion.PromotionManager

import cats.data.Xor
import java.time.Instant
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Random
import slick.driver.PostgresDriver.api._

object SimplePromotion {
  type Percent = Int
}
import SimplePromotion._

final case class SimplePromotion(promotionId: Int = 0, formId: Int = 0, shadowId: Int = 0,  
  percentOff: Percent, totalAmount: Int, applyType: Promotion.ApplyType = Promotion.Auto)

final case class SimplePromotionForm(percentOff: Percent, totalAmount: Int) {

    val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "name" : "${percentOff}% over $totalAmount items",
      "storefrontName" : "${percentOff}% over $totalAmount items",
      "description" : "${percentOff}% full order over $totalAmount items",
      "details" : "This offer applies only when you have a total amount over $totalAmount items",
      "activeFrom" : "${Instant.now}",
      "activeTo" : null,
      "tags" : []
      }
    }"""))
}

final case class SimplePromotionShadow(f: SimplePromotionForm) { 

    val shadow = ObjectUtils.newShadow(parse(
      s"""
        {
          "name" : {"type": "string", "ref": "name"},
          "storefrontName" : {"type": "richText", "ref": "storefrontName"},
          "description" : {"type": "richText", "ref": "description"},
          "details" : {"type": "richText", "ref": "details"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "activeTo" : {"type": "date", "ref": "activeTo"},
          "tags" : {"type": "tags", "ref": "tags"}
        }"""), 
      f.keyMap)
}

trait PromotionGenerator {

  def generatePromotion(applyType: Promotion.ApplyType = Promotion.Auto): 
  SimplePromotion = {
    val percent = Random.nextInt(90)
    val totalAmount = Random.nextInt(10)
    SimplePromotion(
      applyType = applyType,
      percentOff = percent,
      totalAmount = totalAmount)
  }

  def generatePromotions(data: Seq[SimplePromotion])(implicit db: Database) = for {
    context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
    promotions ← * <~ DbResultT.sequence(data.map( d ⇒  {
        val promotionForm = SimplePromotionForm(d.percentOff, d.totalAmount)
        val promotionShadow = SimplePromotionShadow(promotionForm)
        val discountForm = SimpleDiscountForm(d.percentOff, d.totalAmount)
        val discountShadow = SimpleDiscountShadow(discountForm)
        val payload = CreatePromotion(
          applyType = d.applyType,
          form = CreatePromotionForm(attributes = promotionForm.form, 
            discounts = Seq(CreateDiscountForm(attributes = discountForm.form))),
          shadow = CreatePromotionShadow(attributes = promotionShadow.shadow, 
            discounts = Seq(CreateDiscountShadow( attributes = discountShadow.shadow))))
        DbResultT(DBIO.from(PromotionManager.create(payload, context.name).flatMap{
          case Xor.Right(r) ⇒ Result.right(d.copy(promotionId = r.form.id))
          case Xor.Left(l) ⇒  Result.failures(l)
        }))
    }))
  } yield promotions

}
