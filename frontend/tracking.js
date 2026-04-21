// ============================================================
// TRACKING MODULE
// Simulates vehicle movement along routes
// Real-time position updates via setInterval
// ============================================================

const TrackingModule = (() => {
  const STORE_KEY = 'scs_tracking';
  const UPDATE_INTERVAL = 1200; // ms per step

  let _activeTrackers = new Map(); // orderId -> { interval, state }

  // ── Tracker State ─────────────────────────────────────────

  function startTracking(orderId, route, onUpdate, onComplete) {
    if (_activeTrackers.has(orderId)) stopTracking(orderId);

    const state = {
      orderId,
      route,           // array of node IDs
      currentStep: 0,
      totalSteps: route.length - 1,
      status: 'IN_TRANSIT',
      startTime: Date.now(),
      positions: [],
      speed: 1 // steps per interval
    };

    const interval = setInterval(() => {
      if (state.currentStep >= state.totalSteps) {
        clearInterval(interval);
        state.status = 'DELIVERED';
        _save(orderId, state);
        if (typeof onComplete === 'function') onComplete(state);
        _activeTrackers.delete(orderId);
        return;
      }

      state.currentStep += state.speed;
      state.currentStep = Math.min(state.currentStep, state.totalSteps);

      const currentNode = route[state.currentStep];
      const nextNode = route[Math.min(state.currentStep + 1, state.totalSteps)];
      const progress = Math.round((state.currentStep / state.totalSteps) * 100);

      state.currentPosition = currentNode;
      state.nextPosition = nextNode;
      state.progress = progress;
      state.eta = estimateETA(state.totalSteps - state.currentStep, UPDATE_INTERVAL);

      _save(orderId, state);
      if (typeof onUpdate === 'function') onUpdate({ ...state });
    }, UPDATE_INTERVAL);

    _activeTrackers.set(orderId, { interval, state });
    return state;
  }

  function stopTracking(orderId) {
    const tracker = _activeTrackers.get(orderId);
    if (tracker) {
      clearInterval(tracker.interval);
      _activeTrackers.delete(orderId);
    }
  }

  function pauseTracking(orderId) {
    const tracker = _activeTrackers.get(orderId);
    if (tracker) {
      clearInterval(tracker.interval);
      tracker.state.status = 'PAUSED';
    }
  }

  function getPosition(orderId) {
    const raw = localStorage.getItem(STORE_KEY + '_' + orderId);
    return raw ? JSON.parse(raw) : null;
  }

  function _save(orderId, state) {
    localStorage.setItem(STORE_KEY + '_' + orderId,
      JSON.stringify({ ...state, savedAt: Date.now() }));
  }

  function estimateETA(stepsLeft, intervalMs) {
    const msLeft = stepsLeft * intervalMs;
    const mins = Math.round(msLeft / 60000);
    return mins < 1 ? '< 1 min' : `${mins} min`;
  }

  // ── Vehicle Fleet ─────────────────────────────────────────

  const VEHICLES_KEY = 'scs_vehicles';

  function getVehicles() {
    const raw = localStorage.getItem(VEHICLES_KEY);
    return raw ? JSON.parse(raw) : [];
  }

  function saveVehicles(vehicles) {
    localStorage.setItem(VEHICLES_KEY, JSON.stringify(vehicles));
  }

  function getVehicleById(id) {
    return getVehicles().find(v => v.id === id) || null;
  }

  function updateVehicle(id, changes) {
    const vehicles = getVehicles();
    const idx = vehicles.findIndex(v => v.id === id);
    if (idx === -1) return;
    vehicles[idx] = { ...vehicles[idx], ...changes };
    saveVehicles(vehicles);
    return vehicles[idx];
  }

  // ── Delivery Agents ───────────────────────────────────────

  const AGENTS_KEY = 'scs_agents';

  function getAgents() {
    const raw = localStorage.getItem(AGENTS_KEY);
    return raw ? JSON.parse(raw) : [];
  }

  function saveAgents(agents) {
    localStorage.setItem(AGENTS_KEY, JSON.stringify(agents));
  }

  function setAgentAvailable(agentId, available) {
    const agents = getAgents();
    const idx = agents.findIndex(a => a.id === agentId);
    if (idx !== -1) {
      agents[idx].available = available;
      saveAgents(agents);
    }
  }

  // Seed vehicles & agents
  function seed() {
    if (getVehicles().length === 0) {
      saveVehicles([
        { id: 'V001', name: 'Truck Alpha', type: 'truck', weightCapacity: 1000, volumeCapacity: 500, available: true, currentNode: 'W1', fuel: 100 },
        { id: 'V002', name: 'Van Beta',    type: 'van',   weightCapacity: 500,  volumeCapacity: 250, available: true, currentNode: 'W1', fuel: 85  },
        { id: 'V003', name: 'Bike Gamma',  type: 'bike',  weightCapacity: 50,   volumeCapacity: 30,  available: false, currentNode: 'H1', fuel: 60 },
        { id: 'V004', name: 'Truck Delta', type: 'truck', weightCapacity: 800,  volumeCapacity: 400, available: true, currentNode: 'W2', fuel: 95  },
      ]);
    }

    if (getAgents().length === 0) {
      saveAgents([
        { id: 'A001', name: 'Ravi Kumar',   x: 120, y: 180, available: true,  rating: 4.8, deliveries: 142 },
        { id: 'A002', name: 'Sita Devi',    x: 350, y: 280, available: true,  rating: 4.6, deliveries: 98  },
        { id: 'A003', name: 'Mohan Singh',  x: 200, y: 150, available: false, rating: 4.9, deliveries: 201 },
        { id: 'A004', name: 'Lakshmi Nair', x: 400, y: 180, available: true,  rating: 4.7, deliveries: 77  },
        { id: 'A005', name: 'Vijay Reddy',  x: 300, y: 380, available: true,  rating: 4.5, deliveries: 55  },
      ]);
    }
  }

  return {
    startTracking, stopTracking, pauseTracking, getPosition,
    getVehicles, getVehicleById, updateVehicle,
    getAgents, saveAgents, setAgentAvailable,
    seed
  };
})();
