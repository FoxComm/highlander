package models.customer

import java.time.Instant

import shapeless._
import utils.db._
import utils.aliases._
import com.github.tminglei.slickpg.LTree
import utils.db.ExPostgresDriver.api._

case class GroupTemplateInstance(id: Int = 0,
                                 groupTemplateId: Int,
                                 groupId: Int,
                                 scope: LTree,
                                 deletedAt: Option[Instant] = None)
    extends FoxModel[GroupTemplateInstance]

class GroupTemplateInstances(tag: Tag)
    extends FoxTable[GroupTemplateInstance](tag, "group_template_instances") {
  def id              = column[Int]("id", O.PrimaryKey)
  def groupTemplateId = column[Int]("group_template_id")
  def groupId         = column[Int]("group_id")
  def scope           = column[LTree]("scope")
  def deletedAt       = column[Option[Instant]]("deleted_at")

  def * =
    (id, groupTemplateId, groupId, scope, deletedAt) <> ((GroupTemplateInstance.apply _).tupled, GroupTemplateInstance.unapply)
}

object GroupTemplateInstances
    extends FoxTableQuery[GroupTemplateInstance, GroupTemplateInstances](
        new GroupTemplateInstances(_))
    with ReturningId[GroupTemplateInstance, GroupTemplateInstances] {

  def findByScope(scope: LTree): QuerySeq = filter(_.scope === scope)

  def findByScopeAndGroupId(scope: LTree, groupId: Int): QuerySeq =
    findByScope(scope).filter(_.groupId === groupId)

  val returningLens: Lens[GroupTemplateInstance, Int] = lens[GroupTemplateInstance].id
}
