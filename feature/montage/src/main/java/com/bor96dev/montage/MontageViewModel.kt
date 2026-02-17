package com.bor96dev.montage

import com.bor96dev.montage.domain.MontageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MontageViewModel @Inject constructor(
    private val repository: MontageRepository
){
}