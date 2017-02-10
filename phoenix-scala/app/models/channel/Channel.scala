package models.channel

import java.time.Instant

import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.Validation

case class Channel(id: Int = 0,
                   defaultContextId: Int,
                   draftContextId: Int,
                   name: String,
                   createdAt: Instant = Instant.now,
                   updatedAt: Instant = Instant.now,
                   archivedAt: Option[Instant] = None)
    extends FoxModel[Channel]
    with Validation[Channel]

class Channels(tag: Tag) extends FoxTable[Channel](tag, "channels") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def defaultContextId = column[Int]("default_context_id")
  def draftContextId   = column[Int]("draft_context_id")
  def name             = column[String]("name")
  def createdAt        = column[Instant]("created_at")
  def updatedAt        = column[Instant]("updated_at")
  def archivedAt       = column[Option[Instant]]("archived_at")

  def * =
    (id, defaultContextId, draftContextId, name, createdAt, updatedAt, archivedAt) <> ((Channel.apply _).tupled, Channel.unapply)
}

object Channels
    extends FoxTableQuery[Channel, Channels](new Channels(_))
    with ReturningId[Channel, Channels] {
  val returningLens: Lens[Channel, Int] = lens[Channel].id
}
