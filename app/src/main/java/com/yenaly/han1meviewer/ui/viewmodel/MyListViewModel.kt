package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import com.yenaly.han1meviewer.base.BaseViewModel
import com.yenaly.han1meviewer.ui.viewmodel.mylist.FavSubViewModel
import com.yenaly.han1meviewer.ui.viewmodel.mylist.WatchLaterSubViewModel

class MyListViewModel(application: Application) : BaseViewModel(application) {

    val watchLater by sub<WatchLaterSubViewModel>()
    val fav by sub<FavSubViewModel>()
}
