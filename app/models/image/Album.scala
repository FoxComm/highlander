package models.image

import java.time.Instant

import models.objects._
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db._
import utils.{JsonFormatters, Validation}

object Album {
  val kind = "album"
}

case class Album(id: Int = 0,
                 contextId: Int,
                 shadowId: Int,
                 formId: Int,
                 commitId: Int,
                 updatedAt: Instant = Instant.now,
                 createdAt: Instant = Instant.now)
    extends FoxModel[Album]
    with Validation[Album]

class Albums(tag: Tag) extends ObjectHeads[Album](tag, "albums") {
  def * =
    (id, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Album.apply _).tupled, Album.unapply)
}

object Albums extends FoxTableQuery[Album, Albums](new Albums(_)) with ReturningId[Album, Albums] {

  val returningLens: Lens[Album, Int] = lens[Album].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filterByContext(contextId).filter(_.formId === formId)
}
