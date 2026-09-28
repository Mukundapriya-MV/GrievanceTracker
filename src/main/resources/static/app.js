const views = ['dashboard', 'file', 'track', 'complaints', 'departments', 'categories'];
const statusFlow = ['PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
let categories = [];
let departments = [];
let grievances = [];
let escalations = [];
let knownEscalationIds = null;
let staffKey = '';
let toastTimer;

const request = async (path, options = {}) => {
    const method = (options.method || 'GET').toUpperCase();
    const staffWrite = ['POST', 'PUT', 'DELETE'].includes(method)
        && (/^\/(departments|categories)(\/|$)/.test(path)
            || /^\/grievances\/\d+\/status$/.test(path));
    if (staffWrite && !staffKey) {
        staffKey = window.prompt('Enter the server-configured staff access key') || '';
        if (!staffKey) throw new Error('A staff access key is required for this action.');
    }
    const response = await fetch(path, {
        ...options,
        headers: { 'Content-Type': 'application/json', ...(staffWrite ? { 'X-Staff-Key': staffKey } : {}), ...options.headers }
    });
    const result = response.status === 204 ? null : await response.json().catch(() => null);
    if (response.status === 403 && staffWrite) staffKey = '';
    if (!response.ok) throw new Error(result?.message || `Request failed (${response.status})`);
    return result;
};

const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
})[character]);
const statusName = (status) => String(status || '').replaceAll('_', ' ').toLowerCase();
const formatDate = (value) => value ? new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : 'Not set';
const setMessage = (id, message = '', error = false) => {
    const element = document.getElementById(id);
    if (!element) return;
    element.textContent = message;
    element.classList.toggle('error', error);
};
const severityName = (value) => value ? value.toLowerCase().replace(/^./, (letter) => letter.toUpperCase()) : 'Medium';

