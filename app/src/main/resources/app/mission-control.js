import { callSimulation, loadNetwork } from "./simulation-api.js";
import { OrbitalScene } from "./orbital-scene.js";

const element = (id) => document.getElementById(id);
const operationDialog = element("operation-dialog");
const operationForm = element("operation-form");
let scene;
try {
  scene = new OrbitalScene(element("orbital-canvas"), selectEntity);
} catch (error) {
  const status = element("scene-status");
  status.hidden = false;
  status.textContent =
    "3D requires WebGL 2. Enable hardware acceleration or try another browser. All network controls below remain available.";
  element("orbital-canvas").hidden = true;
  document
    .querySelectorAll(
      "[data-camera], #auto-orbit, #motion-toggle, #show-orbits",
    )
    .forEach((control) => {
      control.disabled = true;
    });
  // Keep non-visual operations usable when the browser cannot create WebGL.
  scene = { update() {}, suspend() {}, resume() {}, destroy() {}, motionPaused: true };
}
let networkEntities = {};
let selectedEntityId = null;
let connectedEntityIds = [];
let elapsedMinutes = 0;
let simulationTimer = null;
let simulationPending = false;
let currentOperation = null;
let latestSelectionRequest = 0;
const activityEntries = [];

function readableType(type) {
  return type.replace(/([a-z])([A-Z])/g, "$1 $2");
}

function escapeHtml(value) {
  return String(value).replace(
    /[&<>"']/g,
    (character) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      })[character],
  );
}

function logActivity(message) {
  const time = new Date().toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
  activityEntries.unshift({ message, time });
  activityEntries.length = Math.min(activityEntries.length, 6);
  const list = element("activity-list");
  list.replaceChildren();
  for (const entry of activityEntries) {
    const row = document.createElement("li");
    const timestamp = document.createElement("time");
    const text = document.createElement("span");
    timestamp.textContent = entry.time;
    text.textContent = entry.message;
    row.append(timestamp, text);
    list.append(row);
  }
}

function showNetworkMessage(message, isError = false) {
  const feedback = element("network-feedback");
  feedback.textContent = message;
  feedback.classList.toggle("is-error", isError);
}

async function refreshNetwork() {
  try {
    networkEntities = await loadNetwork();
    if (selectedEntityId && !networkEntities[selectedEntityId]) {
      selectedEntityId = null;
      connectedEntityIds = [];
      latestSelectionRequest++;
    }
    renderNetwork();
    if (selectedEntityId) await selectEntity(selectedEntityId);
  } catch (error) {
    showNetworkMessage(error.message, true);
    stopSimulation();
  }
}

function renderNetwork() {
  const entities = Object.values(networkEntities).sort((a, b) =>
    a.id.localeCompare(b.id),
  );
  const satelliteCount = entities.filter((entity) =>
    entity.type.endsWith("Satellite"),
  ).length;
  element("entity-count").textContent = entities.length;
  element("scene-empty").hidden = entities.length > 0;
  showNetworkMessage(
    entities.length
      ? satelliteCount +
          " satellites · " +
          (entities.length - satelliteCount) +
          " devices in this session."
      : "No entities yet. Add a satellite and a device to begin.",
  );
  const list = element("entity-list");
  list.replaceChildren();
  for (const entity of entities) {
    const isSatellite = entity.type.endsWith("Satellite");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "entity-button";
    button.setAttribute("aria-pressed", String(entity.id === selectedEntityId));
    button.setAttribute("aria-label", "Inspect " + entity.id);
    button.innerHTML =
      '<span class="entity-marker' +
      (isSatellite ? "" : " is-device") +
      '" aria-hidden="true">' +
      (isSatellite ? "◇" : "⊙") +
      '</span><span class="entity-name"></span><span class="entity-angle"></span>';
    const title = button.querySelector(".entity-name");
    title.textContent = entity.id;
    const subtitle = document.createElement("small");
    subtitle.textContent = readableType(entity.type);
    title.append(subtitle);
    button.querySelector(".entity-angle").textContent =
      ((entity.position * 180) / Math.PI).toFixed(1) + "°";
    button.addEventListener("click", () => selectEntity(entity.id));
    list.append(button);
  }
  renderInspector();
  scene.update(networkEntities, selectedEntityId, connectedEntityIds);
}

