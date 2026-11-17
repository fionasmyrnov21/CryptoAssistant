package com.app.crypto.assistant.hfsdsg

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy",Locale.US).withZone(ZoneId.systemDefault())
val inputFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss",Locale.US)
fun date(at:Long)=dateFormat.format(Instant.ofEpochMilli(at))
@Composable fun Page(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=24.dp,end=24.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Column { Text(title,fontSize=30.sp,lineHeight=35.sp,fontWeight=FontWeight.Bold,letterSpacing=(-0.8).sp);Spacer(Modifier.height(6.dp));Text(subtitle,color=Muted,fontSize=13.sp,lineHeight=20.sp) }
        content()
    }
}
@Composable fun CardBlock(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Surface(modifier.fillMaxWidth(),color=Color.White,shape=RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content) }
}
@Composable fun Section(title:String,action:String?=null,onAction:()->Unit={}) { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(title,fontSize=18.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));if(action!=null) TextButton(onClick=onAction) { Text(action,fontSize=12.sp) } } }
@Composable fun Empty(title:String,body:String,action:String?=null,onAction:()->Unit={}) { CardBlock { Text("◇",fontSize=34.sp,color=LocalLevel.current.color);Text(title,fontSize=19.sp,fontWeight=FontWeight.SemiBold);Text(body,color=Muted,fontSize=13.sp,lineHeight=20.sp);if(action!=null) TextButton(onClick=onAction) {Text("$action  →")} } }
@Composable fun Metric(label:String,value:String,modifier:Modifier=Modifier) { Column(modifier,verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(label,color=Muted,fontSize=11.sp);Text(value,fontWeight=FontWeight.Bold,fontSize=19.sp) } }
@Composable fun MetricRow(a:String,av:String,b:String,bv:String) { Row(Modifier.fillMaxWidth()) {Metric(a,av,Modifier.weight(1f));Metric(b,bv,Modifier.weight(1f))} }
@Composable fun Chart(values:List<Float>,modifier:Modifier=Modifier,color:Color=LocalLevel.current.color) {
    if(values.size<2) { Box(modifier.fillMaxWidth(),contentAlignment=Alignment.Center) { Text("Your history will take shape here",color=Muted,fontSize=12.sp) };return }
    Canvas(modifier.fillMaxWidth().semantics { contentDescription="Entered value history. ${values.size} observations." }) {
        val low=values.min();val range=(values.max()-low).coerceAtLeast(1f)
        val path=Path()
        val points=values.mapIndexed { i,v -> androidx.compose.ui.geometry.Offset(size.width*i/(values.size-1),size.height*0.9f-(v-low)/range*size.height*0.75f) }
        points.forEachIndexed { i,p -> if(i==0) path.moveTo(p.x,p.y) else {path.lineTo(p.x,points[i-1].y);path.lineTo(p.x,p.y)} }
        val fill=Path().apply { addPath(path);lineTo(size.width,size.height);lineTo(0f,size.height);close() }
        drawPath(fill,Brush.verticalGradient(listOf(color.copy(alpha=0.17f),color.copy(alpha=0.01f))))
        drawPath(path,color,style=androidx.compose.ui.graphics.drawscope.Stroke(width=3.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
        drawCircle(color,4.dp.toPx(),points.last())
    }
}
@Composable fun History(entries:List<Entry>) {
    var period by rememberSaveable { mutableIntStateOf(3) }
    val now=System.currentTimeMillis()
    val days=listOf(7,30,90,0)[period]
    val financial=entries.filter { it.kind=="trade" || it.kind=="price" }
    val first=financial.minOfOrNull { it.at }?:now
    val start=if(days==0) first else now-days*86400000L
    val times=(financial.map { it.at }.filter { it in start..now } + listOf(start,now)).distinct().sorted()
    CardBlock {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { listOf("1W","1M","3M","All").forEachIndexed { i,s -> FilterChip(selected=period==i,onClick={period=i},label={Text(s)}) } }
        Chart(if(financial.isEmpty()) emptyList() else times.map { entries.summary(it).value.toFloat() },Modifier.height(130.dp))
        Text(if(financial.isEmpty()) "Add a trade to begin your history" else "${date(start)} — ${date(now)} · Manual valuations",fontSize=10.sp,color=Muted)
    }
}
@Composable fun Coin(asset:Int) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(LocalLevel.current.pale),contentAlignment=Alignment.Center) {Text(listOf("◈","◇","⬡","◉","△","▧")[asset],color=LocalLevel.current.color,fontSize=26.sp)}
}
@Composable fun AssetRow(p:Position,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
        Coin(p.asset);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)) {Text(assetNames[p.asset],fontWeight=FontWeight.SemiBold);Text("${units(p.quantity)} units",fontSize=11.sp,color=Muted)}
        Column(horizontalAlignment=Alignment.End) {Text(money(p.value),fontWeight=FontWeight.SemiBold);Text(money(p.unrealized),color=if(p.unrealized.signum()<0) Color(0xFFB84747) else LocalLevel.current.color,fontSize=11.sp)}
    }
}
@Composable fun Overview(entries:List<Entry>,onTrade:()->Unit,onGoals:()->Unit,onActivity:()->Unit,onAsset:(Int)->Unit) {
    val s=entries.summary();val l=LocalLevel.current
    Page("A clearer picture.","${l.name.uppercase()} SPACE  ·  ${l.caption}") {
        Surface(color=l.pale,shape=RoundedCornerShape(28.dp)) {Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("TOTAL PORTFOLIO VALUE",fontSize=10.sp,letterSpacing=1.5.sp,color=l.color)
            Text(money(s.value),fontSize=38.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
            Text("${money(s.profit)} total P&L  ·  ${percent(s.profit,s.buys)}",color=l.color,fontSize=13.sp)
            Button(onClick=onTrade,shape=RoundedCornerShape(14.dp)) { Text("+ Add trade") }
        }}
        MetricRow("Realized P&L",money(s.realized),"Unrealized P&L",money(s.unrealized))
        History(entries)
        Section("Your assets","View activity",onActivity)
        if(s.positions.none { it.quantity.signum()>0 }) Empty("Begin with your first trade","Choose a fictional asset, enter its price and watch your portfolio take shape.","Add a trade",onTrade)
        else CardBlock {s.positions.filter {it.quantity.signum()>0}.forEach {AssetRow(it){onAsset(it.asset)}}}
        Section("A goal to grow toward","View goals",onGoals)
        val goal=entries.firstOrNull { it.kind=="goal" }
        if(goal==null) Empty("Give your numbers a purpose","Set a portfolio value to work toward, at your own pace.","Create a goal",onGoals)
        else GoalCard(goal,s.value,onGoals)
        Text("Fictional assets · Prices entered by you · USD",fontSize=11.sp,color=Muted)
    }
}
@Composable fun Portfolio(entries:List<Entry>,onAsset:(Int)->Unit,onTrade:()->Unit) {
    var search by rememberSaveable {mutableStateOf("")};val s=entries.summary()
    Page("Your portfolio","Six fictional assets. A space to make them your own.") {
        OutlinedTextField(value=search,onValueChange={search=it},placeholder={Text("Search fictional assets")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
        CardBlock {s.positions.filter {assetNames[it.asset].contains(search,true)}.forEach {AssetRow(it){onAsset(it.asset)}};if(assetNames.none {it.contains(search,true)})Text("No matching assets",color=Muted)}
        Button(onClick=onTrade,modifier=Modifier.fillMaxWidth().height(52.dp)) {Text("+ Add trade")}
        Allocation(s)
    }
}
@Composable fun Allocation(s:Summary) {
    CardBlock {Text("Asset allocation",fontWeight=FontWeight.Bold);if(s.value.signum()==0)Text("Add holdings and prices to see your allocation.",color=Muted,fontSize=13.sp)
        s.positions.filter {it.value.signum()>0}.forEach { p -> Row(Modifier.fillMaxWidth()) {Text(assetNames[p.asset],fontSize=12.sp,modifier=Modifier.weight(1f));Text(percent(p.value,s.value),fontSize=12.sp)};LinearProgressIndicator(progress={p.value.divSafe(s.value).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth(),color=LocalLevel.current.color,trackColor=LocalLevel.current.pale) }
    }
}
@Composable fun AssetScreen(asset:Int,entries:List<Entry>,onTrade:()->Unit,onPrice:()->Unit) {
    val s=entries.summary();val p=s.positions[asset]
    Page(assetNames[asset],"Fictional asset · Manual price history") {
        CardBlock {Coin(asset);Text(money(p.price),fontSize=36.sp,fontWeight=FontWeight.Bold);Text("Latest entered price",color=Muted,fontSize=12.sp)
            Text(entries.filter { it.kind=="price" || it.kind=="trade" }.filter {it.json().optInt("asset")==asset}.maxOfOrNull {it.at}?.let {"Updated ${date(it)}"}?:"No price entered yet",color=Muted,fontSize=11.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {Button(onClick=onTrade){Text("Add trade")};OutlinedButton(onClick=onPrice){Text("Update price")}}
        }
        MetricRow("Your quantity",units(p.quantity),"Position value",money(p.value))
        MetricRow("Average entry",money(p.average),"Remaining cost",money(p.cost))
        MetricRow("Realized P&L",money(p.realized),"Unrealized P&L",money(p.unrealized))
        CardBlock {Text("Entered price history",fontWeight=FontWeight.Bold);val prices=entries.filter {it.kind=="price" || it.kind=="trade"}.filter {it.json().optInt("asset")==asset}.sortedBy {it.at};Chart(prices.map {it.json().getString("price").toFloat()},Modifier.height(140.dp));prices.takeLast(5).reversed().forEach {Text("${date(it.at)}   ${money(it.json().getString("price").bd())}",fontSize=12.sp,color=Muted)}}
        Section("Trade history");val trades=entries.filter {it.kind=="trade" && it.json().getInt("asset")==asset};if(trades.isEmpty())Empty("No trades yet","Add your first purchase to track this position.") else CardBlock {trades.reversed().forEach {TradeRow(it)}}
    }
}
@Composable fun TradeRow(entry:Entry,onClick:()->Unit={}) {
    val t=entry.trade()
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {Coin(t.asset);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("${if(t.buy)"Buy" else "Sell"} ${assetNames[t.asset]}",fontSize=13.sp,fontWeight=FontWeight.SemiBold);Text("${date(t.at)} · ${units(t.quantity)} units",fontSize=10.sp,color=Muted)};Text(money(t.quantity*t.price),fontSize=13.sp,fontWeight=FontWeight.SemiBold)}
}
@Composable fun ActivityScreen(entries:List<Entry>,onAdd:()->Unit,onEdit:(Entry)->Unit,vm:CryptoAssistantModel) {
    var filter by rememberSaveable {mutableStateOf("All")};var asset by rememberSaveable {mutableIntStateOf(-1)}
    var deleting by remember {mutableStateOf<Entry?>(null)};var error by remember {mutableStateOf("")};val scope=rememberCoroutineScope()
    Page("Your activity","Every entry, thoughtfully organized.") {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("All","Buy","Sell","Prices").forEach {FilterChip(selected=filter==it,onClick={filter=it},label={Text(it)})}}
        AssetPicker(asset,{asset=it},allowAll=true)
        Button(onClick=onAdd){Text("+ Add trade")}
        val filtered=entries.filter {it.kind=="trade" || it.kind=="price"}.filter { (asset<0 || it.json().getInt("asset")==asset) && when(filter){"Buy"->it.kind=="trade"&&it.json().getBoolean("buy");"Sell"->it.kind=="trade"&&!it.json().getBoolean("buy");"Prices"->it.kind=="price";else->true} }.reversed()
        if(filtered.isEmpty())Empty("A fresh page","Your trades and price updates will appear here.")
        else CardBlock {filtered.forEach {e -> if(e.kind=="trade")TradeRow(e){onEdit(e)} else Text("${assetNames[e.json().getInt("asset")]} · ${money(e.json().getString("price").bd())}\nPrice update · ${date(e.at)}",fontSize=13.sp);Row {if(e.kind=="trade")TextButton(onClick={onEdit(e)}){Text("Edit")};TextButton(onClick={deleting=e}){Text("Delete",color=Color(0xFFB84747))}}}}
        if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
    }
    deleting?.let { e -> AlertDialog(onDismissRequest={deleting=null},title={Text("Delete this entry?")},text={Text("Your portfolio and historical statistics will be recalculated.")},confirmButton={TextButton(onClick={scope.launch {try{vm.remove(e);error=""}catch(ex:Exception){error=ex.message?:"Could not delete entry."};deleting=null}}){Text("Delete")}},dismissButton={TextButton(onClick={deleting=null}){Text("Cancel")}}) }
}
@Composable fun LevelsScreen(entries:List<Entry>,active:Int,onSelect:(Int)->Unit,onStats:(Int)->Unit) {
    Page("Find your perspective.","Four independent spaces. Switch whenever you like.") {
        levels.forEachIndexed { i,l -> val own=entries.filter {it.levelId==i};val s=own.summary();val badges=own.count {it.kind=="badge"}
            Surface(color=l.pale,shape=RoundedCornerShape(26.dp)) {Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth()){Text("0${i+1}  /  ${l.name}",fontSize=23.sp,fontWeight=FontWeight.Bold,color=l.color);Spacer(Modifier.weight(1f));if(active==i)Text("ACTIVE",fontSize=10.sp,color=l.color)}
                Text(l.caption,color=Muted,fontSize=13.sp);MetricRow("Portfolio value",money(s.value),"Milestones","$badges / 4")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {Button(onClick={onSelect(i)},colors=ButtonDefaults.buttonColors(containerColor=l.color)){Text(if(active==i)"Open space" else "Switch space")};TextButton(onClick={onStats(i)}){Text("Statistics",color=l.color)}}
            }}
        }
    }
}
@Composable fun Statistics(entries:List<Entry>,level:Level) {
    val s=entries.summary();val trades=entries.filter {it.kind=="trade"};val ranked=s.positions.filter {p ->trades.any {it.trade().asset==p.asset}}.sortedByDescending {it.realized+it.unrealized}
    Page("${level.name} statistics","A detailed view of your financial journey.") {
        CardBlock {Text("TOTAL VALUE",color=level.color,fontSize=11.sp);Text(money(s.value),fontSize=36.sp,fontWeight=FontWeight.Bold);Text("Total P&L ${money(s.profit)} · ${percent(s.profit,s.buys)}",color=level.color)}
        History(entries)
        CardBlock {MetricRow("Remaining cost",money(s.cost),"Fees paid",money(s.fees));HorizontalDivider(color=Backdrop);MetricRow("Purchase costs",money(s.buys),"Net sale proceeds",money(s.sales));HorizontalDivider(color=Backdrop);MetricRow("Realized P&L",money(s.realized),"Unrealized P&L",money(s.unrealized));HorizontalDivider(color=Backdrop);MetricRow("Purchases",trades.count {it.trade().buy}.toString(),"Sales",trades.count {!it.trade().buy}.toString())}
        Allocation(s)
        if(ranked.isNotEmpty())CardBlock {Text("Performance spotlight",fontWeight=FontWeight.Bold);Text("Best · ${assetNames[ranked.first().asset]}  ${money(ranked.first().realized+ranked.first().unrealized)}",fontSize=13.sp);Text("Lowest · ${assetNames[ranked.last().asset]}  ${money(ranked.last().realized+ranked.last().unrealized)}",fontSize=13.sp)}
        Section("Your milestones")
        CardBlock { listOf("First trade","Price check","Goal setter","Scenario saved").forEach {name -> val unlocked=entries.any {it.kind=="badge"&&it.json().getString("name")==name};Row(Modifier.fillMaxWidth()){Text(if(unlocked)"●" else "○",color=level.color);Spacer(Modifier.width(12.dp));Text(name,modifier=Modifier.weight(1f));Text(if(unlocked)"Achieved" else "To explore",fontSize=11.sp,color=Muted)}} }
    }
}
@Composable fun GoalCard(entry:Entry,value:BigDecimal,onClick:()->Unit) {val j=entry.json();val target=j.getString("target").bd();CardBlock(Modifier.clickable(onClick=onClick)) {Text(j.getString("name"),fontWeight=FontWeight.Bold,fontSize=18.sp);Text("${money(value)} of ${money(target)}",color=Muted,fontSize=12.sp);LinearProgressIndicator(progress={value.divSafe(target).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth(),color=LocalLevel.current.color);Text("${percent(value,target)} · Due ${j.getString("deadline")}",fontSize=12.sp,color=LocalLevel.current.color)}}
@Composable fun GoalsScreen(entries:List<Entry>,onAdd:()->Unit,onEdit:(Entry)->Unit,vm:CryptoAssistantModel) {
    val goals=entries.filter {it.kind=="goal"};val scope=rememberCoroutineScope();var deleting by remember {mutableStateOf<Entry?>(null)}
    Page("Make it meaningful.","Goals follow the value of this level's portfolio.") {Button(onClick=onAdd){Text("+ Create goal")};if(goals.isEmpty())Empty("Your next chapter starts here","Choose a name, a target value and a date.");goals.forEach {g ->GoalCard(g,entries.summary().value){onEdit(g)};Row {TextButton(onClick={onEdit(g)}){Text("Edit goal")};TextButton(onClick={deleting=g}){Text("Delete")}}}}
    deleting?.let {e ->AlertDialog(onDismissRequest={deleting=null},title={Text("Delete goal?")},confirmButton={TextButton(onClick={scope.launch{vm.remove(e);deleting=null}}){Text("Delete")}},dismissButton={TextButton(onClick={deleting=null}){Text("Cancel")}})}
}
