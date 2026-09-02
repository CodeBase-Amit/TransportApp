package com.example.transportapp.core.datastore.session

import com.example.transportapp.core.common.SeedIds

/**
 * The session as DataStore holds it — primitive fields only. `:core:datastore` must not
 * import `:domain:transport` (Spec.md §2), so the public read model ([com.example.transportapp.
 * data.transport.session.UserSession]) lives in `:data:transport` and this snapshot is mapped there.
 */
data class SessionSnapshot(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val companyId: String,
    val companyName: String,
    val branchId: String,
    val branchName: String,
) {
    val isSignedIn: Boolean get() = userId.isNotEmpty()

    companion object {
        /** The mocked offline session (Phase2.md §1, decision 4); ids point at the seeded org. */
        val DEMO = SessionSnapshot(
            userId = "local-demo-user",
            name = "Mahesh Patidar",
            email = "mahesh.patidar@gmail.com",
            role = "OWNER",
            companyId = SeedIds.COMPANY_SHIVSHAKTI,
            companyName = "Shivshakti Roadlines",
            branchId = SeedIds.BRANCH_INDORE,
            branchName = "Indore",
        )

        /** Nobody signed in — S18: release clean installs and post-sign-out land here. */
        val SIGNED_OUT = SessionSnapshot(
            userId = "", name = "", email = "", role = "",
            companyId = "", companyName = "", branchId = "", branchName = "",
        )
    }
}
