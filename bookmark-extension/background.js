const BACKEND = "http://localhost:8080/api";

// ═══════════════════════════════════════════════════════════════
// LIFECYCLE
// ═══════════════════════════════════════════════════════════════

chrome.runtime.onInstalled.addListener(() => {
  console.log("[Convergence] Installed");
  syncChromeBookmarks();
  scheduleSync();
});

chrome.runtime.onStartup.addListener(() => {
  console.log("[Convergence] Browser opened");
  runAllSyncs().finally(() => scheduleSync());
});

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === "convergenceSync") {
    console.log("[Scheduler] Alarm fired — running all syncs");
    runAllSyncs().finally(() => scheduleSync());
  }
});

function scheduleSync() {
  const delay = 5 + Math.random() * 3; // 5-8 min
  chrome.alarms.create("convergenceSync", { delayInMinutes: delay });
  console.log(`[Scheduler] Next sync in ${delay.toFixed(1)} min`);
}

// ═══════════════════════════════════════════════════════════════
// RUN ALL THREE SYNCS
// ═══════════════════════════════════════════════════════════════

async function runAllSyncs() {
  const results = await Promise.allSettled([
    syncChromeBookmarks(),
    syncXBookmarks(),
    syncYouTubeWatchLater()
  ]);

  results.forEach((r, i) => {
    const names = ["Chrome", "X", "YouTube"];
    if (r.status === "rejected") {
      console.warn(`[${names[i]} Sync] Failed:`, r.reason?.message || r.reason);
    }
  });
}

// ═══════════════════════════════════════════════════════════════
// NETWORK INTERCEPT — capture X auth from GraphQL requests
// ═══════════════════════════════════════════════════════════════

