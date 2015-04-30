package marbles

import java.util.concurrent.CountDownLatch
import scala.annotation.tailrec

/** A thread safe memoization table holding values forever.
  *
  * Implementation based on twitter implementation at http://tinyurl.com/twitter-memo
  *
  * @tparam K Type of key.
  * @tparam V Type of value.
  */
class PersistentMemo[K, V] extends Memo[K, V] {

  /** A class of all possible computation states for the key being looked up. */
  private sealed abstract class ComputationState

  /** Denotes that currently the key is not being processed by any other invocation, so this 
    * invocation is responsible for processing it.
    *
    * Values of this class cannot be stored in [[table]].
    *
    * @param latch A count down latch used to signal the completion (successful or not) of the 
    *              computation for the key.
    */
  private case class NotProcessedByOtherInvocation(latch: CountDownLatch) extends ComputationState

  /** A class of computation states where a key is being or has been processed by another 
    * invocation.
    * 
    * Values of this class can be stored in [[table]]. 
    */
  private sealed abstract class ProcessedByOtherInvocation extends ComputationState

  /** Denotes that the key is currently being processed by another invocation, so this invocation
    * has to block until processing completes.
    * 
    * @param latch A count down latch used to signal the completion (successful or not) of the 
    *              computation for the key.
    */
  private case class InProgress(latch: CountDownLatch) extends ProcessedByOtherInvocation

  /** Denotes that the key has been successfully processed by another invocation.
    * 
    * @param value The value computed for the key
    */
  private case class Finished(value: V) extends ProcessedByOtherInvocation

  private[this] var table = Map.empty[K, ProcessedByOtherInvocation]

  /** Evaluates the body for the given key exactly once (unless an exception is thrown) and returns
    * the evaluation result for the key (thread safe).
    *
    * If the body evaluates successfully (no exceptions are thrown) all subsequent invocations will
    * return the same evaluation result. Results are never removed from the memoization table.
    *
    * If an exception is thrown inside the body, no result will be stored and the next invocation
    * will be treated as if the given key has never been processed before. The exception will be
    * thrown to exactly one invoker.
    *
    * @param key The key by which to memoize (function parameters)
    * @param body The body to be evaluated.
    * @return The result of the one successful evaluation of body for the given key.
    */
  def memoize(key: K)(body: => V): V =
    table.get(key) match { // Lookup the key in the (possibly stale) memo table.
      case Some(Finished(value)) => value // Immediately return, avoiding synchronization.
      case _ => handleNonFinishedStates(key)(body)
    }
  
  @tailrec
  private[this] def handleNonFinishedStates(key: K)(body: => V): V = {
    val computationState: ComputationState =
      synchronized { // With the lock, check to see the state of the current key.
        table.get(key) match {
          case None =>
            // Key not found in table, so it is not being processed by any other invocation.

            // Latch will be used to signal processing completion.
            val latch = new CountDownLatch(1)

            // Register key in the table to avoid multiple evaluations.
            table = table + (key -> InProgress(latch))

            NotProcessedByOtherInvocation(latch)

          case Some(state) =>
            // Key found in table, so state is retrieved.
            state
        }
    }

    computationState match {
      case Finished(value) =>
        value

      case InProgress(latch) =>
        // This key is being processed by another invocation.

        // Block until signaled by the processing invocation.
        latch.await()

        // Recursively loop once remote processing completed (either successfully or not).
        handleNonFinishedStates(key)(body)

      case NotProcessedByOtherInvocation(latch) =>
        // Body evaluated outside of the synchronized block.
        val value =
          try {
            body
          } catch {
            case t: Throwable =>
              /* Remove key from table so other invocations will retry.
               * Must synchronize writes to table.
               */
              synchronized {table = table - key}

              // Signal awaiting invocations.
              latch.countDown()

              // Rethrow the exception.
              throw t
          }

        /* Evaluation completed successfully.
         * Update key in table to the Finished state (along with the value).
         * Must synchronize writes to table.
         */
        synchronized {table = table + (key -> Finished(value))}

        // Signal awaiting invocations.
        latch.countDown()

        // Return value to caller
        value
    }
  }


  /** Optionally returns the value associated with a key.
    *
    * Will return a value if there's a completed computation for the given key, otherwise [[None]].
    */
  final def get(key: K): Option[V] =
    table.get(key) collect {case Finished(value) => value}

  /** Returns a map of all completed computations currently stored in the memo.
    *
    * If a key-value pair is successfully memoized causally before invoking [[getSnapshot]] then
    * it is guaranteed to be in the returned map. No guarantee is provided for pairs inserted
    * concurrently with getSnapshot.
    */
  final def getSnapshot: Map[K, V] = table collect {case (key, Finished(value)) => (key, value)}

}

object PersistentMemo {

  /** Creates a new empty [[PersistentMemo]] for the given types.
    *
    * @tparam K Type of key.
    * @tparam V Type of value.
    */
  def empty[K, V] = new PersistentMemo[K, V]

  def fromMap[K, V](m: Map[K, V]): PersistentMemo[K, V] =
    new PersistentMemo[K, V].loadMap(m)

}
