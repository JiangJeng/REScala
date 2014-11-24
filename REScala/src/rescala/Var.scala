package rescala

import rescala.propagation._
import rescala.propagation.turns.Turn
import rescala.propagation.turns.creation.TurnFactory
import rescala.signals.Signal

/** A root Reactive value without dependencies which can be set */
final class Var[T](initval: T) extends Signal[T] {
  pulses.default = Pulse.unchanged(initval)

  def update(newValue: T)(implicit fac: TurnFactory): Unit = set(newValue)
  def set(newValue: T)(implicit fac: TurnFactory): Unit = fac.newTurn { turn =>
    admit(newValue)(turn)
  }

  def admit(newValue: T)(implicit turn: Turn): Unit =
    turn.admit(this) {
      val p = Pulse.diff(newValue, get)
      if (p.isChange) {
        pulses.set(p)
        true
      }
      else false
    }

  override protected[rescala] def reevaluate()(implicit turn: Turn): EvaluationResult =
    EvaluationResult.Done(changed = true)
}

object Var {
  def apply[T](initval: T) = new Var(initval)
}