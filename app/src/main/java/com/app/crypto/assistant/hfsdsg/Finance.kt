package com.app.crypto.assistant.hfsdsg

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

val MC = MathContext(24, RoundingMode.HALF_EVEN)
val ZERO: BigDecimal = BigDecimal.ZERO
fun String.bd(): BigDecimal = toBigDecimalOrNull() ?: throw IllegalArgumentException("Enter valid numbers in all fields.")
fun BigDecimal.divSafe(other: BigDecimal): BigDecimal = if (other.signum() == 0) ZERO else divide(other, MC)
fun money(n: BigDecimal): String = NumberFormat.getCurrencyInstance(Locale.US).format(n)
fun units(n: BigDecimal): String = n.setScale(8, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString()
fun percent(n: BigDecimal, base: BigDecimal): String = if (base.signum() == 0) "—" else "${n.multiply(BigDecimal(100)).divSafe(base).setScale(2, RoundingMode.HALF_EVEN)}%"
val assetNames = listOf("Qelunari", "Zeruvexa", "Ornelyth", "Fenqualiq", "Nuvareliq", "Talquveniq")
data class Trade(val id: String, val asset: Int, val buy: Boolean, val quantity: BigDecimal, val price: BigDecimal, val fee: BigDecimal, val at: Long)
data class Quote(val asset: Int, val price: BigDecimal, val at: Long, val id: String)
data class Position(val asset: Int, val quantity: BigDecimal, val cost: BigDecimal, val price: BigDecimal, val realized: BigDecimal) {
    val value get() = quantity.multiply(price, MC)
    val unrealized get() = value.subtract(cost)
    val average get() = cost.divSafe(quantity)
}
data class Summary(val positions: List<Position>, val buys: BigDecimal, val sales: BigDecimal, val fees: BigDecimal) {
    val value get() = positions.fold(ZERO) { a, p -> a + p.value }
    val cost get() = positions.fold(ZERO) { a, p -> a + p.cost }
    val realized get() = positions.fold(ZERO) { a, p -> a + p.realized }
    val unrealized get() = value - cost
    val profit get() = realized + unrealized
}
fun summarize(trades: List<Trade>, quotes: List<Quote>, until: Long = Long.MAX_VALUE): Summary {
    val eligible = trades.filter { it.at <= until }.sortedWith(compareBy<Trade> { it.at }.thenBy { it.id })
    val positions = assetNames.indices.map { asset ->
        var quantity = ZERO
        var cost = ZERO
        var realized = ZERO
        eligible.filter { it.asset == asset }.forEach { t ->
            require(t.quantity.signum() > 0 && t.price.signum() >= 0 && t.fee.signum() >= 0) { "Quantity must be positive. Prices and fees cannot be negative." }
            if (t.buy) {
                quantity += t.quantity
                cost += t.quantity.multiply(t.price, MC) + t.fee
            } else {
                require(t.quantity <= quantity) { "This change would sell more ${assetNames[asset]} than available on its trade date." }
                val removed = if (t.quantity.compareTo(quantity) == 0) cost else cost.multiply(t.quantity, MC).divSafe(quantity)
                realized += t.quantity.multiply(t.price, MC) - t.fee - removed
                quantity -= t.quantity
                cost -= removed
            }
        }
        val observations = quotes.filter { it.asset == asset && it.at <= until } + eligible.filter { it.asset == asset }.map { Quote(asset, it.price, it.at, it.id) }
        val price = observations.sortedWith(compareBy<Quote> { it.at }.thenBy { it.id }).lastOrNull()?.price ?: ZERO
        Position(asset, quantity, cost, price, realized)
    }
    return Summary(positions, eligible.filter { it.buy }.fold(ZERO) { a,t -> a+t.quantity*t.price+t.fee }, eligible.filter { !it.buy }.fold(ZERO) { a,t -> a+t.quantity*t.price-t.fee }, eligible.fold(ZERO) { a,t -> a+t.fee })
}
data class Outcome(val cost: BigDecimal, val proceeds: BigDecimal, val profit: BigDecimal, val breakEven: BigDecimal)
fun outcome(q: BigDecimal, entry: BigDecimal, exit: BigDecimal, buyFee: BigDecimal, sellFee: BigDecimal): Outcome {
    require(q.signum() > 0 && listOf(entry, exit, buyFee, sellFee).all { it.signum() >= 0 }) { "Use a positive quantity and non-negative prices and fees." }
    val cost = q*entry+buyFee
    val proceeds = q*exit-sellFee
    return Outcome(cost, proceeds, proceeds-cost, (cost+sellFee).divSafe(q))
}
fun averageEntry(rows: List<Triple<BigDecimal, BigDecimal, BigDecimal>>): Pair<BigDecimal, BigDecimal> {
    require(rows.isNotEmpty() && rows.all { it.first.signum() > 0 && it.second.signum() >= 0 && it.third.signum() >= 0 }) { "Use positive quantities and non-negative prices and fees." }
    val quantity = rows.fold(ZERO) { a,r -> a+r.first }
    val cost = rows.fold(ZERO) { a,r -> a+r.first*r.second+r.third }
    return quantity to cost
}
fun growth(initial: BigDecimal, contribution: BigDecimal, annual: BigDecimal, months: Int): List<BigDecimal> {
    require(initial.signum() >= 0 && contribution.signum() >= 0 && annual > BigDecimal(-100) && annual <= BigDecimal(1000) && months in 1..600) { "Use non-negative amounts, 1–600 months, and an annual rate above −100% and up to 1,000%." }
    val annualFactor = BigDecimal.ONE + annual.divSafe(BigDecimal(100))
    var monthly = BigDecimal.ONE
    repeat(80) { monthly = (monthly*BigDecimal(11) + annualFactor.divSafe(monthly.pow(11,MC))).divSafe(BigDecimal(12)) }
    val values = mutableListOf(initial)
    repeat(months) { values.add(values.last().multiply(monthly,MC)+contribution) }
    return values
}
fun rebalance(values: List<BigDecimal>, targets: List<BigDecimal>): List<BigDecimal> {
    require(values.size == targets.size && values.isNotEmpty() && values.all { it.signum() >= 0 } && targets.all { it.signum() >= 0 } && targets.fold(ZERO,BigDecimal::add).compareTo(BigDecimal(100)) == 0) { "Values must be non-negative and target weights must total 100%." }
    val total = values.fold(ZERO,BigDecimal::add)
    return values.indices.map { total*targets[it].divSafe(BigDecimal(100))-values[it] }
}
