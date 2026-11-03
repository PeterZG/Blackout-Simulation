import * as THREE from "three";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";

const JUPITER_RADIUS_KM = 69911;
const DEFAULT_DIRECTION = new THREE.Vector3(3.4, 2.5, 4.2).normalize();

/** Freely orbitable 3D view. Only Java can move simulation entities. */
export class OrbitalScene {
  constructor(canvas, onSelect) {
    Object.assign(this, {
      canvas,
      onSelect,
      entities: {},
      markers: new Map(),
      showOrbits: true,
      autoOrbit: false,
      motionPaused: matchMedia("(prefers-reduced-motion: reduce)").matches,
    });
    this.world = new THREE.Scene();
    this.world.background = new THREE.Color("#070e18");
    this.camera = new THREE.PerspectiveCamera(38, 1, 0.05, 500);
    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    this.renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 2));
    this.renderer.outputColorSpace = THREE.SRGBColorSpace;
    this.controls = new OrbitControls(this.camera, canvas);
    Object.assign(this.controls, {
      enablePan: false,
      enableDamping: true,
      dampingFactor: 0.12,
      rotateSpeed: 0.65,
      minDistance: 2.5,
      maxDistance: 30,
      autoRotateSpeed: 0.6,
      minPolarAngle: 0.01,
      maxPolarAngle: Math.PI - 0.01,
    });
    this.raycaster = new THREE.Raycaster();
    this.labels = document.createElement("div");
    this.labels.className = "scene-labels";
    canvas.parentElement.append(this.labels);
    this.buildWorld();
    this.resetCamera();
    this.resizeObserver = new ResizeObserver(() => this.resize());
    this.resizeObserver.observe(canvas);
    this.onPointerDown = (event) => {
      this.pointerStart = { x: event.clientX, y: event.clientY };
    };
    this.onPointerUp = (event) => {
      if (!this.pointerStart) return;
      const distance = Math.hypot(
        event.clientX - this.pointerStart.x,
        event.clientY - this.pointerStart.y,
      );
      this.pointerStart = null;
      if (distance > 6) return; // Orbiting must not also select a marker.
      const bounds = canvas.getBoundingClientRect();
      this.raycaster.setFromCamera(
        new THREE.Vector2(
          ((event.clientX - bounds.left) / bounds.width) * 2 - 1,
          -((event.clientY - bounds.top) / bounds.height) * 2 + 1,
        ),
        this.camera,
      );
      const hit = this.raycaster.intersectObjects(
        [
          this.planet,
          ...[...this.markers.values()].map((marker) => marker.mesh),
        ],
        true,
      )[0];
      if (hit?.object.userData.entityId)
        this.onSelect(hit.object.userData.entityId);
    };
    this.onKeyDown = (event) => {
      const actions = {
        ArrowLeft: "left",
        ArrowRight: "right",
        ArrowUp: "up",
        ArrowDown: "down",
        "+": "in",
        "=": "in",
        "-": "out",
        Home: "reset",
      };
      if (actions[event.key]) {
        event.preventDefault();
        this.moveCamera(actions[event.key]);
      }
    };
    canvas.addEventListener("pointerdown", this.onPointerDown);
    canvas.addEventListener("pointerup", this.onPointerUp);
    canvas.addEventListener("keydown", this.onKeyDown);
    this.onContextLost = (event) => {
      event.preventDefault();
      document.getElementById("scene-status").hidden = false;
      document.getElementById("scene-status").textContent =
        "3D graphics paused. Reload to restore the view; network controls still work.";
    };
    canvas.addEventListener("webglcontextlost", this.onContextLost);
    canvas.dataset.renderer = "webgl";
    this.resume();
  }

  buildWorld() {
    this.world.add(new THREE.AmbientLight(0xc9dcff, 1.7));
    for (const [color, intensity, position] of [
      [0xffe6c7, 2.4, [4, 3, 5]],
      [0x7ca7dd, 0.7, [-4, 1, -3]],
    ]) {
      const light = new THREE.DirectionalLight(color, intensity);
      light.position.set(...position);
      this.world.add(light);
    }
    this.planetMaterial = new THREE.MeshStandardMaterial({
      color: 0xffffff,
      roughness: 1,
      metalness: 0,
    });
    this.planet = new THREE.Mesh(
      new THREE.SphereGeometry(1, 96, 64),
      this.planetMaterial,
    );
    this.world.add(this.planet);
    new THREE.TextureLoader().load(
      "assets/jupiter_uv.jpeg",
      (texture) => {
        if (this.disposed) {
          texture.dispose();
          return;
        }
        texture.colorSpace = THREE.SRGBColorSpace;
        texture.anisotropy = Math.min(
          8,
          this.renderer.capabilities.getMaxAnisotropy(),
        );
        this.planetMaterial.map = texture;
        this.planetMaterial.needsUpdate = true;
        this.canvas.dataset.texture = "loaded";
      },
      undefined,
      () => {
        document.getElementById("scene-status").hidden = false;
        document.getElementById("scene-status").textContent =
          "Jupiter texture unavailable. The 3D camera and network controls still work.";
      },
    );
    this.guides = new THREE.Group();
    this.links = new THREE.Group();
    this.world.add(this.guides, this.links);
    for (const radius of [1.3, 1.8, 2.3]) this.addOrbit(radius);
    const positions = [];
    for (let index = 0; index < 450; index++) {
      const angle = index * 2.399963;
      const elevation = Math.acos(1 - (2 * (index + 0.5)) / 450);
      positions.push(
        90 * Math.sin(elevation) * Math.cos(angle),
        90 * Math.cos(elevation),
        90 * Math.sin(elevation) * Math.sin(angle),
      );
    }
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute(
      "position",
      new THREE.Float32BufferAttribute(positions, 3),
    );
    this.world.add(
      new THREE.Points(
        geometry,
        new THREE.PointsMaterial({
          color: 0xadc9e8,
          size: 0.09,
          transparent: true,
          opacity: 0.65,
        }),
      ),
    );
  }

  addOrbit(radius) {
    const points = Array.from({ length: 161 }, (_, index) => {
      const angle = (index / 160) * Math.PI * 2;
      return new THREE.Vector3(
        Math.cos(angle) * radius,
        0,
        -Math.sin(angle) * radius,
      );
    });
    const ring = new THREE.Line(
      new THREE.BufferGeometry().setFromPoints(points),
      new THREE.LineDashedMaterial({
        color: 0x42688d,
        dashSize: 0.035,
        gapSize: 0.04,
        transparent: true,
        opacity: 0.7,
      }),
    );
    ring.computeLineDistances();
    this.guides.add(ring);
  }

  createMarker(entity) {
    const satellite = entity.type.endsWith("Satellite");
    const mesh = new THREE.Mesh(
      satellite
        ? new THREE.OctahedronGeometry(0.065)
        : new THREE.SphereGeometry(0.05, 16, 12),
      new THREE.MeshStandardMaterial({
        color: satellite ? 0xb1d0ff : 0xffc88b,
        emissive: satellite ? 0x27476e : 0x71491c,
        emissiveIntensity: 0.65,
        roughness: 0.45,
      }),
    );
    mesh.userData.entityId = entity.id;
    this.world.add(mesh);
    const label = document.createElement("button");
    label.type = "button";
    label.className = "scene-entity-label";
    label.textContent = entity.id;
    label.title = entity.id;
    label.setAttribute("aria-label", "Select " + entity.id + " in 3D view");
    label.addEventListener("click", () => this.onSelect(entity.id));
    this.labels.append(label);
    return { mesh, label };
  }

  update(entities, selectedId, connectedIds = []) {
    this.entities = entities;
    this.selectedId = selectedId;
    for (const [id, marker] of this.markers) {
      if (!entities[id]) {
        this.world.remove(marker.mesh);
        marker.mesh.geometry.dispose();
        marker.mesh.material.dispose();
        marker.label.remove();
        this.markers.delete(id);
      }
    }
    for (const entity of Object.values(entities)) {
      if (!this.markers.has(entity.id))
        this.markers.set(entity.id, this.createMarker(entity));
      const marker = this.markers.get(entity.id);
      const radius = entity.type.endsWith("Satellite")
        ? entity.height / JUPITER_RADIUS_KM
        : 1.025;
      marker.mesh.position.set(
        Math.cos(entity.position) * radius,
        0,
        -Math.sin(entity.position) * radius,
      );
      marker.label.classList.toggle("is-selected", entity.id === selectedId);
      marker.mesh.material.emissiveIntensity =
        entity.id === selectedId ? 1.6 : 0.65;
    }
    this.clearGroup(this.links);
    const selected = this.markers.get(selectedId);
    if (selected)
      for (const id of connectedIds) {
        const target = this.markers.get(id);
        if (!target) continue;
        this.links.add(
          new THREE.Line(
            new THREE.BufferGeometry().setFromPoints([
              selected.mesh.position,
              target.mesh.position,
            ]),
            new THREE.LineDashedMaterial({
              color: 0x93decf,
              dashSize: 0.06,
              gapSize: 0.045,
            }),
          ).computeLineDistances(),
        );
      }
    const extent = Math.max(
      2.3,
      ...Object.values(entities).map((entity) =>
        entity.type.endsWith("Satellite")
          ? entity.height / JUPITER_RADIUS_KM
          : 1,
      ),
    );
    if (extent !== this.extent) {
      this.extent = extent;
      this.controls.maxDistance = Math.max(30, extent * 8);
      this.camera.far = Math.max(500, extent * 15);
      this.camera.updateProjectionMatrix();
      this.resetCamera();
    }
  }

  clearGroup(group) {
    for (const child of [...group.children]) {
      group.remove(child);
      child.geometry.dispose();
      child.material.dispose();
    }
  }

  resetCamera(top = false) {
    const distance =
      Math.max(5.2, (this.extent || 2.3) * 2.8) *
      Math.max(1, 1 / this.camera.aspect);
    // Consume drag inertia before resetting, including on slow rendering devices.
    const dampingEnabled = this.controls.enableDamping;
    this.controls.enableDamping = false;
    this.controls.reset();
    this.controls.target.set(0, 0, 0);
    this.camera.position.copy(
      top ? new THREE.Vector3(0, 1, 0.001) : DEFAULT_DIRECTION,
    );
    this.camera.position.normalize().multiplyScalar(distance);
    this.controls.update();
    this.controls.enableDamping = dampingEnabled;
  }

  moveCamera(action) {
    if (action === "reset" || action === "top") {
      this.resetCamera(action === "top");
    } else {
      // Explicit controls make camera movements available without drag gestures.
      const position = new THREE.Spherical().setFromVector3(
        this.camera.position,
      );
      if (action === "left") position.theta -= Math.PI / 4;
      if (action === "right") position.theta += Math.PI / 4;
      if (action === "up") position.phi -= Math.PI / 8;
      if (action === "down") position.phi += Math.PI / 8;
      if (action === "in") position.radius *= 0.8;
      if (action === "out") position.radius *= 1.25;
      position.phi = THREE.MathUtils.clamp(position.phi, 0.01, Math.PI - 0.01);
      position.radius = THREE.MathUtils.clamp(
        position.radius,
        this.controls.minDistance,
        this.controls.maxDistance,
      );
      this.camera.position.setFromSpherical(position);
      this.controls.update();
    }
  }

  resize() {
    const { width, height } = this.canvas.getBoundingClientRect();
    // Layout can briefly report zero during reflow/zoom. Never multiply the
    // camera by Infinity and poison OrbitControls' spherical coordinates.
    if (width < 1 || height < 1 || !Number.isFinite(width / height)) return;
    this.width = width;
    this.height = height;
    this.renderer.setSize(width, height, false);
    const previousAspect = this.camera.aspect > 0 ? this.camera.aspect : 1;
    this.camera.aspect = width / Math.max(1, height);
    this.camera.position.multiplyScalar(
      Math.max(1, 1 / this.camera.aspect) / Math.max(1, 1 / previousAspect),
    );
    this.camera.updateProjectionMatrix();
    this.canvas.dataset.size = width.toFixed(1) + "," + height.toFixed(1);
  }

  placeLabels() {
    const occupied = [];
    const markers = [...this.markers.entries()].sort(
      ([a], [b]) =>
        Number(b === this.selectedId) - Number(a === this.selectedId),
    );
    for (const [, marker] of markers) {
      const projected = marker.mesh.position.clone().project(this.camera);
      const x = ((projected.x + 1) * this.width) / 2;
      const y = ((1 - projected.y) * this.height) / 2;
      const direction = marker.mesh.position.clone().sub(this.camera.position);
      const distance = direction.length();
      this.raycaster.set(this.camera.position, direction.normalize());
      const planetHit = this.raycaster.intersectObject(this.planet)[0];
      marker.label.hidden =
        projected.z > 1 || (planetHit && planetHit.distance < distance - 0.1);
      if (marker.label.hidden) continue;
      const width = marker.label.offsetWidth;
      const height = marker.label.offsetHeight;
      let found = false;
      for (const offset of [-height - 14, 14, -height * 2 - 20, height + 20]) {
        const left = Math.max(
          8,
          Math.min(this.width - width - 8, x - width / 2),
        );
        const top = y + offset;
        const box = { left, top, right: left + width, bottom: top + height };
        if (top < 45 || box.bottom > this.height - 8) continue;
        if (
          occupied.some(
            (other) =>
              box.left < other.right + 5 &&
              box.right > other.left - 5 &&
              box.top < other.bottom + 5 &&
              box.bottom > other.top - 5,
          )
        )
          continue;
        marker.label.style.transform =
          "translate(" + left + "px, " + top + "px)";
        occupied.push(box);
        found = true;
        break;
      }
      marker.label.hidden = !found;
    }
  }

  draw(time) {
    const seconds = Math.min(0.05, (time - (this.lastTime ?? time)) / 1000);
    this.lastTime = time;
    if (!this.motionPaused) this.planet.rotation.y += seconds * 0.045;
    this.guides.visible = this.showOrbits;
    this.controls.autoRotate = this.autoOrbit && !this.motionPaused;
    this.controls.update(seconds);
    this.renderer.render(this.world, this.camera);
    this.placeLabels();
    // Read-only geometry for browser assertions, not an alternate camera controller.
    this.canvas.dataset.camera = this.camera.position
      .toArray()
      .map((number) => number.toFixed(4))
      .join(",");
    this.canvas.dataset.rotation = this.planet.rotation.y.toFixed(5);
  }

  suspend() {
    this.renderer.setAnimationLoop(null);
  }

  resume() {
    if (this.disposed) return;
    // A page restored from the back-forward cache keeps its existing canvas,
    // camera and GPU resources. Restart only the rendering loop.
    this.lastTime = undefined;
    this.resize();
    this.renderer.setAnimationLoop((time) => this.draw(time));
  }

  destroy() {
    if (this.disposed) return;
    this.disposed = true;
    this.suspend();
    this.resizeObserver.disconnect();
    this.controls.dispose();
    this.canvas.removeEventListener("pointerdown", this.onPointerDown);
    this.canvas.removeEventListener("pointerup", this.onPointerUp);
    this.canvas.removeEventListener("keydown", this.onKeyDown);
    this.canvas.removeEventListener("webglcontextlost", this.onContextLost);
    this.world.traverse((object) => {
      object.geometry?.dispose();
      object.material?.map?.dispose();
      object.material?.dispose();
    });
    this.labels.remove();
    this.renderer.dispose();
  }
}
