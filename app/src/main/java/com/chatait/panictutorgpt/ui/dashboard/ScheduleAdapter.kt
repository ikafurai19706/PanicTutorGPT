package com.chatait.panictutorgpt.ui.dashboard

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chatait.panictutorgpt.R
import com.chatait.panictutorgpt.data.StudyRepository
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(
    private val items: MutableList<ScheduleItem>,
    private val onDeleteClick: ((String) -> Unit)? = null,
    private val onItemClick: ((ScheduleItem) -> Unit)? = null,
    private val onEmptyScheduleDelete: ((String) -> Unit)? = null,
    private val studyRepository: StudyRepository? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val displayDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    private var displayItems = mutableListOf<DisplayItem>()
    private var isPastTestsExpanded = false

    companion object {
        private const val TYPE_SCHEDULE = 0
        private const val TYPE_PAST_TESTS_HEADER = 1
    }

    init {
        updateDisplayItems()
    }

    private fun updateDisplayItems() {
        displayItems.clear()

        val (pastTests, currentAndFutureTests) = items.partition { isPastDate(it.date) }

        // 現在・未来のテストを先に追加
        currentAndFutureTests.sortedBy { it.date }.forEach { schedule ->
            displayItems.add(DisplayItem.Schedule(schedule))
        }

        // 過去のテストがある場合はヘッダーを追加
        if (pastTests.isNotEmpty()) {
            displayItems.add(
                DisplayItem.PastTestsHeader(
                    pastTests = pastTests.sortedByDescending { it.date },
                    isExpanded = isPastTestsExpanded
                )
            )

            // 展開されている場合は過去のテストも表示
            if (isPastTestsExpanded) {
                pastTests.sortedByDescending { it.date }.forEach { schedule ->
                    displayItems.add(DisplayItem.Schedule(schedule))
                }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.scheduleDate)
        val subjectsText: TextView = view.findViewById(R.id.scheduleSubjects)
        val cardView: View = view
    }

    class PastTestsHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerTitle: TextView = view.findViewById(R.id.headerTitle)
        val pastTestCount: TextView = view.findViewById(R.id.pastTestCount)
        val expandIcon: android.widget.ImageView = view.findViewById(R.id.expandIcon)
        val cardView: View = view
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is DisplayItem.Schedule -> TYPE_SCHEDULE
            is DisplayItem.PastTestsHeader -> TYPE_PAST_TESTS_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SCHEDULE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
                ViewHolder(view)
            }
            TYPE_PAST_TESTS_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_past_tests_header, parent, false)
                PastTestsHeaderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayItems[position]

        when (holder) {
            is ViewHolder -> {
                if (item is DisplayItem.Schedule) {
                    bindScheduleItem(holder, item.scheduleItem)
                }
            }
            is PastTestsHeaderViewHolder -> {
                if (item is DisplayItem.PastTestsHeader) {
                    bindPastTestsHeader(holder, item)
                }
            }
        }
    }

    private fun bindScheduleItem(holder: ViewHolder, item: ScheduleItem) {
        holder.dateText.text = item.date

        // 空でない科目のみ表示（チェックマーク付き＋色付き）
        val nonEmptySubjects = item.subjects.withIndex()
            .filter { it.value.isNotBlank() }

        if (nonEmptySubjects.isEmpty()) {
            holder.subjectsText.text = "テスト予定なし"
        } else {
            val spannableBuilder = SpannableStringBuilder()

            nonEmptySubjects.forEachIndexed { index, (i, subject) ->
                val period = i + 1
                val isStudied = studyRepository?.isSubjectStudiedToday(item.date, subject, period) == true

                val text = if (isStudied) {
                    "${period}限: $subject ✓"
                } else {
                    "${period}限: $subject"
                }

                val startIndex = spannableBuilder.length
                spannableBuilder.append(text)
                val endIndex = spannableBuilder.length

                // 勉強完了した科目は緑色に
                if (isStudied) {
                    val greenColor = Color.parseColor("#4CAF50") // マテリアルデザインの緑色
                    spannableBuilder.setSpan(
                        ForegroundColorSpan(greenColor),
                        startIndex,
                        endIndex,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // 最後の項目以外は改行を追加
                if (index < nonEmptySubjects.size - 1) {
                    spannableBuilder.append("\n")
                }
            }

            holder.subjectsText.text = spannableBuilder
        }

        // タッチリスナー設定
        holder.cardView.alpha = 1.0f
        holder.cardView.isClickable = true
        holder.cardView.isLongClickable = true

        if (isPastDate(item.date)) {
            // 過去のテストは通常のタップのみ有効（長押しは無効）
            holder.cardView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
                        onItemClick?.invoke(item)
                        true
                    }
                    else -> false
                }
            }
            // 過去のテストであることを視覚的に示す（少し暗くする）
            holder.cardView.alpha = 0.8f
        } else {
            // 現在または未来のテストは通常通りタッチ操作を有効化
            holder.cardView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startLongPressAnimation(view)
                        startLongPressTimer(view, item)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val wasLongPressCompleted = longPressRunnable == null
                        cancelLongPress(view)
                        if (!wasLongPressCompleted) {
                            // 長押しが完了していない場合のみ通常クリック
                            onItemClick?.invoke(item)
                        }
                        // 長押しが完了した場合は何もしない（削除ダイアログは既に表示済み）
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        cancelLongPress(view)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun bindPastTestsHeader(holder: PastTestsHeaderViewHolder, item: DisplayItem.PastTestsHeader) {
        holder.headerTitle.text = "過去のテスト"
        holder.pastTestCount.text = "${item.pastTests.size}件"

        // ヘッダーのタッチリスナー設定
        holder.cardView.setOnClickListener {
            isPastTestsExpanded = !isPastTestsExpanded
            updateDisplayItems()
            notifyDataSetChanged()
        }

        // 矢印アイコンの回転
        val rotationDegree = if (isPastTestsExpanded) 180f else 0f
        holder.expandIcon.rotation = rotationDegree
    }

    override fun getItemCount(): Int = displayItems.size

    private fun isPastDate(dateString: String): Boolean {
        return try {
            val testDate = displayDateFormat.parse(dateString)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            testDate != null && testDate.before(today)
        } catch (e: Exception) {
            false
        }
    }

    private fun startLongPressAnimation(view: View) {
        // CardViewの元の背景を保存
        val originalBackground = view.background

        // 色付きオーバーレイを作成してCardViewのスタイルを保持
        val originalColor = 0x00FF0000 // 透明な赤
        val pressedColor = 0xAAFF0000.toInt() // 半透明の赤

        val colorAnimator = ValueAnimator.ofArgb(originalColor, pressedColor)
        colorAnimator.duration = 5000 // 5秒
        colorAnimator.addUpdateListener { animator ->
            // オーバーレイ色でCardViewのスタイルを保持したまま色を変更
            view.setBackgroundColor(animator.animatedValue as Int)
            // 元の背景の上に色を重ねる効果を実現
            view.foreground = android.graphics.drawable.ColorDrawable(animator.animatedValue as Int)
        }
        colorAnimator.start()

        // タグにアニメーターと元の背景を保存
        view.tag = mapOf("animator" to colorAnimator, "originalBackground" to originalBackground)
    }

    private fun startLongPressTimer(view: View, item: ScheduleItem) {
        longPressRunnable = Runnable {
            // タイマー実行時にrunnableをnullにして、ACTION_UPで通常クリックが実行されないようにする
            longPressRunnable = null
            showForcedDeleteDialog(view.context, item)
        }
        longPressHandler.postDelayed(longPressRunnable!!, 5000) // 5秒
    }

    private fun cancelLongPress(view: View) {
        longPressRunnable?.let { runnable ->
            longPressHandler.removeCallbacks(runnable)
            longPressRunnable = null
        }

        // 保存されたアニメーターと元の背景を取得
        val tagMap = view.tag as? Map<String, Any>
        val animator = tagMap?.get("animator") as? ValueAnimator
        val originalBackground = tagMap?.get("originalBackground")

        // アニメーション停止
        animator?.cancel()

        // CardViewを完全に元の状態に復元
        if (originalBackground != null) {
            view.background = originalBackground as android.graphics.drawable.Drawable
        }

        // オーバーレイを削除
        view.foreground = null

        view.tag = null
    }

    private fun showForcedDeleteDialog(context: android.content.Context, item: ScheduleItem) {
        AlertDialog.Builder(context)
            .setTitle("削除確認")
            .setMessage("本当に削除しますか？この操作は取り消せません。")
            .setPositiveButton("削除") { _, _ ->
                onDeleteClick?.invoke(item.id)
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun isWithinOneWeek(dateString: String): Boolean {
        return try {
            val testDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
            val currentDate = Date()
            val oneWeekFromNow = Calendar.getInstance().apply {
                time = currentDate
                add(Calendar.DAY_OF_YEAR, 7)
            }.time

            testDate != null && testDate.before(oneWeekFromNow)
        } catch (e: Exception) {
            false
        }
    }

    fun showDeleteConfirmationForEmptySchedule(context: android.content.Context, item: ScheduleItem) {
        if (isWithinOneWeek(item.date)) {
            AlertDialog.Builder(context)
                .setTitle("削除不可")
                .setMessage("一週間前を切ったテストの予定は削除できません。")
                .setPositiveButton("OK", null)
                .show()
        } else {
            AlertDialog.Builder(context)
                .setTitle("削除確認")
                .setMessage("この日のテスト予定を削除します。よろしいですか？")
                .setPositiveButton("削除") { _, _ ->
                    onEmptyScheduleDelete?.invoke(item.id)
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    fun removeItem(itemId: String) {
        val position = items.indexOfFirst { it.id == itemId }
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateItem(updatedItem: ScheduleItem) {
        val position = items.indexOfFirst { it.id == updatedItem.id }
        if (position != -1) {
            items[position] = updatedItem
            notifyItemChanged(position)
        }
    }

    fun refreshData() {
        updateDisplayItems()
        notifyDataSetChanged()
    }

    sealed class DisplayItem {
        data class Schedule(val scheduleItem: ScheduleItem) : DisplayItem()
        data class PastTestsHeader(
            val pastTests: List<ScheduleItem>,
            val isExpanded: Boolean
        ) : DisplayItem()
    }
}
