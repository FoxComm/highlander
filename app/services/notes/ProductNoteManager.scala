package services.notes

import failures.NotFoundFailure404
import models.Note
import models.objects.{ObjectForm, ObjectForms}
import models.product.Product
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.aliases._

object ProductNoteManager extends NoteManager[Int, ObjectForm] {

  def noteType(): Note.ReferenceType = Note.Product

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[ObjectForm] =
    ObjectForms
      .filter(_.id === id)
      .filter(_.kind === ObjectForm.product)
      .mustFindOneOr(NotFoundFailure404(Product, id))
}
