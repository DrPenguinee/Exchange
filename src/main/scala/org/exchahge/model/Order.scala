package org.exchahge.model

import org.exchahge.model.Order.Side

sealed trait Order {
  def client: Client
  def side: Side
  def security: Security
  def price: MoneyAmount
  def quantity: Quantity
}

object Order {
  case class BuyOrder(
      client: Client,
      security: Security,
      price: MoneyAmount,
      quantity: Quantity,
  ) extends Order {
    val side: Side.Buy.type = Side.Buy
  }

  case class SellOrder(
      client: Client,
      security: Security,
      price: MoneyAmount,
      quantity: Quantity,
  ) extends Order {
    val side: Side.Sell.type = Side.Sell
  }

  enum Side {
    case Buy, Sell
  }
}
