package com.aimong.android.core.local

import androidx.room.Database
import androidx.room.RoomDatabase

// TODO: @Database — Room DB 정의
//       entities: [OfflineQueueEntity, ...]
@Database(entities = [], version = 1)
abstract class AimongDatabase : RoomDatabase()
