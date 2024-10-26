package org.exchange.service.balance

import org.exchahge.model.{Balance, Client, Security}
import org.exchahge.service.balance.BalanceService
import org.exchahge.service.balance.error.ClientNotFound
import org.exchahge.service.balance.impl.BalanceServiceImpl
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

object BalanceServiceTest extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("BalanceServiceImpl")(
      test("return ClientNotFound error for non-existent client") {
        val nonExistentClient = Client("non-existent")
        for {
          balanceService <- ZIO.service[BalanceService]
          error <- balanceService.getClientBalance(nonExistentClient).flip
        } yield assertTrue(error == ClientNotFound(nonExistentClient))
      },
      test("return client's balance") {
        val client = Client("client")
        val balance = Balance.empty
        for {
          balanceService <- ZIO.service[BalanceService]
          _ <- balanceService.changeClientBalance(client, balance)
          res <- balanceService.getClientBalance(client)
        } yield assertTrue(res == balance)
      },
      test("return changed client's balance") {
        val client = Client("client")
        val balance1 = Balance.empty
        val balance2 = Balance(100, Map(Security.A -> 5))
        for {
          balanceService <- ZIO.service[BalanceService]
          _ <- balanceService.changeClientBalance(client, balance1)
          res1 <- balanceService.getClientBalance(client)
          _ <- balanceService.changeClientBalance(client, balance2)
          res2 <- balanceService.getClientBalance(client)
        } yield assertTrue(res1 == balance1, res2 == balance2)
      },
    ).provideLayer(BalanceServiceImpl.live)
}
