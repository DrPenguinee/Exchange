package org.exchahge

import org.exchahge.model.*
import org.exchahge.model.Order.{BuyOrder, SellOrder}
import zio.prelude.Validation

import scala.util.matching.Regex

object Parser {
  def parseOrder(line: String): Validation[String, Order] = {
    val orderPattern: Regex = """^(\w+)\t([bs])\t(\w+)\t(\d+)\t(\d+)$""".r
    line match {
      case orderPattern(clientName, rawSide, sec, price, quantity) =>
        for {
          security    <- Validation.fromOptionWith(s"Wrong security name $sec")(Security.fromValue(sec))
          priceInt    <- Validation.fromOptionWith("Price field is not int")(price.toIntOption)
          quantityInt <- Validation.fromOptionWith("Quantity field is not int")(quantity.toIntOption)
          order <- rawSide match
            case "b" =>
              Validation.succeed(
                BuyOrder(
                  Client(clientName),
                  security,
                  MoneyAmount(priceInt),
                  Quantity(quantityInt),
                ),
              )
            case "s" =>
              Validation.succeed(
                SellOrder(
                  Client(clientName),
                  security,
                  MoneyAmount(priceInt),
                  Quantity(quantityInt),
                ),
              )
            case _ => Validation.fail(s"Order side '$rawSide' has wrong format (should be 's' or 'b')")
        } yield order
      case _ => Validation.fail(s"Line '$line' doesn't correspond order format")
    }
  }

  def parseClientBalance(line: String): Validation[String, (Client, Balance)] = {
    val clientWithBalancePattern: Regex = """^(\w+)\t(\d+)\t(\d+)\t(\d+)\t(\d+)\t(\d+)$""".r
    line match {
      case clientWithBalancePattern(clientName, moneyAmount, secA, secB, secC, secD) =>
        for {
          moneyAmountInt <- Validation.fromOptionWith(s"Money amount field '$moneyAmount' is not int")(
            moneyAmount.toIntOption,
          )
          secAInt <- Validation.fromOptionWith(s"Security A amount field '$moneyAmount' is not int")(secA.toIntOption)
          secBInt <- Validation.fromOptionWith(s"Security B amount field '$moneyAmount' is not int")(secB.toIntOption)
          secCInt <- Validation.fromOptionWith(s"Security C amount field '$moneyAmount' is not int")(secC.toIntOption)
          secDInt <- Validation.fromOptionWith(s"Security D amount field '$moneyAmount' is not int")(secD.toIntOption)
        } yield (
          Client(clientName),
          Balance(
            MoneyAmount(moneyAmountInt),
            Map(
              Security.A -> Quantity(secAInt),
              Security.B -> Quantity(secBInt),
              Security.C -> Quantity(secCInt),
              Security.D -> Quantity(secDInt),
            ),
          ),
        )
      case _ => Validation.fail(s"Line '$line' doesn't correspond client format")
    }
  }
}
