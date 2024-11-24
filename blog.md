Several key decisions based on Object-Oriented Programming (OOP) principles and design principles.

1. **Choice of Inheritance vs. Composition**:
   - I opted for inheritance where it was clear that classes share common behavior and properties, such as `Device` and its subclasses (`HandheldDevice`, `DesktopDevice`, `LaptopDevice`). This decision was made to promote code reuse and create a hierarchy that reflects the real-world relationships betIen these entities.

2. **Encapsulation**:
   - All classes use private member variables with public getter and setter methods to control access to the class state. This decision was made to protect the integrity of the data and to ensure that objects are in a valid state at all times.

3. **Single Responsibility Principle**:
   - Each class has a clear responsibility. For example, `BlackoutController` manages the simulation state and interactions between entities, while `Entity`, `Device`, and `Satellite` classes manage their own properties and behaviors. This decision simplifies the system by ensuring that each class has one reason to change.

4. **Open/Closed Principle**:
   - The system is designed to be open for extension but closed for modification. New entity types can be added without altering existing code, such as adding a new `Satellite` subclass. This decision was made to facilitate easy expansion of the system.

5. **Separation of Concerns**:
   - The simulation logic is separated from the entity management. For example, the `simulate` method in `BlackoutController` handles time progression, while entity movement is handled within the `Entity` class. This separation makes the code easier to understand and maintain.

6. **Modularity**:
   - The system is divided into modules, such as file management, entity movement, and simulation control. This decision was made to organize the code into logical units that can be developed and tested independently.

7. **Scalability Considerations**:
   - The design allows for an increasing number of entities and file transfers. I made decisions with scalability in mind, such as using dynamic data structures for storing entities and files.
