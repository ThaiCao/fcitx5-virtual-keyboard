package org.fcitx.fcitx5.android.input.candidates.expanded.window

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.BooleanKey.CandidatesEmpty
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesAttached
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesDetached
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.broadcast.ReturnKeyDrawableComponent
import org.fcitx.fcitx5.android.input.candidates.CandidateViewBuilder
import org.fcitx.fcitx5.android.input.candidates.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.candidates.adapter.BaseCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must

abstract class BaseExpandedCandidateWindow<T : BaseExpandedCandidateWindow<T>> :
    InputWindow.SimpleInputWindow<T>(), InputBroadcastReceiver {

    protected val builder: CandidateViewBuilder by manager.must()
    protected val theme by manager.theme()
    private val fcitx by manager.fcitx()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val bar: KawaiiBarComponent by manager.must()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()
    private val windowManager: InputWindowManager by manager.must()
    private val returnKeyDrawable: ReturnKeyDrawableComponent by manager.must()

    protected val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation

    private lateinit var lifecycleCoroutineScope: LifecycleCoroutineScope
    private lateinit var candidateLayout: ExpandedCandidateLayout

    abstract fun onCreateCandidateLayout(): ExpandedCandidateLayout

    final override fun onCreateView() = onCreateCandidateLayout().also { candidateLayout = it }

    private val keyActionListener = KeyActionListener { it, source ->
        if (it is KeyAction.LayoutSwitchAction) {
            when (it.act) {
                ExpandedCandidateLayout.Keyboard.UpBtnLabel -> prevPage()
                ExpandedCandidateLayout.Keyboard.DownBtnLabel -> nextPage()
            }
        } else {
            commonKeyActionListener.listener.onKeyAction(it, source)
        }
    }

    abstract val adapter: BaseCandidateViewAdapter
    abstract val layoutManager: RecyclerView.LayoutManager

    private var offsetJob: Job? = null

    abstract fun prevPage()

    abstract fun nextPage()

    override fun onAttached() {
        lifecycleCoroutineScope = candidateLayout.findViewTreeLifecycleOwner()!!.lifecycleScope
        bar.expandButtonStateMachine.push(ExpandedCandidatesAttached)
        candidateLayout.embeddedKeyboard.also {
            it.onReturnDrawableUpdate(returnKeyDrawable.resourceId)
            it.keyActionListener = keyActionListener
        }
        offsetJob = horizontalCandidate.expandedCandidateOffset
            .onEach(this::updateCandidatesWithOffset)
            .launchIn(lifecycleCoroutineScope)
    }

    private fun updateCandidatesWithOffset(offset: Int) {
        val candidates = horizontalCandidate.adapter.candidates
        if (candidates.isEmpty()) {
            windowManager.attachWindow(KeyboardWindow)
        } else {
            adapter.updateCandidatesWithOffset(candidates, offset)
            lifecycleCoroutineScope.launch(Dispatchers.Main) {
                candidateLayout.resetPosition()
            }
        }
    }

    override fun onDetached() {
        bar.expandButtonStateMachine.push(
            ExpandedCandidatesDetached,
            CandidatesEmpty to (adapter.candidates.size <= adapter.offset)
        )
        offsetJob?.cancel()
        offsetJob = null
        candidateLayout.embeddedKeyboard.keyActionListener = null
    }

    override fun onPreeditEmptyStateUpdate(empty: Boolean) {
        if (empty) {
            windowManager.attachWindow(KeyboardWindow)
        }
    }

}