chrome.webRequest.onBeforeSendHeaders.addListener(
  (details) => {
    if (!details.url.includes("/i/api/graphql/")) return;

    const h = {};
    for (const header of details.requestHeaders) {
      h[header.name.toLowerCase()] = header.value;
    }

    if (!h["authorization"] || !h["x-csrf-token"]) return;

    let queryId = null;
    if (details.url.toLowerCase().includes("bookmark")) {
      const m = details.url.match(/graphql\/([^/]+)\//);
      if (m) queryId = m[1];
    }

    let featuresJson = null;
    try {
      featuresJson = new URL(details.url).searchParams.get("features");
    } catch (_) {}

    chrome.storage.local.get(["xAuth"], ({ xAuth }) => {
      const updated = {
        bearerToken:   h["authorization"],
        csrfToken:     h["x-csrf-token"],
        cookieString:  h["cookie"] || "",
        authToken:     extractCookie(h["cookie"] || "", "auth_token"),
        queryId:       queryId || xAuth?.queryId || null,
        featuresJson:  featuresJson || xAuth?.featuresJson || null,
        userAgent:     h["user-agent"] || "",
        secChUa:       h["sec-ch-ua"] || "",
        secChPlatform: h["sec-ch-ua-platform"] || ""
      };
      chrome.storage.local.set({ xAuth: updated });
    });
  },
  { urls: ["https://x.com/i/api/*"] },
  ["requestHeaders"]
);

function extractCookie(str, name) {
  const m = str.match(new RegExp(`${name}=([^;]+)`));
  return m ? m[1] : "";
}

// ═══════════════════════════════════════════════════════════════
// CHROME BOOKMARK LISTENERS
// ═══════════════════════════════════════════════════════════════

chrome.bookmarks.onCreated.addListener(() => syncChromeBookmarks());
chrome.bookmarks.onRemoved.addListener(() => syncChromeBookmarks());
chrome.bookmarks.onChanged.addListener(() => syncChromeBookmarks());
chrome.bookmarks.onMoved.addListener(() => syncChromeBookmarks());

// ═══════════════════════════════════════════════════════════════
// 1) CHROME BOOKMARKS SYNC
// ═══════════════════════════════════════════════════════════════

async function syncChromeBookmarks() {
  const { userId, jwtToken } = await chrome.storage.local.get(["userId", "jwtToken"]);
  if (!userId || !jwtToken) return;

  try {
    const tree = await chrome.bookmarks.getTree();
    const flat = flattenBookmarks(tree);
    if (flat.length === 0) return;

    const res = await fetch(`${BACKEND}/bookmarks/chrome/sync`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${jwtToken}`
      },
      body: JSON.stringify({ userId, bookmarks: flat })
    });

    if (res.ok) {
      const data = await res.json();
      console.log(`[Chrome Sync] ${data.data?.message || data.message || "synced"}`);
      await chrome.storage.local.set({
        lastChromeSync: new Date().toISOString(),
        lastChromeSyncResult: data.data || data
      });
    } else {
      console.error("[Chrome Sync] Failed:", res.status);
    }
  } catch (e) {
    console.error("[Chrome Sync] Error:", e.message);
  }
}

function flattenBookmarks(nodes, parentTitle, result = []) {
  for (const node of nodes) {
    if (node.url) {
      result.push({
        chromeId:    node.id,
        title:       node.title || node.url,
        url:         node.url,
        dateAdded:   node.dateAdded ? new Date(node.dateAdded).toISOString() : null,
        parentTitle: parentTitle || null
      });
    }
    if (node.children) {
      flattenBookmarks(node.children, node.title, result);
    }
  }
  return result;
}

// ═══════════════════════════════════════════════════════════════
// 2) X BOOKMARKS SYNC
// ═══════════════════════════════════════════════════════════════

async function syncXBookmarks() {
    const { userId, jwtToken, xAuth } = await chrome.storage.local.get([
        "userId", "jwtToken", "xAuth"
      ]);
      if (!userId || !jwtToken) return;

      await sendXHeartbeat(userId, jwtToken);

      // If we haven't captured the GraphQL queryId, open bookmarks page to trigger network requests
      if (!xAuth?.queryId) {
        console.log("[X Sync] No queryId — opening x.com/i/bookmarks for user to login");
        chrome.tabs.create({ url: "https://x.com/i/bookmarks", active: true });
        return;
      }

  try {
       const ct0Cookie  = await chrome.cookies.get({ url: "https://x.com", name: "ct0" });
    const authCookie = await chrome.cookies.get({ url: "https://x.com", name: "auth_token" });

    // If cookies missing, open X login so user can sign in and generate the cookies
    if (!ct0Cookie || !authCookie) {
      console.log("[X Sync] Not logged into X — opening x.com/login");
      chrome.tabs.create({ url: "https://x.com/login", active: true });
      return;
    }

    const freshCsrf = ct0Cookie.value;
    const freshAuth = authCookie.value;

    await sendXKeys(userId, jwtToken, xAuth, freshCsrf, freshAuth);

    const variables = encodeURIComponent(JSON.stringify({ count: 20 }));
    const features  = xAuth.featuresJson
      ? encodeURIComponent(xAuth.featuresJson)
      : encodeURIComponent("{}");

    const xUrl = `https://x.com/i/api/graphql/${xAuth.queryId}/Bookmarks?variables=${variables}&features=${features}`;

    const xRes = await fetch(xUrl, {
      credentials: "include",
      headers: {
        "authorization":              xAuth.bearerToken,
        "x-csrf-token":               freshCsrf,
        "x-twitter-active-user":      "yes",
        "x-twitter-auth-type":        "OAuth2Session",
        "x-twitter-client-language":  "en",
        "content-type":               "application/json"
      }
    });

    if (!xRes.ok) {
      console.error("[X Sync] X API returned:", xRes.status);
      // Try DOM fallback scrape if API fails
      console.log("[X Sync] Attempting DOM scrape fallback");
      await scrapeXBookmarksViaTab();
      return;
    }

    const rawXData = await xRes.json();

    // If we got a valid GraphQL JSON, try to post it to backend fallback endpoint
    // The backend expects parsed tweet items; if it supports raw fallback, send rawXData
    // Otherwise, attempt DOM scrape fallback below.
    let syncRes = null;
    try {
      syncRes = await fetch(`${BACKEND}/x/sync`, {
        method: "POST",
        headers: {
          "Content-Type":  "application/json",
          "Authorization": `Bearer ${jwtToken}`
        },
        // send rawXData as `rawXData` so backend can detect fallback format
        body: JSON.stringify({ userId, rawXData })
      });
    } catch (e) {
      console.error('[X Sync] send to backend failed:', e?.message || e);
    }

    // If backend didn't accept the raw fallback, try DOM scrape
    if (!syncRes || !syncRes.ok) {
      console.log('[X Sync] Backend fallback failed, trying DOM scrape');
      await scrapeXBookmarksViaTab();
      return;
    }

    if (syncRes.ok) {
      const data = await syncRes.json();
      console.log("[X Sync]", data.data?.message || data.message || "synced");
      await chrome.storage.local.set({
        lastXSync: new Date().toISOString(),
        lastXSyncResult: data.data || data
      });
    }
  } catch (e) {
    console.error("[X Sync] Error:", e.message);
    // As a last resort, try DOM scrape fallback
    try { await scrapeXBookmarksViaTab(); } catch (_) {}
  }
}

// ═══════════════════════════════════════════════════════════════
// Fallback: Scrape X bookmarks via an injected tab DOM script
// If the GraphQL API or queryId approach fails, open the bookmarks page
// in a non-active tab and extract tweet data from the DOM, then send
// a parsed XSyncPayload to the backend `/x/sync` endpoint.
// ═══════════════════════════════════════════════════════════════

async function scrapeXBookmarksViaTab() {
  const { userId, jwtToken } = await chrome.storage.local.get(["userId", "jwtToken"]);
  if (!userId || !jwtToken) return;

  try {
    const tab = await chrome.tabs.create({ url: "https://x.com/i/bookmarks", active: false });

    // Wait for page to load
    await new Promise(resolve => {
      chrome.tabs.onUpdated.addListener(function listener(tabId, info) {
        if (tabId === tab.id && info.status === "complete") {
          chrome.tabs.onUpdated.removeListener(listener);
          resolve();
        }
      });
    });

    // Wait a bit for dynamic content to render
    await new Promise(r => setTimeout(r, 2500));

    const results = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: scrapeXDOM
    });

    // Close the tab
    try { chrome.tabs.remove(tab.id); } catch (_) {}

    const tweets = results?.[0]?.result || [];
    console.log(`[X Tab] Scraped ${tweets.length} tweets`);
    if (tweets.length === 0) return;

    // Transform into XSyncPayload format
    const payload = { userId, bookmarks: tweets };

    const res = await fetch(`${BACKEND}/x/sync`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${jwtToken}`
      },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      const data = await res.json();
      console.log('[X Tab Sync]', data.data?.message || data.message || 'synced');
      await chrome.storage.local.set({ lastXSync: new Date().toISOString(), lastXSyncResult: data.data || data });
    } else {
      console.error('[X Tab Sync] Backend returned', res.status);
    }
  } catch (e) {
    console.error('[X Tab] Error:', e.message);
  }
}

// This function runs inside x.com bookmarks page and extracts basic tweet info.
function scrapeXDOM() {
  const tweets = [];

  // container for bookmarks — X markup changes often; try multiple selectors
  const items = document.querySelectorAll('article, div[role="article"], div[data-testid="tweet"]');

  items.forEach(item => {
    try {
      // tweet id from data attributes or link href
      let tweetUrl = null;
      const link = item.querySelector('a[href*="/status/"]');
      if (link) tweetUrl = link.href;
      if (!tweetUrl) {
        const anchors = item.querySelectorAll('a');
        for (const a of anchors) {
          if (a.href && a.href.includes('/status/')) { tweetUrl = a.href; break; }
        }
      }
      if (!tweetUrl) return;

      const tweetIdMatch = tweetUrl.match(/status\/(\d+)/);
      const tweetId = tweetIdMatch ? tweetIdMatch[1] : null;

      const authorLink = item.querySelector('a[href^="/"][role="link"]') || item.querySelector('div[dir] a[href^="/"]');
      const authorUsername = authorLink ? authorLink.getAttribute('href').replace('/', '') : '';

      const displayNameEl = item.querySelector('div[dir] > span') || item.querySelector('strong');
      const authorDisplayName = displayNameEl ? displayNameEl.textContent.trim() : '';

      const contentEl = item.querySelector('div[lang]') || item.querySelector('p');
      const content = contentEl ? contentEl.textContent.trim() : '';

      const img = item.querySelector('img')?.src || null;

      // counts — may not be available reliably
      const counts = { likeCount: null, retweetCount: null, replyCount: null };
      const statEls = item.querySelectorAll('div[role="group"] a');
      if (statEls && statEls.length >= 3) {
        const parseNum = t => { const n = t?.textContent?.trim().replace(/[,\s]/g,'')||''; return n ? Number(n) : null; };
        counts.replyCount = parseNum(statEls[0]);
        counts.retweetCount = parseNum(statEls[1]);
        counts.likeCount = parseNum(statEls[2]);
      }

      tweets.push({
        tweetId: tweetId || '',
        tweetUrl,
        authorUsername: authorUsername || '',
        authorDisplayName: authorDisplayName || '',
        authorProfileImage: img || '',
        content: content || '',
        mediaUrls: null,
        likeCount: counts.likeCount,
        retweetCount: counts.retweetCount,
        replyCount: counts.replyCount
      });
    } catch (_) {}
  });

  return tweets;
}

async function sendXHeartbeat(userId, jwtToken) {
  try {
    await fetch(`${BACKEND}/x/heartbeat`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${jwtToken}` },
      body: JSON.stringify({ userId })
    });
  } catch (_) {}
}

