/*
 *
 *  * Copyright 2025 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.example.cahier.ui.viewmodels

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param: ApplicationContext private val context: Context
) : ViewModel() {

    private val _isRoleAvailable = MutableStateFlow(false)
    val isRoleAvailable: StateFlow<Boolean> = _isRoleAvailable.asStateFlow()

    private val _isRoleHeld = MutableStateFlow(false)
    val isRoleHeld: StateFlow<Boolean> = _isRoleHeld.asStateFlow()

    private val roleManager: RoleManager? by lazy {
        context.getSystemService(RoleManager::class.java)
    }

    init {
        checkNotesRoleStatus()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun checkNotesRoleStatus() {
        if (roleManager != null) {
            viewModelScope.launch {
                try {
                    _isRoleAvailable.value = roleManager!!.isRoleAvailable(
                        RoleManager.ROLE_NOTES
                    )
                    _isRoleHeld.value = roleManager!!.isRoleHeld(RoleManager.ROLE_NOTES)
                    Log.d(
                        TAG,
                        "Role Status Check: " +
                                "Available=${_isRoleAvailable.value}, Held=${_isRoleHeld.value}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking role status", e)
                    _isRoleAvailable.value = false
                    _isRoleHeld.value = false
                }
            }
        } else {
            _isRoleAvailable.value = false
            _isRoleHeld.value = false
            Log.d(
                TAG,
                "Role Manager not available on this API level or failed to get service."
            )
        }
    }

    fun requestNotesRole(launcher: ActivityResultLauncher<Intent>) {
        roleManager?.let { manager ->
            if (manager.isRoleAvailable(RoleManager.ROLE_NOTES)
                && !manager.isRoleHeld(RoleManager.ROLE_NOTES)
            ) {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                intent.extras?.let { bundle ->
                    for (key in bundle.keySet()) {
                        Log.w(TAG, "Intent extra: $key = ${bundle.getString(key)}")
                    }
                }
                launcher.launch(intent)
            } else {
                Log.w(TAG, "Role not available or already held, cannot request.")
            }
        } ?: Log.e(TAG, "RoleManager not available for requesting role.")
    }

    fun updateRoleHeldStatus() {
        if (roleManager != null) {
            viewModelScope.launch {
                try {
                    val currentlyHeld = roleManager!!.isRoleHeld(RoleManager.ROLE_NOTES)
                    _isRoleHeld.value = currentlyHeld
                    Log.d(TAG, "Role Status Updated: Held=$currentlyHeld")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating role held status", e)
                    _isRoleHeld.value = false
                }
            }
        } else {
            _isRoleHeld.value = false
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}