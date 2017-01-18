package utils.apis

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import models.inventory.ProductVariantMwhSkuId
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalacheck.Gen
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import slick.memory.HeapBackend
import testutils.{Generators, TestBase}
import utils.aliases._
import utils.db.DbResultT

class MiddlewarehouseApiTest
    extends TestBase
    with MockitoSugar
    with PropertyChecks
    with Generators {
  "MWH API" - {
    implicit val au: AU = mock[AU]
    val db              = HeapBackend.Database(global)

    "batch SKU creation should" - {
      val counter = new AtomicInteger()
      val instant = Instant.now
      val api     = mock[Middlewarehouse]
      when(api.createSkus(any[Seq[(Int, CreateSku)]])(any[EC], any[AU]))
        .thenAnswer(new Answer[DbResultT[Vector[ProductVariantMwhSkuId]]] {
          def answer(invocation: InvocationOnMock): DbResultT[Vector[ProductVariantMwhSkuId]] = {
            val current                      = counter.incrementAndGet()
            val input: Seq[(Int, CreateSku)] = invocation.getArgument(0)
            val result: Future[Vector[ProductVariantMwhSkuId]] = Future(input.map {
              case (id, _) ⇒
                ProductVariantMwhSkuId(variantFormId = id, mwhSkuId = id, createdAt = instant)
            }(collection.breakOut))
            DbResultT.fromFuture(result.flatMap { id ⇒
              if (current == counter.getAndIncrement()) Future.successful(id)
              else Future.failed(new RuntimeException("Batches should be executed synchronously"))
            })
          }
        })
      when(api.createSkus(any[Seq[(Int, CreateSku)]], anyInt)(any[EC], any[AU]))
        .thenCallRealMethod()

      "process next batch only after finishing previous one" in {
        val gen: Gen[(List[(Int, CreateSku)], Int)] = for {
          size       ← Gen.chooseNum(50, 500)
          createSkus ← Gen.listOfN(size, Gen.zip(idGen, createSkuGen))
          batchSize  ← Gen.choose(2, size / 10)
        } yield (createSkus, batchSize)

        forAll(gen) {
          case (createSkus, batchSize) ⇒
            db.run(api.createSkus(createSkus, batchSize).value).futureValue.rightVal must
              contain theSameElementsInOrderAs createSkus.map {
              case (id, _) ⇒
                ProductVariantMwhSkuId(variantFormId = id, mwhSkuId = id, createdAt = instant)
            }
        }
      }

      "not blow up on negative batch size" in {
        forAll(Gen.nonEmptyListOf(Gen.zip(idGen, createSkuGen)), Gen.negNum[Int]) {
          (createSkus, batchSize) ⇒
            db.run(api.createSkus(createSkus, batchSize).value).futureValue.rightVal must
            contain theSameElementsInOrderAs createSkus.map {
              case (id, _) ⇒
                ProductVariantMwhSkuId(variantFormId = id, mwhSkuId = id, createdAt = instant)
            }
        }
      }
    }
  }
}
