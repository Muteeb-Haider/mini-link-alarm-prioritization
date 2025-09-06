# 🚀 MINI-LINK Intelligent Alarm Prioritization System

[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](https://www.docker.com/)
[![C++](https://img.shields.io/badge/C++-17-green?logo=cplusplus)](https://isocpp.org/)
[![React](https://img.shields.io/badge/React-18-blue?logo=react)](https://reactjs.org/)
[![Node.js](https://img.shields.io/badge/Node.js-18-green?logo=node.js)](https://nodejs.org/)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive, real-time alarm prioritization system for MINI-LINK network equipment. Features a modern React web dashboard, high-performance C++ prioritization engine, and comprehensive automated testing. Automatically categorizes and prioritizes network alarms based on severity, frequency, impact, and recency with live updates and professional network monitoring capabilities.

## 📋 Table of Contents

- [Features](#-features)
- [Quick Start](#-quick-start)
- [Docker Deployment](#-docker-deployment)
- [Architecture](#️-architecture)
- [API Documentation](#-api-documentation)
- [Configuration](#️-configuration)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features

### 🎯 Core Prioritization Engine
- **Deterministic priority scoring** using weighted factors:
  - Severity (Critical, Major, Minor, Warning, Info)
  - Frequency (occurrences per hour)
  - Impact (affected links and traffic impact percent)
  - Recency (exponential decay; half-life configurable)
  - Service-affecting flag
- **Configurable weights** via `config/scoring.json`
- **Multiple output formats**: JSON (machine-readable) and table (human-readable)

### 🌐 Modern Web Dashboard
- **Real-time React interface** with Material-UI components
- **Live data updates** every 5 seconds with toggle control
- **Professional network monitoring** appearance
- **Responsive design** for desktop, tablet, and mobile
- **Interactive controls** for Top-N filtering and real-time toggling

### 🔄 Real-Time Capabilities
- **Dynamic alarm generation** with realistic MINI-LINK descriptions
- **Live/Static mode toggle** for operational flexibility
- **8 realistic network nodes** (ALPHA-01, BETA-02, GAMMA-03, etc.)
- **40+ alarm types** covering all severity levels
- **Intelligent data aging** and memory management

### 🧪 Comprehensive Testing
- **5 comprehensive JUnit tests** covering all system aspects
- **Performance testing** with large datasets (100+ alarms)
- **Error handling validation** for edge cases
- **Format validation** for both JSON and table outputs
- **Scoring algorithm accuracy** testing

## 🚀 Quick Start

### Prerequisites

- **Docker** (recommended) or
- **Linux environment** with g++, CMake ≥ 3.16
- **OpenJDK 17+**, Maven 3.8+
- **Node.js 18+** and npm (for web dashboard)

### 🐳 Docker Deployment (Recommended)

The easiest way to run the complete system:

```bash
# Clone the repository
git clone https://github.com/yourusername/mini-link-alarm-prioritization.git
cd mini-link-alarm-prioritization

# Build and run with Docker Compose
docker-compose up --build

# Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:3001
```

### 🏗️ Local Development Setup

#### Build C++ Backend
```bash
mkdir -p build && cd build
cmake ../cpp
cmake --build . --config Release
./alarm_cli --input ../data/sample_alarms.json --config ../config/scoring.json --top 5 --format json
```

#### Start Web Dashboard
```bash
# Terminal 1: Start frontend
cd ui
npm install
npm start

# Terminal 2: Start backend API
cd ui/backend
npm install
npm start
```

#### Run Tests
```bash
# Run all JUnit tests
mvn -f java-tests/pom.xml test

# Test C++ CLI directly
./build/alarm_cli --input data/sample_alarms.json --config config/scoring.json --format table --top 3
```

## 🏗️ Architecture

### System Components
- **C++ Backend**: High-performance prioritization engine
- **React Frontend**: Modern, responsive web interface
- **Node.js API**: RESTful backend services
- **Java Tests**: Comprehensive validation suite
- **Docker**: Containerized deployment and testing

### Data Flow
1. **Alarm Input**: JSON alarm data from network equipment
2. **Prioritization**: C++ engine processes and scores alarms
3. **Real-time Updates**: Live data generation and updates
4. **Web Dashboard**: React interface displays prioritized results
5. **Export/Import**: Data exchange with external systems

## 📚 API Documentation

### Endpoints

#### Health Check
```http
GET /api/health
```
Returns system status and health information.

#### Get Configuration
```http
GET /api/config
```
Returns current scoring configuration.

#### Prioritize Alarms
```http
POST /api/prioritize
Content-Type: application/json

{
  "alarms": [...],
  "topN": 10,
  "config": "optional custom config"
}
```

#### Upload Alarm Data
```http
POST /api/upload
Content-Type: multipart/form-data

file: [JSON file with alarm data]
```

### Input Format

The system accepts JSON alarm data in the following format:

```json
{
  "id": "ALM-1001",
  "nodeId": "MINI-LINK-ALPHA-01",
  "severity": "Critical",
  "firstSeen": "2025-08-22T13:05:00Z",
  "lastSeen": "2025-08-23T09:30:00Z",
  "occurrencesPerHour": 8.5,
  "affectedLinks": 3,
  "trafficImpactPct": 45.0,
  "serviceAffecting": true,
  "description": "Link down on interface GE1/0/1"
}
```

### Output Format

Prioritized alarms are returned with additional scoring information:

```json
{
  "id": "ALM-1001",
  "nodeId": "MINI-LINK-ALPHA-01",
  "severity": "Critical",
  "score": 188.77,
  "rank": 1,
  "reason": "Service affecting; Critical severity; Traffic impact 85%; 8 links affected; 15.5 occ/hr; Recency factor 0.99424"
}
```

## ⚙️ Configuration

Weights and parameters are configured in `config/scoring.json`:

```json
{
  "severityWeights": {
    "Critical": 100.0,
    "Major": 70.0,
    "Minor": 40.0,
    "Warning": 20.0,
    "Info": 10.0
  },
  "alphaFrequency": 10.0,
  "betaImpact": 0.8,
  "gammaServiceAffectingBonus": 15.0,
  "recencyHalfLifeHours": 6.0,
  "maxFrequencyPerHour": 100.0,
  "maxAffectedLinks": 20,
  "maxTrafficImpactPct": 100.0
}
```

### Configuration Parameters

- **Severity weights**: Base scores for each severity level
- **Frequency alpha**: Multiplier for occurrences per hour
- **Impact beta**: Multiplier for traffic impact and affected links
- **Recency half-life**: Hours for exponential decay calculation
- **Service affecting bonus**: Additional score for service-impacting alarms
- **Normalization constants**: Maximum values for frequency, links, and traffic impact

## 🧪 Testing

### Test Suite Overview

The project includes comprehensive testing across all components:

- **5 JUnit test classes** covering all system aspects
- **Performance testing** with large datasets (100+ alarms)
- **Error handling validation** for edge cases
- **Format validation** for JSON and table outputs
- **Scoring algorithm accuracy** testing

### Running Tests

```bash
# Run all tests
mvn -f java-tests/pom.xml test

# Run specific test class
mvn -f java-tests/pom.xml test -Dtest=AlarmPrioritizerCLITest

# Run with verbose output
mvn -f java-tests/pom.xml test -X
```

### Test Coverage

- **CLI functionality**: Input/output validation
- **Scoring accuracy**: Algorithm correctness verification
- **Output formats**: JSON and table format testing
- **Error handling**: Invalid input and file handling
- **Performance**: Large dataset processing validation

## 🎯 Use Cases

### Network Operations Centers
- **Real-time monitoring** of MINI-LINK equipment
- **Priority-based alerting** for critical issues
- **Professional dashboard** for network engineers
- **Historical analysis** and trend identification

### Network Engineering
- **Alarm prioritization** for maintenance planning
- **Impact assessment** of network issues
- **Performance optimization** based on alarm patterns
- **Capacity planning** from traffic impact data

### Business Intelligence
- **Network health metrics** and KPIs
- **Trend analysis** of alarm patterns
- **Resource allocation** based on priority scores
- **Risk assessment** and mitigation planning

## 🔧 Troubleshooting

### Common Issues

**Frontend not loading**
```bash
cd ui && npm start
```

**Backend API down**
```bash
cd ui/backend && npm start
```

**C++ CLI errors**
```bash
cmake --build . --config Release
```

**Docker build failures**
```bash
# Clean up and rebuild
docker-compose down
docker-compose up --build --force-recreate
```

**Test failures**
```bash
# Ensure all dependencies are installed
mvn -f java-tests/pom.xml clean install
```

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow the existing code style
- Add tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built for MINI-LINK network equipment prioritization
- Uses modern C++17, React 18, and Node.js technologies
- Comprehensive testing with JUnit and Maven
- Docker containerization for easy deployment

---

**Made with ❤️ for network operations teams**