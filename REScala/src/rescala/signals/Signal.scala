package rescala.signals

import rescala.propagation._
import rescala._
import rescala.events.{WrappedEvent, Event}


trait Signal[+A] extends Changing[A] with Dependency[A] {

  protected[this] var currentValue: A = _

  override def pulse(implicit turn: Turn): Pulse[A] = super.pulse match {
    case NoChangePulse => ValuePulse(currentValue)
    case p @ ValuePulse(value) => p
    case p @ DiffPulse(value, old) => p
  }

  override def applyPulse(implicit turn: Turn): Unit = {
    pulse.valueOption.foreach(currentValue = _)
    super.applyPulse
  }

  def get(implicit turn: MaybeTurn): A = turn.turn match {
    case Some(x) => pulse(x).valueOption.get
    case None => currentValue
  }

  //final def apply(implicit turn: MaybeTurn): A = get

  // only used inside macro and will be replaced there
  final def apply(): A = throw new IllegalAccessException(s"$this.apply called outside of macro")

  /** hook for subclasses to do something when they use their dependencies */
  def onDynamicDependencyUse[T](dependency: Signal[T]): Unit = { }

  def apply[T](signal: DynamicSignal[T]): A = Turn.maybeTurn { turn =>
    signal.onDynamicDependencyUse(this)
    get
  }

  def map[B](f: A => B): Signal[B] = DynamicSignal(this) { s: DynamicSignal[B] => f(apply(s)) }

  /** Return a Signal that gets updated only when e fires, and has the value of this Signal */
  def snapshot(e: Event[_]): Signal[A] = e.snapshot(this)

  /** Switch to (and keep) event value on occurrence of e*/
  def switchTo[U >: A](e: Event[U]): Signal[U] = e.switchTo(this)

  /** Switch to (and keep) event value on occurrence of e*/
  def switchOnce[V >: A](e: Event[_])(newSignal: Signal[V]): Signal[V] = e.switchOnce(this, newSignal)

  /** Switch back and forth between this and the other Signal on occurrence of event e */
  def toggle[V >: A](e: Event[_])(other: Signal[V]): Signal[V] = e.toggle(this, other)

  /** Delays this signal by n occurrences */
  def delay(n: Int): Signal[A] = changed.delay(get, n)

  /** Unwraps a Signal[Event[E]] to an Event[E] */
  def unwrap[E](implicit evidence: A <:< Event[E]): Event[E] =  new WrappedEvent(this.map(evidence))

}
