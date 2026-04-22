const API_BASE = 'http://localhost:8080/api';

async function apiGet(url) {
    try {
        const res = await fetch(`${API_BASE}${url}`);
        return await res.json();
    } catch (e) {
        console.error('API Error:', e);
        return null;
    }
}

async function apiPost(url, data) {
    try {
        const res = await fetch(`${API_BASE}${url}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await res.json();
    } catch (e) {
        console.error('API Error:', e);
        return null;
    }
}

// ==================== FEFO ====================
async function renderFEFO() {
    const stats = await apiGet('/fefo/stats?warehouseId=WH001');
    if (!stats) return;
    
    document.getElementById('fefo-expired-count').textContent = stats.expired || 0;
    document.getElementById('fefo-expiring-count').textContent = stats.expiringSoon || 0;
    document.getElementById('fefo-total-count').textContent = stats.totalBatches || 0;

    // Get expiry alerts
    const alerts = await apiGet('/fefo/alerts?warehouseId=WH001&thresholdHours=72');
    const container = document.getElementById('fefo-expiring-list');
    
    if (!alerts || alerts.length === 0) {
        container.innerHTML = '<div class="empty-msg">No expiring items</div>';
        return;
    }

    let html = '';
    alerts.slice(0, 5).forEach(a => {
        const levelClass = a.alertLevel === 'CRITICAL' ? 'badge-danger' : 
                         a.alertLevel === 'HIGH' ? 'badge-warning' : 'badge-info';
        html += `<div class="alert-item">
            <span><strong>${a.productId}</strong> - ${a.productName}</span>
            <span class="badge ${levelClass}">${a.hoursRemaining}h left</span>
            <span style="font-size:11px">${a.quantity} units</span>
        </div>`;
    });
    container.innerHTML = html;
}

async function getFEFOPick() {
    const productId = document.getElementById('fefo-product-select')?.value;
    const qty = parseInt(document.getElementById('fefo-qty')?.value) || 1;
    
    const result = await apiGet(`/fefo/pick/${productId}?quantity=${qty}&warehouseId=WH001`);
    const container = document.getElementById('fefo-pick-result');
    
    if (!result) return;
    
    if (result.success) {
        let html = `<div class="alert alert-ok">✅ ${result.message}</div>
            <div class="table-wrap">
            <table>
                <thead><tr><th>Batch</th><th>Qty</th><th>Expires</th><th>Status</th><th>Days</th></tr></thead>
            <tbody>`;
        result.batches.forEach(b => {
            html += `<tr>
                <td><span class="order-id">${b.batchId}</span></td>
                <td>${b.quantity}</td>
                <td>${b.expiryDate}</td>
                <td><span class="badge badge-${b.status === 'CRITICAL' ? 'danger' : 'warning'}">${b.status}</span></td>
                <td>${b.daysUntilExpiry}</td>
            </tr>`;
        });
        html += '</tbody></table></div>';
        
        if (result.warnings && result.warnings.length > 0) {
            html += '<div class="alert alert-warning" style="margin-top:10px">';
            result.warnings.forEach(w => html += `<div>⚠️ ${w}</div>`);
            html += '</div>';
        }
        container.innerHTML = html;
    } else {
        container.innerHTML = `<div class="alert alert-critical">❌ ${result.message}</div>`;
    }
}

// ==================== REPLENISHMENT ====================
async function renderReplenishment() {
    const stats = await apiGet('/replenishment/stats?warehouseId=WH001');
    if (!stats) return;
    
    document.getElementById('rep-critical').textContent = stats.criticalStock || 0;
    document.getElementById('rep-pending').textContent = stats.needReorder || 0;

    const queue = await apiGet('/replenishment/queue?warehouseId=WH001');
    const container = document.getElementById('replenishment-queue');
    
    if (!queue || queue.length === 0) {
        container.innerHTML = '<div class="empty-msg">All stock levels healthy</div>';
        return;
    }

    let html = '<table><thead><tr><th>Product</th><th>Current</th><th>Reorder At</th><th>To Order</th><th>Days Left</th><th>Urgency</th><th>Action</th></tr></thead><tbody>';
    queue.slice(0, 10).forEach(item => {
        const urgencyClass = item.urgency === 'CRITICAL' ? 'badge-danger' :
                            item.urgency === 'HIGH' ? 'badge-danger' :
                            item.urgency === 'MEDIUM' ? 'badge-warning' : 'badge-info';
        html += `<tr>
            <td><span class="order-id">${item.productId}</span><br><small>${item.productName}</small></td>
            <td><strong>${item.currentStock}</strong></td>
            <td>${item.reorderPoint}</td>
            <td>${item.reorderQty}</td>
            <td>${item.daysUntilStockout}</td>
            <td><span class="badge ${urgencyClass}">${item.urgency}</span></td>
            <td><button class="btn-sm btn-primary">Reorder</button></td>
        </tr>`;
    });
    html += '</tbody></table>';
    container.innerHTML = html;
}

// ==================== SUBSTITUTION ====================
async function checkSubstitution() {
    const productId = document.getElementById('sub-product-select')?.value;
    if (!productId) return;

    const result = await apiGet(`/substitution/check/${productId}?quantity=1`);
    const container = document.getElementById('sub-check-result');
    
    if (!result) return;
    
    if (result.isAvailable) {
        container.innerHTML = '<div class="alert alert-ok">✅ Product is in stock</div>';
        return;
    }

    if (!result.substitutes || result.substitutes.length === 0) {
        container.innerHTML = '<div class="alert alert-critical">❌ No alternatives available</div>';
        return;
    }

    let html = `<div class="alert alert-warning">⚠️ Out of stock - ${result.message}</div>
        <div class="table-wrap">
        <table>
            <thead><tr><th>Alternative</th><th>Match</th><th>In Stock</th><th>Why?</th></tr></thead>
            <tbody>`;
    result.substitutes.forEach(s => {
        html += `<tr>
            <td><span class="order-id">${s.productId}</span><br><small>${s.productName}</small></td>
            <td><strong>${Math.round(s.similarity * 100)}%</strong></td>
            <td>${s.inStock ? '✅ Yes' : '❌ No'}</td>
            <td>${s.reason}</td>
        </tr>`;
    });
    html += '</tbody></table></div>';
    container.innerHTML = html;
}

async function renderSubstitution() {
    const stats = await apiGet('/substitution/stats?warehouseId=WH001');
    if (!stats) return;
    
    document.getElementById('sub-oos').textContent = stats.outOfStockCount || 0;
    document.getElementById('sub-with-sub').textContent = stats.withSubstitutes || 0;

    const outOfStock = await apiGet('/substitution/out-of-stock?warehouseId=WH001');
    const container = document.getElementById('sub-product-list');
    
    if (!outOfStock || outOfStock.length === 0) {
        container.innerHTML = '<div class="empty-msg">All products in stock</div>';
        return;
    }

    let html = '';
    outOfStock.slice(0, 8).forEach(s => {
        html += `<div class="alert-item">
            <span>${s.productName}</span>
            <span>→</span>
            <span><strong>${s.productId}</strong> (${Math.round(s.similarity * 100)}%)</span>
            ${s.inStock ? '<span class="badge badge-success">Available</span>' : '<span class="badge badge-danger">Out</span>'}
        </div>`;
    });
    container.innerHTML = html;
}

// ==================== SLA ====================
async function renderSLA() {
    const dashboard = await apiGet('/sla/dashboard');
    if (!dashboard) return;
    
    document.getElementById('sla-breached').textContent = dashboard.breached || 0;
    document.getElementById('sla-critical').textContent = dashboard.delayed || 0;
    document.getElementById('sla-at-risk').textContent = dashboard.atRisk || 0;
    document.getElementById('sla-on-track').textContent = dashboard.onTrack || 0;

    const orders = await apiGet('/sla/orders?warehouseId=WH001');
    const container = document.getElementById('sla-table');
    
    if (!orders || orders.length === 0) {
        container.innerHTML = '<tr><td colspan="5" class="empty-row">No active orders</td></tr>';
        return;
    }

    let html = '';
    orders.slice(0, 10).forEach(o => {
        const statusClass = o.slaStatus === 'BREACHED' ? 'badge-danger' :
                           o.slaStatus === 'DELAYED' ? 'badge-danger' :
                           o.slaStatus === 'AT_RISK' ? 'badge-warning' : 'badge-success';
        html += `<tr>
            <td><span class="order-id">${o.orderId}</span></td>
            <td><strong style="color:${o.slaScore < 50 ? '#EF4444' : '#10B981'}">${o.slaScore}</strong></td>
            <td>${o.hoursRemaining}h</td>
            <td><span class="badge ${statusClass}">${o.slaStatus}</span></td>
            <td>${o.recommendation}</td>
        </tr>`;
    });
    container.innerHTML = html;
}

// ==================== DELIVERY ====================
async function findNearestRider() {
    const x = parseFloat(document.getElementById('del-x').value) || 12.97;
    const y = parseFloat(document.getElementById('del-y').value) || 77.75;
    const orderId = document.getElementById('del-order-id')?.value || 'ORD001';
    
    const result = await apiGet(`/delivery/find-riders/${orderId}?limit=3`);
    const container = document.getElementById('del-nearest-result');
    
    if (!result || !result.bestRider) {
        container.innerHTML = '<div class="alert alert-warning">No riders found</div>';
        return;
    }

    let html = `<div class="alert alert-info">Best match: ${result.bestRider.riderName}</div>
        <div class="table-wrap">
        <table>
            <thead><tr><th>Rider</th><th>Distance</th><th>Status</th><th>Rating</th><th>ETA</th></tr></thead>
            <tbody>`;
    
    if (result.bestRider) {
        html += `<tr>
            <td><span class="order-id">${result.bestRider.riderId}</span> - ${result.bestRider.riderName}</td>
            <td>${result.bestRider.distanceKm} km</td>
            <td><span class="badge badge-${result.bestRider.status === 'AVAILABLE' ? 'success' : 'warning'}">${result.bestRider.status}</span></td>
            <td>⭐ ${result.bestRider.rating}</td>
            <td>${result.bestRider.estimatedPickup}</td>
        </tr>`;
    }
    
    result.alternatives?.slice(1).forEach(a => {
        html += `<tr>
            <td><span class="order-id">${a.riderId}</span> - ${a.riderName}</td>
            <td>${a.distanceKm} km</td>
            <td><span class="badge badge-${a.status === 'AVAILABLE' ? 'success' : 'warning'}">${a.status}</span></td>
            <td>⭐ ${a.rating}</td>
            <td>${a.estimatedPickup}</td>
        </tr>`;
    });
    
    html += '</tbody></table></div>';
    container.innerHTML = html;
}

async function renderDeliveryStats() {
    const stats = await apiGet('/delivery/dashboard?warehouseId=WH001');
    if (!stats) return;
    
    document.getElementById('del-total-riders').textContent = (stats.availableRiders || 0) + (stats.busyRiders || 0);
    document.getElementById('del-available').textContent = stats.availableRiders || 0;
    document.getElementById('del-active').textContent = stats.busyRiders || 0;
    document.getElementById('del-pending').textContent = stats.pendingAssignment || 0;
}

// ==================== INIT ====================
function initEngines() {
    const page = document.querySelector('.page.active')?.id?.replace('page-', '');
    
    if (page === 'fefo') renderFEFO();
    else if (page === 'replenishment') renderReplenishment();
    else if (page === 'sla') renderSLA();
    else if (page === 'delivery') renderDeliveryStats();
    else if (page === 'substitution') renderSubstitution();
}

document.addEventListener('DOMContentLoaded', () => {
    setTimeout(initEngines, 500);
});

document.querySelectorAll('[data-page]').forEach(btn => {
    btn.addEventListener('click', () => {
        setTimeout(() => {
            const page = btn.dataset.page;
            if (page === 'fefo') renderFEFO();
            else if (page === 'replenishment') renderReplenishment();
            else if (page === 'sla') renderSLA();
            else if (page === 'delivery') renderDeliveryStats();
            else if (page === 'substitution') renderSubstitution();
        }, 100);
    });
});