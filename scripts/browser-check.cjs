/* Uses the real Java API in a fresh, isolated browser session. */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const { checkReadability } = require("./orbital-text-checks.cjs");
const baseUrl = process.env.QA_BASE_URL || "http://127.0.0.1:4577";
const screenshotDirectory = path.resolve(__dirname, "../docs/screenshots");
let browser;

async function main() {
  fs.mkdirSync(screenshotDirectory, { recursive: true });
  browser = await chromium.launch({
    channel: process.env.BROWSER_CHANNEL || "chrome",
    headless: true,
  });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
  });
  const page = await context.newPage();
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto(baseUrl + "/app/");
  await page
    .getByText("No entities yet. Add a satellite and a device to begin.")
    .waitFor();
  const screenshot = (name) =>
    page.screenshot({
      path: path.join(screenshotDirectory, name + ".png"),
      fullPage: true,
    });
  const checkLayout = async (label) => {
    await checkReadability(page, label);
    await page.evaluate(
      () =>
        new Promise((resolve) =>
          requestAnimationFrame(() => requestAnimationFrame(resolve)),
        ),
    );
    const camera = await page.locator("canvas").getAttribute("data-camera");
    assert(
      camera.split(",").every((value) => Number.isFinite(Number(value))),
      label + ": finite camera coordinates " + camera,
    );
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      label +
        ": page overflow " +
        JSON.stringify(
          await page.evaluate(() =>
            [...document.querySelectorAll("body *")]
              .filter(
                (node) =>
                  node.getClientRects().length &&
                  node.getBoundingClientRect().right > innerWidth + 1,
              )
              .slice(0, 8)
              .map((node) => ({
                tag: node.tagName,
                class: node.className,
                right: node.getBoundingClientRect().right,
              })),
          ),
        ),
    );
    const smallText = await page.evaluate(() =>
      [...document.querySelectorAll("body *")]
        .filter(
          (node) =>
            node.getClientRects().length &&
            [...node.childNodes].some(
              (child) =>
                child.nodeType === Node.TEXT_NODE && child.textContent.trim(),
            ) &&
            parseFloat(getComputedStyle(node).fontSize) < 16,
        )
        .map((node) => node.className),
    );
    assert.deepEqual(smallText, [], label + ": text smaller than 16px");
  };
  for (const width of [320, 390, 768, 1024, 1440, 1920]) {
    await page.setViewportSize({ width, height: 1080 });
    await checkLayout("overview " + width);
    if ([390, 1920].includes(width))
      await screenshot("blackout-overview-" + width);
    await page.locator('[data-action="device"]').click();
    const select = page.getByLabel("Device type", { exact: true });
    assert.equal(await select.locator("option").count(), 3);
    assert(
      (await select.boundingBox()).width > 200,
      "Dropdown must have readable width",
    );
    await checkLayout("device dialog " + width);
    if ([390, 1920].includes(width))
      await page.screenshot({
        path: path.join(
          screenshotDirectory,
          "blackout-device-dialog-" + width + ".png",
        ),
        fullPage: false,
      });
    await page.keyboard.press("Escape");
  }
  await page.setViewportSize({ width: 1440, height: 1080 });
  await checkCamera(page, screenshot);
  await page.locator('[data-action="device"]').click();
  await page.getByLabel("Device ID", { exact: true }).fill("Ground One");
  await page.getByRole("button", { name: "Add device", exact: true }).click();
  await page.getByRole("button", { name: "Inspect Ground One" }).waitFor();
  await page.locator('.entity-actions [data-action="satellite"]').click();
  await page.getByLabel("Satellite ID", { exact: true }).fill("Orbiter One");
  await page
    .getByRole("button", { name: "Add satellite", exact: true })
    .last()
    .click();
  await page.getByRole("button", { name: "Inspect Orbiter One" }).waitFor();
  await page.getByRole("button", { name: "Inspect Ground One" }).click();
  await page.getByRole("button", { name: "Add file", exact: true }).click();
  await page.getByLabel("File name", { exact: true }).fill("telemetry.txt");
  await page.getByLabel("File contents", { exact: true }).fill("Hello Jupiter");
  await page.getByRole("button", { name: "Store file", exact: true }).click();
  await page.getByRole("button", { name: "Send file ↗", exact: true }).click();
  await page
    .getByLabel("Destination", { exact: true })
    .selectOption("Orbiter One");
  await page
    .getByRole("button", { name: "Start transfer", exact: true })
    .click();
  await page.getByRole("button", { name: "Inspect Orbiter One" }).click();
  const progress = page.getByRole("progressbar", {
    name: "telemetry.txt transfer progress",
  });
  await progress.waitFor();
  assert.equal(await progress.getAttribute("value"), "0");
  await page.locator('[data-minutes="1"]').click();
  await page.waitForFunction(
    () => document.querySelector("progress")?.value === 1,
  );
  await screenshot("blackout-transfer-desktop");
  for (let step = 0; step < 3; step++) {
    await page.locator('[data-minutes="5"]').click();
    await page.locator('[data-minutes="5"]').waitFor({ state: "visible" });
    await page.waitForFunction(
      () => !document.querySelector('[data-minutes="5"]').disabled,
    );
  }
  const stored = await page.evaluate(() =>
    fetch("/api/entity/info/?id=Orbiter%20One").then((response) =>
      response.json(),
    ),
  );
  assert.equal(stored.files["telemetry.txt"].isFileComplete, true);
  assert.equal(stored.files["telemetry.txt"].data, "Hello Jupiter");
  await checkEditing(page, context, checkLayout, screenshot);
  await page.locator('[data-action="device"]').click();
  await page.getByLabel("Device ID", { exact: true }).fill("Ground One");
  await page.getByRole("button", { name: "Add device", exact: true }).click();
  await page
    .getByText("This ID already exists. Choose a unique name.")
    .waitFor();
  await page.keyboard.press("Escape");
  await page.getByRole("button", { name: "Flight manual" }).click();
  await page.getByRole("heading", { name: "Your first connection" }).waitFor();
  await page.keyboard.press("Escape");
  const canvasBefore = await page.locator("canvas").screenshot();
  await page.waitForTimeout(500);
  const canvasAfter = await page.locator("canvas").screenshot();
  assert(
    !canvasBefore.equals(canvasAfter),
    "Visual scene should animate " +
      JSON.stringify(
        await page.evaluate(() => ({
          pause: document.querySelector("#motion-toggle").textContent,
          status: document.querySelector("#scene-status").textContent,
          canvas: { ...document.querySelector("canvas").dataset },
        })),
      ),
  );
  await page.locator("#motion-toggle").click();
  await page.waitForTimeout(800);
  const pausedBefore = await page.locator("canvas").screenshot();
  await page.waitForTimeout(300);
  const pausedAfter = await page.locator("canvas").screenshot();
  assert(pausedBefore.equals(pausedAfter), "Pause must stop visual animation");
  await page.locator("#play-button").click();
  await page.waitForFunction(
    () => document.querySelector("#elapsed-time").textContent !== "00:16",
  );
  await page.locator("#play-button").click();
  await page.getByRole("button", { name: "Remove", exact: true }).click();
  await page
    .getByRole("button", { name: "Remove entity", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Inspect Orbiter One" })
    .waitFor({ state: "detached" });
  await checkTouchCamera(browser);
  await checkNativeZoom();
  await checkTransferCancellation(browser);
  await checkGraphicsFallback();
  await checkRequestRecoveryAndPageLifecycle();
  assert.deepEqual(errors, []);
  console.log(
    JSON.stringify(
      {
        result: "passed",
        widths: [320, 390, 768, 1024, 1440, 1920],
        minimumTextPixels: 16,
        realJavaFlows: [
          "create device",
          "create satellite",
          "connect",
          "add file",
          "transfer progress",
          "transfer completion",
          "duplicate rejection",
          "run/pause",
          "remove",
        ],
        animationAndPause: "passed",
        real3D:
          "WebGL, full 360 orbit, poles, zoom, reset, drag, keyboard and touch",
        editingAndIsolation:
          "entity edit, file read/update/delete, transfer cancellation, isolated sessions",
        enlargement: "200% CSS enlargement and native Chrome browser zoom",
        recovery: "stale selection failures, empty file CRUD and back-forward cache graphics resume",
        runtimeErrors: 0,
        screenshotDirectory,
      },
      null,
      2,
    ),
  );
}

async function checkRequestRecoveryAndPageLifecycle() {
  const lifecycleBrowser = await chromium.launch({
    channel: process.env.BROWSER_CHANNEL || "chrome",
    headless: true,
    ignoreDefaultArgs: ["--disable-back-forward-cache"],
  });
  try {
    const context = await lifecycleBrowser.newContext({ reducedMotion: "no-preference" });
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.addInitScript(() => {
      window.addEventListener("pageshow", (event) => {
        document.documentElement.dataset.historyRestored = String(event.persisted);
      });
    });
    await page.goto(baseUrl + "/app/");
    await page.getByText("No entities yet. Add a satellite and a device to begin.").waitFor();
    for (const query of [
      "device/?deviceId=RecoveryGround&type=HandheldDevice&position=0",
      "satellite/?satelliteId=RecoveryOrbiter&type=StandardSatellite&position=0&height=80000",
    ]) {
      assert.equal((await context.request.put(baseUrl + "/api/" + query)).status(), 200);
    }
    await page.getByRole("button", { name: "Refresh network", exact: true }).click();
    await page.getByRole("button", { name: "Inspect RecoveryGround", exact: true }).click();
    await page.getByRole("button", { name: "Add file", exact: true }).click();
    await page.getByLabel("File name", { exact: true }).fill("empty.txt");
    await page.getByRole("button", { name: "Store file", exact: true }).click();
    await page.locator("#operation-dialog").waitFor({ state: "hidden" });
    const stored = await context.request.get(baseUrl + "/api/entity/info/?id=RecoveryGround");
    assert.deepEqual((await stored.json()).files["empty.txt"], {
      filename: "empty.txt", data: "", fileSize: 0, isFileComplete: true,
    });
    await page.locator('[data-file-action="edit-file"]').click();
    await page.getByRole("button", { name: "Save file", exact: true }).click();
    await page.locator("#operation-dialog").waitFor({ state: "hidden" });

    let releaseStaleRequest;
    let staleRequestStarted;
    const heldRequest = new Promise((resolve) => { staleRequestStarted = resolve; });
    const releaseRequest = new Promise((resolve) => { releaseStaleRequest = resolve; });
    await page.route("**/api/entity/entitiesInRange/**", async (route) => {
      if (new URL(route.request().url()).searchParams.get("id") === "RecoveryGround") {
        staleRequestStarted();
        await releaseRequest;
        await route.abort("failed");
      } else await route.continue();
    });
    await page.getByRole("button", { name: "Inspect RecoveryGround", exact: true }).click();
    await heldRequest;
    const nextSelection = page.waitForResponse((response) =>
      response.url().includes("entitiesInRange") && response.url().includes("RecoveryOrbiter"));
    await page.getByRole("button", { name: "Inspect RecoveryOrbiter", exact: true }).click();
    await nextSelection;
    const staleFailure = page.waitForEvent("requestfailed", (request) => request.url().includes("RecoveryGround"));
    releaseStaleRequest();
    await staleFailure;
    await page.evaluate(() => new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    assert.equal(await page.locator("#entity-inspector h3").textContent(), "RecoveryOrbiter");
    assert.equal(await page.locator("#network-feedback").evaluate((element) => element.classList.contains("is-error")), false);
    await page.unroute("**/api/entity/entitiesInRange/**");

    await page.waitForFunction(() => document.querySelector("canvas").dataset.texture === "loaded");
    await page.goto("about:blank");
    // A cache restore does not emit a new document load event.
    await page.goBack({ waitUntil: "commit" });
    await page.waitForFunction(() => document.documentElement.dataset.historyRestored === "true");
    const rotation = await page.locator("canvas").getAttribute("data-rotation");
    await page.waitForFunction((previous) => document.querySelector("canvas").dataset.rotation !== previous, rotation);
    assert.equal(await page.locator("#play-button").getAttribute("aria-pressed"), "false");
    assert.equal(await page.locator("#entity-count").textContent(), "2");
    assert.deepEqual(errors, [], "Recovery and cached-page restoration errors");
  } finally {
    await lifecycleBrowser.close();
  }
}

async function checkTransferCancellation(browser) {
  const context = await browser.newContext();
  try {
    const api = context.request;
    const request = async (path, method = "GET", content) => {
      const response = await api.fetch(baseUrl + "/api/" + path, {
        method,
        ...(content === undefined
          ? {}
          : { data: content, headers: { "Content-Type": "text/plain" } }),
      });
      return { status: response.status(), result: await response.json() };
    };
    assert.equal(
      (
        await request(
          "device/?deviceId=Source&type=HandheldDevice&position=0",
          "PUT",
        )
      ).status,
      200,
    );
    assert.equal(
      (
        await request(
          "satellite/?satelliteId=Target&type=StandardSatellite&position=0&height=80000",
          "PUT",
        )
      ).status,
      200,
    );
    await request(
      "device/file/?deviceId=Source&fileName=active.txt",
      "POST",
      "Hello Jupiter",
    );
    await request(
      "sendFile/?fromId=Source&toId=Target&fileName=active.txt",
      "POST",
    );
    const editing = await request(
      "device/file/?deviceId=Source&fileName=active.txt",
      "PUT",
      "New payload",
    );
    assert.equal(editing.status, 400, "Cannot edit a source during transfer");
    await request("simulate/?n=1", "POST");
    const partial = await request("entity/info/?id=Target");
    assert.equal(partial.result.files["active.txt"].data, "H");
    await request("entity/file/?id=Source&fileName=active.txt", "DELETE");
    await request("simulate/?n=1", "POST");
    assert.deepEqual(
      (await request("entity/info/?id=Target")).result.files,
      {},
      "Delete source cancels partial copy",
    );
    await request(
      "device/file/?deviceId=Source&fileName=active.txt",
      "POST",
      "Hello Jupiter",
    );
    await request(
      "sendFile/?fromId=Source&toId=Target&fileName=active.txt",
      "POST",
    );
    await request("entity/?id=Target&position=0.5&height=90000", "PUT");
    const repositioned = (await request("entity/info/?id=Target")).result;
    assert.equal(repositioned.position, 0.5);
    assert.equal(repositioned.height, 90000);
    assert.deepEqual(
      repositioned.files,
      {},
      "Reposition cancels pending transfers",
    );
    assert.equal(
      (await request("entity/info/?id=Source")).result.files["active.txt"].data,
      "Hello Jupiter",
    );
    const forbidden = await api.post(baseUrl + "/api/simulate/?n=1", {
      headers: { Origin: "https://foreign.example" },
    });
    assert.equal(forbidden.status(), 403, "Cross-origin mutation rejected");
  } finally {
    await context.close();
  }
}

async function checkGraphicsFallback() {
  const fallbackBrowser = await chromium.launch({
    channel: process.env.BROWSER_CHANNEL || "chrome",
    headless: true,
    args: ["--disable-webgl"],
  });
  try {
    const page = await fallbackBrowser.newPage({
      viewport: { width: 390, height: 844 },
    });
    const errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.goto(baseUrl + "/app/");
    await page.getByText("3D requires WebGL 2.", { exact: false }).waitFor();
    assert(await page.locator("#auto-orbit").isDisabled());
    await page.locator('[data-action="device"]').click();
    await page
      .getByLabel("Device ID", { exact: true })
      .fill("No graphics ground");
    await page.getByRole("button", { name: "Add device", exact: true }).click();
    await page
      .getByRole("button", { name: "Inspect No graphics ground", exact: true })
      .waitFor();
    assert.deepEqual(
      errors,
      [],
      "Missing WebGL must not break network controls",
    );
    await page.screenshot({
      path: path.join(screenshotDirectory, "blackout-no-webgl-390.png"),
      fullPage: true,
    });
  } finally {
    await fallbackBrowser.close();
  }
}

async function checkTouchCamera(browser) {
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    hasTouch: true,
    isMobile: true,
  });
  try {
    const page = await context.newPage();
    await page.goto(baseUrl + "/app/");
    await page.waitForFunction(
      () => document.querySelector("canvas").dataset.texture === "loaded",
    );
    await page.evaluate(async () => {
      await fetch(
        "/api/device/?deviceId=Touch%20ground&type=HandheldDevice&position=0",
        { method: "PUT" },
      );
      await fetch(
        "/api/satellite/?satelliteId=Touch%20orbiter&type=StandardSatellite&position=0&height=80000",
        { method: "PUT" },
      );
    });
    await page.locator("#refresh-button").click();
    await page
      .getByRole("button", { name: "Inspect Touch ground", exact: true })
      .waitFor();
    const network = () =>
      page.evaluate(() => fetch("/api/device/all/").then((r) => r.json()));
    const originalNetwork = await network();
    const canvas = page.locator("#orbital-canvas");
    await canvas.scrollIntoViewIfNeeded();
    const bounds = await canvas.boundingBox();
    const session = await context.newCDPSession(page);
    const originalCamera = await canvas.getAttribute("data-camera");
    const y = Math.round(bounds.y + bounds.height / 2);
    const x = Math.round(bounds.x + bounds.width / 3);
    await session.send("Input.dispatchTouchEvent", {
      type: "touchStart",
      touchPoints: [{ x, y, id: 0 }],
    });
    for (let step = 1; step <= 8; step++)
      await session.send("Input.dispatchTouchEvent", {
        type: "touchMove",
        touchPoints: [{ x: x + step * 10, y: y + step * 3, id: 0 }],
      });
    await session.send("Input.dispatchTouchEvent", {
      type: "touchEnd",
      touchPoints: [],
    });
    await page.waitForTimeout(600);
    assert.notEqual(
      await canvas.getAttribute("data-camera"),
      originalCamera,
      "One-finger camera orbit",
    );
    const distance = () =>
      canvas
        .getAttribute("data-camera")
        .then((value) => Math.hypot(...value.split(",").map(Number)));
    const previousDistance = await distance();
    const center = Math.round(bounds.x + bounds.width / 2);
    await session.send("Input.dispatchTouchEvent", {
      type: "touchStart",
      touchPoints: [
        { x: center - 40, y, id: 0 },
        { x: center + 40, y, id: 1 },
      ],
    });
    for (let step = 1; step <= 6; step++)
      await session.send("Input.dispatchTouchEvent", {
        type: "touchMove",
        touchPoints: [
          { x: center - 40 - step * 6, y, id: 0 },
          { x: center + 40 + step * 6, y, id: 1 },
        ],
      });
    await session.send("Input.dispatchTouchEvent", {
      type: "touchEnd",
      touchPoints: [],
    });
    await page.waitForTimeout(400);
    assert(
      (await distance()) < previousDistance - 0.1,
      "Two-finger pinch must zoom",
    );
    assert.deepEqual(
      await network(),
      originalNetwork,
      "Camera gestures must not mutate the Java model",
    );
    await page.getByRole("button", { name: "Reset view", exact: true }).click();
    await page.screenshot({
      path: path.join(screenshotDirectory, "blackout-touch-390.png"),
      fullPage: true,
    });
  } finally {
    await context.close();
  }
}

