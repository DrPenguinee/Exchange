package org.exchahge.model

import org.exchahge.model

import scala.annotation.targetName

case class Quantity(value: Int) extends AnyVal {
  @targetName("plus")
  def +(that: Quantity): Quantity = Quantity(this.value + that.value)

  @targetName("minus")
  def -(that: Quantity): Quantity = Quantity(this.value - that.value)

  @targetName("unaryMinus")
  def unary_- : Quantity = Quantity(-this.value)

  override def toString: String = this.value.toString
}

object Quantity {
  def zero: Quantity = Quantity(0)
}
