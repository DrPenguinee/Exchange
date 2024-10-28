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

  override def addClient(client: Client): UIO[Unit] =
    addClient(client, Balance.empty)

  override def addClient(client: Client, balance: Balance): UIO[Unit] =
    balanceByClient.update(x => if (x.contains(client)) x else x.updated(client, balance))

  override def changeClientBalance(client: Client, update: Balance => Balance): UIO[Unit] =
    balanceByClient.update(_.updatedWith(client)(_.map(update)))
}

object BalanceServiceImpl {
  val live: ULayer[BalanceService] = ZLayer {
    for {
      balanceByClient <- Ref.make(Map.empty[Client, Balance])
    } yield BalanceServiceImpl(balanceByClient)
  }
}
