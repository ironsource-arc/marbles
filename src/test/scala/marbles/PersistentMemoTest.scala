package marbles

import org.scalatest.{FunSpec, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

class PersistentMemoTest extends FunSpec with Matchers {

  it("First result is persistent") {

    val memo = new PersistentMemo[Int, Int]

    memo.memoize(0) {1} shouldBe 1
    memo.memoize(0) {fail(); 2} shouldBe 1
    memo.memoize(0) {fail(); 3} shouldBe 1
  }

  def accessKeyWhileAnotherKeyIsWritten(memo: PersistentMemo[Int, Int])(body: => Int) = {

    val secondFutureDoneP = Promise[Unit]()

    // used to signal that the first call's body has been started
    val firstAccessBodyStartedP = Promise[Unit]()

    val firstAccessF = Future {
      memo.memoize(5) {
        firstAccessBodyStartedP.success(())
        Await.ready(secondFutureDoneP.future, Duration.Inf)
        7
      }
    }

    Await.ready(firstAccessBodyStartedP.future, Duration.Inf)

    val secondAccessFuture = Future(body)

    Await.result(secondAccessFuture, Duration.Inf) shouldBe 3

    firstAccessF.isCompleted shouldBe false

    secondFutureDoneP.success(())
    Await.result(firstAccessF, Duration.Inf) shouldBe 7
  }

  it("First access is unhindered while write of another key is in progress") {

    val memo = new PersistentMemo[Int, Int]

    accessKeyWhileAnotherKeyIsWritten(memo) {memo.memoize(0)(3)}
  }

  it("Second access to a key is unhindered while write of another key is in progress") {

    val memo = new PersistentMemo[Int, Int]

    memo.memoize(0)(3)
    accessKeyWhileAnotherKeyIsWritten(memo) {memo.memoize(0)(3)}
  }

  it("get while write of same key is in progress") {

    val memo = new PersistentMemo[Int, Int]

    val secondFutureDoneP = Promise[Unit]()

    // Used to signal that the first call's body has been started.
    val firstAccessBodyStartedP = Promise[Unit]()

    val firstAccessFuture = Future {
      memo.memoize(0) {
        firstAccessBodyStartedP.success(())
        Await.ready(secondFutureDoneP.future, Duration.Inf)
        7
      }
    }

    Await.ready(firstAccessBodyStartedP.future, Duration.Inf)

    val secondAccessFuture = Future {memo.memoize(0) {fail(); 3}}

    secondFutureDoneP.success(())

    Await.result(firstAccessFuture, Duration.Inf) shouldBe 7

    Await.result(secondAccessFuture, Duration.Inf) shouldBe 7
  }

  it("exception handling") {

    val memo = new PersistentMemo[Int, Int]

    intercept[ArithmeticException] {
      memo.memoize(0) {1 / 0}
    }

    intercept[NoSuchElementException] {
      memo.memoize(0) {List.empty.head}
    }

    memo.memoize(0) {1} shouldBe 1

    memo.memoize(0) {1 / 0} shouldBe 1
  }

  it("exception while another process is waiting on same key") {

    val memo = new PersistentMemo[Int, Int]

    val secondFutureDoneP = Promise[Unit]()

    // used to signal that the first call's body has been started
    val firstAccessBodyStartedP = Promise[Unit]()

    val firstAccessFuture = Future {
      memo.memoize(0) {
        firstAccessBodyStartedP.success(())
        Await.ready(secondFutureDoneP.future, Duration.Inf)
        1 / 0
      }
    }

    Await.ready(firstAccessBodyStartedP.future, Duration.Inf)

    val secondAccessFuture = Future {memo.memoize(0)(3)}

    secondFutureDoneP.success(())

    intercept[ArithmeticException] {
      Await.result(firstAccessFuture, Duration.Inf)
    }

    Await.result(secondAccessFuture, Duration.Inf) shouldBe 3

    memo.memoize(0)(555) shouldBe 3
  }

  describe("getSnapshot") {

    it("Empty") {

      val memo = new PersistentMemo[Int, Int]
      memo.getSnapshot shouldBe Map.empty
    }

    it("With single entry") {

      val memo = new PersistentMemo[Int, Int]

      memo.memoize(0)(1)
      memo.memoize(0)(2)

      memo.getSnapshot shouldBe Map(0 -> 1)
    }

    it("With multiple entries") {

      val memo = new PersistentMemo[Int, Int]

      memo.memoize(0)(0)
      memo.memoize(1)(0)
      memo.memoize(2)(1)
      memo.memoize(42)(24)

      memo.getSnapshot should contain theSameElementsAs Map(0 -> 0, 1 -> 0, 2 -> 1, 42 -> 24)
    }

    it("while operation is in progress") {

      val memo = new PersistentMemo[Int, Int]

      val terminateP = Promise[Unit]()

      // used to signal that the long call's body has been started
      val firstAccessBodyStartedP = Promise[Unit]()

      memo.memoize(1)(55)

      Future {
        memo.memoize(0) {
          firstAccessBodyStartedP.success(())
          Await.ready(terminateP.future, Duration.Inf)
          7
        }
      }

      Await.ready(firstAccessBodyStartedP.future, Duration.Inf)

      val snapshot = memo.getSnapshot
      terminateP.success(())

      snapshot shouldBe Map(1 -> 55)
    }
  }

}
