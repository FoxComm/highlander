package failures

object CustomerGroupFailures {

  case class CustomerGroupTemplateInstanceCannotBeDeleted(customerGroupId: Int,
                                                          templateInstanceId: Int)
      extends Failure {
    override def description: String =
      s"Customer group template instance with id $templateInstanceId cannot be deleted for customer group with id: $customerGroupId"
  }

  case class CustomerGroupMemberCannotBeDeleted(groupId: Int, memberId: Int) extends Failure {
    override def description =
      s"Customer group member $memberId cannot be deleted from group $groupId"
  }
}
