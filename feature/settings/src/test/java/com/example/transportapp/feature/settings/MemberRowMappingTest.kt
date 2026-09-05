package com.example.transportapp.feature.settings

import com.example.transportapp.data.transport.account.MemberRowData
import com.example.transportapp.feature.settings.screen.toRow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S27 regression: toRow() dropped MemberRowData.status, so `invited` was always false —
 * invited rows never rendered, the Invited tab stayed at 0, and the S21 cancel-invite
 * flow (repo work complete) was unreachable from the UI.
 */
class MemberRowMappingTest {

    @Test
    fun `INVITED status maps to an invited row`() {
        val row = MemberRowData("Asha", "asha@x.in", "CLERK", "branch-1", "INVITED").toRow("me@x.in")
        assertTrue("INVITED rows must be marked invited so the invited tab and cancel/resume UI render", row.invited)
    }

    @Test
    fun `ACTIVE status maps to an active row`() {
        val row = MemberRowData("Self", "me@x.in", "OWNER", "branch-1", "ACTIVE").toRow("me@x.in")
        assertFalse(row.invited)
        assertTrue("self rows stay marked", row.isSelf)
    }
}
