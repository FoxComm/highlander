package services.notes

import models.Note
import models.objects.IlluminatedObject
import services.objects.ObjectManager
import services.product.ProductManager
import utils.aliases._
import utils.db._

object ProductNoteManager extends NoteManager[Int, IlluminatedObject] {

  def noteType(): Note.ReferenceType = Note.Product

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[IlluminatedObject] = {
    val getProduct = ProductManager
      .mustFindProductByContextAndFormId404(contextId = defaultContextId, formId = id)

    ObjectManager.getFullObject(getProduct).map { fullObject ⇒
      IlluminatedObject.illuminate(form = fullObject.form, shadow = fullObject.shadow)
    }
  }
}
