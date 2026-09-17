const API = 'http://localhost:8080/api';

// This file is the single connection point between the browser UI and the Spring Boot backend.
// Every page calls a small helper (get/post/put/del) so the front-end can stay consistent.
let currentUser = null;
let activeTicketId = null;
let agentsList = [];

function saveUser(user) {
  currentUser = user;
  try {
    localStorage.setItem('helpdesk_user', JSON.stringify(user));
  } catch (_) {
    // Some browsers block localStorage in restricted contexts; this is treated as optional.
  }
}

function clearUser() {
  currentUser = null;
  try {
    localStorage.removeItem('helpdesk_user');
  } catch (_) {}
}

function loadUser() {
  try {
    const stored = localStorage.getItem('helpdesk_user');
    if (stored) currentUser = JSON.parse(stored);
  } catch (_) {}
}

function requireAuth() {
  loadUser();
  if (!currentUser) {
    window.location.href = 'login.html';
    return false;
  }
  return true;
}

async function http(method, path, body) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' }
  };

  if (body !== undefined) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(API + path, options);
  if (response.status === 204) return null;

  const text = await response.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch (_) {
    data = text;
  }

  if (!response.ok) {
    throw new Error(typeof data === 'string' ? data : JSON.stringify(data));
  }

  return data;
}

const get = (path) => http('GET', path);
const post = (path, body) => http('POST', path, body);
const put = (path, body) => http('PUT', path, body);
const del = (path) => http('DELETE', path);

function setMsg(id, text, ok = false) {
  const element = document.getElementById(id);
  if (!element) return;
  element.textContent = text;
  element.className = 'msg' + (ok ? ' ok' : '');
}

function show(id) {
  const element = document.getElementById(id);
  if (element) element.classList.remove('hidden');
}

function hide(id) {
  const element = document.getElementById(id);
  if (element) element.classList.add('hidden');
}

function badge(status) {
  const map = {
    OPEN: 'badge-open',
    IN_PROGRESS: 'badge-progress',
    RESOLVED: 'badge-resolved',
    CLOSED: 'badge-closed'
  };
  return `<span class="badge ${map[status] || 'badge-open'}">${status}</span>`;
}

function priorityBadge(priority) {
  const map = {
    HIGH: 'badge-high',
    MEDIUM: 'badge-medium',
    LOW: 'badge-low'
  };
  return `<span class="badge ${map[priority] || 'badge-medium'}">${priority}</span>`;
}

function fmtDate(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  });
}

function escHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

async function register() {
  const name = document.getElementById('reg-name')?.value.trim();
  const username = document.getElementById('reg-username')?.value.trim();
  const email = document.getElementById('reg-email')?.value.trim();
  const password = document.getElementById('reg-password')?.value;
  const role = document.getElementById('reg-role')?.value || 'CUSTOMER';

  if (!username || !email || !password) {
    setMsg('reg-msg', 'Please fill in all required fields.');
    return;
  }

  try {
    await post('/auth/register', {
      name: name || username,
      username,
      email,
      password,
      role
    });
    setMsg('reg-msg', '✓ Account created! Redirecting to login…', true);
    setTimeout(() => window.location.href = 'login.html', 1200);
  } catch (error) {
    setMsg('reg-msg', error.message || 'Registration failed.');
  }
}

async function registerAgent() {
  const name = document.getElementById('reg-name')?.value.trim();
  const username = document.getElementById('reg-username')?.value.trim();
  const email = document.getElementById('reg-email')?.value.trim();
  const password = document.getElementById('reg-password')?.value;

  if (!username || !email || !password) {
    setMsg('reg-msg', 'Please fill in all required fields.');
    return;
  }

  try {
    await post('/auth/register', {
      name: name || username,
      username,
      email,
      password,
      role: 'AGENT'
    });
    setMsg('reg-msg', '✓ Agent account created! Redirecting to login…', true);
    setTimeout(() => window.location.href = 'login.html', 1200);
  } catch (error) {
    setMsg('reg-msg', error.message || 'Registration failed.');
  }
}

