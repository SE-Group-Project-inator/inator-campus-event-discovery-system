package com.example.campuseventdiscoverysystem;

import android.content.Intent;
import android.widget.Button;

import com.example.campuseventdiscoverysystem.activities.LoginActivity;
import com.example.campuseventdiscoverysystem.activities.RoleSelectActivity;
import com.example.campuseventdiscoverysystem.activities.SignupActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Smoke tests for RoleSelectActivity (the launcher screen).
 *
 * Verifies the activity inflates without crashing, all three role buttons
 * are present, and clicking each one fires the right Intent with the right
 * "role" extra.
 */
@RunWith(RobolectricTestRunner.class)
public class RoleSelectActivityTest {

    @Test
    public void activity_launchesWithoutCrashing() {
        ActivityController<RoleSelectActivity> controller =
                Robolectric.buildActivity(RoleSelectActivity.class).setup();
        assertNotNull(controller.get());
    }

    @Test
    public void allThreeRoleButtons_arePresent() {
        RoleSelectActivity act =
                Robolectric.buildActivity(RoleSelectActivity.class).setup().get();

        assertNotNull("Student button missing",
                act.findViewById(R.id.btnStudent));
        assertNotNull("Event Manager button missing",
                act.findViewById(R.id.btnEventManager));
        assertNotNull("Admin button missing",
                act.findViewById(R.id.btnAdmin));
    }

    @Test
    public void clickStudentButton_routesToLoginAsStudent() {
        RoleSelectActivity act =
                Robolectric.buildActivity(RoleSelectActivity.class).setup().get();

        ((Button) act.findViewById(R.id.btnStudent)).performClick();

        Intent next = Shadows.shadowOf(act).getNextStartedActivity();
        assertNotNull("No Intent fired", next);
        assertEquals(LoginActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("student", next.getStringExtra("role"));
    }

    @Test
    public void clickEventManagerButton_routesToLoginAsEventManager() {
        RoleSelectActivity act =
                Robolectric.buildActivity(RoleSelectActivity.class).setup().get();

        ((Button) act.findViewById(R.id.btnEventManager)).performClick();

        Intent next = Shadows.shadowOf(act).getNextStartedActivity();
        assertNotNull(next);
        assertEquals(LoginActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("event_manager", next.getStringExtra("role"));
    }

    @Test
    public void clickAdminButton_routesToLoginAsAdmin() {
        RoleSelectActivity act =
                Robolectric.buildActivity(RoleSelectActivity.class).setup().get();

        ((Button) act.findViewById(R.id.btnAdmin)).performClick();

        Intent next = Shadows.shadowOf(act).getNextStartedActivity();
        assertNotNull(next);
        assertEquals(LoginActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("admin", next.getStringExtra("role"));
    }

    @Test
    public void goToSignUp_helperFiresSignupIntentWithRole() {
        // Use an Activity context so startActivity() doesn't require NEW_TASK flag.
        RoleSelectActivity host =
                Robolectric.buildActivity(RoleSelectActivity.class).setup().get();
        // Flush the launcher's own onCreate intents (none expected here, but safe).
        Shadows.shadowOf(host).getNextStartedActivity();

        RoleSelectActivity.goToSignUp(host, "student");

        Intent next = Shadows.shadowOf(host).getNextStartedActivity();
        assertNotNull(next);
        assertEquals(SignupActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("student", next.getStringExtra("role"));
    }
}