async function selectEntity(entityId) {
  const selectionRequest = ++latestSelectionRequest;
  selectedEntityId = entityId;
  connectedEntityIds = [];
  renderNetwork();
  try {
    const connections = await callSimulation("entity/entitiesInRange/", {
      parameters: { id: entityId },
    });
    if (selectionRequest !== latestSelectionRequest || selectedEntityId !== entityId) return;
    connectedEntityIds = connections.map((entity) => entity.id);
    renderInspector();
    scene.update(networkEntities, selectedEntityId, connectedEntityIds);
  } catch (error) {
    if (selectionRequest !== latestSelectionRequest || selectedEntityId !== entityId) return;
    showNetworkMessage(error.message, true);
  }
}

function renderInspector() {
  const inspector = element("entity-inspector");
  const entity = networkEntities[selectedEntityId];
  inspector.hidden = !entity;
  if (!entity) return;
  const files = Object.values(entity.files || {});
  const isSatellite = entity.type.endsWith("Satellite");
  const degrees = ((entity.position * 180) / Math.PI).toFixed(1);
  inspector.innerHTML =
    '<div class="inspector-heading"><h3>' +
    escapeHtml(entity.id) +
    '</h3><button type="button" class="text-button" id="edit-entity">Edit entity</button><button type="button" class="danger-link" id="remove-entity">Remove</button></div>' +
    '<dl class="entity-properties"><div><dt>Position</dt><dd>' +
    degrees +
    "°</dd></div><div><dt>" +
    (isSatellite ? "Orbital radius" : "Location") +
    "</dt><dd>" +
    (isSatellite
      ? Math.round(entity.height).toLocaleString() + " km"
      : "Jupiter surface") +
    "</dd></div></dl>" +
    '<h4 class="inspector-section">COMMUNICABLE ENTITIES</h4>' +
    (connectedEntityIds.length
      ? '<div class="connected-list">' +
        connectedEntityIds
          .map((id) => "<span>" + escapeHtml(id) + "</span>")
          .join("") +
        "</div>"
      : '<p class="inspector-note">No compatible entities are in range.</p>') +
    '<h4 class="inspector-section">FILES <span>(' +
    files.length +
    ")</span></h4>" +
    (files.length
      ? files
          .map((file) => {
            const received = file.isFileComplete
              ? file.fileSize
              : String(file.data || "").length;
            return (
              '<div class="file-entry"><div><span class="file-name">' +
              escapeHtml(file.filename) +
              "</span><span>" +
              (file.isFileComplete
                ? "Complete"
                : received + " / " + file.fileSize) +
              '</span></div><progress max="' +
              Math.max(1, file.fileSize) +
              '" value="' +
              received +
              '" aria-label="' +
              escapeHtml(file.filename) +
              ' transfer progress"></progress><div class="file-actions">' +
              '<button type="button" data-file-action="view-file" data-file="' +
              escapeHtml(file.filename) +
              '">Read</button>' +
              (!isSatellite && file.isFileComplete
                ? '<button type="button" data-file-action="edit-file" data-file="' +
                  escapeHtml(file.filename) +
                  '">Edit</button>'
                : "") +
              '<button type="button" data-file-action="remove-file" data-file="' +
              escapeHtml(file.filename) +
              '">Delete</button></div></div>'
            );
          })
          .join("")
      : '<p class="inspector-note">No files stored on this entity.</p>') +
    '<div class="inspector-actions">' +
    (!isSatellite
      ? '<button type="button" class="button button-secondary" id="add-file">Add file</button>'
      : "") +
    '<button type="button" class="button button-secondary" id="send-file"' +
    (!files.some((file) => file.isFileComplete) ? " disabled" : "") +
    ">Send file ↗</button></div>";
  element("remove-entity").addEventListener("click", () =>
    openOperation("remove"),
  );
  element("edit-entity").addEventListener("click", () =>
    openOperation("edit-entity"),
  );
  inspector
    .querySelectorAll("[data-file-action]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        openOperation(button.dataset.fileAction, button.dataset.file),
      ),
    );
  element("add-file")?.addEventListener("click", () => openOperation("file"));
  element("send-file").addEventListener("click", () =>
    openOperation("transfer"),
  );
}

