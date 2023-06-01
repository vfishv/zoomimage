/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.zoom.sample.ui.subsamplingview

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.github.panpf.assemblyadapter.pager2.AssemblyFragmentStateAdapter
import com.github.panpf.zoom.sample.SampleImage
import com.github.panpf.zoom.sample.databinding.TabPagerFragmentBinding
import com.github.panpf.zoom.sample.ui.base.BindingFragment
import com.github.panpf.zoom.sample.ui.base.ToolbarBindingFragment
import com.google.android.material.tabs.TabLayoutMediator

class SubsamplingViewPagerFragment : ToolbarBindingFragment<TabPagerFragmentBinding>() {

    override fun onViewCreated(
        toolbar: Toolbar,
        binding: TabPagerFragmentBinding,
        savedInstanceState: Bundle?
    ) {
        toolbar.title = "SubsamplingScaleImageView"
        val sampleImages = SampleImage.DOGS + SampleImage.HUGES

        binding.tabPagerPager.adapter = AssemblyFragmentStateAdapter(
            this,
            listOf(SubsamplingViewFragment.ItemFactory()),
            sampleImages.map { it.uri }
        )

        TabLayoutMediator(binding.tabPagerTabLayout, binding.tabPagerPager) { tab, position ->
            tab.text = sampleImages[position].name
        }.attach()
    }
}