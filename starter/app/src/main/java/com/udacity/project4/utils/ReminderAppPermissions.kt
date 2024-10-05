package com.udacity.project4.utils

import java.security.Permission

data class ReminderAppPermissions (
    var permission: String,
    var rationaleTitle: String,
    var rationaleMessage: String,
    var toastMessage: String
)