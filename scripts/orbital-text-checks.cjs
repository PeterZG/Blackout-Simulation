/* Check orbital controls and labels; review WebGL artwork separately. */
const assert = require("node:assert/strict");

function color(value) {
  const channels = value.match(/[\d.]+/g)?.map(Number);
  return value.startsWith("rgb") && channels?.length >= 3
    ? [...channels.slice(0, 3), channels[3] ?? 1] : null;
}
function mix(top, bottom) {
  return top.slice(0, 3).map((channel, index) => channel * top[3] + bottom[index] * (1 - top[3]));
}
function luminance(rgb) {
  const linear = rgb.map(channel => {
    const value = channel / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });
  return linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722;
}

async function checkReadability(page, label) {
  const samples = await page.evaluate(() => {
    const nodes = [...document.querySelectorAll("body *")];
    return nodes.filter(node => {
      const style = getComputedStyle(node);
      return node.getClientRects().length && style.visibility !== "hidden" &&
        !node.closest('.slide, svg, [aria-hidden="true"], [inert]') &&
        ([...node.childNodes].some(child => child.nodeType === Node.TEXT_NODE && child.textContent.trim()) ||
          (node.matches("input,textarea,select") && (node.value || node.placeholder)));
    }).map(node => {
      const style = getComputedStyle(node);
      const backgrounds = [];
      let translucentLayer = false;
      for (let ancestor = node; ancestor; ancestor = ancestor.parentElement) {
        const ancestorStyle = getComputedStyle(ancestor);
        backgrounds.unshift({ color: ancestorStyle.backgroundColor, image: ancestorStyle.backgroundImage });
        if (Number(ancestorStyle.opacity) < 1) translucentLayer = true;
      }
      const placeholder = !node.value && node.placeholder ? getComputedStyle(node, "::placeholder") : null;
      return {
        element: node.tagName.toLowerCase() + (node.id ? "#" + node.id : "") + "." + String(node.className).split(/\s+/).join(".") + " in ." + node.parentElement.className,
        size: parseFloat(style.fontSize), weight: Number(style.fontWeight),
        foreground: placeholder?.color || style.color, backgrounds,
        skipContrast: node.matches(":disabled") || translucentLayer,
      };
    });
  });
  const small = samples.filter(sample => sample.size > 0 && sample.size < 16);
  assert.deepEqual(small.map(({ element, size }) => ({ element, size })), [], label + ": interface text below 16 CSS px");
  const lowContrast = [];
  for (const sample of samples) {
    if (sample.skipContrast) continue;
    const foreground = color(sample.foreground);
    if (!foreground || foreground[3] === 0) continue;
    let backgrounds = [[255, 255, 255]];
    let imageBackground = false;
    for (const layer of sample.backgrounds) {
      const fill = color(layer.color);
      if (fill) backgrounds = backgrounds.map(background => mix(fill, background));
      const stops = (layer.image.match(/rgba?\([^)]*\)/g) || []).map(color).filter(Boolean);
      if (fill?.[3] === 1) imageBackground = false;
      if (layer.image !== "none" && !stops.length) imageBackground = true;
      if (stops.length) backgrounds = backgrounds.flatMap(background => stops.map(stop => mix(stop, background))).slice(0, 64);
    }
    if (imageBackground) continue; // Text over raster art still needs screenshot review.
    const required = sample.size >= 24 || (sample.size >= 18.66 && sample.weight >= 700) ? 3 : 4.5;
    const ratio = Math.min(...backgrounds.map(background => {
      const a = luminance(mix(foreground, background));
      const b = luminance(background);
      return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }));
    if (ratio + 0.02 < required) lowContrast.push({ element: sample.element, foreground: sample.foreground, backgrounds, ratio: Number(ratio.toFixed(2)), required });
  }
  assert.deepEqual(lowContrast, [], label + ": low contrast interface text");
}
module.exports = { checkReadability };
