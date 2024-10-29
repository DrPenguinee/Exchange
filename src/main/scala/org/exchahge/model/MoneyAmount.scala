package org.exchahge.model

import scala.annotation.targetName

case class MoneyAmount(value: Int) extends AnyVal {
  @targetName("plus")
  def +(that: MoneyAmount): MoneyAmount = MoneyAmount(this.value + that.value)

  @targetName("minus")
  def -(that: MoneyAmount): MoneyAmount = MoneyAmount(this.value - that.value)

  @targetName("multiply")
  def *(quantity: Quantity): MoneyAmount = MoneyAmount(this.value * quantity.value)

  override def toString: String = this.value.toString
}

object MoneyAmount {
  def zero: MoneyAmount = MoneyAmount(0)
}
