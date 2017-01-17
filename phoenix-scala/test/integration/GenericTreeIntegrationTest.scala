import com.github.tminglei.slickpg.LTree
import failures.TreeFailures._
import models.objects._
import models.tree._
import org.json4s.JsonDSL._
import org.scalatest.Matchers._
import payloads.GenericTreePayloads._
import responses.GenericTreeResponses.FullTreeResponse.Root
import responses.GenericTreeResponses.TreeResponse
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import utils.MockedApis
import utils.aliases._
import utils.db._

class GenericTreeIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockedApis {

  "GenericTreeIntegrationTest" - {
    "GET /v1/tree/default/test" - {
      "should return full tree" in new TestTree {
        val responseTree = genericTreesApi(tree.name).get().as[Root].tree.nodes

        responseTree.children must have size 2
        nodeByPath(responseTree, Seq(1, 3)).get.children must have size 3
      }
    }

    "POST /v1/tree/default/test" - {

      "should create tree" in new TestObjects {
        val testTree = NodePayload("test",
                                   testObjects.head.id,
                                   List(NodePayload("test", testObjects(1).id, Nil),
                                        NodePayload("test", testObjects(2).id, Nil)))

        val treeResponse = genericTreesApi("test").create(testTree).as[Root].tree
        treeResponse.nodes.children must have size 2
      }

      "should rewrite tree" in new TestTree {
        val testTree = NodePayload("test",
                                   testObjects.head.id,
                                   List(NodePayload("test", testObjects(1).id, Nil),
                                        NodePayload("test", testObjects(2).id, Nil)))

        val treeResponse = genericTreesApi("test").create(testTree).as[Root].tree
        treeResponse.nodes.children must have size 2
      }

      "should update subtree" in new TestTree {
        val testTree = NodePayload("test",
                                   testObjects.head.id,
                                   List(NodePayload("test", testObjects(1).id, Nil),
                                        NodePayload("test", testObjects(2).id, Nil)))

        val treeResponse = genericTreesApi(tree.name).createInPath("1.3.4", testTree).as[Root].tree

        treeResponse.nodes.objectId mustEqual testObjects.head.id
        treeResponse.nodes.children must have size 2

        val overriddenNode = nodeByPath(treeResponse.nodes, Seq(1, 3, 4))

        overriddenNode.get.children.size shouldEqual 2
        overriddenNode.get.children.foreach(_.children.size shouldEqual 0)
      }

      "fails if subtree doesn't exits" in new TestTree {
        val testTree = NodePayload("test",
                                   testObjects.head.id,
                                   List(NodePayload("test", testObjects(1).id, Nil),
                                        NodePayload("test", testObjects(2).id, Nil)))

        genericTreesApi(tree.name)
          .createInPath("1.5.10", testTree)
          .mustFailWith404(TreeNotFound(tree.name, ctx.name, "1.5.10"))
      }
    }

    "PATCH /v1/tree/default/tree" - {
      "should move nodes" in new TestTree {
        val treeResponse =
          genericTreesApi(tree.name).moveNode(MoveNodePayload(Some(2), 4)).as[Root].tree

        treeResponse must not be null
        treeResponse.nodes.objectId mustEqual testObjects.head.id
        treeResponse.nodes.children.map(_.index) should contain allOf (2, 3)

        nodeByPath(treeResponse.nodes, Seq(1, 2)).get.children.map(_.index) mustEqual Seq(4)
        nodeByPath(treeResponse.nodes, Seq(1, 3)).get.children
          .map(_.index) should contain allOf (5, 6)
      }

      "fails to make node to be child of its child" in new TestTree {
        genericTreesApi(tree.name)
          .moveNode(MoveNodePayload(Some(4), 3))
          .mustFailWith400(ParentChildSwapFailure(4, 3))
      }
    }

    "PATCH v1/tree/default/test/:path" - {
      "should update node content" in new TestTree {
        val treeResponse = genericTreesApi(tree.name)
          .moveNodeInPath("1.2", NodeValuesPayload("test", testObjects.head.id))
          .as[Root]
          .tree

        treeResponse.nodes.kind must === ("test")
        nodeByPath(treeResponse.nodes, Seq(1, 2)).get.objectId must === (testObjects.head.id)
      }
    }
  }

  trait TestObjects {
    val context = ObjectContexts.filter(_.name === "default").one.run.futureValue.get

    val testObjects = ObjectForms
      .createAllReturningModels(
        0 to 2 map (index ⇒ ObjectForm(0, "testKind", s"key$index" → s"val$index")))
      .gimme
  }

  trait TestTree extends TestObjects {
    val (tree, treeNodes) = createNodes("test", testObjects.head.id)

    private def createNodes(testKind: String, testObjectId: Int)(
        implicit ec: EC): (GenericTree, Seq[GenericTreeNode]) =
      (for {
        tree ← * <~ GenericTrees.create(GenericTree(0, "testTree", context.id))
        testData = Seq(
          GenericTreeNode(0, tree.id, 1, LTree("1"), testKind, testObjectId),
          GenericTreeNode(0, tree.id, 2, LTree(List("1.2")), testKind, testObjectId),
          GenericTreeNode(0, tree.id, 3, LTree(List("1.3")), testKind, testObjectId),
          GenericTreeNode(0, tree.id, 4, LTree(List("1.3.4")), testKind, testObjectId),
          GenericTreeNode(0, tree.id, 5, LTree(List("1.3.5")), testKind, testObjectId),
          GenericTreeNode(0, tree.id, 6, LTree(List("1.3.6")), testKind, testObjectId)
        )
        treeNodes ← * <~ GenericTreeNodes.createAllReturningModels(testData)
      } yield (tree, treeNodes)).gimme
  }

  def nodeByPath(tree: TreeResponse.Node, path: Seq[Int]): Option[TreeResponse.Node] = {
    val headMatches = for {
      tree      ← Option(tree)
      headIndex ← path.headOption
      if tree.index == headIndex
    } yield tree

    if (path.tail.isEmpty) headMatches
    else headMatches.flatMap(_.children.map(nodeByPath(_, path.tail)).find(_.isDefined).flatten)
  }
}
