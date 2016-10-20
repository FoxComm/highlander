package failures
import utils.friendlyClassName

object ArchiveFailures {

  case class LinkArchivedSkuFailure(description: String) extends Failure

  object LinkArchivedSkuFailure {
    def apply[T](target: T, targetId: Any, code: String): LinkArchivedSkuFailure = {
      LinkArchivedSkuFailure(
          s"Cannot attach archived sku with code $code to ${friendlyClassName(target)} with id " +
            s"${targetId}")
    }
  }

  case class AddImagesToArchivedAlbumFailure(albumId: Int) extends Failure {
    override def description: String = s"Cannot add image to archived album with id=${albumId}"
  }

  case class ProductIsPresentInCarts(productId: Int) extends Failure {
    override def description: String = s"Can't archive product with id=$productId due because it's present in carts"
  }
}
