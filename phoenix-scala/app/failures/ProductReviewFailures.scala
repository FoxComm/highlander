package failures

import models.objects.ObjectForm

object ProductReviewFailures {

  case class ProductReviewIsArchived(id: ObjectForm#Id) extends Failure {
    override def description: String = s"Cannot update deleted review: $id"
  }

}
