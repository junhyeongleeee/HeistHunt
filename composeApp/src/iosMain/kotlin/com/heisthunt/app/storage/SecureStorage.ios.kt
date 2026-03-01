package com.heisthunt.app.storage

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

@OptIn(ExperimentalForeignApi::class)
actual class SecureStorage {
    private val serviceName = "com.heisthunt.app"

    init {
        migrateFromUserDefaultsIfNeeded()
    }

    actual fun saveString(key: String, value: String) {
        remove(key)
        val valueData = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: run {
            println("❌ [Keychain] Failed to encode value for key=$key")
            return
        }
        val status = SecItemAdd(buildQuery(key = key, valueData = valueData), null)
        if (status != errSecSuccess) {
            println("❌ [Keychain] Save failed: key=$key status=$status")
        }
    }

    actual fun getString(key: String): String? = memScoped {
        val resultRef = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(buildQuery(key = key, returnData = true), resultRef.ptr)
        if (status == errSecSuccess) {
            val data = CFBridgingRelease(resultRef.value) as? NSData
            data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding) as? String }
        } else null
    }

    actual fun remove(key: String) {
        SecItemDelete(buildQuery(key = key))
    }

    actual fun clear() {
        println("🔐 [Keychain] Clearing all stored tokens")
        listOf(
            StorageKeys.ACCESS_TOKEN,
            StorageKeys.REFRESH_TOKEN,
            StorageKeys.USER_ID,
            StorageKeys.USER_EMAIL,
            StorageKeys.USER_NICKNAME
        ).forEach { remove(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildQuery(
        key: String,
        returnData: Boolean = false,
        valueData: NSData? = null
    ): CFDictionaryRef {
        // Cast to MutableMap to use subscript operators, avoiding the NSCopying
        // protocol requirement that Kotlin/Native doesn't expose via wildcard import
        val dict = NSMutableDictionary() as MutableMap<Any?, Any?>
        dict[kSecClass] = kSecClassGenericPassword
        dict[kSecAttrService] = serviceName as NSString
        dict[kSecAttrAccount] = key as NSString
        if (returnData) {
            dict[kSecReturnData] = kCFBooleanTrue
            dict[kSecMatchLimit] = kSecMatchLimitOne
        }
        valueData?.let { dict[kSecValueData] = it }
        return dict as CFDictionaryRef
    }

    private fun migrateFromUserDefaultsIfNeeded() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        val migrationFlagKey = "heisthunt_keychain_migrated_v1"

        if (userDefaults.boolForKey(migrationFlagKey)) {
            handleFreshInstall()
            return
        }

        val keys = listOf(
            StorageKeys.ACCESS_TOKEN,
            StorageKeys.REFRESH_TOKEN,
            StorageKeys.USER_ID,
            StorageKeys.USER_EMAIL,
            StorageKeys.USER_NICKNAME
        )

        var migrated = false
        keys.forEach { k ->
            val value = userDefaults.stringForKey(k)
            if (value != null) {
                saveString(k, value)
                userDefaults.removeObjectForKey(k)
                migrated = true
            }
        }

        if (migrated) {
            println("🔐 [Keychain] Migrated tokens from NSUserDefaults to Keychain")
        }

        userDefaults.setBool(true, forKey = migrationFlagKey)
        userDefaults.synchronize()
    }

    private fun handleFreshInstall() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        val installedFlagKey = "heisthunt_app_has_installed"

        if (!userDefaults.boolForKey(installedFlagKey)) {
            println("🔐 [Keychain] Fresh install detected, clearing stale Keychain data")
            clear()
            userDefaults.setBool(true, forKey = installedFlagKey)
            userDefaults.synchronize()
        }
    }
}