async function checkNativeZoom() {
  // An empty profile directory asks Playwright for a disposable Chrome profile.
  // The user's browser settings and profile are never opened or changed.
  const context = await chromium.launchPersistentContext("", {
    channel: process.env.BROWSER_CHANNEL || "chrome",
    headless: true,
    viewport: null,
    args: ["--window-size=1440,1000"],
  });
  try {
    const settings = context.pages()[0];
    await settings.goto("chrome://settings/appearance", {
      waitUntil: "domcontentloaded",
    });
    await settings.evaluate(
      () =>
        new Promise((resolve) =>
          chrome.settingsPrivate.setDefaultZoom(2, resolve),
        ),
    );
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.goto(baseUrl + "/app/");
    await page.waitForFunction(
      () => document.querySelector("canvas").dataset.texture === "loaded",
    );
    const metrics = await page.evaluate(() => ({
      inner: innerWidth,
      outer: outerWidth,
      dpr: devicePixelRatio,
      scroll: document.documentElement.scrollWidth,
    }));
    assert(
      metrics.inner < metrics.outer * 0.55 && metrics.dpr >= 2,
      "Actual browser 200% zoom",
    );
    assert(metrics.scroll <= metrics.inner, "Native zoom page reflow");
    await page.locator('[data-action="device"]').click();
    await page.getByLabel("Device ID", { exact: true }).fill("Zoom Ground");
    await page.getByRole("button", { name: "Add device", exact: true }).click();
    await page
      .getByRole("button", { name: "Inspect Zoom Ground", exact: true })
      .waitFor();
    await page.getByRole("button", { name: "Add file", exact: true }).click();
    await page.getByLabel("File name", { exact: true }).fill("zoom.txt");
    await page
      .getByLabel("File contents", { exact: true })
      .fill("Readable at 200 percent");
    await page.getByRole("button", { name: "Store file", exact: true }).click();
    await page.locator('[data-file-action="view-file"]').click();
    assert.equal(
      await page.getByLabel("File contents", { exact: true }).inputValue(),
      "Readable at 200 percent",
    );
    assert(
      await page
        .locator("dialog[open]")
        .evaluate((node) => node.scrollWidth <= node.clientWidth),
    );
    await page.keyboard.press("Escape");
    await page.getByRole("button", { name: "Top view", exact: true }).click();
    await page.getByRole("button", { name: "Reset view", exact: true }).click();
    await page.evaluate(() => scrollTo(0, 0));
    const session = await context.newCDPSession(page);
    const layout = await session.send("Page.getLayoutMetrics");
    const capture = await session.send("Page.captureScreenshot", {
      format: "png",
      captureBeyondViewport: true,
      clip: {
        x: 0,
        y: 0,
        width: layout.contentSize.width,
        height: layout.contentSize.height,
        scale: 1,
      },
    });
    fs.writeFileSync(
      path.join(screenshotDirectory, "blackout-chrome-zoom-200.png"),
      Buffer.from(capture.data, "base64"),
    );
    assert.deepEqual(errors, [], "Native zoom runtime errors");
    console.log("Native Chrome zoom:", metrics);
  } finally {
    await context.close();
  }
}

