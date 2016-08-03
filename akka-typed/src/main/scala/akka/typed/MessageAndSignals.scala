/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed

/**
 * Envelope that is published on the eventStream for every message that is
 * dropped due to overfull queues.
 */
final case class Dropped(msg: Any, recipient: ActorRef[Nothing])

/**
 * Exception that an actor fails with if it does not handle a Terminated message.
 */
final case class DeathPactException(ref: ActorRef[Nothing]) extends RuntimeException(s"death pact with $ref was triggered")

/**
 * Envelope for dead letters.
 */
final case class DeadLetter(msg: Any)

/**
 * System signals are notifications that are generated by the system and
 * delivered to the Actor behavior in a reliable fashion (i.e. they are
 * guaranteed to arrive in contrast to the at-most-once semantics of normal
 * Actor messages).
 */
sealed trait Signal

/**
 * Lifecycle signal that is fired upon creation of the Actor. This will be the
 * first message that the actor processes.
 */
@SerialVersionUID(1L)
final case object PreStart extends Signal

/**
 * Lifecycle signal that is fired upon restart of the Actor before replacing
 * the behavior with the fresh one (i.e. this signal is received within the
 * behavior that failed).
 */
@SerialVersionUID(1L)
final case object PreRestart extends Signal

/**
 * Lifecycle signal that is fired upon restart of the Actor after replacing
 * the behavior with the fresh one (i.e. this signal is received within the
 * fresh replacement behavior).
 */
@SerialVersionUID(1L)
final case object PostRestart extends Signal

/**
 * Lifecycle signal that is fired after this actor and all its child actors
 * (transitively) have terminated. The [[Terminated]] signal is only sent to
 * registered watchers after this signal has been processed.
 *
 * <b>IMPORTANT NOTE:</b> if the actor terminated by switching to the
 * `Stopped` behavior then this signal will be ignored (i.e. the
 * Stopped behavior will do nothing in reaction to it).
 */
@SerialVersionUID(1L)
final case object PostStop extends Signal

/**
 * Lifecycle signal that is fired when an Actor that was watched has terminated.
 * Watching is performed by invoking the
 * [[akka.typed.ActorContext]] `watch` method. The DeathWatch service is
 * idempotent, meaning that registering twice has the same effect as registering
 * once. Registration does not need to happen before the Actor terminates, a
 * notification is guaranteed to arrive after both registration and termination
 * have occurred. Termination of a remote Actor can also be effected by declaring
 * the Actor’s home system as failed (e.g. as a result of being unreachable).
 */
@SerialVersionUID(1L)
final case class Terminated(ref: ActorRef[Nothing])(failed: Throwable) extends Signal {
  def wasFailed: Boolean = failed ne null
  def failure: Throwable = failed
  def failureOption: Option[Throwable] = Option(failed)
}

/**
 * The parent of an actor decides upon the fate of a failed child actor by
 * encapsulating its next behavior in one of the four wrappers defined within
 * this class.
 *
 * Failure responses have an associated precedence that ranks them, which is in
 * descending importance:
 *
 *  - Escalate
 *  - Stop
 *  - Restart
 *  - Resume
 */
object Failed {

  sealed trait Decision

  @SerialVersionUID(1L)
  case object NoFailureResponse extends Decision

  /**
   * Resuming the child actor means that the result of processing the message
   * on which it failed is just ignored, the previous state will be used to
   * process the next message. The message that triggered the failure will not
   * be processed again.
   */
  @SerialVersionUID(1L)
  case object Resume extends Decision

  /**
   * Restarting the child actor means resetting its behavior to the initial
   * one that was provided during its creation (i.e. the one which was passed
   * into the [[Props]] constructor). The previously failed behavior will
   * receive a [[PreRestart]] signal before this happens and the replacement
   * behavior will receive a [[PostRestart]] signal afterwards.
   */
  @SerialVersionUID(1L)
  case object Restart extends Decision

  /**
   * Stopping the child actor will free its resources and eventually
   * (asynchronously) unregister its name from the parent. Completion of this
   * process can be observed by watching the child actor and reacting to its
   * [[Terminated]] signal.
   */
  @SerialVersionUID(1L)
  case object Stop extends Decision

  /**
   * The default response to a failure in a child actor is to escalate the
   * failure, entailing that the parent actor fails as well. This is equivalent
   * to an exception unwinding the call stack, but it applies to the supervision
   * hierarchy instead.
   */
  @SerialVersionUID(1L)
  case object Escalate extends Decision

}
