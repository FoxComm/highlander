package objectframework.db

import core.db.ExPostgresDriver.api._
import core.db._

import objectframework.content._

object ContentQueries {
  type QuerySeq     = Query[(Commits, Forms, Shadows), (Commit, Form, Shadow), Seq]
  type HeadQuerySeq = Query[(Heads, Commits, Forms, Shadows), (Head, Commit, Form, Shadow), Seq]

  def filterLatestById(id: Form#Id, viewId: View#Id, kind: String): HeadQuerySeq =
    for {
      form   ← Forms.filter(_.id === id)
      commit ← Commits if commit.formId === form.id
      head   ← Heads if head.commitId === commit.id && head.viewId === viewId && head.kind === kind
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (head, commit, form, shadow)

  def filterByCommit(commitId: Commit#Id, kind: String): QuerySeq =
    for {
      commit ← Commits.filter(_.id === commitId)
      form   ← Forms if form.id === commit.formId && form.kind === kind
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (commit, form, shadow)

  def filterRelation(kind: String, commits: Seq[Commit#Id]): QuerySeq =
    for {
      commit ← Commits.filter(_.id.inSet(commits))
      form   ← Forms if form.id === commit.formId && form.kind === kind
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (commit, form, shadow)

  type QueryCommitSeq = Query[Commits, Commit, Seq]
  def filterCommits(kind: String, commits: Seq[Commit#Id]): QueryCommitSeq =
    for {
      commit ← Commits.filter(_.id.inSet(commits))
      form   ← Forms if form.id === commit.formId && form.kind === kind
    } yield commit

  type IntSeq = Query[Rep[Int], Int, Seq]
  def filterCommitIds(kind: String, commits: Seq[Commit#Id]): IntSeq =
    filterCommits(kind, commits).map(_.id)

}
