package org.exchahge.model

import org.exchahge.model.Security.*

case class Balance(money: MoneyAmount, securities: Map[Security, Quantity]) {
  def view: String =
    s"$money\t${securities.getOrElse(A, 0)}\t${securities.getOrElse(B, 0)}\t${securities.getOrElse(C, 0)}\t${securities.getOrElse(D, 0)}"
}

object Balance {
  def empty: Balance = Balance(MoneyAmount.zero, Map.empty)
}
