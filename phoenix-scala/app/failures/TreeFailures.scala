package failures

object TreeFailures {

  case class ParentChildSwapFailure(newParent: Int, newChild: Int) extends Failure {
    override def description =
      s"Cannot make $newParent parent of $newChild: " +
        s"node $newParent is currently part of subtree of $newChild"
  }

  object TreeNodeNotFound {
    def apply(treeId: Int, nodeIndex: Int) =
      NotFoundFailure404(s"Tree node with index=$nodeIndex in tree $treeId cannot be found")

    def apply(treeName: String, context: String, nodeIndex: Int) =
      NotFoundFailure404(
        s"Tree node with index=$nodeIndex in tree $treeName and context $context cannot be found")

    def apply(treeName: String, context: String, path: String) =
      NotFoundFailure404(
        s"Tree node with path=$path in tree $treeName and context $context cannot be found")
  }

  object TreeNotFound {
    def apply(treeName: String, contextName: String) =
      NotFoundFailure404(s"Tree $treeName is not defined for context $contextName")
    def apply(treeName: String, contextName: String, path: String) =
      NotFoundFailure404(s"Tree $treeName has no nodes at path '$path' for context $contextName")
  }
}
