package name.alexy.test.tinderauto;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import name.alexy.test.tinderauto.phoneservice.FacebookSmsParser;

import static name.alexy.test.tinderauto.AppsAuto.COUNTRY;
import static name.alexy.test.tinderauto.AppsAuto.COUNTRY_CODE;
import static name.alexy.test.tinderauto.AppsAuto.FILE_BIRTHDAYS;
import static name.alexy.test.tinderauto.AppsAuto.FILE_NAMES;
import static name.alexy.test.tinderauto.AppsAuto.FILE_SURNAMES;
import static name.alexy.test.tinderauto.AppsAuto.FIND_TIMEOUT;
import static name.alexy.test.tinderauto.AppsAuto.FIND_TIMEOUT_SHORT;
import static name.alexy.test.tinderauto.AppsAuto.LAUNCH_TIMEOUT;
import static name.alexy.test.tinderauto.AppsAuto.PASSWORD;
import static name.alexy.test.tinderauto.AppsAuto.SMS_NEXT_NUMBER_RETRIES;
import static name.alexy.test.tinderauto.AppsAuto.clickIfExistsByText;
import static name.alexy.test.tinderauto.AppsAuto.pressMultipleTimes;
import static org.junit.Assert.fail;

/**
 * Created by alexeykrichun on 09/10/2017.
 */

public class FacebookAuto {
    static final String FB_PACKAGE = "com.facebook.katana";
    private final UiDevice mDevice;

    public FacebookAuto(UiDevice mDevice) {
        this.mDevice = mDevice;
    }


    String createFacebookAccount() throws Exception {

        String phone = PhoneSmsHelper.getFacebookFreePhoneNumber();
        if (TextUtils.isEmpty(phone)) {
            return null;
        }

        // Launch the app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(FB_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(FB_PACKAGE).depth(0)), LAUNCH_TIMEOUT);

        UiObject createAccountButton = mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/login_create_account_button"));

        if (createAccountButton == null) {
            fail("Can't create FB account");
        }

        createAccountButton.click();

        //next
        mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/finish_button")).click();

        //permissions
        pressMultipleTimes(mDevice, "DENY");

        enterName();

        int screensLeft = 4;
        long phoneTime = new Date().getTime();
        while (screensLeft > 0) {
            String title = mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/header_text")).getText();
            if (TextUtils.equals(title, "Enter Your Mobile Number")) {
                enterPhone(phone);
                screensLeft--;
            } else if (title.contains("Birth") && (title.contains("Date") || title.contains("day"))) {
                enterBirthday();
                screensLeft--;
            } else if (title.contains("Gender")) {
                enterSex();
                screensLeft--;
            } else if (title.contains("Password")) {
                enterPassword();
                screensLeft--;
            }
        }

        //finish signing up
        try {
            mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/finish_without_contacts")).click();
        } catch (UiObjectNotFoundException e) {
            e.getMessage();
        }

        try {
            mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/finish_button")).click();
        } catch (UiObjectNotFoundException e) {
            e.getMessage();
        }

        PhoneSmsHelper.addUsedPhone(phone);

        //wait for sms request
        mDevice.wait(Until.hasObject(By.text("DENY")), FIND_TIMEOUT_SHORT);
        pressMultipleTimes(mDevice, "DENY");

        //save password
        Log.d("FacebookAuto", "waiting for SAVE PASSWORD");
        UiObject savePassword = mDevice.findObject(new UiSelector().textMatches("(?i)SAVE PASSWORD"));

        if (savePassword.waitForExists(FIND_TIMEOUT)){
            savePassword.click();
        } else {
            Log.d("FacebookAuto", "no SAVE PASSWORD");
        }

        try {
            //Next Time, Log In With One Tap
            Log.d("FacebookAuto", "waiting for Next Time, Log In With One Tap");
            mDevice.wait(Until.hasObject(By.text("Next Time, Log In With One Tap")), FIND_TIMEOUT_SHORT);
            mDevice.findObject(new UiSelector().text("OK")).click();
        } catch (UiObjectNotFoundException e) {
            Log.d("FacebookAuto", "no Next Time, Log In With One Tap");
            e.getMessage();
        }

        try {
            //Log In With One Tap
            Log.d("FacebookAuto", "waiting for Next Time, Log In With One Tap");
            mDevice.wait(Until.hasObject(By.text("Log In With One Tap")), FIND_TIMEOUT_SHORT);
            mDevice.findObject(new UiSelector().className(Button.class).textMatches("(OK)|(Continue)")).click();
        } catch (UiObjectNotFoundException e) {
            Log.d("FacebookAuto", "no Log In With One Tap");
            e.getMessage();
        }

        UiObject smsTitle = mDevice.findObject(new UiSelector().text("Enter the code from your SMS"));
        UiObject skip = mDevice.findObject(new UiSelector().textMatches("(?i)SKIP"));

        do {
            Log.d("FacebookAuto", "waiting for sms title");
            if (!smsTitle.waitForExists(500) && skip.exists()) {
                skip.click();
            }
        } while (!smsTitle.exists() && skip.exists());

        //sms


        if (smsTitle.exists()) {
            Log.d("FacebookAuto", "sms title found");

            String code = requestSmsCode(phone, phoneTime);

            if (TextUtils.isEmpty(code)) {
                Log.e("FacebookAuto", "No facebook code received");
                phone = newNumber(phoneTime);

                if (TextUtils.isEmpty(phone)) {
                    return null;
                }
            } else {
                mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/code_input")).setText(code);
                mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/continue_button")).click();
            }
        } else {
            Log.w("FacebookAuto", "No SMS requested");
        }

        pressMultipleTimes(mDevice, "SKIP");

        pressMultipleTimes(mDevice, "DENY");

        return phone;
    }