function showToast(message) {
    let toast = document.getElementById('app-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'app-toast';
        toast.className = 'app-toast';
        toast.setAttribute('role', 'status');
        toast.setAttribute('aria-live', 'polite');
        document.body.append(toast);
    }
    toast.textContent = message;
    toast.classList.add('visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove('visible'), 6500);
}

function navigate() {
    const requestedView = location.hash.slice(1);
    const activeView = views.includes(requestedView) ? requestedView : 'dashboard';
    document.querySelectorAll('.view').forEach((section) => {
        section.hidden = section.id !== `view-${activeView}`;
    });
    document.querySelectorAll('[data-nav]').forEach((link) => {
        if (link.dataset.nav === activeView) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    });
    if (!views.includes(requestedView)) history.replaceState(null, '', '#dashboard');
    if (activeView === 'dashboard') renderDashboard();
}

function setSelectOptions(select, items, placeholder, label) {
    const previous = select.value;
    select.innerHTML = `<option value="">${placeholder}</option>` + items.map((item) =>
        `<option value="${item.id}">${escapeHtml(label(item))}</option>`).join('');
    if (items.some((item) => String(item.id) === previous)) select.value = previous;
}

function refreshSelects() {
    setSelectOptions(document.getElementById('category'), categories, 'Choose an issue type', (item) => item.name);
    setSelectOptions(document.getElementById('category-department'), departments, 'Choose a department', (item) => item.name);
    setSelectOptions(document.getElementById('filter-category'), categories, 'All categories', (item) => item.name);
    setSelectOptions(document.getElementById('filter-department'), departments, 'All departments', (item) => item.name);
}

function slaFor(category, severity) {
    if (!category) return null;
    const field = `slaDays${severityName(severity)}`;
    return category[field] ?? category.slaDays ?? null;
}

function updateSlaPreview() {
    const category = categories.find((item) => String(item.id) === document.getElementById('category').value);
    const severity = document.getElementById('severity').value;
    const preview = document.getElementById('sla-preview');
    if (!category || !severity) {
        preview.hidden = true;
        return;
    }
    const days = slaFor(category, severity);
    preview.textContent = `SLA: ${days} ${days === 1 ? 'day' : 'days'} · ${category.department?.name || 'Department assigned automatically'}`;
    preview.hidden = false;
}

function categoryCard(category) {
    const days = `${category.slaDaysLow}/${category.slaDaysMedium}/${category.slaDaysHigh}/${category.slaDaysCritical}`;
    return `<article class="management-row">
        <div><strong>${escapeHtml(category.name)}</strong><p>${escapeHtml(category.department?.name || 'No department')} · SLA low / medium / high / critical: ${days} days</p></div>
        <div class="row-actions"><button class="button button-light" data-edit-category="${category.id}" type="button">Edit</button><button class="button button-danger" data-delete-category="${category.id}" type="button">Delete</button></div>
    </article>`;
}

function departmentCard(department) {
    return `<article class="management-row">
        <div><strong>${escapeHtml(department.name)}</strong><p>${escapeHtml(department.description || 'No description')}</p></div>
        <div class="row-actions"><button class="button button-light" data-edit-department="${department.id}" type="button">Edit</button><button class="button button-danger" data-delete-department="${department.id}" type="button">Delete</button></div>
    </article>`;
}

function renderManagement() {
    document.getElementById('department-list').innerHTML = departments.length
        ? departments.map(departmentCard).join('') : '<p class="empty-state">No departments yet.</p>';
    document.getElementById('category-list').innerHTML = categories.length
        ? categories.map(categoryCard).join('') : '<p class="empty-state">No categories yet.</p>';
    refreshSelects();
}

function renderEscalationAlert(escalation) {
    const grievance = escalation.grievance || {};
    return `<article class="alert-card">
        <div class="alert-heading"><span class="alert-mark">!</span><strong>SLA BREACH - COMPLAINT ESCALATED</strong></div>
        <p><strong>Complaint #${escapeHtml(grievance.id)}</strong> · ${escapeHtml(grievance.category?.name || 'Issue')} · ${escapeHtml(severityName(grievance.severity))}</p>
        <p>Escalated to ${escapeHtml(escalation.escalatedToOfficer)} · ${escapeHtml(escalation.reason)}</p>
        <time>${escapeHtml(formatDate(escalation.escalatedAt))}</time>
        <a href="#track" data-track-id="${escapeHtml(grievance.id)}">View complaint</a>
    </article>`;
}

function complaintCard(item, compact = false) {
    const escalated = escalations.some((entry) => entry.grievance?.id === item.id);
    const nextStatus = statusFlow[statusFlow.indexOf(item.status) + 1];
    const category = item.category?.name || 'Complaint';
    const department = item.department?.name || 'Unassigned';
    const statusClass = String(item.status || '').toLowerCase().replaceAll('_', '-');
    const action = compact ? `<a class="text-link" href="#track" data-track-id="${item.id}">Track</a>`
        : item.status === 'CLOSED'
            ? (item.rating == null
                ? `<select class="rating-select" data-rating="${item.id}" aria-label="Rating for complaint ${item.id}"><option value="">Rate 1-5</option>${[1, 2, 3, 4, 5].map((value) => `<option value="${value}">${value}</option>`).join('')}</select><button class="button button-dark" data-rate="${item.id}" type="button">Rate</button>`
                : `<span class="badge">Rated ${item.rating}/5</span>`)
            : `<button class="button button-dark" data-advance="${item.id}" type="button">Mark ${escapeHtml(statusName(nextStatus))}</button>`;
    return `<article class="complaint-row">
        <div class="complaint-main"><div class="complaint-topline"><span class="complaint-id">#${escapeHtml(item.id)}</span><span class="complaint-category">${escapeHtml(category)}</span><span class="badge ${statusClass}">${escapeHtml(statusName(item.status))}</span><span class="badge severity ${String(item.severity || 'MEDIUM').toLowerCase()}">${escapeHtml(severityName(item.severity))}</span>${escalated ? '<span class="badge escalated">Escalated</span>' : ''}</div>
        <p class="complaint-description">${escapeHtml(item.description)}</p><p class="complaint-meta">${escapeHtml(item.location)} · ${escapeHtml(department)} · SLA ${escapeHtml(item.slaDurationDays ?? item.slaDays ?? '?')} days · Due ${escapeHtml(formatDate(item.slaDeadline))}</p></div>
        <div class="complaint-actions">${action}</div>
    </article>`;
}

function filteredGrievances() {
    const categoryId = document.getElementById('filter-category').value;
    const departmentId = document.getElementById('filter-department').value;
    const severity = document.getElementById('filter-severity').value;
    const status = document.getElementById('filter-status').value;
    return grievances.filter((item) => (!categoryId || String(item.category?.id) === categoryId)
        && (!departmentId || String(item.department?.id) === departmentId)
        && (!severity || item.severity === severity)
        && (!status || item.status === status));
}

function renderComplaints() {
    const filtered = filteredGrievances();
    document.getElementById('complaints-list').innerHTML = filtered.length
        ? filtered.slice().reverse().map((item) => complaintCard(item)).join('')
        : '<p class="empty-state">No complaints match these filters.</p>';
}

function renderDashboard() {
    const open = grievances.filter((item) => !['RESOLVED', 'CLOSED'].includes(item.status)).length;
    const resolved = grievances.filter((item) => ['RESOLVED', 'CLOSED'].includes(item.status)).length;
    document.getElementById('stat-total').textContent = grievances.length;
    document.getElementById('stat-open').textContent = open;
    document.getElementById('stat-resolved').textContent = resolved;
    document.getElementById('stat-escalated').textContent = escalations.length;
    const recent = grievances.slice().reverse().slice(0, 5);
    document.getElementById('dashboard-recent').innerHTML = recent.length
        ? recent.map((item) => complaintCard(item, true)).join('')
        : '<p class="empty-state">No complaints have been filed yet.</p>';
    document.getElementById('dashboard-alerts').innerHTML = escalations.length
        ? escalations.slice().reverse().slice(0, 5).map(renderEscalationAlert).join('')
        : '<p class="empty-state">No SLA escalations. Alerts will appear here automatically.</p>';
}

function renderTracking(grievance) {
    const escalated = escalations.filter((entry) => entry.grievance?.id === grievance.id);
    const days = grievance.slaDurationDays ?? grievance.slaDays ?? '?';
    document.getElementById('tracking-result').classList.remove('empty-result');
    document.getElementById('tracking-result').innerHTML = `<div class="tracking-details">
        <div class="tracking-heading"><span class="complaint-id">COMPLAINT #${escapeHtml(grievance.id)}</span><span class="badge ${String(grievance.status).toLowerCase().replaceAll('_', '-')}">${escapeHtml(statusName(grievance.status))}</span><span class="badge severity ${String(grievance.severity || 'MEDIUM').toLowerCase()}">${escapeHtml(severityName(grievance.severity))}</span></div>
        <h3>${escapeHtml(grievance.category?.name || 'Complaint')}</h3><p>${escapeHtml(grievance.description)}</p>
        <dl><dt>Citizen</dt><dd>${escapeHtml(grievance.citizenName)}</dd><dt>Location</dt><dd>${escapeHtml(grievance.location)}</dd><dt>Department</dt><dd>${escapeHtml(grievance.department?.name || 'Unassigned')}</dd><dt>Severity</dt><dd>${escapeHtml(severityName(grievance.severity))}</dd><dt>SLA duration</dt><dd>${escapeHtml(days)} ${Number(days) === 1 ? 'day' : 'days'}</dd><dt>Deadline</dt><dd>${escapeHtml(formatDate(grievance.slaDeadline))}</dd><dt>Escalation</dt><dd>${escalated.length ? 'Escalated' : 'Not escalated'}</dd></dl>
        ${grievance.status === 'CLOSED' && grievance.rating == null ? `<div class="citizen-rating"><label for="track-rating">Rate this resolution</label><select id="track-rating"><option value="">Choose 1-5</option>${[1,2,3,4,5].map((value) => `<option value="${value}">${value} / 5</option>`).join('')}</select><button class="button button-dark" data-track-rate="${grievance.id}" type="button">Submit rating</button></div>` : ''}
    </div>`;
    document.getElementById('tracking-escalations').innerHTML = escalated.length
        ? escalated.slice().reverse().map(renderEscalationAlert).join('')
        : '<p class="empty-state">No escalation recorded for this complaint.</p>';
}

async function loadAll() {
    try {
        const results = await Promise.all([request('/categories'), request('/departments'), request('/grievances'), request('/escalations')]);
        categories = results[0];
        departments = results[1];
        grievances = results[2];
        escalations = results[3];
        refreshSelects();
        renderManagement();
        renderDashboard();
        renderComplaints();
        updateSlaPreview();
        const newAlerts = knownEscalationIds === null ? [] : escalations.filter((item) => !knownEscalationIds.has(item.id));
        knownEscalationIds = new Set(escalations.map((item) => item.id));
        if (newAlerts.length) {
            const newest = newAlerts[newAlerts.length - 1];
            showToast(`SLA breach: complaint #${newest.grievance?.id} escalated to ${newest.escalatedToOfficer}.`);
        }
    } catch (error) {
        showToast(`Could not refresh service data: ${error.message}`);
    }
}

async function trackComplaint(id) {
    try {
        const grievance = await request(`/grievances/${encodeURIComponent(id)}`);
        const allEscalations = await request('/escalations');
        escalations = allEscalations;
        renderTracking(grievance);
        document.getElementById('tracking-id').value = grievance.id;
        setMessage('tracking-message', 'Complaint found.');
    } catch (error) {
        setMessage('tracking-message', error.message, true);
    }
}

function resetDepartmentForm() {
    document.getElementById('department-form').reset();
    document.getElementById('department-edit-id').value = '';
    document.getElementById('department-form-title').textContent = 'Add department';
    document.getElementById('department-submit').textContent = 'Add department';
    document.getElementById('department-cancel').hidden = true;
}
function resetCategoryForm() {
    document.getElementById('category-form').reset();
    document.getElementById('category-edit-id').value = '';
    document.getElementById('category-form-title').textContent = 'Add category';
    document.getElementById('category-submit').textContent = 'Add category';
    document.getElementById('category-cancel').hidden = true;
}

async function submitForm(form, messageId, action) {
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    setMessage(messageId, 'Saving...');
    try {
        await action();
        setMessage(messageId, 'Saved successfully.');
        await loadAll();
    } catch (error) {
        setMessage(messageId, error.message, true);
    } finally {
        button.disabled = false;
    }
}

document.querySelectorAll('[data-nav]').forEach((link) => link.addEventListener('click', () => {}));
window.addEventListener('hashchange', navigate);
document.querySelectorAll('[data-refresh]').forEach((button) => button.addEventListener('click', loadAll));
document.getElementById('category').addEventListener('change', updateSlaPreview);
document.getElementById('severity').addEventListener('change', updateSlaPreview);

for (const filterId of ['filter-category', 'filter-department', 'filter-severity', 'filter-status']) {
    document.getElementById(filterId).addEventListener('change', renderComplaints);
}

document.getElementById('grievance-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    await submitForm(form, 'form-message', async () => {
        const grievance = await request('/grievances', { method: 'POST', body: JSON.stringify({
            citizenName: values.get('citizenName'), category: { id: Number(values.get('category')) },
            severity: values.get('severity'), location: values.get('location'), description: values.get('description')
        }) });
        form.reset();
        updateSlaPreview();
        location.hash = '#track';
        await loadAll();
        const saved = grievances.find((item) => item.id === grievance.id) || grievance;
        renderTracking(saved);
        document.getElementById('tracking-id').value = grievance.id;
        showToast(`Complaint submitted. Your ID is #${grievance.id}.`);
    });
});

