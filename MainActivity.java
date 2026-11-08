package com.matematicasdivertidas.misionnumeros;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Base64;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Misión Números: el juego (HTML) corre dentro de un WebView.
 * Puentes hacia Android:
 *  - AndroidTTS: voz del teléfono (el WebView de Android no trae speechSynthesis).
 *  - AndroidShare: envía la imagen del avance directamente a WhatsApp.
 */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final String HOST = "appassets.androidplatform.net";
    private static final String START = "https://" + HOST + "/assets/www/index.html";

    private WebView web;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF12293B);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Android 15 dibuja de borde a borde: se deja espacio para barras del sistema y muescas.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                l = i.left; t = i.top; r = i.right; b = i.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft(); t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight(); b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(l, t, r, b);
            return insets;
        });

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);              // guarda el progreso (localStorage)
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setSupportMultipleWindows(false);
        s.setTextZoom(100);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (HOST.equals(u.getHost())) return false;
                openExternal(u);                   // wa.me y demás enlaces se abren fuera del juego
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                notifyVoices();
            }
        });

        web.addJavascriptInterface(new TtsBridge(), "AndroidTTS");
        web.addJavascriptInterface(new ShareBridge(), "AndroidShare");

        tts = new TextToSpeech(this, this);

        if (state != null) web.restoreState(state);
        else web.loadUrl(START);
    }

    /* ---------------- voz ---------------- */

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            try { tts.setLanguage(Locale.forLanguageTag("es-US")); } catch (Exception ignored) { }
            notifyVoices();
        }
    }

    private void notifyVoices() {
        if (web == null) return;
        runOnUiThread(() -> web.evaluateJavascript(
                "window.__ttsVoicesChanged && window.__ttsVoicesChanged()", null));
    }

    private class TtsBridge {
        @JavascriptInterface
        public String voices() {
            JSONArray a = new JSONArray();
            if (!ttsReady) return "[]";
            try {
                Set<Voice> vs = tts.getVoices();
                if (vs != null) {
                    for (Voice v : vs) {
                        String lang = v.getLocale().toLanguageTag();
                        if (!(lang.startsWith("es") || lang.startsWith("en"))) continue;
                        Set<String> f = v.getFeatures();
                        if (f != null && f.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                        JSONObject o = new JSONObject();
                        o.put("name", v.getName());
                        o.put("lang", lang);
                        o.put("net", v.isNetworkConnectionRequired());
                        a.put(o);
                    }
                }
            } catch (Exception ignored) { }
            return a.toString();
        }

        @JavascriptInterface
        public void speak(String text, String lang, float pitch, float rate, float volume, String voiceName, boolean flush) {
            if (!ttsReady || text == null || text.isEmpty()) return;
            try {
                Voice chosen = null;
                if (voiceName != null && !voiceName.isEmpty()) {
                    Set<Voice> vs = tts.getVoices();
                    if (vs != null) for (Voice v : vs) if (v.getName().equals(voiceName)) { chosen = v; break; }
                }
                if (chosen != null) tts.setVoice(chosen);
                else tts.setLanguage(Locale.forLanguageTag(lang == null || lang.isEmpty() ? "es-US" : lang));
                tts.setPitch(Math.max(0.1f, Math.min(2f, pitch)));
                tts.setSpeechRate(Math.max(0.1f, Math.min(2f, rate)));
                Bundle p = new Bundle();
                p.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, Math.max(0f, Math.min(1f, volume)));
                tts.speak(text, flush ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, p, "mn" + System.nanoTime());
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void stop() {
            if (ttsReady) tts.stop();
        }
    }

    /* ---------------- compartir por WhatsApp ---------------- */

    private class ShareBridge {
        @JavascriptInterface
        public void shareImage(final String dataUrl) {
            runOnUiThread(() -> doShare(dataUrl));
        }
    }

    private void doShare(String dataUrl) {
        try {
            String b64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            File dir = new File(getCacheDir(), "share");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "avance-mision-numeros.png");
            try (FileOutputStream out = new FileOutputStream(f)) { out.write(bytes); }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            for (String pkg : new String[]{"com.whatsapp", "com.whatsapp.w4b"}) {
                try {
                    Intent w = new Intent(send);
                    w.setPackage(pkg);
                    startActivity(w);
                    return;
                } catch (ActivityNotFoundException ignored) { }
            }
            startActivity(Intent.createChooser(send, "Compartir avance"));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo compartir la imagen", Toast.LENGTH_LONG).show();
        }
    }

    private void openExternal(Uri u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay una aplicación para abrir este enlace", Toast.LENGTH_LONG).show();
        }
    }

    /* ---------------- ciclo de vida ---------------- */

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("(window.__appBack && window.__appBack()) ? '1' : '0'", r -> {
            if (r == null || !r.contains("1")) finish();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ttsReady) tts.stop();
        web.onPause();
        web.pauseTimers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.resumeTimers();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
