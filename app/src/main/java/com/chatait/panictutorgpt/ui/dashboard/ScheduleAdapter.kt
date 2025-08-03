package com.chatait.panictutorgpt.ui.dashboard

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chatait.panictutorgpt.R
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(
    private val items: MutableList<ScheduleItem>,
    private val onDeleteClick: ((String) -> Unit)? = null,
    private val onItemClick: ((ScheduleItem) -> Unit)? = null,
    private val onEmptyScheduleDelete: ((String) -> Unit)? = null
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.scheduleDate)
        val subjectsText: TextView = view.findViewById(R.id.scheduleSubjects)
        val cardView: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.dateText.text = item.date

        // 空でない科目のみ表示
        val nonEmptySubjects = item.subjects.withIndex()
            .filter { it.value.isNotBlank() }

        holder.subjectsText.text = if (nonEmptySubjects.isEmpty()) {
            "テスト予定なし"
        } else {
            nonEmptySubjects.joinToString("\n") { (i, s) -> "${i+1}限: $s" }
        }

        // クリックリスナー設定
        holder.cardView.setOnClickListener {
            onItemClick?.invoke(item)
        }

        // タッチリスナー設定（長押し処理）
        holder.cardView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startLongPressAnimation(view)
                    startLongPressTimer(view, item)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelLongPress(view)
                    false
                }
                else -> false
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun startLongPressAnimation(view: View) {
        val originalColor = Color.WHITE // デフォルトの白色
        val pressedColor = Color.RED // プレス時の赤色

        val colorAnimator = ValueAnimator.ofArgb(originalColor, pressedColor)
        colorAnimator.duration = 5000 // 5秒
        colorAnimator.addUpdateListener { animator ->
            view.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimator.start()

        view.tag = colorAnimator
    }

    private fun startLongPressTimer(view: View, item: ScheduleItem) {
        longPressRunnable = Runnable {
            showForcedDeleteDialog(view.context, item)
        }
        longPressHandler.postDelayed(longPressRunnable!!, 5000) // 5秒
    }

    private fun cancelLongPress(view: View) {
        longPressRunnable?.let { runnable ->
            longPressHandler.removeCallbacks(runnable)
            longPressRunnable = null
        }

        // アニメーション停止と色をリセット
        (view.tag as? ValueAnimator)?.cancel()
        view.setBackgroundColor(Color.WHITE) // 白色にリセット
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
            val testDate = dateFormat.parse(dateString)
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
}
