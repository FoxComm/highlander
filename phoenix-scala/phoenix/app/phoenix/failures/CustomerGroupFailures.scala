package phoenix.failures

import core.failures.Failure
import phoenix.models.customer.CustomerGroup.GroupType

object CustomerGroupFailures {

  case class CustomerGroupTypeIsWrong(groupId: Int, realGroupType: GroupType, expected: Set[GroupType])
      extends Failure {
    override def description =
      s"Customer group with id $groupId has $realGroupType type but is expected to have ${expected.mkString(",")}"
  }

  case class CustomerGroupMemberPayloadContainsSameIdsInBothSections(groupId: Int,
                                                                     toAdd: Set[Int],
                                                                     toDelete: Set[Int])
      extends Failure {
    override def description =
      s"Customer group with id $groupId has same ids (${toAdd.intersect(toDelete).mkString(",")}) in add section (${toAdd
        .mkString(",")}) and in delete section (${toDelete.mkString(",")})"
  }
}
