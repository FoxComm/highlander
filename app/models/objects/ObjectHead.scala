package models.objects

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant

/**
 * Abstract class to help define an object head object which points to the latest
 * version of some object in the context specified.
 */
abstract class ObjectHeads[C <: utils.ModelWithIdParameter[C]](tag: Tag, table: String) extends GenericTable.TableWithId[C](tag, table)  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def contextId = column[Int]("context_id")
  def shadowId = column[Int]("shadow_id")
  def formId = column[Int]("form_id")
  def commitId = column[Int]("commit_id")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def context = foreignKey(ObjectContexts.tableName, contextId, ObjectContexts)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, shadowId, ObjectShadows)(_.id)
  def form = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
  def commit = foreignKey(ObjectCommits.tableName, commitId, ObjectCommits)(_.id)
}