async function sendXKeys(userId, jwtToken, xAuth, freshCsrf, freshAuth) {
  try {
    await fetch(`${BACKEND}/x/keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${jwtToken}` },
      body: JSON.stringify({
        userId,
        authToken: freshAuth,
        csrfToken: freshCsrf,
        queryId: xAuth.queryId,
        featuresJson: xAuth.featuresJson,
        fullCookieString: `auth_token=${freshAuth}; ct0=${freshCsrf}`,
        fingerprint: {
          userAgent:      xAuth.userAgent,
          secChUa:        xAuth.secChUa,
          secChPlatform:  xAuth.secChPlatform
        }
      })
    });
  } catch (_) {}
}

// ═══════════════════════════════════════════════════════════════
// 3) YOUTUBE WATCH LATER SYNC
//    Extension scrapes the page directly using cookies.
//    No Playwright needed — we ARE the browser.
// ═══════════════════════════════════════════════════════════════

async function syncYouTubeWatchLater() {
  const { userId, jwtToken, ytLoggedIn } = await chrome.storage.local.get([
    "userId", "jwtToken", "ytLoggedIn"
  ]);
  if (!userId || !jwtToken) return;

  // Check if user has YouTube cookies (logged in)
  const loginCookie = await chrome.cookies.get({
    url: "https://www.youtube.com",
    name: "LOGIN_INFO"
  });

  if (!loginCookie) {
    console.log("[YouTube Sync] Not logged in — skipping");
    await chrome.storage.local.set({ ytLoggedIn: false });
    return;
  }

  await chrome.storage.local.set({ ytLoggedIn: true });

  try {
    // Use YouTube's internal browse API (same as what the page uses)
    // This avoids needing to scrape HTML
    const videos = await fetchWatchLaterViaInternalAPI();

    if (!videos || videos.length === 0) {
      console.log("[YouTube Sync] No videos found or API failed, trying HTML scrape");
      await scrapeWatchLaterViaTab();
      return;
    }

    // Send to backend
    const res = await fetch(`${BACKEND}/youtube/extension/sync`, {
      method: "POST",
      headers: {
        "Content-Type":  "application/json",
        "Authorization": `Bearer ${jwtToken}`
      },
      body: JSON.stringify({ userId, videos })
    });

    if (res.ok) {
      const data = await res.json();
      console.log("[YouTube Sync]", data.data?.message || data.message || "synced");
      await chrome.storage.local.set({
        lastYTSync: new Date().toISOString(),
        lastYTSyncResult: data.data || data
      });
    }
  } catch (e) {
    console.error("[YouTube Sync] Error:", e.message);
  }
}

