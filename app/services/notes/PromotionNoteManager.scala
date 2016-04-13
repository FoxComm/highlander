package services.notes

import models.Note
import models.promotion.{Promotion, Promotions}
import utils.Slick._
import utils.aliases._

object PromotionNoteManager extends NoteManager[Int, Promotion] {

  def noteType(): Note.ReferenceType = Note.Promotion

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Promotion] =
    Promotions.mustFindById404(id)
}
