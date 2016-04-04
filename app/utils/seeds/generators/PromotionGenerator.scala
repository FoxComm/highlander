package utils.seeds.generators

import models.product.SimpleContext
import models.promotion._
import models.objects._
import scala.util.Random
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import payloads.{CreatePromotion, CreatePromotionForm, CreatePromotionShadow,
  CreateDiscountForm, CreateDiscountShadow}
import services.promotion.PromotionManager

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import slick.driver.PostgresDriver.api._
import java.time.Instant

object SimplePromotion {
  type Percent = Int
}
import SimplePromotion._

final case class SimplePromotion(promotionId: Int = 0, formId: Int = 0, shadowId: Int = 0,  
  percentOff: Percent, totalAmount: Int)

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

  def generatePromotion: SimplePromotion = {
    val percent = Random.nextInt(90)
    val totalAmount = Random.nextInt(10)
    SimplePromotion(
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
          form = CreatePromotionForm(attributes = promotionForm.form, 
            discounts = Seq(CreateDiscountForm(attributes = discountForm.form))),
          shadow = CreatePromotionShadow(attributes = promotionShadow.shadow, 
            discounts = Seq(CreateDiscountShadow( attributes = discountShadow.shadow))))
        DbResultT(DBIO.from(PromotionManager.create(payload, context.name)))
    }))
  } yield promotions

}
