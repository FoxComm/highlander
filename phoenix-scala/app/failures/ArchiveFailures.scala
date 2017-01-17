package failures
import utils.friendlyClassName

object ArchiveFailures {

  case class LinkArchivedSkuFailure(description: String) extends Failure

  object LinkArchivedSkuFailure {
    def apply[T](target: T, targetId: Any, code: String): LinkArchivedSkuFailure = {
      LinkArchivedSkuFailure(
        s"Cannot attach archived sku with code $code to ${friendlyClassName(target)} with id $targetId")
    }
  }

  case class AddImagesToArchivedAlbumFailure(albumId: Int) extends Failure {
    override def description: String = s"Cannot add image to archived album with id=$albumId"
  }

  case class ProductIsPresentInCarts(formId: Int) extends Failure {
    override def description: String =
      s"Can't archive product with formId=$formId because it's present in carts"
  }

  case class SkuIsPresentInCarts(code: String) extends Failure {
    override def description: String =
      s"Can't archive SKU with code=$code because it's present in carts"
  }
}
