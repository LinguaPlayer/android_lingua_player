package org.videolan.vlc.gui.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingFragmentPagerAdapter(fragmentActivity: FragmentActivity, private var count: Int) : FragmentStateAdapter(fragmentActivity) {

    private var fragmentList: MutableList<FragmentName> = mutableListOf(
            FragmentName.WELCOME,
            FragmentName.TRANSLATE,
            FragmentName.SHADOWING,
            FragmentName.SMARTSUB,
            FragmentName.SCAN,
            FragmentName.THEME
    )

    fun onCustomizedChanged(customizeEnabled: Boolean) {
        count = if (customizeEnabled) {
            fragmentList.add(5, FragmentName.FOLDERS)
            7
        } else {
            fragmentList.remove(FragmentName.FOLDERS)
            6
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = count

    override fun getItemId(position: Int): Long {
        return fragmentList[position].ordinal.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        val fragment = FragmentName.values()[itemId.toInt()]
        return fragmentList.contains(fragment)
    }

    override fun createFragment(position: Int): Fragment {
        return when (fragmentList[position]) {
            FragmentName.WELCOME -> OnboardingWelcomeFragment.newInstance()
            FragmentName.TRANSLATE -> OnboardingTranslateFragment.newInstance()
            FragmentName.SHADOWING -> OnboardingShadowingFragment.newInstance()
            FragmentName.SMARTSUB -> OnboardingSmartSub.newInstance()
            FragmentName.SCAN -> OnboardingScanningFragment.newInstance()
            FragmentName.FOLDERS -> OnboardingFoldersFragment.newInstance()
            FragmentName.THEME -> OnboardingThemeFragment.newInstance()
        }
    }

    enum class FragmentName {
        WELCOME,
        TRANSLATE,
        SHADOWING,
        SMARTSUB,
        SCAN,
        FOLDERS,
        THEME
    }
}
