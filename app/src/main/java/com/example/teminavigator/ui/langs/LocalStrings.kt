package com.example.teminavigator.ui.langs

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Provides the [AppStrings] of the currently selected language to the whole
 * Compose UI tree.
 *
 * Provide it once near the root of the UI with the strings of the chosen
 * [AppLanguage], for example `CompositionLocalProvider(LocalStrings provides language.strings) { ... }`.
 * Composables then read their texts with `LocalStrings.current`, so no strings
 * need to be passed down as parameters. Switching the language swaps the
 * provided value and the UI updates accordingly.
 *
 * It is a static composition local, meaning a change of value recomposes the
 * entire subtree below the provider. That is fine here, as language changes are rare.
 *
 * There is no default value on purpose: reading it without a provider throws
 * an error, which makes a missing setup visible immediately.
 *
 * @throws IllegalStateException when read outside a provider.
 */
val LocalStrings = staticCompositionLocalOf<AppStrings>{
    error("No AppStrings found")
}