package models.channel

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import failures._
import payloads.ChannelPayloads.CreateChannelPayload
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.Validation

case class Channel(id: Int,
                   scope: LTree,
                   defaultContextId: Int,
                   draftContextId: Int,
                   location: Channel.Location,
                   name: String,
                   createdAt: Instant,
                   updatedAt: Instant,
                   archivedAt: Option[Instant])
    extends FoxModel[Channel]
    with Validation[Channel]

object Channel {
  sealed trait Location
  case object Local  extends Location
  case object Remote extends Location
  case object Legacy extends Location

  object Location extends ADT[Location] {
    def types = sealerate.values[Location]
  }

  def build(payload: CreateChannelPayload,
            defaultContextId: Int,
            draftContextId: Int,
            scope: LTree): Channel =
    Channel(id = 0,
            scope = scope,
            defaultContextId = defaultContextId,
            draftContextId = draftContextId,
            location = payload.location,
            name = payload.name,
            createdAt = Instant.now,
            updatedAt = Instant.now,
            archivedAt = None)

  implicit val locationColumnType: JdbcType[Location] with BaseTypedType[Location] =
    Location.slickColumn
}

class Channels(tag: Tag) extends FoxTable[Channel](tag, "channels") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope            = column[LTree]("scope")
  def defaultContextId = column[Int]("default_context_id")
  def draftContextId   = column[Int]("draft_context_id")
  def location         = column[Channel.Location]("location")
  def name             = column[String]("name")
  def createdAt        = column[Instant]("created_at")
  def updatedAt        = column[Instant]("updated_at")
  def archivedAt       = column[Option[Instant]]("archived_at")

  def * =
    (id, scope, defaultContextId, draftContextId, location, name, createdAt, updatedAt, archivedAt) <> ((Channel.apply _).tupled, Channel.unapply)
}

object Channels
    extends FoxTableQuery[Channel, Channels](new Channels(_))
    with ReturningId[Channel, Channels] {
  val returningLens: Lens[Channel, Int] = lens[Channel].id

  def filterActive(id: Int): QuerySeq =
    filter(_.id === id).filter(_.archivedAt.isEmpty === true)

  def mustFindActive404(id: Int)(implicit ec: EC): DbResultT[Channel] =
    filterActive(id).one.mustFindOr(NotFoundFailure404(s"channel with id=$id not found"))
}