function addInput(
  name,
  label,
  {
    type = "text",
    value = "",
    hint,
    min,
    max,
    step,
    full = false,
    placeholder,
  } = {},
) {
  const wrapper = document.createElement("div");
  wrapper.className = "form-field" + (full ? " full-width" : "");
  const caption = document.createElement("label");
  caption.htmlFor = "operation-" + name;
  caption.textContent = label;
  const control = document.createElement(
    type === "textarea" ? "textarea" : "input",
  );
  control.id = caption.htmlFor;
  control.name = name;
  if (type !== "textarea") control.type = type;
  control.required = true;
  control.value = value;
  if (min !== undefined) control.min = min;
  if (max !== undefined) control.max = max;
  if (step !== undefined) control.step = step;
  if (placeholder) control.placeholder = placeholder;
  if (type === "text") control.maxLength = 80;
  wrapper.append(caption, control);
  if (hint) {
    const help = document.createElement("p");
    help.className = "field-hint";
    help.id = control.id + "-help";
    help.textContent = hint;
    control.setAttribute("aria-describedby", help.id);
    wrapper.append(help);
  }
  element("dialog-fields").append(wrapper);
}

function addSelect(name, label, choices, full = true) {
  const wrapper = document.createElement("div");
  wrapper.className = "form-field" + (full ? " full-width" : "");
  const caption = document.createElement("label");
  caption.htmlFor = "operation-" + name;
  caption.textContent = label;
  const select = document.createElement("select");
  select.id = caption.htmlFor;
  select.name = name;
  select.required = true;
  for (const [value, text] of choices) select.add(new Option(text, value));
  wrapper.append(caption, select);
  element("dialog-fields").append(wrapper);
}

