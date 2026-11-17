package com.app.crypto.assistant.hfsdsg

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.time.*

@Composable fun Field(label:String,value:String,onChange:(String)->Unit,numeric:Boolean=true) {OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(16.dp),keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text))}
@Composable fun AssetPicker(asset:Int,onSelect:(Int)->Unit,allowAll:Boolean=false) {
    var expanded by remember {mutableStateOf(false)}
    Box {OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {Text(if(asset<0)"All assets ▾" else "${assetNames[asset]} ▾")};DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {if(allowAll)DropdownMenuItem(text={Text("All assets")},onClick={onSelect(-1);expanded=false});assetNames.forEachIndexed {i,n ->DropdownMenuItem(text={Text(n)},onClick={onSelect(i);expanded=false})}}}
}
data class Editor(val kind:String,val entry:Entry?=null,val asset:Int=0): java.io.Serializable
@Composable fun EntryEditor(editor:Editor,level:Int,vm:CryptoAssistantModel,onClose:()->Unit) {
    val existing=editor.entry?.json()
    var asset by rememberSaveable {mutableIntStateOf(existing?.optInt("asset")?:editor.asset)}
    var buy by rememberSaveable {mutableStateOf(existing?.optBoolean("buy")?:true)}
    var quantity by rememberSaveable {mutableStateOf(existing?.optString("quantity")?:"")}
    var price by rememberSaveable {mutableStateOf(existing?.optString("price")?:"")}
    var fee by rememberSaveable {mutableStateOf(existing?.optString("fee")?:"0")}
    var timestamp by rememberSaveable {mutableStateOf(LocalDateTime.ofInstant(Instant.ofEpochMilli(editor.entry?.at?:System.currentTimeMillis()),ZoneId.systemDefault()).format(inputFormat))}
    var name by rememberSaveable {mutableStateOf(existing?.optString("name")?:"")}
    var target by rememberSaveable {mutableStateOf(existing?.optString("target")?:"")}
    var deadline by rememberSaveable {mutableStateOf(existing?.optString("deadline")?:LocalDate.now().plusMonths(3).toString())}
    var error by remember {mutableStateOf("")};var saving by remember {mutableStateOf(false)};val scope=rememberCoroutineScope()
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(color=Backdrop,shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth().padding(12.dp).heightIn(max=700.dp).imePadding()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row {Text(when(editor.kind){"trade"->if(editor.entry==null)"Add a trade" else "Edit trade";"price"->"Update price";else->if(editor.entry==null)"Create a goal" else "Edit goal"},fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));TextButton(onClick=onClose){Text("Close")}}
                Text("${levels[level].name} space · USD",color=Muted,fontSize=12.sp)
                if(editor.kind=="goal") {Field("Goal name",name,{name=it},false);Field("Target portfolio value ($)",target,{target=it});Field("Deadline (yyyy-MM-dd)",deadline,{deadline=it},false)}
                else {AssetPicker(asset,{asset=it});if(editor.kind=="trade"){Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){FilterChip(selected=buy,onClick={buy=true},label={Text("Buy")});FilterChip(selected=!buy,onClick={buy=false},label={Text("Sell")})};Field("Quantity",quantity,{quantity=it})};Field("Price per unit ($)",price,{price=it});if(editor.kind=="trade")Field("Fee ($)",fee,{fee=it});Field("Local date & time (yyyy-MM-dd HH:mm:ss)",timestamp,{timestamp=it},false)}
                if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,fontSize=13.sp)
                Button(enabled=!saving,onClick={scope.launch {
                    try {
                        saving=true
                        val j=JSONObject()
                        val at:Long
                        if(editor.kind=="goal") {require(name.isNotBlank()&&name.length<=80){"Enter a goal name of up to 80 characters."};require(target.bd().signum()>0){"Target must be positive."};val day=LocalDate.parse(deadline);require(day>=LocalDate.now()){ "Choose today or a future deadline." };j.put("name",name.trim()).put("target",target.bd().toPlainString()).put("deadline",day.toString());at=editor.entry?.at?:System.currentTimeMillis()}
                        else {val p=price.bd();require(p.signum()>=0){"Price cannot be negative."};at=LocalDateTime.parse(timestamp,inputFormat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();require(at<=System.currentTimeMillis()){ "Entries cannot be dated in the future." };j.put("asset",asset).put("price",p.toPlainString());if(editor.kind=="trade"){val q=quantity.bd();val f=fee.bd();require(q.signum()>0&&q.scale()<=8&&f.signum()>=0){"Use a positive quantity (up to 8 decimals) and a non-negative fee."};j.put("buy",buy).put("quantity",q.toPlainString()).put("fee",f.toPlainString())}}
                        vm.save(level,editor.kind,j,at,editor.entry?.id?:"${System.currentTimeMillis()}-${java.util.UUID.randomUUID()}")
                        onClose()
                    } catch(e:Exception){error=if(e is java.time.format.DateTimeParseException)"Enter a valid date in the displayed format." else e.message?:"Could not save entry."} finally {saving=false}
                }},modifier=Modifier.fillMaxWidth().height(52.dp)) {Text(if(saving)"Saving…" else "Save ${if(editor.kind=="price")"price" else editor.kind}")}
            }
        }
    }
}
val calcNames=listOf("Trade Outcome","Average Entry","Growth Planner","Rebalance")
val calcSubtitles=listOf("See the full picture, fees included.","Many entries. One clear average.","Let your assumptions take shape.","Bring your allocation into balance.")
@Composable fun LabScreen(entries:List<Entry>,onOpen:(Int)->Unit,onSaved:(String)->Unit) {
    Page("A little what-if.","Four tools to turn questions into perspective.") {
        calcNames.forEachIndexed {i,n ->CardBlock(Modifier.clickable{onOpen(i)}) {Row {Text(listOf("↗","≋","⌁","▦")[i],fontSize=30.sp,color=LocalLevel.current.color);Spacer(Modifier.weight(1f));Text("0${i+1}",color=Muted,fontSize=12.sp)};Text(n,fontSize=22.sp,fontWeight=FontWeight.Bold);Text(calcSubtitles[i],fontSize=13.sp,color=Muted);Text("Explore calculator  →",fontSize=12.sp,color=LocalLevel.current.color)}}
        Section("Saved perspectives")
        val scenarios=entries.filter {it.kind=="scenario"}
        if(scenarios.isEmpty())Empty("Keep an interesting idea","Save a calculation to revisit its inputs and results.")
        scenarios.reversed().forEach {s ->CardBlock(Modifier.clickable {onSaved(s.id)}) {Text(calcNames[s.json().getInt("type")],fontWeight=FontWeight.Bold);Text(date(s.at),fontSize=12.sp,color=Muted);Text("Open saved calculation →",fontSize=12.sp,color=LocalLevel.current.color)}}
    }
}
@Composable fun CalculatorScreen(type:Int,entries:List<Entry>,vm:CryptoAssistantModel,level:Int,saved:Entry?=null) {
    val fields=rememberSaveable(saved?.id,type,saver=Saver<SnapshotStateMap<String,String>,String>(save={JSONObject(it.toMap()).toString()},restore={raw -> mutableStateMapOf<String,String>().apply {val j=JSONObject(raw);j.keys().forEach {k->put(k,j.getString(k))}}})) {mutableStateMapOf<String,String>().apply {if(saved!=null){val j=saved.json().getJSONObject("inputs");j.keys().forEach {put(it,j.getString(it))}}}}
    var rows by rememberSaveable(saved?.id,type) {mutableIntStateOf(saved?.json()?.optInt("rows",2)?:2)}
    var result by remember {mutableStateOf<List<Pair<String,String>>>(emptyList())};var chart by remember {mutableStateOf<List<Float>>(emptyList())};var error by remember {mutableStateOf("")};var message by remember {mutableStateOf("")};val scope=rememberCoroutineScope()
    fun value(key:String,default:String="")=fields[key]?:default
    fun number(key:String,default:String="")=value(key,default).bd()
    fun calculate() {
        error="";message=""
        try {
            chart=emptyList()
            result=when(type) {
                0 -> {val o=outcome(number("q"),number("entry"),number("exit"),number("bf","0"),number("sf","0"));listOf("Total cost" to money(o.cost),"Net proceeds" to money(o.proceeds),"Profit / loss" to money(o.profit),"ROI" to percent(o.profit,o.cost),"Break-even price" to money(o.breakEven))}
                1 -> {val (q,c)=averageEntry((0 until rows).map {Triple(number("q$it"),number("p$it"),number("f$it","0"))});listOf("Total quantity" to units(q),"Total invested" to money(c),"Average entry" to money(c.divSafe(q)))}
                2 -> {val initial=number("initial");val contribution=number("monthly");val months=value("months").toIntOrNull()?:throw IllegalArgumentException("Enter a whole number of months.");val points=growth(initial,contribution,number("rate"),months);val invested=initial+contribution*BigDecimal(months);chart=points.map {it.toFloat()};listOf("Projected value" to money(points.last()),"Your contributions" to money(invested),"Hypothetical growth" to money(points.last()-invested))}
                else -> {val values=(0 until rows).map {number("v$it")};val target=(0 until rows).map {number("t$it")};val changes=rebalance(values,target);changes.mapIndexed {i,n -> "${assetNames[i]} · ${if(n.signum()>=0)"Buy" else "Sell"}" to money(n.abs())}}
            }
        }catch(e:Exception){result=emptyList();error=e.message?:"Check your inputs."}
    }
    LaunchedEffect(saved?.id) {if(saved!=null)calculate()}
    Page(calcNames[type],calcSubtitles[type]) {
        CardBlock {
            when(type) {
                0 -> {Field("Quantity",value("q"),{fields["q"]=it;result=emptyList()});Field("Entry price ($)",value("entry"),{fields["entry"]=it;result=emptyList()});Field("Exit price ($)",value("exit"),{fields["exit"]=it;result=emptyList()});Field("Buy fee ($)",value("bf","0"),{fields["bf"]=it;result=emptyList()});Field("Sell fee ($)",value("sf","0"),{fields["sf"]=it;result=emptyList()})}
                1 -> {repeat(rows){i ->Text("Purchase ${i+1}",fontWeight=FontWeight.Bold);Field("Quantity",value("q$i"),{fields["q$i"]=it;result=emptyList()});Field("Price ($)",value("p$i"),{fields["p$i"]=it;result=emptyList()});Field("Fee ($)",value("f$i","0"),{fields["f$i"]=it;result=emptyList()})};Row{if(rows<10)TextButton(onClick={rows++;result=emptyList()}){Text("+ Add purchase")};if(rows>1)TextButton(onClick={rows--;result=emptyList()}){Text("Remove last")}}}
                2 -> {Field("Starting amount ($)",value("initial"),{fields["initial"]=it;result=emptyList()});Field("Monthly contribution ($)",value("monthly"),{fields["monthly"]=it;result=emptyList()});Field("Assumed annual return (%)",value("rate"),{fields["rate"]=it;result=emptyList()});Field("Number of months",value("months"),{fields["months"]=it;result=emptyList()});Text("Effective annual rate. Contributions are added at month end. Based on your assumptions.",color=Muted,fontSize=12.sp)}
                else -> {TextButton(onClick={val positions=entries.summary().positions;rows=6;positions.forEachIndexed {i,p ->fields["v$i"]=p.value.toPlainString()};result=emptyList()}){Text("Use this level's holdings")};repeat(rows){i ->Text(assetNames[i],fontWeight=FontWeight.Bold);Field("Current value ($)",value("v$i"),{fields["v$i"]=it;result=emptyList()});Field("Target weight (%)",value("t$i"),{fields["t$i"]=it;result=emptyList()})};Row{if(rows<6)TextButton(onClick={rows++;result=emptyList()}){Text("+ Add asset")};if(rows>2)TextButton(onClick={rows--;result=emptyList()}){Text("Remove last")}};Text("Target weights must total 100%. Suggestions exclude fees and do not create trades.",color=Muted,fontSize=12.sp)}
            }
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
            Button(onClick={calculate()},modifier=Modifier.fillMaxWidth().height(52.dp)){Text("Calculate  →")}
        }
        if(result.isNotEmpty()) {
            Surface(color=LocalLevel.current.pale,shape=RoundedCornerShape(24.dp)) {Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){Text("YOUR PERSPECTIVE",color=LocalLevel.current.color,fontSize=10.sp);result.forEach {(label,value)->Metric(label,value)};if(chart.isNotEmpty())Chart(chart,Modifier.height(110.dp))}}
            Button(onClick={scope.launch {try {val inputs=JSONObject();fields.forEach {(k,v)->inputs.put(k,v)};vm.save(level,"scenario",JSONObject().put("type",type).put("rows",rows).put("inputs",inputs));message="Saved to ${levels[level].name}."}catch(e:Exception){error="Could not save scenario."}}},modifier=Modifier.fillMaxWidth()){Text("Save perspective")}
        }
        if(message.isNotEmpty())Text(message,color=LocalLevel.current.color)
    }
}
