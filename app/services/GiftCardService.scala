package services

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.model.HttpResponse
import models.GiftCards
import responses.GiftCardResponse
import slick.driver.PostgresDriver.api._
import utils.Http._
import utils.Slick.implicits._

object GiftCardService {
  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Future[HttpResponse] = {
    GiftCards.findByCode(code).one.run().map {
      case Some(gc) ⇒ render(GiftCardResponse.build(gc))
      case None ⇒ notFoundResponse
    }
  }
}