# SpigotX 🚀

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Spigot](https://img.shields.io/badge/Spigot-1.8%2B-brightgreen.svg)](https://www.spigotmc.org/)
[![JitPack](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)

> A modern, lightweight utility library for Spigot plugin development that eliminates boilerplate code and provides elegant builder patterns for commands, GUIs, and events.

## 🚀 Overview

SpigotX transforms complex Spigot plugin development into simple, readable code. It offers a suite of tools to streamline command creation, GUI management, and event handling, making it easier for developers to build robust and maintainable plugins.

### Key Features
- **Command System**: Three flexible ways to create commands.
- **GUI Framework**: Easy creation of inventory GUIs with pagination and auto-updates.
- **Event Handling**: Simplified event management with customizable filters and priorities.

### Who This Project Is For
- Spigot plugin developers looking to simplify their codebase.
- Developers who want to reduce boilerplate code and improve code readability.
- Anyone interested in contributing to a modern, lightweight utility library for Spigot.

## ✨ Features

### 🎯 **Command System**
- **Builder Pattern**: Fluent API for simple commands.
- **Annotations**: Class-based organization for complex plugins.
- **Direct Registration**: Lambda-style for quick prototyping.

### 🖼️ **GUI Framework**
- **Easy Creation**: Builder pattern for inventory GUIs.
- **Pagination**: Built-in support for multi-page inventories.
- **Auto-Updates**: Dynamic content with automatic refresh.
- **Event Handling**: Simple event handling for GUI interactions.

### 📈 **Event Handling**
- **Custom Filters**: Filter events based on player, world, or other criteria.
- **Priority Management**: Set event priorities to control execution order.
- **Cancellation Handling**: Handle event cancellations gracefully.

## 🛠️ Tech Stack

- **Programming Language**: Java
- **Frameworks**: Spigot API
- **Tools**: Maven, IntelliJ IDEA, Eclipse, NetBeans, VS Code

## 📦 Installation

### Prerequisites
- Java 8 or later
- Maven or Gradle
- Spigot server

### Quick Start

#### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.AdamTroyan</groupId>
        <artifactId>SpigotX</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

#### Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.AdamTroyan:SpigotX:1.0.0'
}
```

### Alternative Installation Methods
- **Docker**: Use Docker to set up a development environment.
- **Development Setup**: Clone the repository and build using Maven or Gradle.

## 🎯 Usage

### Basic Usage
```java
public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Initialize SpigotX
        SpigotX.initialize(this);

        // Create a command in one line
        SpigotX.registerCommand("hello", (sender, args) -> {
            sender.sendMessage("§aHello from SpigotX!");
            return true;
        });

        // Create a GUI with builder pattern
        GUI gui = GUIBuilder.create()
            .title("§6My GUI")
            .size(27)
            .item(13, new ItemStack(Material.DIAMOND))
            .build();
    }
}
```

### Advanced Usage
- **Custom Commands**: Use annotations to create complex command structures.
- **Advanced GUI**: Create multi-page GUIs with custom animations and event handling.
- **Event Filters**: Filter events based on specific criteria.

## 📁 Project Structure
```
SpigotX/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/adam/
│   │   │       ├── commands/
│   │   │       ├── events/
│   │   │       ├── gui/
│   │   │       ├── placeholders/
│   │   │       ├── scheduler/
│   │   │       ├── SpigotX.java
│   │   │       └── utils/
│   │   └── resources/
│   └── test/
│       └── java/
│           └── dev/adam/
├── .gitignore
├── pom.xml
└── README.md
```

## 🔧 Configuration

### Environment Variables
- Set environment variables for custom configurations.

### Configuration Files
- Use `config.yml` for plugin-specific configurations.

### Customization Options
- Customize the behavior of commands, GUIs, and events through configuration files and annotations.

## 🤝 Contributing

### How to Contribute
- Fork the repository.
- Create a new branch for your feature or bug fix.
- Write clean, well-commented code.
- Submit a pull request.

### Development Setup
- Clone the repository.
- Build the project using Maven or Gradle.
- Run the tests to ensure everything works as expected.

### Code Style Guidelines
- Follow the Java coding conventions.
- Use meaningful variable and method names.
- Keep the code DRY (Don't Repeat Yourself).

### Pull Request Process
- Ensure your code is well-tested.
- Provide clear and concise commit messages.
- Address any feedback from the maintainers.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors & Contributors

- **Adam Troyan**: Maintainer and primary developer.
- **Contributors**: List of contributors who have helped improve the project.

## 🐛 Issues & Support

### How to Report Issues
- Create a new issue on the GitHub repository.
- Provide a clear and concise description of the issue.
- Include any relevant code snippets or screenshots.

### Where to Get Help
- Join the Spigot community forums.
- Ask questions on Stack Overflow.
- Contact the maintainers directly.

## 🗺️ Roadmap

### Planned Features
- Add support for more Spigot API features.
- Improve GUI customization options.
- Enhance event handling capabilities.

### Known Issues
- List any known issues and their status.

### Future Improvements
- Add more advanced features based on community feedback.
- Improve documentation and examples.
- Enhance performance and stability.

---

**Additional Guidelines:**
- Use modern markdown features (badges, collapsible sections, etc.)
- Include practical, working code examples
- Make it visually appealing with appropriate emojis
- Ensure all code snippets are syntactically correct for Java
- Include relevant badges (build status, version, license, etc.)
- Make installation instructions copy-pasteable
- Focus on clarity and developer experience
