package failures
import utils.friendlyClassName

object ArchiveFailures {

  case class LinkArchivedVariantFailure(description: String) extends Failure

  object LinkArchivedVariantFailure {
    def apply[T](target: T, targetId: Any, code: String): LinkArchivedVariantFailure = {
      LinkArchivedVariantFailure(
          s"Cannot attach archived variant with code $code to ${friendlyClassName(target)} with id $targetId")
    }
  }

  case class AddImagesToArchivedAlbumFailure(albumId: Int) extends Failure {
    override def description: String = s"Cannot add image to archived album with id=$albumId"
  }

  case class ProductIsPresentInCarts(formId: Int) extends Failure {
    override def description: String =
      s"Can't archive product with formId=$formId because it's present in carts"
  }

  case class VariantIsPresentInCarts(code: String) extends Failure {
    override def description: String =
      s"Can't archive Variant with code=$code because it's present in carts"
  }
}
