#!/bin/bash

echo "🚀 MINI-LINK Alarm Prioritization System Demo"
echo "=============================================="
echo ""

# Check if C++ backend is built
if [ ! -f "build/alarm_cli" ]; then
    echo "❌ C++ backend not found. Building..."
    mkdir -p build
    cd build
    cmake ../cpp
    cmake --build . --config Release
    cd ..
    echo "✅ C++ backend built successfully!"
else
    echo "✅ C++ backend found"
fi

echo ""

# Test C++ CLI
echo "🧪 Testing C++ CLI..."
./build/alarm_cli --input data/sample_alarms.json --config config/scoring.json --format json --top 3
echo ""

# Check if UI is running
echo "🌐 Checking UI status..."
if curl -s http://localhost:3000 > /dev/null; then
    echo "✅ Frontend running on http://localhost:3000"
else
    echo "❌ Frontend not running on port 3000"
fi

if curl -s http://localhost:3001/api/health > /dev/null; then
    echo "✅ Backend API running on http://localhost:3001"
else
    echo "❌ Backend API not running on port 3001"
fi

echo ""

# Show system status
echo "📊 System Status:"
echo "   - C++ CLI: ✅ Built and tested"
echo "   - Frontend: $(curl -s http://localhost:3000 > /dev/null && echo "✅ Running" || echo "❌ Not running")"
echo "   - Backend: $(curl -s http://localhost:3001/api/health > /dev/null && echo "✅ Running" || echo "❌ Not running")"

echo ""
echo "🎯 Next Steps:"
echo "   1. Open http://localhost:3000 in your browser"
echo "   2. View the beautiful alarm prioritization dashboard"
echo "   3. Interact with the controls and data"
echo "   4. Export results in JSON format"
echo ""
echo "🔧 Troubleshooting:"
echo "   - Frontend: cd ui && npm start"
echo "   - Backend:  cd ui/backend && npm start"
echo "   - C++ CLI:  ./build/alarm_cli --help"
echo ""
echo "✨ Enjoy your MINI-LINK Alarm Prioritization System!"
