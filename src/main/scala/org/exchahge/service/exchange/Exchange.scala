package org.exchahge.service.exchange

import org.exchahge.model.Order
import zio.Task

trait Exchange {
  def processOrder(order: Order): Task[Unit]
}
