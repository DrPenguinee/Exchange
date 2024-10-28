package org.exchahge.service.balance

import org.exchahge.model.{Balance, Client}
import org.exchahge.service.balance.error.ClientNotFound
import zio.{IO, UIO}

/**
 * Сервис управления балансами клиентов.
 */
trait BalanceService {
  def getClientBalance(client: Client): IO[ClientNotFound, Balance]
  def getAllBalances: UIO[Map[Client, Balance]]
  def addClient(client: Client): UIO[Unit]
  def addClient(client: Client, balance: Balance): UIO[Unit]
  def changeClientBalance(client: Client, change: Balance => Balance): UIO[Unit]
}
