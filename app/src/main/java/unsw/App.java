package unsw;

import spark.Request;
import spark.Spark;
import unsw.blackout.BlackoutController;
import unsw.blackout.FileTransferException;
import unsw.response.models.EntityInfoResponse;
import unsw.utils.Angle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import scintilla.Scintilla;

public class App {
    private static synchronized BlackoutController getBlackoutController(Request request) {
        spark.Session session = request.session(true);
        session.maxInactiveInterval(3600);
        BlackoutController controller = session.attribute("simulation");
        if (controller == null) {
            controller = new BlackoutController();
            session.attribute("simulation", controller);
        }
        return controller;
    }

    public static void main(String[] args) throws Exception {
        Scintilla.initialize();
        GsonBuilder gsonBuilder = new GsonBuilder();

        JsonSerializer<Angle> serializer = (Angle angle, Type typeOfId, JsonSerializationContext context) -> {
            return new JsonPrimitive(angle.toRadians());
        };
        gsonBuilder.registerTypeAdapter(Angle.class, serializer);
        Gson gson = gsonBuilder.create();

        Spark.before("/api/*", (request, response) -> {
            response.type("application/json; charset=utf-8");
            String origin = request.headers("Origin");
            if (origin != null && !origin.equals(request.scheme() + "://" + request.host())) {
                Spark.halt(403, gson.toJson(Map.of("error", "Use the simulator from its own local address")));
            }
        });
        Spark.exception(IllegalArgumentException.class, (exception, request, response) -> {
            response.status(400);
            response.type("application/json; charset=utf-8");
            response.body(gson.toJson(Map.of("error", exception.getMessage() == null
                    ? "Check the request parameters" : exception.getMessage())));
        });

        Spark.get("/", (request, response) -> {
            response.redirect("/app/");
            return null;
        });

        Spark.put("/api/device/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                Angle position = readPosition(request);
                boolean isMoving = SimulationInput.movingDevice(request.queryParams("isMoving"));
                bc.createDevice(required(request, "deviceId"), required(request, "type"), position, isMoving);
                return "";
            }
        }, gson::toJson);

        Spark.delete("/api/device/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                String id = required(request, "deviceId");
                if (!bc.listDeviceIds().contains(id)) throw new IllegalArgumentException("Device not found");
                bc.removeDevice(id);
                return "";
            }
        }, gson::toJson);

        Spark.put("/api/satellite/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                Angle position = readPosition(request);
                double height = readRadius(request);
                bc.createSatellite(required(request, "satelliteId"), required(request, "type"), height, position);
                return "";
            }
        }, gson::toJson);

        Spark.delete("/api/satellite/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                String id = required(request, "satelliteId");
                if (!bc.listSatelliteIds().contains(id)) throw new IllegalArgumentException("Satellite not found");
                bc.removeSatellite(id);
                return "";
            }
        }, gson::toJson);

        Spark.get("/api/entity/info/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                EntityInfoResponse entity = bc.getInfo(required(request, "id"));
                if (entity == null) Spark.halt(404, gson.toJson(Map.of("error", "Entity not found")));
                return entity;
            }
        }, gson::toJson);

        Spark.post("/api/device/file/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                bc.addFileToDevice(required(request, "deviceId"), required(request, "fileName"), request.body());
                return "";
            }
        }, gson::toJson);

        Spark.put("/api/entity/", "application/json", (request, response) -> {
            BlackoutController controller = getBlackoutController(request);
            synchronized (controller) {
                String id = required(request, "id");
                EntityInfoResponse entity = controller.getInfo(id);
                if (entity == null) throw new IllegalArgumentException("Entity not found");
                double radius = entity.getType().endsWith("Satellite") ? readRadius(request) : entity.getHeight();
                controller.updateEntity(id, readPosition(request), radius);
                return "";
            }
        }, gson::toJson);

        Spark.put("/api/device/file/", "application/json", (request, response) -> {
            BlackoutController controller = getBlackoutController(request);
            synchronized (controller) {
                controller.updateDeviceFile(required(request, "deviceId"),
                        required(request, "fileName"), request.body());
                return "";
            }
        }, gson::toJson);

        Spark.delete("/api/entity/file/", "application/json", (request, response) -> {
            BlackoutController controller = getBlackoutController(request);
            synchronized (controller) {
                controller.removeFile(required(request, "id"), required(request, "fileName"));
                return "";
            }
        }, gson::toJson);

        registerReadAndTransferRoutes(gson);
        Scintilla.start();
    }

    private static void registerReadAndTransferRoutes(Gson gson) {
        Spark.get("/api/device/all/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                Map<String, EntityInfoResponse> result = new HashMap<>();
                bc.listDeviceIds().forEach(id -> result.put(id, bc.getInfo(id)));
                return result;
            }
        }, gson::toJson);

        Spark.get("/api/satellite/all/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                Map<String, EntityInfoResponse> result = new HashMap<>();
                bc.listSatelliteIds().forEach(id -> result.put(id, bc.getInfo(id)));
                return result;
            }
        }, gson::toJson);

        Spark.get("/api/entity/entitiesInRange/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                String id = required(request, "id");
                if (bc.getInfo(id) == null) throw new IllegalArgumentException("Entity not found");
                return bc.communicableEntitiesInRange(id).stream().map(e -> bc.getInfo(e))
                        .collect(Collectors.toList());
            }
        }, gson::toJson);

        Spark.post("/api/sendFile/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                try {
                    bc.sendFile(required(request, "fileName"), required(request, "fromId"),
                            required(request, "toId"));
                    return "";
                } catch (FileTransferException ex) {
                    response.status(409);
                    return Map.of("error", ex.getMessage());
                }
            }
        }, gson::toJson);

        Spark.post("/api/createSlope/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                bc.createSlope(Integer.parseInt(required(request, "startAngle")),
                        Integer.parseInt(required(request, "endAngle")),
                        Integer.parseInt(required(request, "gradient")));
                return "";
            }
        }, gson::toJson);

        Spark.post("/api/simulate/", "application/json", (request, response) -> {
            BlackoutController bc = getBlackoutController(request);
            synchronized (bc) {
                int n = SimulationInput.minutes(request.queryParams("n"));

                List<Map<String, EntityInfoResponse>> results = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    bc.simulate();

                    Map<String, EntityInfoResponse> result = new HashMap<>();
                    bc.listSatelliteIds().forEach(id -> result.put(id, bc.getInfo(id)));
                    bc.listDeviceIds().forEach(id -> result.put(id, bc.getInfo(id)));
                    results.add(result);
                }

                return results;
            }
        }, gson::toJson);

    }

    private static String required(Request request, String name) {
        String value = request.queryParams(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value.trim();
    }

    private static Angle readPosition(Request request) {
        return SimulationInput.position(required(request, "position"));
    }

    private static double readRadius(Request request) {
        return SimulationInput.orbitalRadius(required(request, "height"));
    }
}
