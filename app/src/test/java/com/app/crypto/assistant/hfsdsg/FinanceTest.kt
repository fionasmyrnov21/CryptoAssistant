package com.app.crypto.assistant.hfsdsg

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FinanceTest {
    private fun b(s:String)=BigDecimal(s)
    private fun equal(expected:String,actual:BigDecimal)=assertEquals(0,b(expected).compareTo(actual))
    private fun purchase(id:String="1",at:Long=1)=Trade(id,0,true,b("10"),b("20"),b("2"),at)
    @Test fun partialSaleIncludesBothFees() {
        val s=summarize(listOf(purchase(),Trade("2",0,false,b("4"),b("30"),b("1"),2)),listOf(Quote(0,b("25"),3,"3")))
        equal("6",s.positions[0].quantity);equal("121.2",s.cost);equal("38.2",s.realized);equal("28.8",s.unrealized);equal("67",s.profit);equal("3",s.fees)
    }
    @Test fun editingEarlierBuyReplaysAverageCost() {
        val s=summarize(listOf(purchase().copy(price=b("10")),Trade("2",0,false,b("4"),b("30"),b("1"),2)),emptyList())
        equal("61.2",s.cost);equal("78.2",s.realized)
    }
    @Test(expected=IllegalArgumentException::class) fun oversellingHistoricalHoldingsFails() {summarize(listOf(purchase(at=2),Trade("0",0,false,b("1"),b("30"),b("0"),1)),emptyList())}
    @Test fun fullSaleClearsCostAndQuantity() {val s=summarize(listOf(purchase(),Trade("2",0,false,b("10"),b("21"),b("1"),2)),emptyList());equal("0",s.cost);equal("0",s.value);equal("7",s.realized)}
    @Test fun valuationUsesOnlyKnownPastPrice() {val s=summarize(listOf(purchase()),listOf(Quote(0,b("50"),5,"q")),3);equal("200",s.value)}
    @Test fun emptyPortfolioHasNoInventedValue() {equal("0",summarize(emptyList(),emptyList()).value);assertEquals("—",percent(ZERO,ZERO))}
    @Test fun outcomeAndBreakEven() {val o=outcome(b("10"),b("20"),b("30"),b("2"),b("1"));equal("97",o.profit);equal("20.3",o.breakEven)}
    @Test fun averageIncludesFees() {val(q,c)=averageEntry(listOf(Triple(b("2"),b("10"),b("1")),Triple(b("3"),b("20"),b("2"))));equal("5",q);equal("83",c);equal("16.6",c.divSafe(q))}
    @Test fun zeroGrowthAndEndMonthContributions() {equal("220",growth(b("100"),b("10"),ZERO,12).last())}
    @Test fun effectiveAnnualGrowth() {assertTrue((growth(b("100"),ZERO,b("12"),12).last()-b("112")).abs()<b("0.0000001"))}
    @Test fun negativeGrowthWorks() {assertTrue((growth(b("100"),ZERO,b("-50"),12).last()-b("50")).abs()<b("0.0000001"))}
    @Test fun rebalanceConservesPortfolio() {val moves=rebalance(listOf(b("80"),b("20")),listOf(b("50"),b("50")));equal("-30",moves[0]);equal("30",moves[1]);equal("0",moves.reduce(BigDecimal::add))}
    @Test(expected=IllegalArgumentException::class) fun invalidAllocationRejected(){rebalance(listOf(b("80"),b("20")),listOf(b("30"),b("30")))}
}
