package org.simple.clinic.deeplink

import org.simple.clinic.user.User

sealed class DeepLinkEvent

data class UserFetched(val user: User?) : DeepLinkEvent()
