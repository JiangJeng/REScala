package examples.range

import rescala._




class Range1(protected var _start : Int, protected var _length : Int){
  protected var _end = _start + _length // backs up end

  // getters and setters, maintaining correct state
  def start = _start
  def end = _end
  def length = _length
  def start_=(s : Int): Unit = {
    _start = s
    _end = _start + _length
  }
  def end_=(e : Int): Unit = {
    _end = e
    _length = e - _start
  }
  def length_=(e : Int): Unit = {
    _length = e
    _end = _start + _length
  }
}

class Range2(var start : Int, var length : Int){
  def end = start + length
  def end_=(e : Int) = length = e - start
}


class Range3(val start : Var[Int], val length : Var[Int]) {
	// end is a signal, leading to transparent caching
	lazy val end = Signal{ start() + length()}
	lazy val last = Signal{ end() - 1 }
	def end_=(e : Int) = length.set(e - start.now)

	// invariant
	length.changed += {(x : Int) =>
	  if(x < 0) throw new IllegalArgumentException}

	// convenience functions
	def contains(number : Int) = number > start.now && number < end.now
	def contains(other : Range3) = start.now < other.start.now && end.now > other.end.now
	def intersects(other : Range3) = contains(other.start.now) || contains(other.end.now)
}


// immutable range
class Range(val start : Int, val length : Int) {
	lazy val end = start + length
}