function openOperation(action, filename = null) {
  stopSimulation();
  currentOperation = { action, entityId: selectedEntityId, filename };
  const fields = element("dialog-fields");
  fields.replaceChildren();
  element("dialog-error").hidden = true;
  const entity = networkEntities[selectedEntityId];
  let title, description, submit;
  if (action === "satellite" || action === "device") {
    const isSatellite = action === "satellite";
    title = isSatellite ? "Add satellite" : "Add device";
    submit = title;
    description = isSatellite
      ? "Place a satellite in the network. Orbital radius is measured from the centre of Jupiter."
      : "Place a device on the surface of Jupiter.";
    addInput("id", isSatellite ? "Satellite ID" : "Device ID", {
      full: true,
      placeholder: isSatellite ? "e.g. Relay One" : "e.g. Ground Station One",
    });
    addSelect(
      "type",
      isSatellite ? "Satellite type" : "Device type",
      (isSatellite
        ? ["StandardSatellite", "TeleportingSatellite", "RelaySatellite"]
        : ["HandheldDevice", "LaptopDevice", "DesktopDevice"]
      ).map((type) => [type, readableType(type)]),
    );
    addInput("position", "Position (degrees)", {
      type: "number",
      min: 0,
      max: 359.99,
      step: ".01",
      value: "0",
      full: !isSatellite,
      hint: "0° points right; angles increase anticlockwise.",
    });
    if (isSatellite) {
      addInput("height", "Orbital radius (km)", {
        type: "number",
        min: 69912,
        max: 10000000,
        step: 1,
        value: "80000",
        hint: "Must be above Jupiter’s 69,911 km surface.",
      });
    } else {
      const label = document.createElement("label");
      label.className = "checkbox-field";
      label.innerHTML =
        '<input type="checkbox" name="isMoving" /> Moving device (extended simulation rules)';
      fields.append(label);
    }
  } else if (action === "edit-entity") {
    if (!entity) return;
    const isSatellite = entity.type.endsWith("Satellite");
    title = "Edit entity";
    submit = "Save entity";
    description =
      "Reposition " +
      entity.id +
      ". Active transfers involving this entity will be cancelled; completed files are kept. The ID and type stay unchanged.";
    addInput("position", "Position (degrees)", {
      type: "number",
      min: 0,
      max: 359.99,
      step: ".01",
      value: ((entity.position * 180) / Math.PI).toFixed(2),
      full: true,
    });
    if (isSatellite)
      addInput("height", "Orbital radius (km)", {
        type: "number",
        min: 69912,
        max: 10000000,
        step: 1,
        value: String(Math.round(entity.height)),
        full: true,
      });
  } else if (action === "remove-file") {
    if (!entity?.files[filename]) return;
    title = "Delete file";
    submit = "Delete file";
    description =
      "Delete " +
      filename +
      " from " +
      entity.id +
      "? Active transfers of this file will be cancelled and unfinished copies removed. Completed copies on other entities are kept.";
  } else if (["file", "edit-file", "view-file"].includes(action)) {
    if (!entity) return;
    title =
      action === "file"
        ? "Add file"
        : action === "edit-file"
          ? "Edit file"
          : "Read file";
    submit =
      action === "file"
        ? "Store file"
        : action === "edit-file"
          ? "Save file"
          : "Close";
    description =
      action === "file"
        ? "Store a text payload on " + entity.id + "."
        : filename +
          " on " +
          entity.id +
          (action === "view-file"
            ? ". Received contents are shown below."
            : ". Files with active outgoing transfers cannot be edited.");
    addInput("fileName", "File name", {
      full: true,
      placeholder: "e.g. telemetry.txt",
      value: filename || "",
    });
    addInput("content", "File contents", {
      type: "textarea",
      full: true,
      placeholder: "Enter a short text payload.",
      value: filename ? entity.files[filename].data : "",
    });
    element("operation-content").maxLength = 100000;
    // Empty text files are valid model data, including after editing.
    element("operation-content").required = false;
    if (action !== "file") element("operation-fileName").readOnly = true;
    if (action === "view-file") {
      element("operation-content").readOnly = true;
      element("operation-content").required = false;
    }
  } else if (action === "transfer") {
    if (!entity) return;
    title = "Send file";
    submit = "Start transfer";
    description = "Choose a file from " + entity.id + " and a destination.";
    addSelect(
      "fileName",
      "File",
      Object.values(entity.files || {})
        .filter((file) => file.isFileComplete)
        .map((file) => [file.filename, file.filename]),
    );
    addSelect(
      "toId",
      "Destination",
      Object.values(networkEntities)
        .filter((other) => other.id !== entity.id)
        .map((other) => [
          other.id,
          other.id + " · " + readableType(other.type),
        ]),
    );
  } else if (action === "slope") {
    title = "Configure slope";
    submit = "Add slope";
    description =
      "Define a surface slope for the original moving-device extension.";
    addInput("startAngle", "Start angle (degrees)", {
      type: "number",
      min: 0,
      max: 359,
      step: 1,
      value: "0",
    });
    addInput("endAngle", "End angle (degrees)", {
      type: "number",
      min: 0,
      max: 359,
      step: 1,
      value: "90",
    });
    addInput("gradient", "Gradient", {
      type: "number",
      step: 1,
      value: "1",
      full: true,
    });
  } else if (action === "remove") {
    if (!entity) return;
    title = "Remove entity";
    submit = "Remove entity";
    description =
      "Remove " +
      entity.id +
      " and cancel its active transfers? This cannot be undone.";
  }
  element("dialog-title").textContent = title;
  element("dialog-description").textContent = description;
  element("submit-operation").textContent = submit;
  operationDialog.showModal();
}

operationForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const operation = { ...currentOperation };
  const formValues = Object.fromEntries(new FormData(operationForm));
  const submitButton = element("submit-operation");
  submitButton.disabled = true;
  element("close-dialog").disabled = true;
  element("cancel-dialog").disabled = true;
  const originalLabel = submitButton.textContent;
  submitButton.textContent = "Working…";
  element("dialog-error").hidden = true;
  try {
    if (operation.action === "satellite" || operation.action === "device") {
      const isSatellite = operation.action === "satellite";
      formValues.id = formValues.id.trim();
      if (!formValues.id) throw new Error("Enter a name, not just spaces.");
      if (Object.hasOwn(networkEntities, formValues.id))
        throw new Error("This ID already exists. Choose a unique name.");
      await callSimulation(isSatellite ? "satellite/" : "device/", {
        method: "PUT",
        parameters: {
          [isSatellite ? "satelliteId" : "deviceId"]: formValues.id.trim(),
          type: formValues.type,
          position: (Number(formValues.position) * Math.PI) / 180,
          ...(isSatellite
            ? { height: formValues.height }
            : { isMoving: formValues.isMoving === "on" }),
        },
      });
      selectedEntityId = formValues.id.trim();
      logActivity(
        "Added " +
          readableType(formValues.type).toLowerCase() +
          " " +
          selectedEntityId +
          ".",
      );
    } else if (operation.action === "edit-entity") {
      await callSimulation("entity/", {
        method: "PUT",
        parameters: {
          id: operation.entityId,
          position: (Number(formValues.position) * Math.PI) / 180,
          ...(formValues.height ? { height: formValues.height } : {}),
        },
      });
      logActivity("Updated " + operation.entityId + ".");
    } else if (operation.action === "remove-file") {
      await callSimulation("entity/file/", {
        method: "DELETE",
        parameters: { id: operation.entityId, fileName: operation.filename },
      });
      logActivity(
        "Deleted " + operation.filename + " from " + operation.entityId + ".",
      );
    } else if (["file", "edit-file"].includes(operation.action)) {
      await callSimulation("device/file/", {
        method: operation.action === "file" ? "POST" : "PUT",
        parameters: {
          deviceId: operation.entityId,
          fileName: formValues.fileName,
        },
        content: formValues.content,
      });
      logActivity(
        "Stored " + formValues.fileName + " on " + operation.entityId + ".",
      );
    } else if (operation.action === "transfer") {
      await callSimulation("sendFile/", {
        method: "POST",
        parameters: {
          fromId: operation.entityId,
          toId: formValues.toId,
          fileName: formValues.fileName,
        },
      });
      logActivity(
        "Started " +
          formValues.fileName +
          ": " +
          operation.entityId +
          " → " +
          formValues.toId +
          ".",
      );
    } else if (operation.action === "slope") {
      await callSimulation("createSlope/", {
        method: "POST",
        parameters: formValues,
      });
      logActivity(
        "Added a surface slope from " +
          formValues.startAngle +
          "° to " +
          formValues.endAngle +
          "°.",
      );
    } else if (operation.action === "remove") {
      const entity = networkEntities[operation.entityId];
      await callSimulation(
        entity.type.endsWith("Satellite") ? "satellite/" : "device/",
        {
          method: "DELETE",
          parameters: {
            [entity.type.endsWith("Satellite") ? "satelliteId" : "deviceId"]:
              entity.id,
          },
        },
      );
      logActivity("Removed " + entity.id + ".");
      selectedEntityId = null;
      connectedEntityIds = [];
      latestSelectionRequest++;
    }
    operationDialog.close();
    await refreshNetwork();
  } catch (error) {
    element("dialog-error").textContent = error.message;
    element("dialog-error").hidden = false;
  } finally {
    submitButton.disabled = false;
    element("close-dialog").disabled = false;
    element("cancel-dialog").disabled = false;
    submitButton.textContent = originalLabel;
  }
});
operationDialog.addEventListener("cancel", (event) => {
  if (element("submit-operation").disabled) event.preventDefault();
});

