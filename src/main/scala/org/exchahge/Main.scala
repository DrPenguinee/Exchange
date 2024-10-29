package org.exchahge

import org.exchahge.model.Security.{A, B, C, D}
import org.exchahge.model.{Balance, Order, Quantity}
import org.exchahge.service.balance.BalanceService
import org.exchahge.service.balance.impl.BalanceServiceImpl
import org.exchahge.service.exchange.Exchange
import org.exchahge.service.exchange.impl.ExchangeImpl
import zio.prelude.Validation
import zio.{RIO, Scope, UIO, ULayer, URIO, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.io.FileWriter
import scala.io.BufferedSource

object Main extends ZIOAppDefault {
  private val clientsFileSource: URIO[Scope, BufferedSource] = getFileSourceFrom("clients.txt")
  private val ordersFileSource: URIO[Scope, BufferedSource]  = getFileSourceFrom("orders.txt")

  private val bufferedWriter: RIO[Scope, FileWriter] =
    ZIO.acquireRelease(ZIO.attempt(new FileWriter("result.txt")))(writer => ZIO.succeed(writer.close()))

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = (for {
    clientsWithBalances <- getInfoFromFileSource(clientsFileSource, Parser.parseClientBalance)
    orders              <- getInfoFromFileSource(ordersFileSource, Parser.parseOrder)

    balanceService <- ZIO.service[BalanceService]
    _ <- ZIO.foreachDiscard(clientsWithBalances) { (client, balance) =>
      balanceService.addClient(client, balance)
    }

    exchange <- ZIO.service[Exchange]
    _        <- ZIO.foreachDiscard(orders)(exchange.processOrder)

    allBalances <- balanceService.getAllBalances

    writer <- bufferedWriter
    _ <- ZIO.foreachDiscard(allBalances.toList.sortBy(_._1.name)) { (client, balance) =>
      ZIO.attemptBlockingIO(
        writer.write(s"$client\t${balance.view}\n"),
      )
    }
  } yield ()).provideSome[Scope](BalanceServiceImpl.live, ExchangeImpl.live)

  private def getFileSourceFrom(path: String): URIO[Scope, BufferedSource] =
    ZIO.acquireRelease(ZIO.succeed(scala.io.Source.fromFile(path)))(source => ZIO.succeed(source.close()))

  private def getInfoFromFileSource[A](
      fileSource: URIO[Scope, BufferedSource],
      parse: String => Validation[String, A],
  ): UIO[List[A]] = ZIO.scoped {
    fileSource.map { source =>
      val lines = source.getLines().toList
      lines.map(parse).flatMap(_.toOption)
    }
  }

  // debug function
  private def balancesSumView(balances: List[Balance]): String = {
    val moneySum = balances.map(_.money.value).sum
    val sumA     = balances.map(_.securities.getOrElse(A, Quantity.zero).value).sum
    val sumB     = balances.map(_.securities.getOrElse(B, Quantity.zero).value).sum
    val sumC     = balances.map(_.securities.getOrElse(C, Quantity.zero).value).sum
    val sumD     = balances.map(_.securities.getOrElse(D, Quantity.zero).value).sum
    s"money: $moneySum, A: $sumA, B: $sumB, C: $sumC, D: $sumD"
  }
}
