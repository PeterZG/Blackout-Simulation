
# Blackout Simulation: Assignment 1

This project simulates the interaction of devices and satellites in a networked environment, showcasing Object-Oriented Programming (OOP) principles and robust software design. The simulation supports file transfers, entity movement, and dynamic range calculations.

## Features

1. **Device and Satellite Management**:
   - Models devices (e.g., handheld, desktop) and satellites (e.g., relay, standard).
   - Handles dynamic movement and state updates over time.

2. **File Transfer Simulation**:
   - Simulates file transfers between devices via satellites.
   - Tracks transfer progress and dynamically updates entity states.

3. **Time Progression**:
   - Updates the simulation with each time step, reflecting changes in movement, transfers, and availability.

4. **Extensibility**:
   - Easily extendable to include new device or satellite types.

## Design Principles

1. **Modularity**:
   - Core logic is encapsulated in modules like `BlackoutController` and `Transfer`.

2. **Encapsulation**:
   - Private attributes and public methods ensure data integrity.

3. **Separation of Concerns**:
   - Separate classes handle simulation control, entity management, and utilities.

4. **Error Handling**:
   - Implements custom exceptions (e.g., `FileTransferException`) for robust error management.

## Technology Stack

### Backend:
- **Language**: Java
- **Framework**: Spark (lightweight HTTP framework for handling simulation requests)
- **JSON Serialization**: Gson (to handle JSON conversions)
- **Design Patterns**:
  - Controller Pattern: `BlackoutController` manages simulation.
  - Custom Exception Pattern: `FileTransferException` handles specific errors.

### Frontend:
- Not explicitly included; interactions are simulated via HTTP requests or CLI.

## File Structure

```
src/
├── main/
│   ├── java/
│   │   ├── scintilla/          # Utility classes and helpers
│   │   ├── unsw/               # Core functionality
│   │   │   ├── App.java        # Entry point for the simulation
│   │   │   ├── blackout/       # Main simulation logic (e.g., BlackoutController)
│   │   │   ├── item/           # File and resource management
│   │   │   ├── response/       # Response models and handlers
│   │   │   ├── utils/          # Helper methods (e.g., angle calculations)
│   ├── resources/              # Static resources or configurations
├── test/
│   ├── blackout/               # Unit tests for simulation components
```

## Usage

### Compile
Use Gradle to compile the project:
```bash
./gradlew build
```

### Run Simulation
Run the simulation via Spark server:
```bash
./gradlew run
```

### Testing
Run the test suite to verify implementation:
```bash
./gradlew test
```

## Example Simulation
Example HTTP requests to interact with the simulation:
```bash
# Create a new device
curl -X POST -d '{"device": "handheld", "id": "D1"}' http://localhost:4567/device

# Simulate time progression
curl -X POST -d '{"time": 10}' http://localhost:4567/progress

# Get the current state of all entities
curl -X GET http://localhost:4567/state
```

## Future Improvements

1. **Enhanced Frontend**:
   - Add a web-based interface for real-time interaction with the simulation.

2. **Optimized Algorithms**:
   - Improve file transfer logic for better scalability.

3. **Increased Test Coverage**:
   - Add more edge case tests to validate robustness.

## Acknowledgments

This project was developed as part of COMP2511 Assignment 1 at UNSW.