document.getElementById('tracking-form').addEventListener('submit', (event) => {
    event.preventDefault();
    trackComplaint(document.getElementById('tracking-id').value);
});

document.getElementById('complaints-list').addEventListener('click', async (event) => {
    const advance = event.target.closest('[data-advance]');
    const rate = event.target.closest('[data-rate]');
    try {
        if (advance) {
            const item = await request(`/grievances/${advance.dataset.advance}`);
            const next = statusFlow[statusFlow.indexOf(item.status) + 1];
            await request(`/grievances/${item.id}/status`, { method: 'PUT', body: JSON.stringify({ status: next }) });
            showToast(`Complaint #${item.id} moved to ${statusName(next)}.`);
            await loadAll();
        } else if (rate) {
            const rating = document.querySelector(`[data-rating="${rate.dataset.rate}"]`).value;
            if (!rating) throw new Error('Choose a rating from 1 to 5.');
            await request(`/grievances/${rate.dataset.rate}/rating`, { method: 'PUT', body: JSON.stringify({ rating: Number(rating) }) });
            await loadAll();
        }
    } catch (error) {
        showToast(error.message);
    }
});

document.getElementById('tracking-result').addEventListener('click', async (event) => {
    const button = event.target.closest('[data-track-rate]');
    if (!button) return;
    const rating = document.getElementById('track-rating').value;
    if (!rating) return showToast('Choose a rating from 1 to 5.');
    try {
        await request(`/grievances/${button.dataset.trackRate}/rating`, { method: 'PUT', body: JSON.stringify({ rating: Number(rating) }) });
        await loadAll();
        await trackComplaint(button.dataset.trackRate);
        showToast('Thank you for rating the resolution.');
    } catch (error) {
        showToast(error.message);
    }
});

