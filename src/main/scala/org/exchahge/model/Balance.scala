package org.exchahge.model

case class Balance(money: Int, securities: Map[Security, Int])

object Balance {
  def empty: Balance = Balance(0, Map.from(Security.values.map(_ -> 0)))
}
