package org.exchange.service.exchange

import org.exchahge.model.*
import org.exchahge.model.Order.{BuyOrder, SellOrder}
import org.exchahge.service.balance.BalanceService
import org.exchahge.service.exchange.Exchange
import org.exchahge.service.exchange.impl.ExchangeImpl
import org.exchange.service.balance.BalanceServiceMock
import zio.mock.Expectation
import zio.test.{Assertion, Spec, TestEnvironment, ZIOSpecDefault, assertCompletes}
import zio.{Scope, ULayer, ZIO}

object ExchangeTest extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("ExchangeImpl")(
    test("match sell and buy order") {
      val clientAlice = Client("Alice")
      val clientBob   = Client("Bob")

      val security = Security.C
      val price    = MoneyAmount(150)
      val quantity = Quantity(2)

      val sellOrder = SellOrder(clientAlice, security, price, quantity)
      val buyOrder  = BuyOrder(clientBob, security, price, quantity)

      val expectedAliceBalanceChange: Balance => Balance = aliceBalance =>
        aliceBalance.copy(
          money = aliceBalance.money + price * quantity,
          securities = aliceBalance.securities.updatedWith(security) {
            case Some(securityQuantity) => Some(securityQuantity - quantity)
            case None                   => Some(-quantity)
          },
        )

      val expectedBobBalanceChange: Balance => Balance = bobBalance =>
        bobBalance.copy(
          money = bobBalance.money - price * quantity,
          securities = bobBalance.securities.updatedWith(security) {
            case Some(securityQuantity) => Some(securityQuantity + quantity)
            case None                   => Some(quantity)
          },
        )

      val testBalance = Balance.empty

      val balanceServiceMock: ULayer[BalanceService] =
        BalanceServiceMock.ChangeClientBalance(
          Assertion.assertion("Alice's balance change") { case (client, changeBalance) =>
            client == clientAlice && changeBalance(testBalance) == expectedAliceBalanceChange(testBalance)
          },
          Expectation.unit,
        ) && BalanceServiceMock.ChangeClientBalance(
          Assertion.assertion("Bob's balance change") { case (client, changeBalance) =>
            client == clientBob && changeBalance(testBalance) == expectedBobBalanceChange(testBalance)
          },
          Expectation.unit,
        )

      (for {
        exchange <- ZIO.service[Exchange]
        _        <- exchange.processOrder(sellOrder)
        _        <- exchange.processOrder(buyOrder)
      } yield assertCompletes).provideLayer(balanceServiceMock >>> ExchangeImpl.live)
    },
    test("use each order once") {
      val clientAlice = Client("Alice")
      val clientBob   = Client("Bob")

      val security = Security.C
      val price    = MoneyAmount(150)
      val quantity = Quantity(2)

      val sellOrder = SellOrder(clientAlice, security, price, quantity)
      val buyOrder  = BuyOrder(clientBob, security, price, quantity)

      val expectedAliceBalanceChange: Balance => Balance = aliceBalance =>
        aliceBalance.copy(
          money = aliceBalance.money + price * quantity,
          securities = aliceBalance.securities.updatedWith(security) {
            case Some(securityQuantity) => Some(securityQuantity - quantity)
            case None                   => Some(-quantity)
          },
        )

      val expectedBobBalanceChange: Balance => Balance = bobBalance =>
        bobBalance.copy(
          money = bobBalance.money - price * quantity,
          securities = bobBalance.securities.updatedWith(security) {
            case Some(securityQuantity) => Some(securityQuantity + quantity)
            case None                   => Some(quantity)
          },
        )

      val testBalance = Balance.empty

      val balanceServiceMock: ULayer[BalanceService] =
        BalanceServiceMock.ChangeClientBalance(
          Assertion.assertion("Alice's balance change") { case (client, changeBalance) =>
            client == clientAlice && changeBalance(testBalance) == expectedAliceBalanceChange(testBalance)
          },
          Expectation.unit,
        ) && BalanceServiceMock.ChangeClientBalance(
          Assertion.assertion("Bob's balance change") { case (client, changeBalance) =>
            client == clientBob && changeBalance(testBalance) == expectedBobBalanceChange(testBalance)
          },
          Expectation.unit,
        )

      (for {
        exchange <- ZIO.service[Exchange]
        _        <- exchange.processOrder(sellOrder)
        _        <- exchange.processOrder(buyOrder)
        _        <- exchange.processOrder(buyOrder)
      } yield assertCompletes).provideLayer(balanceServiceMock >>> ExchangeImpl.live)
    },
    suite("don't process orders")(
      test("when buy and sell order by the same client") {
        val clientAlice = Client("Alice")

        val security = Security.C
        val price    = MoneyAmount(150)
        val quantity = Quantity(2)

        val sellOrder = SellOrder(clientAlice, security, price, quantity)
        val buyOrder  = BuyOrder(clientAlice, security, price, quantity)

        val balanceServiceMock: ULayer[BalanceService] = BalanceServiceMock.empty

        (for {
          exchange <- ZIO.service[Exchange]
          _        <- exchange.processOrder(sellOrder)
          _        <- exchange.processOrder(buyOrder)
        } yield assertCompletes).provideLayer(balanceServiceMock >>> ExchangeImpl.live)
      },
      test("when securities don't match") {
        val clientAlice = Client("Alice")
        val clientBob   = Client("Bob")

        val price    = MoneyAmount(150)
        val quantity = Quantity(2)

        val sellOrder = SellOrder(clientAlice, Security.C, price, quantity)
        val buyOrder  = BuyOrder(clientBob, Security.D, price, quantity)

        val balanceServiceMock: ULayer[BalanceService] = BalanceServiceMock.empty

        (for {
          exchange <- ZIO.service[Exchange]
          _        <- exchange.processOrder(sellOrder)
          _        <- exchange.processOrder(buyOrder)
        } yield assertCompletes).provideLayer(balanceServiceMock >>> ExchangeImpl.live)
      },
      test("when prices don't match") {
        val clientAlice = Client("Alice")
        val clientBob   = Client("Bob")

        val security   = Security.C
        val alicePrice = MoneyAmount(150)
        val bobPrice   = MoneyAmount(200)
        val quantity   = Quantity(2)

        val sellOrder = SellOrder(clientAlice, security, alicePrice, quantity)
        val buyOrder  = BuyOrder(clientBob, security, bobPrice, quantity)

        val balanceServiceMock: ULayer[BalanceService] = BalanceServiceMock.empty

        (for {
          exchange <- ZIO.service[Exchange]
          _        <- exchange.processOrder(sellOrder)
          _        <- exchange.processOrder(buyOrder)
        } yield assertCompletes).provideLayer(balanceServiceMock >>> ExchangeImpl.live)
      },
      test("when quantities don't match") {
        val clientAlice = Client("Alice")
        val clientBob   = Client("Bob")

        val security      = Security.C
        val price         = MoneyAmount(150)
        val aliceQuantity = Quantity(2)
        val bobQuantity   = Quantity(5)

        val sellOrder = SellOrder(clientAlice, security, price, aliceQuantity)
        val buyOrder  = BuyOrder(clientBob, security, price, bobQuantity)

        val balanceServiceMock: ULayer[BalanceService] = BalanceServiceMock.empty

        (for {
          exchange <- ZIO.service[Exchange]
          _        <- exchange.processOrder(sellOrder)
          _        <- exchange.processOrder(buyOrder)
        } yield assertCompletes).provideLayer(balanceServiceMock >>> ExchangeImpl.live)
      },
    ),
  )
}
