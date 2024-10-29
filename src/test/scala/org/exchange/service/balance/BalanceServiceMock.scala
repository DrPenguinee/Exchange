package org.exchange.service.balance

import org.exchahge.model.{Balance, Client}
import org.exchahge.service.balance.BalanceService
import org.exchahge.service.balance.error.ClientNotFound
import zio.mock.{Mock, Proxy}
import zio.{IO, UIO, URLayer, ZIO, ZLayer}

object BalanceServiceMock extends Mock[BalanceService] {
  object GetClientBalance extends Effect[Client, ClientNotFound, Balance]
  object GetAllBalances   extends Effect[Unit, Nothing, Map[Client, Balance]]
  object AddClient {
    object _1 extends Effect[Client, Nothing, Unit]
    object _2 extends Effect[(Client, Balance), Nothing, Unit]
  }
  object ChangeClientBalance extends Effect[(Client, Balance => Balance), Nothing, Unit]

  override val compose: URLayer[Proxy, BalanceService] = ZLayer {
    for {
      proxy <- ZIO.service[Proxy]
    } yield new BalanceService {
      override def getClientBalance(client: Client): IO[ClientNotFound, Balance] =
        proxy(GetClientBalance, client)

      override def getAllBalances: UIO[Map[Client, Balance]] =
        proxy(GetAllBalances)

      override def addClient(client: Client): UIO[Unit] =
        proxy(AddClient._1, client)

      override def addClient(client: Client, balance: Balance): UIO[Unit] =
        proxy(AddClient._2, client, balance)

      override def changeClientBalance(client: Client, change: Balance => Balance): UIO[Unit] =
        proxy(ChangeClientBalance, client, change)
    }
  }

}
