const $ = id => document.getElementById(id);

document.addEventListener("DOMContentLoaded", async () => {

  // ── Load saved credentials ─────────────────────────────────
  const { userId, jwtToken } = await chrome.storage.local.get(["userId", "jwtToken"]);
  if (userId)   $("userId").value   = userId;
  if (jwtToken) $("jwtToken").value = jwtToken;

  refreshStatus();

  // ── Save ───────────────────────────────────────────────────
  $("saveBtn").addEventListener("click", async () => {
    const uid = $("userId").value.trim();
    const jwt = $("jwtToken").value.trim();
    if (!uid) return show("Enter your user ID", "err");
    if (!jwt) return show("Enter your JWT token", "err");

    await chrome.storage.local.set({ userId: uid, jwtToken: jwt });
    show("✅ Saved. Sync running automatically.", "ok");
    refreshStatus();
  });

  // ── Sync buttons ───────────────────────────────────────────
  $("chromeBtn").addEventListener("click", () => trigger("syncChrome",  "Syncing Chrome..."));
  $("xBtn")    .addEventListener("click", () => trigger("syncX",       "Syncing X..."));
  $("ytBtn")   .addEventListener("click", () => trigger("syncYouTube", "Syncing YouTube..."));
  $("allBtn")  .addEventListener("click", () => trigger("manualSync",  "Syncing all..."));

  // ── YouTube login ──────────────────────────────────────────
  $("ytLoginBtn").addEventListener("click", () => {
    show("Opening YouTube login...", "inf");
    $("ytLoginBtn").disabled = true;

    chrome.runtime.sendMessage({ action: "youtubeLogin" }, (res) => {
      $("ytLoginBtn").disabled = false;
      if (res?.success) {
        show("✅ YouTube logged in!", "ok");
      } else {
        show("❌ " + (res?.error || "Login failed"), "err");
      }
      refreshStatus();
    });
  });

  // ── Auto-refresh ───────────────────────────────────────────
  setInterval(refreshStatus, 10_000);
});

// ══════════════════════════════════════════════════════════════
function trigger(action, msg) {
  const btns = ["chromeBtn", "xBtn", "ytBtn", "allBtn"];
  btns.forEach(id => $(id).disabled = true);
  show(msg, "inf");

  chrome.runtime.sendMessage({ action }, (res) => {
    btns.forEach(id => $(id).disabled = false);
    if (res?.success) {
      show("✅ Done!", "ok");
    } else {
      show("❌ " + (res?.error || "Failed"), "err");
    }
    refreshStatus();
  });
}

// ══════════════════════════════════════════════════════════════
function refreshStatus() {
  chrome.runtime.sendMessage({ action: "getStatus" }, (data) => {
    if (!data) return;

    // ── Chrome card ──────────────────────────────────────────
    $("chromeCard").style.display = "block";
    $("lastChrome").textContent = data.lastChromeSync
      ? new Date(data.lastChromeSync).toLocaleString() : "Never";
    $("chromeNew").textContent   = data.lastChromeSyncResult?.newBookmarks   ?? "—";
    $("chromeTotal").textContent = data.lastChromeSyncResult?.totalBookmarks ?? "—";

    // ── X card ───────────────────────────────────────────────
    $("xCard").style.display = "block";
    const hasQueryId = !!data.xAuth?.queryId;
    $("xQueryId").innerHTML = hasQueryId
      ? `<span class="badge active">✅ Captured</span>`
      : `<span class="badge warn">⚠️ Visit x.com/i/bookmarks</span>`;

    $("lastX").textContent  = data.lastXSync
      ? new Date(data.lastXSync).toLocaleString() : "Never";
    $("xNew").textContent   = data.lastXSyncResult?.newBookmarks   ?? "—";
    $("xTotal").textContent = data.lastXSyncResult?.totalBookmarks ?? "—";

    const xResult = data.lastXSyncResult;
    if (xResult?.message?.includes("backend_fallback")) {
      $("xMode").innerHTML = `<span class="badge warn">🔄 Backend</span>`;
    } else if (xResult?.message?.includes("extension")) {
      $("xMode").innerHTML = `<span class="badge active">⚡ Extension</span>`;
    } else {
      $("xMode").innerHTML = `<span class="badge off">—</span>`;
    }

    // ── YouTube card ─────────────────────────────────────────
    $("ytCard").style.display = "block";
    const ytIn = data.ytLoggedIn;
    $("ytLoggedIn").innerHTML = ytIn
      ? `<span class="badge active">✅ Yes</span>`
      : `<span class="badge warn">⚠️ Not logged in</span>`;

    // Hide login button if already logged in
    $("ytLoginBtn").style.display = ytIn ? "none" : "block";

    $("lastYT").textContent = data.lastYTSync
      ? new Date(data.lastYTSync).toLocaleString() : "Never";
    $("ytNew").textContent   = data.lastYTSyncResult?.newVideos    ?? "—";
    $("ytTotal").textContent = data.lastYTSyncResult?.totalVideos  ?? "—";
  });
}

// ══════════════════════════════════════════════════════════════
function show(msg, type) {
  const el = $("status");
  el.textContent = msg;
  el.className = `status ${type === "ok" ? "ok" : type === "err" ? "err" : "inf"}`;
  el.style.display = "block";
  if (type !== "inf") setTimeout(() => (el.style.display = "none"), 4000);
}