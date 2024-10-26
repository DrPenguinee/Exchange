package org.exchahge.service.balance.impl

import org.exchahge.model.{Balance, Client}
import org.exchahge.service.balance.BalanceService
import org.exchahge.service.balance.error.ClientNotFound
import zio.{IO, Ref, UIO, ULayer, ZLayer}

private[balance] class BalanceServiceImpl(
    balanceByClient: Ref[Map[Client, Balance]],
) extends BalanceService {

  override def getClientBalance(client: Client): IO[ClientNotFound, Balance] =
    balanceByClient.get.map(_.get(client)).someOrFail(ClientNotFound(client))

  override def getAllBalances: UIO[Map[Client, Balance]] = balanceByClient.get

  override def changeClientBalance(
      client: Client,
      updatedBalance: Balance,
  ): UIO[Unit] =
    balanceByClient.update(_ + (client -> updatedBalance))
}

object BalanceServiceImpl {
  val live: ULayer[BalanceService] = ZLayer {
    for {
      balanceByClient <- Ref.make(Map.empty[Client, Balance])
    } yield BalanceServiceImpl(balanceByClient)
  }
}
