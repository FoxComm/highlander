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

}