// ── YouTube Internal Browse API ─────────────────────────────
// YouTube's frontend hits this endpoint to load playlist data.
// We replicate the same call using the user's cookies.

async function fetchWatchLaterViaInternalAPI() {
  try {
    // First get SAPISIDHASH for auth
    const sapisid   = await chrome.cookies.get({ url: "https://www.youtube.com", name: "SAPISID" });
    const sidCookie = await chrome.cookies.get({ url: "https://www.youtube.com", name: "SID" });

    if (!sapisid || !sidCookie) return null;

    const timestamp = Math.floor(Date.now() / 1000);
    const origin    = "https://www.youtube.com";
    const hashInput = `${timestamp} ${sapisid.value} ${origin}`;

    // SHA-1 hash for SAPISIDHASH
    const encoder = new TextEncoder();
    const data    = encoder.encode(hashInput);
    const hashBuf = await crypto.subtle.digest("SHA-1", data);
    const hashArr = Array.from(new Uint8Array(hashBuf));
    const hashHex = hashArr.map(b => b.toString(16).padStart(2, "0")).join("");
    const sapisidhash = `SAPISIDHASH ${timestamp}_${hashHex}`;

    // Call YouTube's internal browse API
    const res = await fetch("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false", {
      method: "POST",
      credentials: "include",
      headers: {
        "content-type":    "application/json",
        "authorization":   sapisidhash,
        "x-origin":        origin,
        "x-youtube-client-name":    "1",
        "x-youtube-client-version": "2.20250101.00.00"
      },
      body: JSON.stringify({
        context: {
          client: {
            clientName: "WEB",
            clientVersion: "2.20250101.00.00",
            hl: "en",
            gl: "US"
          }
        },
        browseId: "VLWL"   // VL prefix + WL = Watch Later playlist
      })
    });

    if (!res.ok) {
      console.error("[YouTube API] Status:", res.status);
      return null;
    }

    const json = await res.json();
    return parseYouTubeBrowseResponse(json);

  } catch (e) {
    console.error("[YouTube API] Failed:", e.message);
    return null;
  }
}

