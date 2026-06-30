package com.kangtaeyoung.daynote.core

import java.util.UUID

actual fun randomUuid(): String = UUID.randomUUID().toString()

actual fun nowMillis(): Long = System.currentTimeMillis()
