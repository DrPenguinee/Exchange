package org.exchahge.service.exchange.impl

import monocle.Lens
import org.exchahge.ListOps.*
import org.exchahge.model.Order.{BuyOrder, SellOrder}
import org.exchahge.model.{Balance, Order, Security}
import org.exchahge.service.balance.BalanceService
import org.exchahge.service.exchange.Exchange
import org.exchahge.service.exchange.impl.ExchangeImpl.OrderBook
import zio.{Ref, Task, UIO, URLayer, ZIO, ZLayer}

private class ExchangeImpl(
    balanceService: BalanceService,
    ordersRef: Ref[Map[Security, OrderBook]],
) extends Exchange {

  def processOrder(order: Order): Task[Unit] = {
    ordersRef.get.map(_.apply(order.security)).flatMap { orderBook =>
      order match {
        case buyOrder: BuyOrder =>
          updateOrderBook(buyOrder, orderBook, OrderBook.buys, OrderBook.sells).flatMap {
            case Some(buyOrder, sellOrder) => executeOrders(buyOrder, sellOrder)
            case None                      => ZIO.unit
          }

        case sellOrder: SellOrder =>
          updateOrderBook(sellOrder, orderBook, OrderBook.sells, OrderBook.buys).flatMap {
            case Some(sellOrder, buyOrder) => executeOrders(buyOrder, sellOrder)
            case None                      => ZIO.unit
          }
      }
    }
  }

  private def updateOrderBook[A <: Order, B <: Order](
      order: A,
      orderBook: OrderBook,
      lensA: Lens[OrderBook, List[A]],
      lensB: Lens[OrderBook, List[B]],
  ): UIO[Option[(A, B)]] = {
    val maybeComplementOrder: Option[B] =
      lensB.get(orderBook).find(o => o.price == order.price && o.quantity == order.quantity)
    maybeComplementOrder match {
      case Some(complementOrder) if order.client != complementOrder.client =>
        ordersRef
          .update(
            _.updatedWith(order.security)(
              _.map(lensB.modify(_.removeElement(complementOrder))),
            ),
          )
          .as(Some(order, complementOrder))
      case _ =>
        ordersRef.update(_.updatedWith(order.security)(_.map(lensA.modify(_ :+ order)))).as(None)
    }
  }

  private def executeOrders(
      buyOrder: BuyOrder,
      sellOrder: SellOrder,
  ): Task[Unit] =
    ZIO
      .fail(new IllegalArgumentException(s"$buyOrder doesn't correspond to $sellOrder"))
      .when(
        buyOrder.security != sellOrder.security || buyOrder.price != sellOrder.price || buyOrder.quantity != sellOrder.quantity,
      ) *> {

      val security = buyOrder.security
      val money    = buyOrder.price * buyOrder.quantity
      val quantity = buyOrder.quantity

      val updateBuyerBalance = (buyerBalance: Balance) =>
        buyerBalance.copy(
          money = buyerBalance.money - money,
          securities = buyerBalance.securities.updatedWith(security) {
            case Some(securityQuantity) => Some(securityQuantity + quantity)
            case None                   => Some(quantity)
          },
        )

      val updateSellerBalance = (sellerBalance: Balance) =>
        sellerBalance.copy(
          money = sellerBalance.money + money,
          securities = sellerBalance.securities.updatedWith(security) {
            case Some(securityQuantity) => Some(securityQuantity - quantity)
            case None                   => Some(-quantity)
          },
        )

      for {
        _ <- balanceService.changeClientBalance(buyOrder.client, updateBuyerBalance)
        _ <- balanceService.changeClientBalance(sellOrder.client, updateSellerBalance)
      } yield ()
    }
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
      ordersRef      <- Ref.make(Map.from(Security.values.map(_ -> OrderBook.empty)))
    } yield ExchangeImpl(balanceService, ordersRef)
  }
}
