package com.example.transportapp.feature.settings

import com.example.transportapp.feature.settings.screen.CompanyProfileViewModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S27 regression: CompanyProfileScreen instantiates its VM with `hiltViewModel()`, which
 * requires the class to be registered with Hilt. A VM without @HiltViewModel compiles fine
 * and then crashes at runtime the moment the user opens Company Profile
 * ("Cannot create an instance for CompanyProfileViewModel"). This test keeps the
 * annotation from being lost again.
 */
class CompanyProfileHiltBindingTest {

    @Test
    fun `company profile view model bytecode carries the HiltViewModel annotation`() {
        // @HiltViewModel is CLASS-retention: runtime reflection cannot see it, but Hilt's
        // processor reads the class file. Assert the descriptor is in the bytecode itself.
        val bytes = CompanyProfileViewModel::class.java
            .getResourceAsStream("CompanyProfileViewModel.class")
            ?.readBytes()
            ?: error("CompanyProfileViewModel.class not on the test classpath")
        val bytecode = String(bytes, Charsets.ISO_8859_1)
        assertTrue(
            "CompanyProfileViewModel must be @HiltViewModel: CompanyProfileScreen uses hiltViewModel(), " +
                "so a missing annotation is a runtime crash on navigation",
            bytecode.contains("Ldagger/hilt/android/lifecycle/HiltViewModel;"),
        )
    }

    @Test
    fun `company profile view model exposes hilt inject constructor`() {
        val ctor = CompanyProfileViewModel::class.java.constructors.single()
        assertNotNull("VM must keep its @Inject constructor", ctor)
        assertTrue(ctor.isAnnotationPresent(javax.inject.Inject::class.java))
    }
}
