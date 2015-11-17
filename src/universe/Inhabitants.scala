package universe

import rescala.Signals
import universe.AEngine.engine
import universe.AEngine.engine._

import Animal._

class Carnivore(implicit world: World) extends Animal {

  private val sleepy = energy map {_ < Animal.SleepThreshold}
  private val canHunt = energy map {_ > Animal.AttackThreshold}

  // only adult carnivores with min energy can hunt, others eat plants
  override val findFood: Signal[PartialFunction[BoardElement, BoardElement]] = Signals.static(isAdult, canHunt) { t =>
    if (isAdult.get(t) && canHunt.get(t)) {case p: Herbivore => p}: PartialFunction[BoardElement, BoardElement]
    else {case p: Plant => p}: PartialFunction[BoardElement, BoardElement]
  }


  override def reachedState(prey: BoardElement): AnimalState = prey match {
    case p: Herbivore => Attacking(p)
    case _ => Idling
  }


  override protected def nextAction(pos: Pos): AnimalState = {
    if (sleepy.now) Sleeping
    else super.nextAction(pos)
  }
}

class Herbivore(implicit world: World) extends Animal {

  override val findFood: Signal[PartialFunction[BoardElement, BoardElement]] = //#SIG
    Var {
      {case p: Plant => p}: PartialFunction[BoardElement, BoardElement]
    }

  override def reachedState(plant: BoardElement): AnimalState = plant match {
    case p: Plant => Eating(p)
    case _ => Idling
  }
}

trait Female extends Animal {

  // counts down to 0
  private val mate: Var[Option[Animal]] = Var(None) //#VAR
  val isPregnant = mate.map {_.isDefined} //#SIG
  private val becomePregnant: Event[Unit] = isPregnant.changedTo(true) //#EVT //#IF
  private val pregnancyTime: Signal[Int] = becomePregnant.reset(()) { _ => //#SIG  //#IF
      world.time.hour.changed.iterate(Animal.PregnancyTime)(_ - (if (isPregnant.now) 1 else 0)) //#IF //#IF //#SIG
    }
  private val giveBirth: Event[Unit] = pregnancyTime.changedTo(0) //#EVT //#IF
  override val isFertile = Signals.lift(isAdult, isPregnant) {_ && !_} //#SIG

  // override val energyDrain = Signal { super.energyDrain() * 2 }
  // not possible

  giveBirth += { _ => //#HDL
    world.plan {
      val father = mate.now.get
      val child = createOffspring(father)
      world.board.getPosition(this).foreach { mypos =>
        world.board.nearestFree(mypos).foreach { target =>
          world.spawn(child, target)
        }
      }
      mate.set(None)
    }
  }
  def procreate(father: Animal): Unit = {
    if (isPregnant.now) return
    mate.set(Some(father))
  }


  def createOffspring(father: Animal): Animal = {
    val male = world.randomness.nextBoolean()
    val nHerbivores = List(this, father).map(_.isInstanceOf[Herbivore]).count(_ == true)
    val herbivore =
      if (nHerbivores == 0) false // both parents are a carnivores, child is carnivore
      else if (nHerbivores == 2) true // both parents are herbivores, child is herbivore
      else world.randomness.nextBoolean() // mixed parents, random

    world.newAnimal(herbivore, male)
  }
}


trait Male extends Animal {
  val seeksMate = Signals.lift(isFertile, energy) {_ && _ > Animal.ProcreateThreshold}

  override def nextAction(pos: Pos): AnimalState = {
    if (seeksMate.now) {
      val findFemale: PartialFunction[BoardElement, Female] = {
        case f: Female if f.isFertile.now => f
      }
      val neighbors = world.board.neighbors(pos)
      val females = neighbors.collectFirst(findFemale)

      val nextAction: AnimalState = females match {
        case Some(female) => Procreating(female)
        case None => // I have to look for females nearby
          world.board.nearby(pos, Animal.ViewRadius).collectFirst(findFemale) match {
            case Some(target) =>
              val destination = world.board.getPosition(target)
              if (destination.isDefined) Moving(pos.directionTo(destination.get))
              else super.nextAction(pos)
            case None => super.nextAction(pos)
          }
      }
      nextAction
    }
    else super.nextAction(pos)
  }
}


class FemaleHerbivore(implicit world: World) extends Herbivore with Female
class MaleHerbivore(implicit world: World) extends Herbivore with Male
class FemaleCarnivore(implicit world: World) extends Carnivore with Female
class MaleCarnivore(implicit world: World) extends Carnivore with Male








