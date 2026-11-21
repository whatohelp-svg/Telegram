package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class RiseGramEarningsActivity extends BaseFragment {
    private static final String PREFS = "risegram_api_session";
    private static final String TOKEN_KEY = "session_token";
    private static final int BLUE = 0xFF229ED9;

    private EditText phoneInput, codeInput, amountInput;
    private TextView statusView, gramBalanceView, gramEquivalentView, starsBalanceView;
    private LinearLayout loginPanel;
    private Button connectButton;
    private String sessionToken;
    private boolean busy;

    private interface ApiCall { JSONObject run() throws Exception; }
    private interface ApiResult { void onResult(JSONObject result); }

    private static final class ApiException extends IOException {
        final int status;
        ApiException(int status, String message) { super(message); this.status = status; }
    }

    @Override
    protected boolean useSimpleActionBar() { return true; }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setBackButtonDrawable(new BackDrawable(false));
        simpleActionBar.setTitle("Gram");
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        sessionToken = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TOKEN_KEY, null);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        content.setPadding(pad, pad, pad, pad);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        fragmentView = scroll;

        content.addView(label(context, "Gram", 28, Color.WHITE, true), lp());
        content.addView(label(context, "Криптовалюта RiseGram", 14, 0xFFB8C4CF, false), top(lp(), 4));
        content.addView(label(context, "Фиксированный курс: 1 Gram = 100 RiseStars", 14, 0xFFE5B44A, true), top(lp(), 10));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF1C2935);
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        card.setBackground(cardBg);
        content.addView(card, top(lp(), 18));

        card.addView(label(context, "КОЛИЧЕСТВО GRAM", 12, 0xFF9BAEBC, true), lp());
        gramBalanceView = label(context, "— Gram", 30, Color.WHITE, true);
        card.addView(gramBalanceView, top(lp(), 4));
        card.addView(label(context, "ЭКВИВАЛЕНТ ПО КУРСУ", 12, 0xFF9BAEBC, true), top(lp(), 15));
        gramEquivalentView = label(context, "— RiseStars", 20, 0xFFE5B44A, true);
        card.addView(gramEquivalentView, top(lp(), 4));
        card.addView(label(context, "ОТДЕЛЬНЫЙ БАЛАНС RISESTARS", 12, 0xFF9BAEBC, true), top(lp(), 15));
        starsBalanceView = label(context, "— RiseStars", 18, Color.WHITE, true);
        card.addView(starsBalanceView, top(lp(), 4));

        content.addView(label(context, "Конвертация валюты", 17, Color.WHITE, true), top(lp(), 22));
        amountInput = input(context, "Сумма (до 2 знаков после запятой)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        content.addView(amountInput, top(lp(), 8));
        Button gramToStars = button(context, "Обменять Gram → RiseStars");
        content.addView(gramToStars, top(lp(), 9));
        gramToStars.setOnClickListener(v -> exchange("gram_to_stars"));
        Button starsToGram = button(context, "Обменять RiseStars → Gram");
        content.addView(starsToGram, top(lp(), 8));
        starsToGram.setOnClickListener(v -> exchange("stars_to_gram"));

        loginPanel = new LinearLayout(context);
        loginPanel.setOrientation(LinearLayout.VERTICAL);
        loginPanel.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable loginBg = new GradientDrawable();
        loginBg.setColor(0xFF1C2935);
        loginBg.setCornerRadius(AndroidUtilities.dp(14));
        loginPanel.setBackground(loginBg);
        content.addView(loginPanel, top(lp(), 22));
        loginPanel.addView(label(context, "Привязка аккаунта RiseGram", 16, Color.WHITE, true), lp());
        loginPanel.addView(label(context, "Подтверди аккаунт один раз, чтобы загрузить настоящие количества Gram и RiseStars. Это не меняет вход в Telegram.", 13, 0xFF9BAEBC, false), top(lp(), 5));
        phoneInput = input(context, "Номер аккаунта RiseGram", InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(UserConfig.getInstance(currentAccount).getClientPhone());
        loginPanel.addView(phoneInput, top(lp(), 9));
        codeInput = input(context, "Код из бота RiseGram", InputType.TYPE_CLASS_NUMBER);
        loginPanel.addView(codeInput, top(lp(), 8));
        Button request = button(context, "Получить код");
        loginPanel.addView(request, top(lp(), 9));
        request.setOnClickListener(v -> requestCode());
        Button verify = button(context, "Подтвердить аккаунт");
        loginPanel.addView(verify, top(lp(), 8));
        verify.setOnClickListener(v -> verifyCode());

        connectButton = button(context, sessionToken == null || sessionToken.isEmpty() ? "Привязать аккаунт" : "Сменить аккаунт");
        content.addView(connectButton, top(lp(), 14));
        connectButton.setOnClickListener(v -> {
            if (loginPanel.getVisibility() == View.VISIBLE) {
                loginPanel.setVisibility(View.GONE);
                connectButton.setText(sessionToken == null || sessionToken.isEmpty() ? "Привязать аккаунт" : "Сменить аккаунт");
            } else {
                loginPanel.setVisibility(View.VISIBLE);
                connectButton.setText("Скрыть");
            }
        });
        loginPanel.setVisibility(sessionToken == null || sessionToken.isEmpty() ? View.VISIBLE : View.GONE);

        statusView = label(context, "", 13, 0xFF9BAEBC, false);
        content.addView(statusView, top(lp(), 14));

        if (sessionToken != null && !sessionToken.isEmpty()) loadBalances();
        else setStatus("Привяжи аккаунт RiseGram для загрузки реальных количеств валюты");
        return fragmentView;
    }

    private static LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }
    private static LinearLayout.LayoutParams top(LinearLayout.LayoutParams p, int dp) { p.topMargin = AndroidUtilities.dp(dp); return p; }

    private static TextView label(Context c, String text, int size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private static EditText input(Context c, String hint, int type) {
        EditText v = new EditText(c);
        v.setSingleLine(true);
        v.setTextSize(16);
        v.setHint(hint);
        v.setInputType(type);
        v.setTextColor(Color.WHITE);
        v.setHintTextColor(0xFF7F909D);
        v.setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(11), AndroidUtilities.dp(13), AndroidUtilities.dp(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C2935);
        bg.setCornerRadius(AndroidUtilities.dp(12));
        v.setBackground(bg);
        return v;
    }

    private static Button button(Context c, String text) {
        Button v = new Button(c);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setAllCaps(false);
        v.setTextSize(15);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLUE);
        bg.setCornerRadius(AndroidUtilities.dp(12));
        v.setBackground(bg);
        return v;
    }

    private void setStatus(String value) { if (statusView != null) statusView.setText(value); }

    private void runApi(String loading, ApiCall call, ApiResult result) {
        if (busy) return;
        busy = true;
        setStatus(loading);
        Utilities.globalQueue.postRunnable(() -> {
            JSONObject response = null;
            Exception failure = null;
            try { response = call.run(); } catch (Exception e) { failure = e; }
            JSONObject finalResponse = response;
            Exception finalFailure = failure;
            AndroidUtilities.runOnUIThread(() -> {
                busy = false;
                if (finalFailure != null) {
                    if (finalFailure instanceof ApiException && ((ApiException) finalFailure).status == 401) clearSession();
                    setStatus(finalFailure.getMessage() == null ? "Ошибка запроса к серверу RiseGram" : finalFailure.getMessage());
                    return;
                }
                result.onResult(finalResponse == null ? new JSONObject() : finalResponse);
            });
        });
    }

    private JSONObject apiRequest(String method, String path, String token, JSONObject body) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BuildVars.RISEGRAM_API_URL + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            byte[] bytes;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (stream != null) {
                    try (InputStream input = stream) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                    }
                }
                bytes = output.toByteArray();
            }
            JSONObject json = bytes.length == 0 ? new JSONObject() : new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (status < 200 || status >= 300 || !json.optBoolean("ok", true)) {
                throw new ApiException(status, json.optString("message", json.optString("error", "HTTP " + status)));
            }
            return json;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void requestCode() {
        String phone = phoneInput == null ? "" : phoneInput.getText().toString().trim();
        if (phone.isEmpty()) { setStatus("Введи номер аккаунта RiseGram"); return; }
        runApi("Отправляем код…", () -> {
            JSONObject body = new JSONObject();
            body.put("phone", phone);
            return apiRequest("POST", "/api/auth/request", null, body);
        }, response -> setStatus("Запрос принят. Проверь чат бота RiseGram — код действует 5 минут"));
    }

    private void verifyCode() {
        String phone = phoneInput == null ? "" : phoneInput.getText().toString().trim();
        String code = codeInput == null ? "" : codeInput.getText().toString().trim();
        if (phone.isEmpty() || code.isEmpty()) { setStatus("Введи номер и код из бота RiseGram"); return; }
        runApi("Проверяем код…", () -> {
            JSONObject body = new JSONObject();
            body.put("phone", phone);
            body.put("code", code);
            return apiRequest("POST", "/api/auth/verify", null, body);
        }, response -> {
            sessionToken = response.optString("token", "");
            if (sessionToken.isEmpty()) { setStatus("Сервер не выдал сессию"); return; }
            getParentActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(TOKEN_KEY, sessionToken).apply();
            loginPanel.setVisibility(View.GONE);
            connectButton.setText("Сменить аккаунт");
            setStatus("Аккаунт RiseGram подключён. Загружаем баланс…");
            loadBalances();
        });
    }

    private void clearSession() {
        sessionToken = null;
        if (getParentActivity() != null) getParentActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(TOKEN_KEY).apply();
        if (loginPanel != null) loginPanel.setVisibility(View.VISIBLE);
        if (connectButton != null) connectButton.setText("Привязать аккаунт");
        setStatus("Сессия RiseGram истекла. Подключи аккаунт заново");
    }

    private void loadBalances() {
        if (sessionToken == null || sessionToken.isEmpty()) return;
        final String token = sessionToken;
        runApi("Обновляем баланс…", () -> apiRequest("GET", "/api/me", token, null), response -> {
            JSONObject user = response.optJSONObject("user");
            if (user == null) { setStatus("Сервер не вернул профиль RiseGram"); return; }
            updateBalances(user.optDouble("gram_balance", 0), user.optDouble("risestars", 0));
            setStatus("Баланс загружен с сервера RiseGram");
        });
    }

    private void exchange(String direction) {
        if (sessionToken == null || sessionToken.isEmpty()) { setStatus("Сначала подключи аккаунт RiseGram"); return; }
        String value = amountInput == null ? "" : amountInput.getText().toString().trim().replace(',', '.');
        final BigDecimal amount;
        try {
            amount = new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
        } catch (Exception e) {
            setStatus("Введи положительную сумму с точностью до 2 знаков после запятой");
            return;
        }
        if (amount.signum() <= 0) { setStatus("Сумма должна быть больше нуля"); return; }
        final String token = sessionToken;
        runApi("Проводим обмен…", () -> {
            JSONObject body = new JSONObject();
            body.put("amount", amount.doubleValue());
            body.put("direction", direction);
            return apiRequest("POST", "/api/exchange", token, body);
        }, response -> {
            JSONObject user = response.optJSONObject("user");
            JSONObject balances = response.optJSONObject("balances");
            if (user != null) updateBalances(user.optDouble("gram_balance", 0), user.optDouble("risestars", 0));
            else if (balances != null) updateBalances(balances.optDouble("gram_balance", 0), balances.optDouble("risestars", 0));
            else loadBalances();
            setStatus("Обмен завершён. Балансы обновлены");
        });
    }

    private void updateBalances(double gram, double risestars) {
        if (gramBalanceView != null) gramBalanceView.setText(String.format(Locale.US, "%.2f Gram", gram));
        if (gramEquivalentView != null) gramEquivalentView.setText(String.format(Locale.US, "%.2f RiseStars", gram * 100.0d));
        if (starsBalanceView != null) starsBalanceView.setText(String.format(Locale.US, "%.2f RiseStars", risestars));
    }
}