async function login() {
  const username = document.getElementById('login-username')?.value.trim();
  const password = document.getElementById('login-password')?.value;

  if (!username || !password) {
    setMsg('login-msg', 'Please enter your username and password.');
    return;
  }

  try {
    const user = await post('/auth/login', { username, password });
    saveUser(user);
    window.location.href = 'dashboard.html';
  } catch (error) {
    setMsg('login-msg', error.message || 'Login failed.');
  }
}

async function logout() {
  if (!currentUser) {
    window.location.href = 'login.html';
    return;
  }

  try {
    await post('/auth/logout', { username: currentUser.username });
  } catch (_) {
    // Logout should still work even if the API fails; the local session is cleared anyway.
  }

  clearUser();
  window.location.href = 'login.html';
}

async function initDashboard() {
  if (!requireAuth()) return;

  const welcome = document.getElementById('welcome-text');
  if (welcome) {
    welcome.textContent = `${currentUser.username} · ${currentUser.role}`;
  }

  const role = currentUser.role || 'CUSTOMER';

  // Customers see their own tickets; agents/admins see the full queue.
  if (role === 'AGENT' || role === 'ADMIN') {
    document.querySelectorAll('.agent-only').forEach((element) => element.classList.remove('hidden'));
  }

  if (role === 'CUSTOMER') {
    const allTickets = document.querySelector('[data-tab="all-tickets"]');
    if (allTickets) allTickets.remove();
  }

  if (role !== 'CUSTOMER') {
    const newTicket = document.querySelector('[data-tab="new-ticket"]');
    if (newTicket) newTicket.remove();
  }

  document.querySelectorAll('.tab-btn').forEach((button) => {
    button.addEventListener('click', () => switchTab(button.dataset.tab));
  });

  document.getElementById('logout-btn')?.addEventListener('click', logout);
  document.getElementById('nt-submit')?.addEventListener('click', createTicket);
  document.getElementById('refresh-tickets-btn')?.addEventListener('click', loadMyTickets);
  document.getElementById('refresh-all-btn')?.addEventListener('click', loadAllTickets);
  document.getElementById('modal-close')?.addEventListener('click', closeModal);
  document.getElementById('assign-agent-btn')?.addEventListener('click', assignAgent);
  document.getElementById('update-status-btn')?.addEventListener('click', updateStatus);
  document.getElementById('delete-ticket-btn')?.addEventListener('click', deleteTicket);
  document.getElementById('post-comment-btn')?.addEventListener('click', postComment);

  document.getElementById('modal-overlay')?.addEventListener('click', (event) => {
    if (event.target === document.getElementById('modal-overlay')) {
      closeModal();
    }
  });

  if (role === 'AGENT' || role === 'ADMIN') {
    try {
      agentsList = await get('/users/agents');
    } catch (_) {
      agentsList = [];
    }
  }

  switchTab(role === 'CUSTOMER' ? 'tickets' : 'all-tickets');
}

function switchTab(name) {
  document.querySelectorAll('.tab-btn').forEach((button) => {
    button.classList.toggle('active', button.dataset.tab === name);
  });

  document.querySelectorAll('.tab-content').forEach((panel) => panel.classList.add('hidden'));
  const targetPanel = document.getElementById('tab-' + name);
  if (targetPanel) targetPanel.classList.remove('hidden');

  if (name === 'tickets') loadMyTickets();
  if (name === 'all-tickets') loadAllTickets();
}

async function loadMyTickets() {
  const container = document.getElementById('tickets-list');
  if (!container) return;
  container.innerHTML = '<p class="muted">Loading…</p>';

  try {
    const tickets = await get(`/tickets/customer/${currentUser.id}`);
    renderTicketList(tickets, container);
  } catch (error) {
    container.innerHTML = `<p class="msg">${escHtml(error.message)}</p>`;
  }
}

