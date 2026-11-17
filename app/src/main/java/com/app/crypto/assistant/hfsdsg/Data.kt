package com.app.crypto.assistant.hfsdsg

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID

@Entity(tableName = "entries", indices = [Index("levelId")])
data class Entry(@PrimaryKey val id: String, val levelId: Int, val kind: String, val data: String, val at: Long): java.io.Serializable
@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY at, id") fun observe(): Flow<List<Entry>>
    @Query("SELECT * FROM entries ORDER BY at, id") suspend fun snapshot(): List<Entry>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(entry: Entry)
    @Query("DELETE FROM entries WHERE id = :id") suspend fun delete(id: String)
}
@Database(entities = [Entry::class], version = 1, exportSchema = false)
abstract class CryptoAssistantDatabase: RoomDatabase() { abstract fun entries(): EntryDao }
val Application.preferences by preferencesDataStore("veyra_preferences")
fun Entry.json() = JSONObject(data)
fun Entry.trade(): Trade { val j=json(); return Trade(id,j.getInt("asset"),j.getBoolean("buy"),j.getString("quantity").bd(),j.getString("price").bd(),j.getString("fee").bd(),at) }
fun Entry.quote(): Quote { val j=json(); return Quote(j.getInt("asset"),j.getString("price").bd(),at,id) }
fun List<Entry>.summary(until: Long = Long.MAX_VALUE) = summarize(filter { it.kind=="trade" }.map { it.trade() },filter { it.kind=="price" }.map { it.quote() },until)
class CryptoAssistantModel(app: Application): AndroidViewModel(app) {
    private val mutex=Mutex()
    private val db=Room.databaseBuilder(app,CryptoAssistantDatabase::class.java,"veyra.db").build()
    val entries=db.entries().observe().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val prefs=app.preferences.data.map { (it[intPreferencesKey("level")] ?: 0) to (it[booleanPreferencesKey("onboarded")] ?: false) }.stateIn(viewModelScope,SharingStarted.Eagerly,null)
    fun select(level: Int) { viewModelScope.launch { getApplication<Application>().preferences.edit { it[intPreferencesKey("level")]=level } } }
    fun finishIntro() { viewModelScope.launch { getApplication<Application>().preferences.edit { it[booleanPreferencesKey("onboarded")]=true } } }
    suspend fun save(level: Int, kind: String, json: JSONObject, at: Long = System.currentTimeMillis(), id: String = UUID.randomUUID().toString()) {
        mutex.withLock {
        val entry=Entry(id,level,kind,json.toString(),at)
        if (kind=="trade") (db.entries().snapshot().filter { it.levelId==level && it.id!=id }+entry).summary()
        db.entries().put(entry)
        val badge = when(kind) { "trade" -> "First trade"; "price" -> "Price check"; "goal" -> "Goal setter"; "scenario" -> "Scenario saved"; else -> null }
        if (badge!=null && db.entries().snapshot().none { it.id=="badge-$level-$kind" }) db.entries().put(Entry("badge-$level-$kind",level,"badge",JSONObject().put("name",badge).toString(),System.currentTimeMillis()))
        }
    }
    suspend fun remove(entry: Entry) {
        mutex.withLock {
        if(entry.kind=="trade") db.entries().snapshot().filter { it.levelId==entry.levelId && it.id!=entry.id }.summary()
        db.entries().delete(entry.id)
        }
    }
}
