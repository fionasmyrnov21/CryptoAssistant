package com.app.crypto.assistant.hfsdsg

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*

data class Level(val name: String,val color: Color,val pale: Color,val caption: String)
val levels=listOf(Level("Seed",Color(0xFF14866D),Color(0xFFE4F3EC),"A little clarity, every day."),Level("Clarity",Color(0xFF2767B0),Color(0xFFE8F0FC),"See the bigger picture."),Level("Insight",Color(0xFF7055B8),Color(0xFFF0EAFB),"Turn numbers into perspective."),Level("Horizon",Color(0xFFA65B14),Color(0xFFFFEEDD),"Make room for what comes next."))
val LocalLevel=staticCompositionLocalOf { levels[0] }
val Ink=Color(0xFF202F34)
val Muted=Color(0xFF718086)
val Backdrop=Color(0xFFF5F7F8)
@Composable fun CryptoAssistantApp(vm: CryptoAssistantModel=viewModel()) {
    val prefs by vm.prefs.collectAsState()
    val entries by vm.entries.collectAsState()
    val levelId=prefs?.first ?: 0
    val level=levels[levelId]
    MaterialTheme(colorScheme=lightColorScheme(primary=level.color,secondary=level.color,background=Backdrop,surface=Color.White,onSurface=Ink,onBackground=Ink,primaryContainer=level.pale,onPrimaryContainer=Ink,secondaryContainer=level.pale,onSecondaryContainer=level.color)) {
        CompositionLocalProvider(LocalLevel provides level) {
            if(prefs==null) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            else if(prefs?.second!=true) Intro { vm.finishIntro() }
            else {
                val nav=rememberNavController()
                val back by nav.currentBackStackEntryAsState()
                val route=back?.destination?.route ?: "overview"
                val roots=listOf("overview","portfolio","lab","levels")
                val ime=WindowInsets.ime.getBottom(LocalDensity.current)>0
                BackHandler(enabled=route in roots && !ime) { if(route!="overview") nav.navigate("overview") { popUpTo("overview"); launchSingleTop=true } }
                var editor by rememberSaveable { mutableStateOf<Editor?>(null) }
                val own=entries.filter { it.levelId==levelId }
                Scaffold(containerColor=Backdrop,bottomBar={
                    NavigationBar(containerColor=Color.White,tonalElevation=0.dp) {
                        listOf("Overview" to "◈","Portfolio" to "▦","Lab" to "⌁","Levels" to "◇").forEachIndexed { i,(name,icon) ->
                            NavigationBarItem(selected=route==roots[i],onClick={nav.navigate(roots[i]){popUpTo("overview");launchSingleTop=true}},icon={Text(icon,fontSize=25.sp)},label={Text(name,fontSize=11.sp)},colors=NavigationBarItemDefaults.colors(indicatorColor=level.pale,selectedIconColor=level.color,selectedTextColor=level.color))
                        }
                    }
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                            if(route !in roots) {
                                TextButton(onClick={nav.popBackStack()}) { Text("‹ Back") }
                                Spacer(Modifier.weight(1f))
                            }
                            else Text(stringResource(R.string.app_name),modifier=Modifier.weight(1f),fontSize=22.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-1).sp,maxLines=2)
                            TextButton(onClick={nav.navigate("levels")}) { Box(Modifier.size(8.dp).background(level.color,CircleShape)); Spacer(Modifier.width(8.dp));Text(level.name) }
                        }
                        NavHost(nav,startDestination="overview",modifier=Modifier.weight(1f)) {
                            composable("overview") { Overview(own,onTrade={editor=Editor("trade")},onGoals={nav.navigate("goals")},onActivity={nav.navigate("activity")},onAsset={nav.navigate("asset/$it")}) }
                            composable("portfolio") { Portfolio(own,{nav.navigate("asset/$it")},{editor=Editor("trade")}) }
                            composable("asset/{asset}") { b -> val a=b.arguments?.getString("asset")?.toIntOrNull()?:0; AssetScreen(a,own,{editor=Editor("trade",asset=a)},{editor=Editor("price",asset=a)}) }
                            composable("activity") { ActivityScreen(own,{editor=Editor("trade")},{editor=Editor("trade",it)},vm) }
                            composable("lab") { LabScreen(own,{nav.navigate("calc/$it")},{nav.navigate("saved/$it")}) }
                            composable("calc/{type}") { b -> CalculatorScreen(b.arguments?.getString("type")?.toIntOrNull()?:0,own,vm,levelId) }
                            composable("saved/{id}") { b -> val saved=own.find { it.id==b.arguments?.getString("id") }; if(saved!=null) CalculatorScreen(saved.json().getInt("type"),own,vm,levelId,saved) }
                            composable("levels") { LevelsScreen(entries,levelId,{vm.select(it);nav.navigate("overview"){popUpTo("overview");launchSingleTop=true}},{nav.navigate("statistics/$it")}) }
                            composable("statistics/{level}") { b -> val id=b.arguments?.getString("level")?.toIntOrNull()?:0; CompositionLocalProvider(LocalLevel provides levels[id]) { Statistics(entries.filter { it.levelId==id },levels[id]) } }
                            composable("goals") { GoalsScreen(own,{editor=Editor("goal")},{editor=Editor("goal",it)},vm) }
                        }
                    }
                    editor?.let { e -> EntryEditor(e,levelId,vm,onClose={editor=null}) }
                }
            }
        }
    }
}
@Composable fun Intro(done:()->Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler { if(page>0) page-- }
    val level=levels[page]
    val titles=listOf("Your assets.\nYour perspective.","Four spaces.\nOne fresh start.","Explore every\npossibility.","Small steps.\nClearer horizons.")
    val descriptions=listOf("A calmer way to track fictional crypto. Log trades and set prices, entirely on your terms.","Switch between four color-coded levels. Each has its own portfolio, goals and milestones.","Find your average entry, explore trade outcomes, plan growth and rebalance with confidence.","See your progress through thoughtful statistics and goals. Start with a clean, empty portfolio.")
    Column(Modifier.fillMaxSize().background(Backdrop).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(stringResource(R.string.app_name),modifier=Modifier.weight(1f),fontSize=24.sp,fontWeight=FontWeight.ExtraBold,maxLines=2);Spacer(Modifier.width(12.dp));Text("0${page+1} / 04",color=Muted) }
        AnimatedContent(page,label="intro") { p ->
            Column(verticalArrangement=Arrangement.spacedBy(24.dp)) {
                Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(32.dp)).background(level.pale).padding(22.dp),contentAlignment=Alignment.Center) {
                    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("ILLUSTRATIVE PREVIEW",fontSize=10.sp,color=level.color,letterSpacing=2.sp)
                        when(p) {
                            0 -> CardBlock { Text("Portfolio value",color=Muted); Text("$12,840.00",fontSize=32.sp,fontWeight=FontWeight.Bold); Text("+$840.00  ·  7.00%",color=level.color); Chart(listOf(2f,3f,2.6f,4f,3.8f,5.7f,6.2f),Modifier.height(65.dp),level.color) }
                            1 -> { levels.chunked(2).forEach { pair -> Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { pair.forEach { l -> Surface(color=Color.White,shape=RoundedCornerShape(20.dp),modifier=Modifier.weight(1f)) { Column(Modifier.padding(18.dp)) { Text("◇",fontSize=28.sp,color=l.color);Text(l.name,fontWeight=FontWeight.Bold);Text("Your own space",fontSize=10.sp,color=Muted) } } } } }
                            }
                            2 -> CardBlock { Text("TRADE OUTCOME",color=level.color,fontSize=10.sp);Text("What if you exit at $24?",fontSize=18.sp,fontWeight=FontWeight.Bold); Text("+$380.00",fontSize=34.sp,color=level.color,fontWeight=FontWeight.Bold);Text("Explore assumptions before you act.",color=Muted,fontSize=12.sp) }
                            else -> CardBlock { Text("YOUR NEXT MILESTONE",color=level.color,fontSize=10.sp);Text("A little further, together.",fontSize=19.sp,fontWeight=FontWeight.Bold); LinearProgressIndicator(progress={0.65f},modifier=Modifier.fillMaxWidth(),color=level.color);Text("65% of an illustrative goal",color=Muted);Text("◈  Price check    ◇  Goal setter",color=level.color,fontSize=12.sp) }
                        }
                    }
                }
                Text(titles[p],fontSize=36.sp,lineHeight=41.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
                Text(descriptions[p],fontSize=15.sp,lineHeight=23.sp,color=Muted)
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { repeat(4) { Box(Modifier.width(if(it==page) 28.dp else 7.dp).height(7.dp).background(if(it==page) level.color else Color(0xFFD7DFDF),CircleShape)) } }
            Button(onClick={if(page<3) page++ else done()},modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=level.color)) { Text(if(page==3) "Start tracking  →" else "Continue  →") }
            Text("Fictional assets. Your data stays on this device.",fontSize=11.sp,color=Muted)
        }
    }
}
