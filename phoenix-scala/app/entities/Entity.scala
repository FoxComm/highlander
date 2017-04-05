package entities

import models.objects._
import slick.lifted.Rep
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._

case class Entity(id: Int, commitId: Int, slug: String, attributes: Json)

object Entity {
  def build(form: ObjectForm, shadow: ObjectShadow, commit: ObjectCommit) = {}
}

class Entities {
  type QueryCommitFn = () ⇒ Query[ObjectCommits, ObjectCommit, Seq]
  type QueryCore = Query[(ObjectForms, ObjectShadows, ObjectCommits),
                         (ObjectForm, ObjectShadow, ObjectCommit),
                         Seq]

  // def filter(ref: EntityReference, contextId: ObjectContext#Id): QueryCore =
  //   filterCore(fnFilterCommitByHead(ref, contextId))

  def filterByCommit(commitId: Int): QueryCore =
    filterCore(fnFilterCommitById(commitId))

  private def fnFilterCommitById(commitId: Int): QueryCommitFn =
    () ⇒ ObjectCommits.filter(_.id === commitId)

  private def filterCore(filterCommit: QueryCommitFn): QueryCore =
    for {
      commit ← filterCommit()
      form   ← ObjectForms if commit.formId === form.id
      shadow ← ObjectShadows if commit.shadowId === shadow.id
    } yield (form, shadow, commit)
}
