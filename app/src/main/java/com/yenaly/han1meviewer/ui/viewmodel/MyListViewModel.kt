package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import com.yenaly.han1meviewer.base.BaseViewModel
import com.yenaly.han1meviewer.ui.viewmodel.mylist.FavSubViewModel
import com.yenaly.han1meviewer.ui.viewmodel.mylist.WatchLaterSubViewModel

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/04 004 22:46
 */
class MyListViewModel(application: Application) : BaseViewModel(application) {

    val watchLater by sub<WatchLaterSubViewModel>()
    val fav by sub<FavSubViewModel>()
}