async function loadAllTickets() {
  const container = document.getElementById('all-tickets-list');
  if (!container) return;
  container.innerHTML = '<p class="muted">Loading…</p>';

  try {
    const tickets = await get('/tickets');
    renderTicketList(tickets, container);
  } catch (error) {
    container.innerHTML = `<p class="msg">${escHtml(error.message)}</p>`;
  }
}

function renderTicketList(tickets, container) {
  if (!tickets || tickets.length === 0) {
    container.innerHTML = '<p class="muted">No tickets found.</p>';
    return;
  }

  container.innerHTML = tickets.map((ticket) => `
    <div class="ticket-row" data-id="${ticket.id}">
      <div class="ticket-row-main">
        <span class="ticket-title">${escHtml(ticket.title)}</span>
        <span class="ticket-meta">${fmtDate(ticket.createdAt)}</span>
      </div>
      <div class="ticket-row-badges">
        ${badge(ticket.status)}
        ${priorityBadge(ticket.priority)}
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.ticket-row').forEach((row) => {
    row.addEventListener('click', () => openTicket(row.dataset.id));
  });
}

async function createTicket() {
  const title = document.getElementById('nt-title')?.value.trim();
  const description = document.getElementById('nt-desc')?.value.trim();
  const priority = document.getElementById('nt-priority')?.value || 'MEDIUM';

  if (!title || !description) {
    setMsg('nt-msg', 'Please fill in all required fields.');
    return;
  }

  try {
    await post('/tickets', {
      title,
      description,
      priority,
      customerId: currentUser.id
    });

    setMsg('nt-msg', '✓ Ticket created!', true);
    const titleInput = document.getElementById('nt-title');
    const descriptionInput = document.getElementById('nt-desc');
    if (titleInput) titleInput.value = '';
    if (descriptionInput) descriptionInput.value = '';

    setTimeout(() => switchTab('tickets'), 500);
  } catch (error) {
    setMsg('nt-msg', error.message || 'Failed to create ticket.');
  }
}

async function openTicket(id) {
  activeTicketId = id;
  setMsg('agent-action-msg', '');
  setMsg('delete-msg', '');
  setMsg('comment-msg', '');

  try {
    const ticket = await get(`/tickets/${id}`);

    const modalTitle = document.getElementById('modal-title');
    const modalDesc = document.getElementById('modal-desc');
    const modalMeta = document.getElementById('modal-meta');

    if (modalTitle) modalTitle.textContent = ticket.title;
    if (modalDesc) modalDesc.textContent = ticket.description;

    let agentLabel = '';
    if (ticket.agentId) {
      const agent = agentsList.find((entry) => entry.id === ticket.agentId);
      agentLabel = agent
        ? `<span class="muted">Agent: ${escHtml(agent.username)}</span>`
        : `<span class="muted">Agent ID: ${escHtml(ticket.agentId)}</span>`;
    }

    if (modalMeta) {
      modalMeta.innerHTML = `
        <span>${badge(ticket.status)}</span>
        <span>${priorityBadge(ticket.priority)}</span>
        <span class="muted">${fmtDate(ticket.createdAt)}</span>
        ${agentLabel}
      `;
    }

    const role = currentUser.role;
    if (role === 'AGENT' || role === 'ADMIN') {
      show('agent-actions');

      const statusSelect = document.getElementById('status-select');
      if (statusSelect) statusSelect.value = ticket.status || 'OPEN';

      const agentSelect = document.getElementById('assign-agent-select');
      if (agentSelect) {
        agentSelect.innerHTML = '<option value="">— select an agent —</option>';
        agentsList.forEach((agent) => {
          const option = document.createElement('option');
          option.value = agent.id;
          option.textContent = agent.username;
          if (agent.id === ticket.agentId) option.selected = true;
          agentSelect.appendChild(option);
        });
      }
    } else {
      hide('agent-actions');
    }

    if (role === 'ADMIN') {
      show('admin-delete');
    } else {
      hide('admin-delete');
    }

    await loadComments(id);
    show('modal-overlay');
    document.body.style.overflow = 'hidden';
  } catch (error) {
    alert('Could not load ticket: ' + error.message);
  }
}

function closeModal() {
  hide('modal-overlay');
  document.body.style.overflow = '';
  activeTicketId = null;
}

async function assignAgent() {
  if (!activeTicketId) return;

  const agentId = document.getElementById('assign-agent-select')?.value;
  if (!agentId) {
    setMsg('agent-action-msg', 'Please select an agent.');
    return;
  }

  try {
    await put(`/tickets/${activeTicketId}/assign`, { agentId });
    setMsg('agent-action-msg', '✓ Agent assigned.', true);
  } catch (error) {
    setMsg('agent-action-msg', error.message || 'Failed to assign agent.');
  }
}

async function updateStatus() {
  if (!activeTicketId) return;

  const status = document.getElementById('status-select')?.value;
  try {
    await put(`/tickets/${activeTicketId}/status`, { status });
    setMsg('agent-action-msg', `✓ Status updated to ${status}.`, true);

    if (currentUser.role === 'CUSTOMER') {
      loadMyTickets();
    } else {
      loadAllTickets();
    }
  } catch (error) {
    setMsg('agent-action-msg', error.message || 'Failed to update status.');
  }
}

async function deleteTicket() {
  if (!activeTicketId) return;
  if (!confirm('Delete this ticket? This cannot be undone.')) return;

  try {
    await del(`/tickets/${activeTicketId}`);
    closeModal();
    loadAllTickets();
  } catch (error) {
    setMsg('delete-msg', error.message || 'Failed to delete ticket.');
  }
}

async function loadComments(ticketId) {
  const container = document.getElementById('comments-list');
  if (!container) return;
  container.innerHTML = '<p class="muted" style="font-size:.85rem;">Loading comments…</p>';

  try {
    const comments = await get(`/comments/ticket/${ticketId}`);
    if (!comments || comments.length === 0) {
      container.innerHTML = '<p class="muted" style="font-size:.85rem;">No comments yet.</p>';
      return;
    }

    container.innerHTML = comments.map((comment) => `
      <div class="comment-item">
        <div class="comment-body">${escHtml(comment.body)}</div>
        <div class="comment-meta">${fmtDate(comment.createdAt)}</div>
      </div>
    `).join('');
  } catch (error) {
    container.innerHTML = `<p class="msg" style="font-size:.85rem;">${escHtml(error.message)}</p>`;
  }
}

async function postComment() {
  if (!activeTicketId) return;

  const body = document.getElementById('comment-body')?.value.trim();
  if (!body) {
    setMsg('comment-msg', 'Comment cannot be empty.');
    return;
  }

  try {
    await post('/comments', {
      body,
      ticketId: activeTicketId,
      userId: currentUser.id
    });

    const commentInput = document.getElementById('comment-body');
    if (commentInput) commentInput.value = '';
    setMsg('comment-msg', '');
    await loadComments(activeTicketId);
  } catch (error) {
    setMsg('comment-msg', error.message || 'Failed to post comment.');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  loadUser();

  const page = location.pathname.split('/').pop();

  if (page === 'dashboard.html') {
    initDashboard();
    return;
  }

  document.getElementById('login-btn')?.addEventListener('click', login);
  document.getElementById('login-password')?.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') login();
  });

  document.getElementById('reg-btn')?.addEventListener('click', register);
  document.getElementById('reg-agent-btn')?.addEventListener('click', registerAgent);

  // If the user is already logged in, send them to the dashboard immediately.
  try {
    if (currentUser) {
      window.location.href = 'dashboard.html';
    }
  } catch (_) {}
});
