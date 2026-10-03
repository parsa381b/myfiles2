package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context in default locale`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("My Files", appName)
  }

  @Test
  fun `read string from context in Persian locale`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale("fa"))
    val localizedContext = context.createConfigurationContext(config)
    val appNameFa = localizedContext.getString(R.string.app_name)
    assertEquals("فایل‌های من", appNameFa)
  }
}
