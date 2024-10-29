package org.exchange.service.balance

import org.exchahge.model.{Balance, Client, MoneyAmount, Quantity, Security}
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
          error          <- balanceService.getClientBalance(nonExistentClient).flip
        } yield assertTrue(error == ClientNotFound(nonExistentClient))
      },
      test("add client (with default empty balance)") {
        val client = Client("client")
        for {
          balanceService <- ZIO.service[BalanceService]
          _              <- balanceService.addClient(client)
          res            <- balanceService.getClientBalance(client)
        } yield assertTrue(res == Balance.empty)
      },
      test("add client (with start balance)") {
        val client  = Client("client")
        val balance = Balance(money = MoneyAmount(5), Map(Security.B -> Quantity(42)))
        for {
          balanceService <- ZIO.service[BalanceService]
          _              <- balanceService.addClient(client, balance)
          res            <- balanceService.getClientBalance(client)
        } yield assertTrue(res == balance)
      },
      test("change client's balance") {
        val client = Client("client")
        val balanceChange = (balance: Balance) =>
          balance.copy(
            money = balance.money + MoneyAmount(100),
            securities = balance.securities.updated(Security.A, Quantity(5)),
          )
        for {
          balanceService <- ZIO.service[BalanceService]
          _              <- balanceService.addClient(client)
          res1           <- balanceService.getClientBalance(client)
          _              <- balanceService.changeClientBalance(client, balanceChange)
          res2           <- balanceService.getClientBalance(client)
        } yield assertTrue(res1 == Balance.empty, res2 == balanceChange(Balance.empty))
      },
    ).provideLayer(BalanceServiceImpl.live)
}