function parseYouTubeBrowseResponse(json) {
  const videos = [];

  try {
    // Navigate the deeply nested YouTube response
    const tabs = json?.contents?.twoColumnBrowseResultsRenderer?.tabs;
    if (!tabs) return videos;

    const tab = tabs[0];
    const sectionListRenderer =
      tab?.tabRenderer?.content?.sectionListRenderer?.contents;

    if (!sectionListRenderer) return videos;

    for (const section of sectionListRenderer) {
      const items =
        section?.itemSectionRenderer?.contents?.[0]?.playlistVideoListRenderer?.contents ||
        section?.playlistVideoListRenderer?.contents ||
        [];

      for (const item of items) {
        const renderer = item?.playlistVideoRenderer;
        if (!renderer) continue;

        const videoId = renderer.videoId;
        if (!videoId) continue;

        const title = renderer.title?.runs?.[0]?.text ||
                      renderer.title?.simpleText || "";

        const channel = renderer.shortBylineText?.runs?.[0]?.text ||
                        renderer.ownerText?.runs?.[0]?.text || "";

        const thumbnail =
          renderer.thumbnail?.thumbnails?.slice(-1)?.[0]?.url ||
          `https://img.youtube.com/vi/${videoId}/mqdefault.jpg`;

        const lengthText = renderer.lengthText?.simpleText || "";

        videos.push({
          videoId,
          title,
          channelName: channel,
          thumbnailUrl: thumbnail,
          url: `https://www.youtube.com/watch?v=${videoId}`,
          duration: lengthText
        });
      }
    }
  } catch (e) {
    console.error("[YouTube Parse] Error:", e.message);
  }

  console.log(`[YouTube API] Parsed ${videos.length} videos`);
  return videos;
}

// ── Fallback: Scrape via injected tab ───────────────────────
// If the internal API fails, open a hidden tab and scrape DOM

async function scrapeWatchLaterViaTab() {
  const { userId, jwtToken } = await chrome.storage.local.get(["userId", "jwtToken"]);
  if (!userId || !jwtToken) return;

  try {
    // Create tab (non-active so user doesn't see it)
    const tab = await chrome.tabs.create({
      url: "https://www.youtube.com/playlist?list=WL",
      active: false
    });

    // Wait for page to load
    await new Promise(resolve => {
      chrome.tabs.onUpdated.addListener(function listener(tabId, info) {
        if (tabId === tab.id && info.status === "complete") {
          chrome.tabs.onUpdated.removeListener(listener);
          resolve();
        }
      });
    });

    // Wait extra for dynamic content
    await new Promise(r => setTimeout(r, 4000));

    // Inject script to scrape video data
    const results = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: scrapeWatchLaterDOM
    });

    // Close the tab
    chrome.tabs.remove(tab.id);

    const videos = results?.[0]?.result || [];
    console.log(`[YouTube Tab] Scraped ${videos.length} videos`);

    if (videos.length === 0) return;

    const res = await fetch(`${BACKEND}/youtube/extension/sync`, {
      method: "POST",
      headers: {
        "Content-Type":  "application/json",
        "Authorization": `Bearer ${jwtToken}`
      },
      body: JSON.stringify({ userId, videos })
    });

    if (res.ok) {
      const data = await res.json();
      console.log("[YouTube Tab Sync]", data.data?.message || "synced");
      await chrome.storage.local.set({
        lastYTSync: new Date().toISOString(),
        lastYTSyncResult: data.data || data
      });
    }
  } catch (e) {
    console.error("[YouTube Tab] Error:", e.message);
  }
}