    @Nullable
    private String requestSmsCode(String phone, long phoneTime) throws IOException, ParseException, UiObjectNotFoundException {
        int resendSmsRetries = AppsAuto.RESEND_SMS_ATTEMPTS;
        String code;

        do {
            code = new FacebookSmsParser().getCode(phone, phoneTime - 20000, AppsAuto.SMS_REPEAT, AppsAuto.SMS_DELAY);

            if (TextUtils.isEmpty(code) && resendSmsRetries > 0) {
                Log.d("FacebookAuto", "Resend sms, attempts left " + resendSmsRetries);
                mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/conf_code_bottom_option_1")).click();
                clickIfExistsByText(mDevice, "GET SMS");
            }
        } while (resendSmsRetries-- > 0);
        return code;
    }

     private String newNumber(long phoneTime) throws Exception {
        String phone;
        String code;

        int retries = 1;

        do {
            mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/conf_code_bottom_option_2")).click();
            phone = PhoneSmsHelper.getFacebookFreePhoneNumber();

            if (TextUtils.isEmpty(phone)) {
                return null;
            }

            Log.w("FacebookAuto", "New phone " + phone + " retry " + retries);
            PhoneSmsHelper.addUsedPhone(phone);
            enterPhone(phone);

            code = requestSmsCode(phone, phoneTime);

            if (TextUtils.isEmpty(code)) {
                Log.e("FacebookAuto", "No facebook code received");
            }

        } while (TextUtils.isEmpty(code) && retries++ < SMS_NEXT_NUMBER_RETRIES);

        if (TextUtils.isEmpty(code)) {
            fail("No SMS received");
        }

        mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/code_input")).setText(code);
        mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/continue_button")).click();

        return phone;
    }

    private void enterPassword() throws UiObjectNotFoundException {
        mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/password_input")).setText(PASSWORD);
        mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/finish_button")).click();
    }

    private void enterPhone(String phone) throws UiObjectNotFoundException {
        //country selector
        mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/country_name_selector")).click();
        mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/country_search_edit_text")).setText(COUNTRY);
        mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/country_name").textStartsWith(COUNTRY)).click();
        //number
        String localPhone = phone;
        if (phone.startsWith(COUNTRY_CODE)) {
            localPhone = phone.substring(COUNTRY_CODE.length());
        }

        mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/phone_input")).setText(localPhone);
        UiObject button = mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/finish_button"));

        if (!button.exists()) {
            button = mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/continue_button"));
        }

        button.click();
    }

    private void enterSex() throws UiObjectNotFoundException {
        mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/gender_female")).click();
        mDevice.findObject(new UiSelector().resourceId("com.facebook.katana:id/finish_button")).click();
    }

    private void enterBirthday() throws Exception {
        String birthday = Utils.getRandomLine(FILE_BIRTHDAYS);
        String[] parts = birthday.split("-");  //format: 01-Jan-2000

        List<UiObject2> inputs = mDevice.findObjects(By.clazz(EditText.class).res("android:id/numberpicker_input"));

        if (inputs.size() == 3) {
            //month
            inputs.get(0).click();
            inputs.get(0).setText(parts[1]);
            //day
            inputs.get(1).click();
            inputs.get(1).setText(parts[0]);
            //year
            inputs.get(2).click();
            inputs.get(2).setText(parts[2]);
            inputs.get(0).click();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/finish_button")).click();

        // is today your birthday / is your age ...
        pressMultipleTimes(mDevice, "YES");
    }

    private void enterName() throws Exception {
        String firstName = Utils.getRandomLine(FILE_NAMES);
        String lastName = Utils.getRandomLine(FILE_SURNAMES);

        mDevice.findObject(new UiSelector().className(EditText.class).resourceId("com.facebook.katana:id/first_name_input")).setText(firstName);
        mDevice.findObject(new UiSelector().className(EditText.class).resourceId("com.facebook.katana:id/last_name_input")).setText(lastName);
        mDevice.findObject(new UiSelector().className(Button.class).resourceId("com.facebook.katana:id/finish_button")).click();
    }

}
