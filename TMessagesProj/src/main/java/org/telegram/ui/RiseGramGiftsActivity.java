package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class RiseGramGiftsActivity extends BaseFragment {
    private static final String PREFS = "risegram_api_session";
    private static final String TOKEN_KEY = "session_token";
    private static final int BLUE = 0xFF229ED9;
    private static final int CARD = 0xFF1C2935;

    private String sessionToken;
    private LinearLayout content;
    private LinearLayout dropsContainer;
    private LinearLayout inventoryContainer;
    private TextView statusView;
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
        simpleActionBar.setTitle("NFT-подарки");
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        sessionToken = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TOKEN_KEY, null);
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        content.setPadding(pad, pad, pad, AndroidUtilities.dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        fragmentView = scroll;

        content.addView(label(context, "Коллекционные подарки RiseGram", 22, Color.WHITE, true), lp());
        content.addView(label(context,
                "Ограниченные NFT-дропы. Покупка списывает RiseStars с общего баланса аккаунта, а подарок отправляется в связанный Telegram.",
                14, 0xFFB8C4CF, false), top(lp(), 6));

        statusView = label(context, "", 13, 0xFF9BAEBC, false);
        content.addView(statusView, top(lp(), 12));

        Button refresh = button(context, "Обновить каталог");
        content.addView(refresh, top(lp(), 10));
        refresh.setOnClickListener(v -> loadGifts());

        content.addView(label(context, "ДОСТУПНЫЕ ДРОПЫ", 12, 0xFF9BAEBC, true), top(lp(), 20));
        dropsContainer = new LinearLayout(context);
        dropsContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(dropsContainer, top(lp(), 8));

        content.addView(label(context, "МОЯ КОЛЛЕКЦИЯ", 12, 0xFF9BAEBC, true), top(lp(), 22));
        inventoryContainer = new LinearLayout(context);
        inventoryContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(inventoryContainer, top(lp(), 8));
        if (sessionToken == null || sessionToken.isEmpty()) {
            setStatus("Сначала подключи аккаунт RiseGram в разделе Gram");
            Button back = button(context, "Вернуться к балансу Gram");
            content.addView(back, top(lp(), 10));
            back.setOnClickListener(v -> finishFragment());
        } else {
            loadGifts();
        }
        return fragmentView;
    }

    private static LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static LinearLayout.LayoutParams top(LinearLayout.LayoutParams p, int dp) {
        p.topMargin = AndroidUtilities.dp(dp);
        return p;
    }

    private static TextView label(Context context, String text, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(AndroidUtilities.dp(radius));
        return drawable;
    }

    private static Button button(Context context, String text) {
        Button view = new Button(context);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setAllCaps(false);
        view.setTextSize(14);
        view.setBackground(background(BLUE, 12));
        return view;
    }

    private void setStatus(String text) {
        if (statusView != null) statusView.setText(text);
    }

    private void runApi(String loading, ApiCall call, ApiResult result) {
        if (busy) return;
        busy = true;
        setStatus(loading);
        Utilities.globalQueue.postRunnable(() -> {
            JSONObject response = null;
            Exception failure = null;
            try {
                response = call.run();
            } catch (Exception e) {
                failure = e;
            }
            JSONObject finalResponse = response;
            Exception finalFailure = failure;
            AndroidUtilities.runOnUIThread(() -> {
                busy = false;
                if (finalFailure != null) {
                    if (finalFailure instanceof ApiException && ((ApiException) finalFailure).status == 401) {
                        clearSession();
                    }
                    setStatus(finalFailure.getMessage() == null
                            ? "Ошибка запроса к серверу RiseGram" : finalFailure.getMessage());
                    return;
                }
                result.onResult(finalResponse == null ? new JSONObject() : finalResponse);
            });
        });
    }

    private JSONObject apiRequest(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BuildVars.RISEGRAM_API_URL + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + sessionToken);
            }
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
            JSONObject json = bytes.length == 0
                    ? new JSONObject() : new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (status < 200 || status >= 300 || !json.optBoolean("ok", true)) {
                throw new ApiException(status, json.optString("message",
                        json.optString("error", "HTTP " + status)));
            }
            return json;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void clearSession() {
        sessionToken = null;
        if (getParentActivity() != null) {
            getParentActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().remove(TOKEN_KEY).apply();
        }
    }

    private void loadGifts() {
        if (sessionToken == null || sessionToken.isEmpty()) {
            setStatus("Сначала подключи аккаунт RiseGram в разделе Gram");
            return;
        }
        runApi("Загружаем NFT-дропы…", () -> apiRequest("GET", "/api/gifts", null), response -> {
            renderDrops(response);
            setStatus("Каталог и баланс обновлены");
        });
    }

    private void renderDrops(JSONObject response) {
        if (dropsContainer == null || getParentActivity() == null) return;
        dropsContainer.removeAllViews();
        JSONArray drops = response.optJSONArray("drops");
        int botStars = response.isNull("bot_stars_balance") ? -1 : response.optInt("bot_stars_balance", -1);
        if (drops == null || drops.length() == 0) {
            dropsContainer.addView(label(getParentActivity(),
                    "Сейчас активных дропов нет. Загляни позже.", 14, 0xFFB8C4CF, false), lp());
        } else {
            for (int i = 0; i < drops.length(); i++) {
                JSONObject drop = drops.optJSONObject(i);
                if (drop != null) addDropCard(drop, botStars);
            }
        }

        inventoryContainer.removeAllViews();
        JSONArray inventory = response.optJSONArray("inventory");
        if (inventory != null && inventory.length() > 0) {
            for (int i = 0; i < Math.min(inventory.length(), 20); i++) {
                JSONObject item = inventory.optJSONObject(i);
                if (item == null) continue;
                String state = "pending".equals(item.optString("status")) ? " · в обработке" : "";
                String itemText = item.optString("emoji", "🎁") + " " + item.optString("gift_name", "NFT")
                        + " · №" + item.optInt("serial_no") + state;
                TextView row = label(getParentActivity(), itemText, 14, Color.WHITE, false);
                row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8),
                        AndroidUtilities.dp(12), AndroidUtilities.dp(8));
                inventoryContainer.addView(row, top(lp(), 4));
            }
        } else {
            inventoryContainer.addView(label(getParentActivity(),
                    "Пока нет купленных подарков", 14, 0xFFB8C4CF, false), top(lp(), 4));
        }
    }

    private void addDropCard(JSONObject drop, int botStars) {
        Context context = getParentActivity();
        if (context == null) return;
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12),
                AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        card.setBackground(background(CARD, 14));

        String emoji = drop.optString("emoji", "🎁");
        String name = drop.optString("gift_name", "Коллекционный подарок");
        card.addView(label(context, emoji + "  " + name, 17, Color.WHITE, true), lp());

        int stock = drop.optInt("stock_remaining", 0);
        int total = drop.optInt("stock_total", 0);
        int risePrice = (int) Math.round(drop.optDouble("price_risestars", 0));
        int required = drop.optInt("telegram_stars_required",
                drop.optInt("telegram_stars", 0) + drop.optInt("upgrade_stars", 0));
        card.addView(label(context,
                "Цена: " + String.format(Locale.US, "%,d", risePrice) + " RiseStars"
                        + "   ·   Осталось: " + stock + "/" + total,
                14, 0xFFE5B44A, true), top(lp(), 7));

        int canDeliverValue = drop.isNull("can_deliver") ? -1 : (drop.optBoolean("can_deliver") ? 1 : 0);
        String deliveryText;
        if (botStars < 0 || canDeliverValue < 0) {
            deliveryText = "Резерв Telegram Stars проверяется при покупке";
        } else {
            deliveryText = "Резерв бота: " + botStars + " / " + required + " Telegram Stars";
        }
        card.addView(label(context, deliveryText, 12, 0xFF9BAEBC, false), top(lp(), 5));

        Button buy = button(context, "Купить NFT-подарок");
        boolean soldOut = stock <= 0;
        boolean noBotReserve = botStars >= 0 && botStars < required;
        buy.setEnabled(!soldOut && !noBotReserve && canDeliverValue != 0);
        buy.setAlpha(buy.isEnabled() ? 1f : 0.5f);
        if (soldOut) buy.setText("Тираж закончился");
        else if (noBotReserve || canDeliverValue == 0) buy.setText("Временно недоступно");
        card.addView(buy, top(lp(), 8));
        buy.setOnClickListener(v -> confirmPurchase(drop.optInt("id"), name, risePrice));
        dropsContainer.addView(card, top(lp(), 8));
    }

    private void confirmPurchase(int dropId, String name, int price) {
        if (dropId <= 0 || sessionToken == null || sessionToken.isEmpty()) return;
        new AlertDialog.Builder(getParentActivity())
                .setTitle("Купить NFT-подарок?")
                .setMessage(name + "\nЦена: " + String.format(Locale.US, "%,d", price)
                        + " RiseStars\nПодарок будет отправлен в связанный Telegram-аккаунт.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Купить", (dialog, which) -> buyGift(dropId))
                .show();
    }

    private void buyGift(int dropId) {
        runApi("Резервируем тираж и отправляем подарок…",
                () -> apiRequest("POST", "/api/gifts/" + dropId + "/buy", null),
                response -> {
                    JSONObject user = response.optJSONObject("user");
                    if (user != null) {
                        setStatus("Подарок отправлен. Баланс RiseStars: "
                                + String.format(Locale.US, "%,.2f", user.optDouble("risestars", 0)));
                    } else {
                        setStatus("Telegram подтвердил отправку подарка");
                    }
                    loadGifts();
                });
    }
}
