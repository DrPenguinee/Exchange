package org.exchahge.service.balance

import org.exchahge.model.{Balance, Client}
import org.exchahge.service.balance.error.ClientNotFound
import org.exchahge.service.balance.impl.BalanceServiceImpl
import zio.{IO, Ref, UIO, ULayer, ZLayer}

/**
 * Сервис управления балансами клиентов.
 */
trait BalanceService {
  def getClientBalance(client: Client): IO[ClientNotFound, Balance]
  def getAllBalances: UIO[Map[Client, Balance]]
  def changeClientBalance(client: Client, updatedBalance: Balance): UIO[Unit]
}