async function checkCamera(page, screenshot) {
  const canvas = page.locator("#orbital-canvas");
  await page.waitForFunction(
    () => document.querySelector("canvas").dataset.texture === "loaded",
  );
  assert.equal(await canvas.getAttribute("data-renderer"), "webgl");
  const camera = async () =>
    (await canvas.getAttribute("data-camera")).split(",").map(Number);
  const close = (a, b) =>
    Math.hypot(...a.map((number, index) => number - b[index])) < 0.02;
  await page.getByRole("button", { name: "Reset view", exact: true }).click();
  await page.waitForTimeout(150);
  const initial = await camera();
  const before = await canvas.screenshot();
  const quadrants = new Set();
  for (let turn = 0; turn < 8; turn++) {
    await page
      .getByRole("button", { name: "Rotate camera right", exact: true })
      .click();
    await page.waitForTimeout(60);
    const [x, , z] = await camera();
    quadrants.add(Math.sign(x) + "," + Math.sign(z));
    if (turn === 3) {
      assert(
        !before.equals(await canvas.screenshot()),
        "Far side must actually render differently",
      );
      await screenshot("blackout-3d-far-side");
    }
  }
  assert.equal(
    quadrants.size,
    4,
    "Camera must visit all four azimuth quadrants",
  );
  assert(close(initial, await camera()), "Eight 45° turns must complete 360°");
  for (let tilt = 0; tilt < 8; tilt++)
    await page
      .getByRole("button", { name: "Tilt camera up", exact: true })
      .click();
  await page.waitForTimeout(100);
  assert((await camera())[1] > 5, "Camera must reach north pole");
  for (let tilt = 0; tilt < 8; tilt++)
    await page
      .getByRole("button", { name: "Tilt camera down", exact: true })
      .click();
  await page.waitForTimeout(100);
  assert((await camera())[1] < -5, "Camera must reach south pole");
  await screenshot("blackout-3d-south-pole");
  await page.getByRole("button", { name: "Reset view", exact: true }).click();
  await page.getByRole("button", { name: "Zoom in", exact: true }).click();
  await page.waitForTimeout(80);
  assert(Math.hypot(...(await camera())) < Math.hypot(...initial), "Zoom in");
  await page.getByRole("button", { name: "Zoom out", exact: true }).click();
  await page.waitForTimeout(80);
  assert(close(initial, await camera()), "Zoom out restores distance");
  await canvas.scrollIntoViewIfNeeded();
  const bounds = await canvas.boundingBox();
  await page.mouse.move(
    bounds.x + bounds.width * 0.45,
    bounds.y + bounds.height * 0.5,
  );
  await page.mouse.down();
  await page.mouse.move(
    bounds.x + bounds.width * 0.7,
    bounds.y + bounds.height * 0.65,
    { steps: 12 },
  );
  await page.mouse.up();
  await page.waitForTimeout(800);
  assert(
    !close(initial, await camera()),
    "Mouse drag must orbit the actual camera",
  );
  await canvas.focus();
  const beforeKey = await camera();
  await page.keyboard.press("ArrowLeft");
  await page.waitForTimeout(200);
  assert(!close(beforeKey, await camera()), "Arrow key must orbit");
  await page.keyboard.press("Home");
  await page.waitForTimeout(200);
  assert(close(initial, await camera()), "Home must reset");
  await page.getByRole("button", { name: "Auto orbit", exact: true }).click();
  await page.waitForTimeout(500);
  assert(!close(initial, await camera()), "Auto orbit must move the camera");
  await page.getByRole("button", { name: "Auto orbit", exact: true }).click();
  await page.getByRole("button", { name: "Reset view", exact: true }).click();
}

