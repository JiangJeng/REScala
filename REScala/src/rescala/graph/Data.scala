package rescala.graph

import java.util.concurrent.atomic.AtomicInteger

import scala.util.DynamicVariable


/** support for dynamic dependency discovery */
object Globals {
  val dynamicDependencyBag = new DynamicVariable(Set[Reactive]())
  /** runs the given code while collecting dynamically used reactives */
  def collectDependencies[T](f: => T): (T, Set[Reactive]) = dynamicDependencyBag.withValue(Set()) { (f, dynamicDependencyBag.value) }
  /** mark a reactive as dynamically used */
  def useDependency(dependency: Reactive): Unit = dynamicDependencyBag.value = dynamicDependencyBag.value + dependency

  val dynamicNameVar = new DynamicVariable("")
  def named[S](n: String)(f: => S): S = dynamicNameVar.withValue(n)(f)

  val counter = new AtomicInteger(0)
}

sealed trait EvaluationResult

object EvaluationResult {
  case class Static(changed: Boolean) extends EvaluationResult
  case class Dynamic(changed: Boolean, diff: DepDiff) extends EvaluationResult
}

case class DepDiff(novel: Set[Reactive], old: Set[Reactive]) {
  lazy val added = novel.diff(old)
  lazy val removed = old.diff(novel)
}


sealed trait Pulse[+P] {

  import rescala.graph.Pulse._

  def fold[Q](ifNone: => Q, ifChange: P => Q): Q
  def current: Option[P]
  final def toOption: Option[P] = fold(None, Some.apply)
  final def isChange: Boolean = fold(false, _ => true)
  final def map[Q](f: P => Q): Pulse[Q] = fold(none, f.andThen(change))
  final def flatMap[Q](f: P => Pulse[Q]): Pulse[Q] = fold(none, f)
  final def filter(p: P => Boolean): Pulse[P] = fold(none, up => if (p(up)) change(up) else none)
  final def keep: Pulse[P] = fold(this, unchanged)
}

object Pulse {
  def fromOption[P](opt: Option[P]): Pulse[P] = opt.fold[Pulse[P]](NoChange())(Diff(_))

  def change[P](value: P): Pulse[P] = Diff(value)

  def unchanged[P](value: P): Pulse[P] = NoChange(Some(value))

  def diff[P](newValue: P, oldValue: P): Pulse[P] =
    if (null == oldValue) change(newValue)
    else if (newValue == oldValue) unchanged(oldValue)
    else Diff(newValue, Some(oldValue))

  def diffPulse[P](newValue: P, oldPulse: Pulse[P]): Pulse[P] = oldPulse match {
    case NoChange(None) => change(newValue)
    case NoChange(Some(value)) => diff(newValue, value)
    case Diff(update, current) => diff(newValue, update)
  }

  val none: Pulse[Nothing] = NoChange()

  final case class NoChange[+P](override val current: Option[P] = None) extends Pulse[P] {
    override def fold[Q](ifNone: => Q, ifChange: (P) => Q): Q = ifNone
  }
  final case class Diff[+P](update: P, override val current: Option[P] = None) extends Pulse[P] {
    override def fold[Q](ifNone: => Q, ifChange: (P) => Q): Q = ifChange(update)
  }
}