document.getElementById('department-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const id = document.getElementById('department-edit-id').value;
    const data = { name: document.getElementById('department-name').value, description: document.getElementById('department-description').value };
    await submitForm(form, 'department-message', async () => {
        await request(id ? `/departments/${id}` : '/departments', { method: id ? 'PUT' : 'POST', body: JSON.stringify(data) });
        resetDepartmentForm();
    });
});

document.getElementById('department-cancel').addEventListener('click', resetDepartmentForm);
document.getElementById('department-list').addEventListener('click', async (event) => {
    const edit = event.target.closest('[data-edit-department]');
    const remove = event.target.closest('[data-delete-department]');
    if (edit) {
        const item = departments.find((entry) => entry.id === Number(edit.dataset.editDepartment));
        document.getElementById('department-edit-id').value = item.id;
        document.getElementById('department-name').value = item.name;
        document.getElementById('department-description').value = item.description || '';
        document.getElementById('department-form-title').textContent = 'Edit department';
        document.getElementById('department-submit').textContent = 'Save changes';
        document.getElementById('department-cancel').hidden = false;
        document.getElementById('department-name').focus();
    } else if (remove && window.confirm('Delete this department? Departments in use cannot be deleted.')) {
        try {
            await request(`/departments/${remove.dataset.deleteDepartment}`, { method: 'DELETE' });
            showToast('Department deleted.');
            await loadAll();
        } catch (error) { showToast(error.message); }
    }
});

