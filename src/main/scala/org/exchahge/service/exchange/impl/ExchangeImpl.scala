package org.exchahge.service.exchange.impl

import monocle.Lens
import org.exchahge.ListOps.*
import org.exchahge.model.Order.{BuyOrder, SellOrder}
import org.exchahge.model.{Order, Security}
import org.exchahge.service.balance.BalanceService
import org.exchahge.service.balance.error.ClientNotFound
import org.exchahge.service.exchange.Exchange
import org.exchahge.service.exchange.impl.ExchangeImpl.OrderBook
import zio.{IO, Ref, Task, UIO, URLayer, ZIO, ZLayer}

private class ExchangeImpl(
    balanceService: BalanceService,
    ordersRef: Ref[Map[Security, OrderBook]],
) extends Exchange {

  def processOrder(order: Order): Task[Unit] = {
    ordersRef.get.map(_.apply(order.security)).flatMap { orderBook =>
      order match {
        case buyOrder: BuyOrder =>
          val maybeComplementOrder: Option[SellOrder] =
            orderBook.sells.find(o => o.price == order.price && o.quantity == order.quantity)
          maybeComplementOrder match {
            case Some(complementOrder) if buyOrder.client != complementOrder.client =>
              executeOrders(buyOrder, complementOrder) *>
                ordersRef.update(
                  _.updatedWith(order.security)(
                    _.map(book => book.copy(sells = book.sells.removeElement(complementOrder))),
                  ),
                )
            case _ =>
              ordersRef.update(_.updatedWith(order.security)(_.map(book => book.copy(buys = book.buys :+ buyOrder))))
          }

        case sellOrder: SellOrder =>
          val maybeComplementOrder: Option[BuyOrder] =
            orderBook.buys.find(o => o.price == order.price && o.quantity == order.quantity)
          maybeComplementOrder match {
            case Some(complementOrder) if sellOrder.client != complementOrder.client =>
              executeOrders(complementOrder, sellOrder).as(
                ordersRef.update(
                  _.updatedWith(order.security)(
                    _.map(book => book.copy(buys = book.buys.removeElement(complementOrder))),
                  ),
                ),
              )
            case _ =>
              ordersRef.update(_.updatedWith(order.security)(_.map(book => book.copy(sells = book.sells :+ sellOrder))))
          }
      }
    }
  }

  private def updateOrderBook[A <: Order, B <: Order](order: A, orderBook: OrderBook, lensA: Lens[OrderBook, List[A]], lensB: Lens[OrderBook, List[B]]): UIO[Option[(A, B)]] = {
    val maybeComplementOrder: Option[B] =
      lensB.get(orderBook).find(o => o.price == order.price && o.quantity == order.quantity)
    maybeComplementOrder match {
      case Some(complementOrder) if order.client != complementOrder.client =>
          ordersRef.update(
            _.updatedWith(order.security)(
              _.map(lensB.modify(_.removeElement(complementOrder))),
            ),
          ).as(Some(order, complementOrder))
      case _ =>
        ordersRef.update(_.updatedWith(order.security)(_.map(lensA.modify(_ :+ order)))).as(None)
    }
  }

  private def executeOrders(
      buyOrder: BuyOrder,
      sellOrder: SellOrder,
  ): IO[ClientNotFound, Unit] =
    for {
      balances <- balanceService
        .getClientBalance(buyOrder.client)
        .zipPar(balanceService.getClientBalance(sellOrder.client))

      (buyerBalance, sellerBalance) = balances

      updatedBuyerBalance = buyerBalance.copy(
        money = buyerBalance.money - buyOrder.price * buyOrder.quantity,
        securities = buyerBalance.securities.updatedWith(buyOrder.security)(
          _.map(_ + buyOrder.quantity),
        ),
      )

      updatedSellerBalance = sellerBalance.copy(
        money = sellerBalance.money + sellOrder.price * sellOrder.quantity,
        securities = sellerBalance.securities.updatedWith(sellOrder.security)(
          _.map(_ - sellOrder.quantity),
        ),
      )

      _ <- balanceService.changeClientBalance(buyOrder.client, updatedBuyerBalance)
      _ <- balanceService.changeClientBalance(sellOrder.client, updatedSellerBalance)
    } yield ()
}

object ExchangeImpl {
  case class OrderBook(
      buys: List[BuyOrder],
      sells: List[SellOrder],
  )

  object OrderBook {
    def empty: OrderBook = OrderBook(List.empty, List.empty)

    val buys: Lens[OrderBook, List[BuyOrder]] =
      Lens[OrderBook, List[BuyOrder]](_.buys)(buys => orderBook => orderBook.copy(buys = buys))
    val sells: Lens[OrderBook, List[SellOrder]] =
      Lens[OrderBook, List[SellOrder]](_.sells)(sells => orderBook => orderBook.copy(sells = sells))
  }

  val live: URLayer[BalanceService, Exchange] = ZLayer {
    for {
      balanceService <- ZIO.service[BalanceService]
      ordersRef <- Ref.make(Map.from(Security.values.map(_ -> OrderBook.empty)))
    } yield ExchangeImpl(balanceService, ordersRef)
  }
}