async function checkEditing(page, context, checkLayout, screenshot) {
  await page
    .getByRole("button", { name: "Inspect Ground One", exact: true })
    .click();
  await page.locator('[data-file-action="view-file"]').click();
  assert.equal(
    await page.getByLabel("File contents", { exact: true }).inputValue(),
    "Hello Jupiter",
  );
  assert(
    (await page
      .getByLabel("File contents", { exact: true })
      .getAttribute("readonly")) !== null,
  );
  await page.keyboard.press("Escape");
  await page.locator('[data-file-action="edit-file"]').click();
  await page
    .getByLabel("File contents", { exact: true })
    .fill("Updated telemetry");
  await page.getByRole("button", { name: "Save file", exact: true }).click();
  await page.locator("#operation-dialog").waitFor({ state: "hidden" });
  await page.getByRole("button", { name: "Edit entity", exact: true }).click();
  await page.getByLabel("Position (degrees)", { exact: true }).fill("45");
  await page.getByRole("button", { name: "Save entity", exact: true }).click();
  await page.locator("#operation-dialog").waitFor({ state: "hidden" });
  const updated = await page.evaluate(() =>
    fetch("/api/entity/info/?id=Ground%20One").then((r) => r.json()),
  );
  assert(Math.abs(updated.position - Math.PI / 4) < 0.0001);
  assert.equal(updated.files["telemetry.txt"].data, "Updated telemetry");
  await page
    .getByRole("button", { name: "Inspect Orbiter One", exact: true })
    .click();
  await page.getByRole("button", { name: "Edit entity", exact: true }).click();
  await page.getByLabel("Orbital radius (km)", { exact: true }).fill("90000");
  await page.getByRole("button", { name: "Save entity", exact: true }).click();
  await page.locator("#operation-dialog").waitFor({ state: "hidden" });
  const orbiter = await page.evaluate(() =>
    fetch("/api/entity/info/?id=Orbiter%20One").then((r) => r.json()),
  );
  assert.equal(orbiter.height, 90000);
  assert.equal(orbiter.files["telemetry.txt"].data, "Hello Jupiter");
  for (const [name, type] of [
    ["Laptop Long Name " + "x".repeat(60), "LaptopDevice"],
    ["Desktop", "DesktopDevice"],
  ]) {
    await page.locator('[data-action="device"]').click();
    await page.getByLabel("Device ID", { exact: true }).fill(name);
    await page.getByLabel("Device type", { exact: true }).selectOption(type);
    await page.getByRole("button", { name: "Add device", exact: true }).click();
    await page
      .getByRole("button", { name: "Inspect " + name, exact: true })
      .waitFor();
  }
  for (const type of ["RelaySatellite", "TeleportingSatellite"]) {
    await page.locator('.entity-actions [data-action="satellite"]').click();
    await page.getByLabel("Satellite ID", { exact: true }).fill(type);
    await page.getByLabel("Satellite type", { exact: true }).selectOption(type);
    await page
      .getByRole("button", { name: "Add satellite", exact: true })
      .last()
      .click();
    await page
      .getByRole("button", { name: "Inspect " + type, exact: true })
      .waitFor();
  }
  await page
    .getByRole("button", { name: "Inspect Ground One", exact: true })
    .click();
  const second = await page.context().browser().newContext();
  const isolated = await second.request.get(baseUrl + "/api/device/all/");
  assert.deepEqual(
    await isolated.json(),
    {},
    "A new browser session must not share entities",
  );
  await second.close();
  const failures = await page.evaluate(async () => {
    const results = [];
    for (const [url, method] of [
      ["/api/entity/info/?id=missing", "GET"],
      ["/api/device/?deviceId=bad&type=Unknown&position=0", "PUT"],
      [
        "/api/satellite/?satelliteId=bad&type=StandardSatellite&position=0&height=NaN",
        "PUT",
      ],
      ["/api/simulate/?n=0", "POST"],
      ["/api/simulate/?n=121", "POST"],
      ["/api/simulate/?n=invalid", "POST"],
      ["/api/device/?deviceId=badFlag&type=HandheldDevice&position=0&isMoving=yes", "PUT"],
      ["/api/device/?deviceId=badAngle&type=HandheldDevice&position=east", "PUT"],
      ["/api/satellite/?satelliteId=badRadius&type=StandardSatellite&position=0&height=10000001", "PUT"],
    ]) {
      const response = await fetch(url, { method });
      results.push({
        status: response.status,
        message: (await response.json()).error,
      });
    }
    return results;
  });
  assert.deepEqual(
    failures.map((r) => r.status),
    [404, 400, 400, 400, 400, 400, 400, 400, 400],
  );
  assert(
    failures.every((r) => r.message?.length),
    "Invalid requests need readable errors",
  );
  for (const width of [320, 390, 768, 1024, 1440, 1920]) {
    await page.setViewportSize({ width, height: 1080 });
    await checkLayout("populated view " + width);
    const layout = await page.evaluate(() => {
      const ignored =
        "input, textarea, select, canvas, .skip-link";
      return [...document.querySelectorAll("main *")]
        .filter((node) => {
          if (
            node.matches(ignored) ||
            !node.getClientRects().length ||
            node.closest("dialog:not([open])")
          )
            return false;
          return (
            node.clientWidth > 0 &&
            node.scrollWidth > node.clientWidth + 2 &&
            !["inline", "contents"].includes(getComputedStyle(node).display)
          );
        })
        .map((node) => ({
          class: node.className || node.tagName,
          width: node.clientWidth,
          scroll: node.scrollWidth,
          labels: node.querySelector(".scene-entity-label")
            ? [...node.querySelectorAll(".scene-entity-label")].map(
                (label) => ({
                  text: label.textContent,
                  hidden: label.hidden,
                  transform: label.style.transform,
                  width: label.offsetWidth,
                  right: label.getBoundingClientRect().right,
                }),
              )
            : undefined,
        }));
    });
    assert.deepEqual(layout, [], "Nested content overflow " + width);
    await page
      .getByRole("button", { name: "Edit entity", exact: true })
      .click();
    await checkLayout("populated edit " + width);
    await page.keyboard.press("Escape");
    if ([390, 1920].includes(width))
      await screenshot("blackout-populated-" + width);
  }
  await page.setViewportSize({ width: 1440, height: 1080 });
  await page.evaluate(() => {
    document.body.style.zoom = "2";
  });
  await checkLayout("200% enlargement");
  await page.getByRole("button", { name: "Edit entity", exact: true }).click();
  await checkLayout("200% dialog enlargement");
  await page.keyboard.press("Escape");
  await screenshot("blackout-enlargement-200");
  await page.evaluate(() => {
    document.body.style.zoom = "";
  });
  await context.setOffline(true);
  await page
    .getByRole("button", { name: "Refresh network", exact: true })
    .click();
  await page
    .getByText("The local simulator is unavailable.", { exact: false })
    .waitFor();
  await context.setOffline(false);
  await page
    .getByRole("button", { name: "Refresh network", exact: true })
    .click();
  await page.waitForFunction(
    () =>
      !document
        .querySelector("#network-feedback")
        .classList.contains("is-error"),
  );
  await page.locator('[data-file-action="remove-file"]').click();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  assert.equal(
    await page.locator(".file-entry").count(),
    1,
    "Cancel must preserve file",
  );
  await page.locator('[data-file-action="remove-file"]').click();
  await page.getByRole("button", { name: "Delete file", exact: true }).click();
  await page.locator(".file-entry").waitFor({ state: "detached" });
  await page
    .getByRole("button", { name: "Inspect Orbiter One", exact: true })
    .click();
}
main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => {
    if (browser) await browser.close();
  });
