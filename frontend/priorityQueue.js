// ============================================================
// PRIORITY QUEUE (Max-Heap for Orders)
// Orders sorted by: priority score = urgency + deadline proximity
// ============================================================

class PriorityQueue {
  constructor(comparator = (a, b) => a.score - b.score) {
    this.heap = [];
    this.comparator = comparator; // min-heap by default (lowest score = highest priority)
  }

  enqueue(item) {
    this.heap.push(item);
    this._bubbleUp(this.heap.length - 1);
  }

  dequeue() {
    if (this.isEmpty()) return null;
    if (this.heap.length === 1) return this.heap.pop();
    const top = this.heap[0];
    this.heap[0] = this.heap.pop();
    this._sinkDown(0);
    return top;
  }

  peek() {
    return this.heap[0] || null;
  }

  isEmpty() { return this.heap.length === 0; }
  size() { return this.heap.length; }

  // Get all items sorted by priority (non-destructive)
  getSorted() {
    return [...this.heap].sort(this.comparator);
  }

  _bubbleUp(i) {
    while (i > 0) {
      const parent = Math.floor((i - 1) / 2);
      if (this.comparator(this.heap[parent], this.heap[i]) <= 0) break;
      [this.heap[parent], this.heap[i]] = [this.heap[i], this.heap[parent]];
      i = parent;
    }
  }

  _sinkDown(i) {
    const n = this.heap.length;
    while (true) {
      let smallest = i;
      const l = 2 * i + 1, r = 2 * i + 2;
      if (l < n && this.comparator(this.heap[l], this.heap[smallest]) < 0) smallest = l;
      if (r < n && this.comparator(this.heap[r], this.heap[smallest]) < 0) smallest = r;
      if (smallest === i) break;
      [this.heap[smallest], this.heap[i]] = [this.heap[i], this.heap[smallest]];
      i = smallest;
    }
  }
}

// ============================================================
// DELIVERY SCHEDULER
// Uses Priority Queue to sort orders by deadline + priority
// ============================================================
class DeliveryScheduler {
  constructor() {
    // Lower score = more urgent
    this.queue = new PriorityQueue((a, b) => a.score - b.score);
  }

  // Calculate priority score: lower = deliver first
  _calcScore(order) {
    const now = Date.now();
    const deadline = new Date(order.deadline).getTime();
    const timeLeft = Math.max(0, deadline - now) / (1000 * 60 * 60); // hours

    const PRIORITY_WEIGHT = { HIGH: 0, MEDIUM: 10, LOW: 20 };
    const priorityPenalty = PRIORITY_WEIGHT[order.priority] || 10;

    // Urgent + high-priority = low score = dequeued first
    return timeLeft + priorityPenalty;
  }

  addOrder(order) {
    const score = this._calcScore(order);
    this.queue.enqueue({ ...order, score });
  }

  nextOrder() {
    return this.queue.dequeue();
  }

  getSchedule() {
    return this.queue.getSorted().map((o, idx) => ({
      rank: idx + 1,
      ...o
    }));
  }

  reschedule(orders) {
    this.queue = new PriorityQueue((a, b) => a.score - b.score);
    orders.forEach(o => this.addOrder(o));
  }

  size() { return this.queue.size(); }
}