document.getElementById('category-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const id = document.getElementById('category-edit-id').value;
    const data = {
        name: document.getElementById('category-name').value,
        department: { id: Number(document.getElementById('category-department').value) },
        slaDaysLow: Number(document.getElementById('sla-low').value),
        slaDaysMedium: Number(document.getElementById('sla-medium').value),
        slaDaysHigh: Number(document.getElementById('sla-high').value),
        slaDaysCritical: Number(document.getElementById('sla-critical').value)
    };
    await submitForm(form, 'category-message', async () => {
        await request(id ? `/categories/${id}` : '/categories', { method: id ? 'PUT' : 'POST', body: JSON.stringify(data) });
        resetCategoryForm();
    });
});

document.getElementById('category-cancel').addEventListener('click', resetCategoryForm);
document.getElementById('category-list').addEventListener('click', async (event) => {
    const edit = event.target.closest('[data-edit-category]');
    const remove = event.target.closest('[data-delete-category]');
    if (edit) {
        const item = categories.find((entry) => entry.id === Number(edit.dataset.editCategory));
        document.getElementById('category-edit-id').value = item.id;
        document.getElementById('category-name').value = item.name;
        document.getElementById('category-department').value = item.department.id;
        document.getElementById('sla-low').value = item.slaDaysLow;
        document.getElementById('sla-medium').value = item.slaDaysMedium;
        document.getElementById('sla-high').value = item.slaDaysHigh;
        document.getElementById('sla-critical').value = item.slaDaysCritical;
        document.getElementById('category-form-title').textContent = 'Edit category & SLA';
        document.getElementById('category-submit').textContent = 'Save changes';
        document.getElementById('category-cancel').hidden = false;
        document.getElementById('category-name').focus();
    } else if (remove && window.confirm('Delete this category? Categories used by complaints cannot be deleted.')) {
        try {
            await request(`/categories/${remove.dataset.deleteCategory}`, { method: 'DELETE' });
            showToast('Category deleted.');
            await loadAll();
        } catch (error) { showToast(error.message); }
    }
});

document.body.addEventListener('click', (event) => {
    const link = event.target.closest('[data-track-id]');
    if (!link) return;
    event.preventDefault();
    location.hash = '#track';
    document.getElementById('tracking-id').value = link.dataset.trackId;
    trackComplaint(link.dataset.trackId);
});

navigate();
loadAll();
setInterval(loadAll, 20000);
