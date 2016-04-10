package models.promotion

import models.inventory._
import models.objects._
import models.Aliases.Json
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}

import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._
import java.time.Instant
import java.security.MessageDigest

object PromotionHelpers { 

  def name(f: ObjectForm, s: ObjectShadow) : JValue = {
    ObjectUtils.get("name", f, s)
  }

  def description(f: ObjectForm, s: ObjectShadow) : JValue = {
    ObjectUtils.get("description", f, s)
  }
}
