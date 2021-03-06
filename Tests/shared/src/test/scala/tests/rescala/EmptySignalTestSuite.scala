package tests.rescala


class EmptySignalTestSuite extends RETests {

  allEngines("basic Empty Signal Test"){ engine => import engine._

    val v = Var.empty[Int]

    intercept[NoSuchElementException](v.now)

    var res = -100

    v.observe(res = _)

    assert(res == -100, "sanity")

    val s = v.map(1.+)

    intercept[NoSuchElementException](s.now)

    v.set(100)

    assert(res == 100, "observed?")
    assert(v.now == 100, "changed from empty to value")
    assert(s.now == 101, "changed from empty to value 2")



  }

  allEngines("flatten empty signal when mapping event"){ engine => import engine._

    val v = Var.empty[Event[Unit]]

    val e1 = Evt[Unit]

    val e2 = e1 map { _ => v.flatten.count }

    var s: Signal[Int] = null

    e2.observe(s = _)

    e1.fire()

    assert(s != null, "sanity")

    assert(s.now == 0, "mapped event")


    val e3 = Evt[Unit]

    v.set(e3)

    assert(s.now == 0, "mapped event after var set")

    e3.fire()

    assert(s.now == 1, "mapped event after event fire")
  }

  allEngines("unwrap Empty Signal"){ engine => import engine._
    val v = Var.empty[Event[Int]]
    val e = v.flatten

    var res = -100

    e.observe(res = _)

    assert(res === -100, "sanity")

    val evt = Evt[Int]

    v.set(evt)

    evt.fire(20)

    assert(res === 20, "could unwrap after Val was empty")

  }

  allEngines("propagate emptiness"){ engine => import engine._
    val v = Var[Int](6)
    val v2 = Var[Int](6)
    val sig = Signal { v() + v2() }
    val e = sig.changed
    val folded = e.fold(0)(_ - _)

    assert(v.now === 6)
    assert(sig.now === v.now + v2.now)
    assert(folded.now === 0)

    v.setEmpty()
    intercept[NoSuchElementException](v.now)
    intercept[NoSuchElementException](sig.now)
    assert(v2.now === 6)
    assert(folded.now === 0)

    v.set(10)
    assert(v.now === 10)
    assert(v2.now === 6)
    assert(sig.now === v.now + v2.now)
    assert(folded.now === -16)


  }


}
