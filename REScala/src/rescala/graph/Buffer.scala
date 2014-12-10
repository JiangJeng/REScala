package rescala.graph

import rescala.synchronization.{TurnLock, Pessimistic}
import rescala.turns.Turn

final class Buffer[A](initialValue: A, initialStrategy: (A, A) => A, writeLock: TurnLock) extends Commitable {

  @volatile var current: A = initialValue
  @volatile private var update: Option[A] = None
  @volatile private var owner: Turn = null
  @volatile var commitStrategy: (A, A) => A = initialStrategy

  def transform(f: (A) => A)(implicit turn: Turn): A = {
    val value = f(get)
    set(value)
    value
  }
  def set(value: A)(implicit turn: Turn): Unit = {
    assert(owner == null || owner == turn, s"buffer owned by $owner written by $turn")
    turn match {
      case pessimistic: Pessimistic => assert(writeLock == null || writeLock.isLockedBy(pessimistic.key))
      case _ =>
    }
    update = Some(value)
    owner = turn
    turn.plan(this)
  }
  def base(implicit turn: Turn) = current
  def get(implicit turn: Turn): A = if(turn eq owner) update.getOrElse(current) else current
  def release(implicit turn: Turn): Unit = {
    update = None
    owner = null
  }
  def commit(implicit turn: Turn): Unit = {
    current = commitStrategy(current, get)
    release
  }
}

trait Commitable {
  def commit(implicit turn: Turn): Unit
  def release(implicit turn: Turn): Unit
}

object Buffer {
  def apply[A](default: A, commitStrategy: (A, A) => A, writeLock: TurnLock): Buffer[A] = new Buffer[A](default, commitStrategy, writeLock)
}