function stopSimulation() {
  if (simulationTimer) clearInterval(simulationTimer);
  simulationTimer = null;
  element("play-button").setAttribute("aria-pressed", "false");
  element("play-button").textContent = "▶ Run simulation";
}

async function advanceTime(minutes, announce = true) {
  if (simulationPending) return;
  simulationPending = true;
  document.querySelectorAll("[data-minutes]").forEach((button) => {
    button.disabled = true;
  });
  try {
    const frames = await callSimulation("simulate/", {
      method: "POST",
      parameters: { n: minutes },
    });
    if (frames.length) networkEntities = frames[frames.length - 1];
    elapsedMinutes += frames.length;
    element("elapsed-time").textContent =
      String(Math.floor(elapsedMinutes / 60)).padStart(2, "0") +
      ":" +
      String(elapsedMinutes % 60).padStart(2, "0");
    renderNetwork();
    if (selectedEntityId) await selectEntity(selectedEntityId);
    if (announce)
      logActivity(
        "Advanced simulation by " +
          minutes +
          (minutes === 1 ? " minute." : " minutes."),
      );
  } catch (error) {
    showNetworkMessage(error.message, true);
    stopSimulation();
  } finally {
    simulationPending = false;
    document.querySelectorAll("[data-minutes]").forEach((button) => {
      button.disabled = false;
    });
  }
}

document
  .querySelectorAll("[data-action]")
  .forEach((button) =>
    button.addEventListener("click", () =>
      openOperation(button.dataset.action),
    ),
  );
document.querySelectorAll("[data-minutes]").forEach((button) =>
  button.addEventListener("click", () => {
    stopSimulation();
    advanceTime(Number(button.dataset.minutes));
  }),
);
element("play-button").addEventListener("click", () => {
  if (simulationTimer) {
    stopSimulation();
    logActivity("Paused simulation.");
    return;
  }
  element("play-button").setAttribute("aria-pressed", "true");
  element("play-button").textContent = "Ⅱ Pause simulation";
  logActivity("Running: one simulation minute per second.");
  advanceTime(1, false);
  simulationTimer = setInterval(() => advanceTime(1, false), 1000);
});
element("refresh-button").addEventListener("click", refreshNetwork);
element("show-orbits").addEventListener("change", (event) => {
  scene.showOrbits = event.target.checked;
});
element("motion-toggle").setAttribute(
  "aria-pressed",
  String(scene.motionPaused),
);
element("motion-toggle").textContent = scene.motionPaused
  ? "Resume visual motion"
  : "Pause visual motion";
element("motion-toggle").addEventListener("click", (event) => {
  scene.motionPaused = !scene.motionPaused;
  event.currentTarget.setAttribute("aria-pressed", String(scene.motionPaused));
  event.currentTarget.textContent = scene.motionPaused
    ? "Resume visual motion"
    : "Pause visual motion";
});
element("slope-button").addEventListener("click", () => openOperation("slope"));

document.querySelectorAll("[data-camera]").forEach((button) => {
  button.addEventListener("click", () =>
    scene.moveCamera(button.dataset.camera),
  );
});
element("auto-orbit").addEventListener("click", (event) => {
  scene.autoOrbit = !scene.autoOrbit;
  if (scene.autoOrbit && scene.motionPaused) element("motion-toggle").click();
  event.currentTarget.setAttribute("aria-pressed", String(scene.autoOrbit));
});
element("close-dialog").addEventListener("click", () =>
  operationDialog.close(),
);
element("cancel-dialog").addEventListener("click", () =>
  operationDialog.close(),
);
element("help-button").addEventListener("click", () =>
  element("manual-dialog").showModal(),
);
element("close-manual").addEventListener("click", () =>
  element("manual-dialog").close(),
);
window.addEventListener("pagehide", (event) => {
  stopSimulation();
  if (event.persisted) scene.suspend();
  else scene.destroy();
});
window.addEventListener("pageshow", (event) => {
  if (!event.persisted) return;
  scene.resume();
  refreshNetwork();
});
refreshNetwork();
