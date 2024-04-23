package co.reachfive.identity.sdk.demo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class AuthenticatedActivityPagerAdapter(fm: FragmentManager): FragmentStatePagerAdapter(fm) {

    private var fragments: List<Fragment> = listOf()
    private var fragmentsTitle: List<String> = listOf()

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getItem(position: Int): Fragment {
        return fragments.get(position)
    }

    fun addFragment(fr: Fragment, title: String) {
        fragments+=fr
        fragmentsTitle+=title
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentsTitle.get(position)
    }
}