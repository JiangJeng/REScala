package reader

import rescala._
/*
 * Implementation of an observable method
 */
class Observable[T, U](body: T => U) extends (T => U) {
  // before and after, modeled as primitive events
  lazy val before = Evt[T]
  lazy val after = Evt[(T,U)]

  /*
  * Instrumented method implementation:
  * trigger events before and after the actual method execution
  */
  def apply(t: T): U = {
    before(t)
    val res = body(t)
    after((t, res))
    res
  }
}

object Observable {
  def apply[T,U](f: T => U) = new Observable(f)
}
