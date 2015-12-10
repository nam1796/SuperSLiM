package com.tonicartos.superslim.slm

import com.tonicartos.superslim.Child
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.SectionState
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.adapter.Section

class LinearSectionConfig(gutterStart: Int = Section.Config.DEFAULT_GUTTER, gutterEnd: Int = Section.Config.DEFAULT_GUTTER,
                          @HeaderStyle headerStyle: Int = Section.Config.DEFAULT_HEADER_STYLE) : Section.Config(gutterStart, gutterEnd, headerStyle) {

    override fun onMakeSection(oldState: SectionState?): SectionState = LinearSectionState(this, oldState)
}

internal class LinearSectionState(val configuration: LinearSectionConfig, oldState: SectionState? = null) : SectionState(configuration, oldState) {
    override fun doLayout(helper: LayoutHelper) {
        LinearSlm.onLayout(helper, this)
    }
}

private object LinearSlm : SectionLayoutManager<LinearSectionState> {
    override fun onLayout(helper: LayoutHelper, section: LinearSectionState) {
        var currentPos = section.headPosition

        var y = 0
        while (y < helper.layoutLimit && currentPos < section.numChildren) {
            val child = section.getChildAt(helper, currentPos)
            child.addToRecyclerView()
            child.measure()
            child.layout(0, y, child.measuredWidth, y + child.measuredHeight)

            val childHeight = child.height
            if (helper.isPreLayout && child.isRemoved) {
                helper.addIgnoredHeight(child.height)
            }
            y += childHeight
            currentPos += 1

            child.done()
        }

        section.height = y
        section.tailPosition = currentPos

        layoutDisappearingViews(y, helper, section)
    }

    private fun layoutDisappearingViews(yStart: Int, helper: LayoutHelper, section: LinearSectionState) {
        if (section.tailPosition == section.numChildren || helper.isPreLayout || !helper.willRunPredictiveAnimations || !helper.supportsPredictiveItemAnimations) {
            return
        }

        var y = yStart
        for (currentPosition in section.tailPosition..section.numChildren) {
            if (helper.scrap[0] !in section) {
                // No more scrap to process for this section.
                break
            }

            // Add disappearing children. These will disappear from scrap as they are added, and eventually we will
            // break out of this section when the helper no longer has scrap for this section.
            val child = section.getChildAt(helper, currentPosition)
            child.animationState = Child.ANIM_DISAPPEARING
            child.addToRecyclerView()
            child.measure()
            child.layout(0, y, child.measuredWidth, y + child.measuredHeight)

            y += child.height

            child.done()
        }

        section.height = y
    }

    /**
     * Fill revealed area where content has been scrolled down the screen by dy.
     */
    override fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: LinearSectionState): Int {
        var y = dy

        var currentPos = section.headPosition - 1
        while (y >= 0 && currentPos >= 0) {
            val child = section.getChildAt(helper, currentPos)
            child.addToRecyclerView(0)
            child.measure()
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            child.layout(0, y - childHeight, childWidth, y)

            y -= childHeight
            currentPos -= 1

            child.done()
        }

        section.headPosition = currentPos
        section.height += dy - y

        return dy - y
    }

    /**
     * Fill revealed area where content has been scrolled up the screen by dy.
     */
    override fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: LinearSectionState): Int {
        var y = section.height - dy

        var currentPos = section.tailPosition + 1
        while (y < helper.layoutLimit && currentPos < section.numChildren) {
            val child = section.getChildAt(helper, currentPos)
            child.addToRecyclerView(0)
            child.measure()
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            child.layout(0, y, childWidth, y + childHeight)
            y += childHeight
            currentPos += 1

            child.done()
        }

        section.tailPosition = currentPos
        section.height = y

        return y - (section.height - dy)
    }
}