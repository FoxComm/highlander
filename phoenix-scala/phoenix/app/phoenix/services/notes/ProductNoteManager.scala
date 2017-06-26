package phoenix.services.notes

import core.db._
import objectframework.models.IlluminatedObject
import objectframework.services.ObjectManager
import phoenix.models.Note
import phoenix.services.product.ProductManager
import phoenix.utils.aliases._

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
