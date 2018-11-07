package bayrakh.kaktus

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.db.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val init = arrayListOf(
                BudgetChange(title = "Продукты", text = "- 7000 р."),
                BudgetChange(title = "Общественный транспорт", text = "- 150 р."),
                BudgetChange(title = "Продукты", text = "- 7100 р.")
        )

        budgetChangeList.layoutManager = LinearLayoutManager(this)
        budgetChangeList.adapter = BudgetChangeAdapter(init)
        val db = Database.getInstance(this)
        db.getChanges()
                .subscribe { list ->
                    budgetChangeList.adapter = BudgetChangeAdapter(init + list)
                }

        fab.setOnClickListener {
            val count = budgetChangeList.adapter.itemCount
            db.addChange(BudgetChange(title = "Новая трата #$count", text = "-100 р."))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    data class BudgetChange(
            var id: Long? = null,
            val title: String,
            val text: String
    )

    class Database(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "my-db", null, 1) {
        companion object {
            private var INSTANCE: Database? = null

            fun getInstance(context: Context): Database {
                synchronized(Database::class) {
                    val instance = INSTANCE
                    if (instance == null) {
                        val result = Database(context.applicationContext)
                        INSTANCE = result
                        return result
                    }
                    return instance
                }
            }

            fun destroyInstance() {
                INSTANCE = null
            }
        }

        override fun onCreate(db: SQLiteDatabase?) {
            db?.apply {
                createTable("Changes", true,
                        "id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                        "title" to TEXT,
                        "text" to TEXT
                )
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Here you can upgrade tables, as usual
            db.dropTable("Changes", true)
        }

        private val changesTableChanged = PublishSubject.create<String>()

        fun getChanges(): Observable<List<BudgetChange>> {
            return Observable.concat(Observable.just("new"), changesTableChanged)
                    .map {
                        use {
                            select("Changes", "id", "title", "text")
                                    .parseList(classParser<BudgetChange>())
                        }
                    }
        }

        fun addChange(change: BudgetChange) {
            use {
                insert("Changes",
                        "title" to change.title,
                        "text" to change.text
                )
            }
            changesTableChanged.onNext("insert")
        }
    }

    class BudgetChangeAdapter(private var _list: List<BudgetChange>) : RecyclerView.Adapter<BudgetChangeAdapter.BudgetChangeViewHolder>() {
        override fun getItemCount() = _list.size

        override fun onBindViewHolder(holder: BudgetChangeAdapter.BudgetChangeViewHolder?, position: Int) {
            holder?.fromValue(_list[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BudgetChangeAdapter.BudgetChangeViewHolder {
            val view = LayoutInflater.from(parent?.context).inflate(R.layout.budget_change, parent, false)
            return BudgetChangeAdapter.BudgetChangeViewHolder(view)
        }

        class BudgetChangeViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
            val title = itemView?.findViewById<TextView>(R.id.Title)
            val text = itemView?.findViewById<TextView>(R.id.Text)

            fun fromValue(obj: BudgetChange) {
                title?.text = obj.title
                text?.text = obj.text
            }
        }

        var list: List<BudgetChange>
            get() = _list
            set(newValue) {
                _list = newValue
            }
    }
}