// This runs INSIDE the YouTube page context
function scrapeWatchLaterDOM() {
  const videos = [];
  const items = document.querySelectorAll("ytd-playlist-video-renderer");

  items.forEach(item => {
    try {
      const titleEl   = item.querySelector("#video-title");
      const channelEl = item.querySelector("#channel-name a, .ytd-channel-name a");
      const thumbEl   = item.querySelector("img#img");

      const href  = titleEl?.getAttribute("href") || "";
      const title = titleEl?.getAttribute("title") || titleEl?.textContent?.trim() || "";

      let videoId = null;
      if (href.includes("v=")) {
        videoId = href.split("v=")[1].split("&")[0];
      }
      if (!videoId) return;

      videos.push({
        videoId,
        title,
        channelName:  channelEl?.textContent?.trim() || "",
        thumbnailUrl: thumbEl?.src || `https://img.youtube.com/vi/${videoId}/mqdefault.jpg`,
        url:          `https://www.youtube.com/watch?v=${videoId}`,
        duration:     item.querySelector(".badge-shape-wiz__text")?.textContent?.trim() || ""
      });
    } catch (_) {}
  });

  return videos;
}

// ═══════════════════════════════════════════════════════════════
// YOUTUBE LOGIN — open youtube in a new tab for user to log in
// ═══════════════════════════════════════════════════════════════

async function openYouTubeLogin() {
  const tab = await chrome.tabs.create({
    url: "https://accounts.google.com/signin/v2/identifier?service=youtube&continue=https%3A%2F%2Fwww.youtube.com",
    active: true
  });

  // Watch for when user reaches youtube.com (login complete)
  return new Promise((resolve) => {
    chrome.tabs.onUpdated.addListener(function listener(tabId, info, updatedTab) {
      if (tabId === tab.id &&
          info.status === "complete" &&
          updatedTab.url?.includes("youtube.com") &&
          !updatedTab.url?.includes("accounts.google.com")) {

        chrome.tabs.onUpdated.removeListener(listener);
        chrome.storage.local.set({ ytLoggedIn: true });
        console.log("[YouTube] Login detected");
        resolve({ success: true });
      }
    });

    // Timeout after 3 min
    setTimeout(() => resolve({ success: false, error: "Login timeout" }), 180_000);
  });
}

// ═══════════════════════════════════════════════════════════════
// MESSAGE HANDLER — from popup
// ═══════════════════════════════════════════════════════════════

chrome.runtime.onMessage.addListener((req, _sender, sendResponse) => {

  if (req.action === "manualSync") {
    runAllSyncs()
      .then(() => sendResponse({ success: true }))
      .catch(e => sendResponse({ success: false, error: e.message }));
    return true;
  }

  if (req.action === "syncChrome") {
    syncChromeBookmarks()
      .then(() => sendResponse({ success: true }))
      .catch(e => sendResponse({ success: false, error: e.message }));
    return true;
  }

  if (req.action === "syncX") {
    syncXBookmarks()
      .then(() => sendResponse({ success: true }))
      .catch(e => sendResponse({ success: false, error: e.message }));
    return true;
  }

  if (req.action === "syncYouTube") {
    syncYouTubeWatchLater()
      .then(() => sendResponse({ success: true }))
      .catch(e => sendResponse({ success: false, error: e.message }));
    return true;
  }

  if (req.action === "youtubeLogin") {
    openYouTubeLogin()
      .then(r => sendResponse(r))
      .catch(e => sendResponse({ success: false, error: e.message }));
    return true;
  }

  if (req.action === "getStatus") {
    chrome.storage.local.get([
      "userId", "xAuth", "ytLoggedIn",
      "lastChromeSync", "lastXSync", "lastYTSync",
      "lastChromeSyncResult", "lastXSyncResult", "lastYTSyncResult"
    ], (data) => sendResponse(data));
    return true;
  }
});