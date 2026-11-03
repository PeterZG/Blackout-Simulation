/**
 * The browser's only connection to the Java simulation.
 * Query names match the existing API; angles are sent in radians.
 */
export async function callSimulation(
  endpoint,
  { method = "GET", parameters = {}, content } = {},
) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  const query = new URLSearchParams(parameters);
  try {
    const response = await fetch(
      "/api/" + endpoint + (query.size ? "?" + query : ""),
      {
        method,
        signal: controller.signal,
        credentials: "same-origin",
        headers: {
          "Content-Type":
            content === undefined
              ? "application/json"
              : "text/plain;charset=UTF-8",
        },
        ...(content !== undefined ? { body: content } : {}),
      },
    );
    if (!response.ok) {
      const detail = await response.json().catch(() => null);
      throw new Error(
        detail?.error ||
          "The simulator could not complete this request (HTTP " +
            response.status +
            "). Check the entity details and try again.",
      );
    }
    const result = await response.json();
    // Older coursework servers may return a domain error inside a JSON string.
    if (typeof result === "string" && result.length > 0) {
      throw new Error(
        result.replace(/([a-z])([A-Z])/g, "$1 $2").replace(":", ": "),
      );
    }
    return result;
  } catch (error) {
    if (error.name === "AbortError")
      throw new Error(
        "The simulator took too long to respond. Please try again.",
      );
    if (error instanceof TypeError)
      throw new Error(
        "The local simulator is unavailable. Start the Java server, then refresh the network.",
      );
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

export async function loadNetwork() {
  // Sequential first requests share the same newly created server session.
  const devices = await callSimulation("device/all/");
  const satellites = await callSimulation("satellite/all/");
  return { ...devices, ...satellites };
}
