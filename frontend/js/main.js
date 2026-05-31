// ===== XSS PROTECTION =====
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
// ========== API CONFIG ==========
const BACKEND_URL = 'http://localhost:8080';
const API_BASE = `${BACKEND_URL}/api`;

// ========== AUTH HELPERS ==========
function normalizeRole(role) {
  const r = (role || 'USER').toString().trim().toUpperCase();
  if (r === 'VOLUNTEER' || r === 'DONOR') return 'USER';
  if (r === 'CREATOR') return 'ORGANIZATION';
  if (r === 'ADMIN' || r === 'ORGANIZATION' || r === 'USER') return r;
  return 'USER';
}

function extractUserFromAuthPayload(payload) {
  if (!payload) return null;
  const user = payload.user?.id ? payload.user : (payload.id ? payload : null);
  if (!user) return null;
  user.role = normalizeRole(user.role);
  return user;
}

function getCurrentUser() {
  const raw = localStorage.getItem('currentUser');
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    const user = extractUserFromAuthPayload(parsed);
    if (user && parsed.user?.id) {
      localStorage.setItem('currentUser', JSON.stringify(user));
    }
    return user;
  } catch {
    return null;
  }
}

function saveCurrentUser(user) {
  if (!user) return;
  user.role = normalizeRole(user.role);
  localStorage.setItem('currentUser', JSON.stringify(user));
}

function getAccessToken() {
  return localStorage.getItem('accessToken');
}

function saveAccessToken(token) {
  if (token) localStorage.setItem('accessToken', token);
  else localStorage.removeItem('accessToken');
}

function getAuthHeaders(extra = {}) {
  const headers = { ...extra };
  const token = getAccessToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const user = getCurrentUser();
  if (user?.email) headers['X-User-Email'] = user.email;
  return headers;
}

function persistAuthResponse(authResponse) {
  if (!authResponse) return null;
  if (authResponse.token) saveAccessToken(authResponse.token);
  const user = extractUserFromAuthPayload(authResponse);
  if (user?.id) saveCurrentUser(user);
  return user;
}

async function refreshCurrentUserFromServer() {
  const user = getCurrentUser();
  if (!user?.id && !getAccessToken()) return null;
  try {
    const meRes = await fetch(`${API_BASE}/auth/me`, { headers: getAuthHeaders() });
    if (meRes.ok) {
      const fresh = await meRes.json();
      const merged = { ...(user || {}), ...fresh, role: fresh.role || user?.role };
      merged.role = normalizeRole(merged.role);
      saveCurrentUser(merged);
      return merged;
    }
    return user;
  } catch (e) {
    console.warn('Could not refresh user profile from server', e);
    return user;
  }
}

function logout() {
  localStorage.removeItem('currentUser');
  localStorage.removeItem('accessToken');
  window.location.href = 'login.html';
}

/** Headers for admin API calls (requires prior admin login). */
function getAdminHeaders(extra = {}) {
  return getAuthHeaders(extra);
}


// ========== IMAGE PERSISTENCE ==========

async function uploadAndSaveImage(userId, file, type) {
  // type: 'avatar' or 'cover'
  if (!file || !userId) return null;

  // 1. Preview immediately (base64)
  const base64 = await fileToBase64(file);
  const user = getCurrentUser();
  if (user) {
    if (type === 'avatar') user.avatar = base64;
    else user.cover = base64;
    saveCurrentUser(user);
  }

  // 2. Try upload to backend
  try {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${BACKEND_URL}/api/users/${userId}/${type}`, {
      method: 'POST',
      body: formData
    });
    if (res.ok) {
      const data = await res.json();
      const backendUrl = BACKEND_URL + (data.avatarUrl || data.coverUrl || '');
      const freshUser = getCurrentUser();
      if (freshUser) {
        if (type === 'avatar') { freshUser.avatar = backendUrl; freshUser.avatarUrl = backendUrl; }
        else { freshUser.cover = backendUrl; freshUser.coverUrl = backendUrl; }
        saveCurrentUser(freshUser);
      }
      return backendUrl;
    }
  } catch(e) {
    console.warn('Backend upload failed, using local storage only');
  }

  // 3. Fallback: base64 already saved in localStorage
  return base64;
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = e => resolve(e.target.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function loadSavedImages() {
  // Called on every page load - restores photos from localStorage
  const user = getCurrentUser();
  if (!user) return;

  // normalize keys
  if (!user.avatar && user.avatarUrl) user.avatar = user.avatarUrl;
  if (!user.cover && user.coverUrl) user.cover = user.coverUrl;
  if (user.avatar || user.cover) saveCurrentUser(user);

  // Profile/Org pages use these ids
  const avatarEl = document.getElementById('profileAvatar') || document.getElementById('orgLogo');
  const coverEl = document.getElementById('coverPhoto') || document.getElementById('orgCover');

  // Prefer backend urls if available, otherwise fallback to stored avatar/cover
  if (avatarEl && (user.avatarUrl || user.avatar)) avatarEl.src = user.avatarUrl || user.avatar;
  if (coverEl && (user.coverUrl || user.cover)) coverEl.src = user.coverUrl || user.cover;
}


// ========== ROLE-BASED PAGE GUARD ==========
// Call this on any protected page, passing the allowed roles array.
// e.g. guardPage(['USER', 'VOLUNTEER']) or guardPage(['ORGANIZATION']) etc.
function guardPage(allowedRoles) {
  const user = getCurrentUser();
  if (!user) { window.location.href = 'login.html'; return null; }

  const role = (user.role || '').toUpperCase();
  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(role)) {
    // Redirect to the correct home page for this user's role
    redirectByRole(role);
    return null;
  }
  return user;
}

function redirectByRole(role) {
  if (role === 'ADMIN') window.location.href = 'admin-dashboard.html';
  else if (role === 'ORGANIZATION') window.location.href = 'organization.html';
  else window.location.href = 'profile.html'; // USER / VOLUNTEER
}

// ========== DYNAMIC HEADER (Nav Account Info) ==========
// Injects the logged-in user's avatar + name into .nav-actions on any page.
// On index.html it replaces the Login/Register buttons with the user menu.
function updateNavForLoggedInUser() {
  const user = getCurrentUser();
  const navActions = document.querySelector('.nav-actions');
  if (!navActions) return;

  if (!user) {
    // Not logged in — leave the nav as-is (shows Login/Register or Sign Out link)
    return;
  }

  const role = (user.role || 'USER').toUpperCase();
  const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(user.name || 'User')}&background=3B82F6&color=fff&size=40`;

  // Build the user dropdown
  navActions.innerHTML = `
    <div class="user-menu-dropdown" id="navUserDropdown" style="cursor:pointer;display:flex;align-items:center;gap:0.5rem;position:relative;">
      <img src="${avatarUrl}" alt="${user.name}" class="user-avatar-small" style="width:36px;height:36px;border-radius:50%;object-fit:cover;">
      <span style="font-weight:500;">${user.name}</span>
      <i class="fas fa-chevron-down" style="font-size:0.75rem;"></i>
      <div class="nav-dropdown-menu" id="navDropdownMenu" style="
        display:none;position:absolute;top:calc(100% + 10px);right:0;
        background:#fff;border:1px solid #e5e7eb;border-radius:10px;
        min-width:180px;box-shadow:0 8px 24px rgba(0,0,0,0.12);z-index:1000;overflow:hidden;">
        ${role === 'ADMIN' ? `<a href="admin-dashboard.html" class="nav-drop-item"><i class="fas fa-tachometer-alt"></i> Dashboard</a>` : ''}
        ${role === 'ORGANIZATION' ? `<a href="organization.html" class="nav-drop-item"><i class="fas fa-building"></i> My Organization</a>` : ''}
        ${(role === 'USER' || role === 'VOLUNTEER') ? `<a href="profile.html" class="nav-drop-item"><i class="fas fa-user"></i> My Profile</a>` : ''}
        <a href="index.html" class="nav-drop-item"><i class="fas fa-home"></i> Home</a>
        <div style="border-top:1px solid #e5e7eb;margin:4px 0;"></div>
        <a href="#" class="nav-drop-item" style="color:#ef4444;" onclick="logout();return false;"><i class="fas fa-sign-out-alt"></i> Sign Out</a>
      </div>
    </div>
  `;

  // Dropdown toggle
  const dropdown = document.getElementById('navUserDropdown');
  const menu = document.getElementById('navDropdownMenu');
  if (dropdown && menu) {
    dropdown.addEventListener('click', (e) => {
      e.stopPropagation();
      menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
    });
    document.addEventListener('click', () => { menu.style.display = 'none'; });
  }

  // Add dropdown item styles dynamically if not already added
  if (!document.getElementById('navDropdownStyles')) {
    const style = document.createElement('style');
    style.id = 'navDropdownStyles';
    style.textContent = `
      .nav-drop-item {
        display: flex; align-items: center; gap: 0.6rem;
        padding: 10px 16px; font-size: 0.9rem; color: #374151;
        text-decoration: none; transition: background 0.15s;
      }
      .nav-drop-item:hover { background: #f3f4f6; color: #111; }
      .nav-drop-item i { width: 16px; text-align: center; }
    `;
    document.head.appendChild(style);
  }
}

// ========== API CALLS ==========

async function parseApiError(res, fallback = 'Request failed') {
  const data = await res.json().catch(() => ({}));
  return data.error || data.message || fallback;
}

// --- AUTH ---
async function apiRegister(name, email, password, role) {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password, role })
  });
  if (!res.ok) throw new Error('Registration failed');
  return res.json();
}

async function apiLogin(email, password) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || 'Invalid email or password');
  return data;
}

// --- CAMPAIGNS ---
async function apiGetCampaigns() {
  const res = await fetch(`${API_BASE}/campaigns`);
  if (!res.ok) throw new Error('Failed to load campaigns');
  return res.json();
}

async function apiGetActiveCampaigns() {
  const res = await fetch(`${API_BASE}/campaigns/active`);
  if (!res.ok) throw new Error('Failed to load active campaigns');
  return res.json();
}

async function apiGetPendingCampaigns() {
  const res = await fetch(`${API_BASE}/campaigns/pending`);
  if (!res.ok) throw new Error('Failed to load pending campaigns');
  return res.json();
}


async function apiGetApprovedCampaigns() {
  const res = await fetch(`${API_BASE}/campaigns/approved`);
  if (!res.ok) throw new Error('Failed to load approved campaigns');
  return res.json();
}

async function apiGetPendingCampaigns() {
  const res = await fetch(`${API_BASE}/campaigns/pending`);
  if (!res.ok) throw new Error('Failed to load pending campaigns');
  return res.json();
}


async function apiGetCampaignById(id) {
  const res = await fetch(`${API_BASE}/campaigns/${id}`);
  if (!res.ok) throw new Error('Campaign not found');
  return res.json();
}

async function apiCreateCampaign(campaignData) {
  const res = await fetch(`${API_BASE}/campaigns`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify(campaignData)
  });
  if (!res.ok) throw new Error(await parseApiError(res, 'Failed to create campaign'));
  return res.json();
}

async function apiUpdateCampaign(id, campaignData) {
  const res = await fetch(`${API_BASE}/campaigns/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(campaignData)
  });
  if (!res.ok) throw new Error('Failed to update campaign');
  return res.json();
}

async function apiDeleteCampaign(id) {
  const res = await fetch(`${API_BASE}/campaigns/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete campaign');
}

// --- DONATIONS ---
async function apiSubmitDonation(userId, campaignId, amount) {
  const res = await fetch(`${API_BASE}/donations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, campaignId, amount })
  });
  if (!res.ok) throw new Error('Donation failed');
  return res.json();
}

async function apiGetDonationHistory(userId) {
  const res = await fetch(`${API_BASE}/donations/user/${userId}`);
  if (!res.ok) throw new Error('Failed to load donation history');
  return res.json();
}

// --- VOLUNTEERS ---
async function apiApplyVolunteer(userId, campaignId, whyJoin, skills, availability, experience, phone) {
  const res = await fetch(`${API_BASE}/volunteers/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, campaignId, whyJoin, skills, availability, experience, phone })
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || 'Volunteer application failed');
  return data;
}

async function apiGetAdminVolunteersForDashboard() {
  const res = await fetch(`${API_BASE}/admin/volunteers/dashboard`, { headers: getAdminHeaders() });
  if (!res.ok) throw new Error('Failed to load volunteers');
  return res.json();
}

async function apiGetAdminDonations() {
  const res = await fetch(`${API_BASE}/admin/donations`, { headers: getAdminHeaders() });
  if (!res.ok) throw new Error('Failed to load donations');
  return res.json();
}

async function apiGetVolunteersByCampaign(campaignId) {
  const res = await fetch(`${API_BASE}/volunteers?campaignId=${campaignId}`);
  if (!res.ok) throw new Error('Failed to load volunteers');
  return res.json();
}

async function apiGetVolunteersByUser(userId) {
  const res = await fetch(`${API_BASE}/volunteers/user/${userId}`);
  if (!res.ok) throw new Error('Failed to load volunteer applications');
  return res.json();
}

async function apiApproveVolunteer(id) {
  const res = await fetch(`${API_BASE}/volunteers/${id}/approve`, { method: 'PUT' });
  if (!res.ok) throw new Error('Failed to approve volunteer');
  return res.json();
}

// --- ADMIN ---
async function apiGetAllUsers() {
  const res = await fetch(`${API_BASE}/admin/users`, { headers: getAdminHeaders() });
  if (!res.ok) throw new Error('Failed to load users');
  return res.json();
}

async function apiGetDashboardStats() {
  const res = await fetch(`${API_BASE}/admin/statistics`, { headers: getAdminHeaders() });
  if (!res.ok) throw new Error('Failed to load stats');
  return res.json();
}

// --- ADMIN campaign lists ---
async function apiGetAdminPendingCampaigns() {
  const res = await fetch(`${API_BASE}/campaigns/pending`);
  if (!res.ok) throw new Error('Failed to load pending campaigns');
  return res.json();
}

async function apiGetAdminApprovedCampaigns() {
  const res = await fetch(`${API_BASE}/campaigns/approved`);
  if (!res.ok) throw new Error('Failed to load approved campaigns');
  return res.json();
}


async function apiDeleteUser(id) {
  const res = await fetch(`${API_BASE}/admin/users/${id}`, {
    method: 'DELETE',
    headers: getAdminHeaders()
  });
  if (!res.ok) {
    // backend might return JSON error => show it
    const err = await parseApiError(res, 'Failed to delete user');
    throw new Error(err);
  }
}

async function apiDeleteVolunteer(id) {
  const res = await fetch(`${API_BASE}/admin/volunteers/${id}`, {
    method: 'DELETE',
    headers: getAdminHeaders()
  });
  if (!res.ok) {
    const err = await parseApiError(res, 'Failed to delete volunteer');
    throw new Error(err);
  }
}


// ========== GLOBAL STATE ==========
let campaignsData = [];
let currentFilter = 'all';
let currentSearch = '';
let selectedCampaignId = null;

// ========== STATS ANIMATION ==========
function animateStats() {
  const statNumbers = document.querySelectorAll('.stat-number');
  if (statNumbers.length === 0) return;

  statNumbers.forEach(number => {
    let target = number.getAttribute('data-target');
    if (!target) return;

    let targetNum = 0;
    if (target.includes('K')) targetNum = parseFloat(target) * 1000;
    else if (target.includes('M')) targetNum = parseFloat(target) * 1000000;
    else targetNum = parseFloat(target.replace(/[$,]/g, ''));

    if (!isNaN(targetNum)) animateNumber(number, targetNum, target.includes('$'), target);
  });
}

function animateNumber(element, targetNum, isCurrency, originalText) {
  const duration = 2000;
  const increment = targetNum / (duration / 16);
  let current = 0;
  const hasPlus = originalText?.includes('+');

  const timer = setInterval(() => {
    current += increment;
    if (current >= targetNum) { current = targetNum; clearInterval(timer); }

    let displayValue;
    if (isCurrency) displayValue = '$' + Math.floor(current).toLocaleString();
    else displayValue = Math.floor(current).toLocaleString();
    if (hasPlus) displayValue += '+';
    element.innerText = displayValue;
  }, 16);
}

// ========== MOBILE NAV ==========
function toggleMobileMenu() {
  const menu = document.querySelector('.nav-menu');
  const sidebar = document.querySelector('.dashboard-sidebar');
  const overlay = document.getElementById('mobile-overlay') || createOverlay();

  if (menu) {
    menu.classList.toggle('active');
    document.body.style.overflow = menu.classList.contains('active') ? 'hidden' : '';
  }
  if (sidebar) {
    sidebar.classList.toggle('open');
    overlay.style.display = sidebar.classList.contains('open') ? 'block' : 'none';
    document.body.style.overflow = sidebar.classList.contains('open') ? 'hidden' : '';
  }
}

function closeMobileMenus() {
  const menu = document.querySelector('.nav-menu');
  const sidebar = document.querySelector('.dashboard-sidebar');
  const overlay = document.getElementById('mobile-overlay');
  if (menu) menu.classList.remove('active');
  if (sidebar) { sidebar.classList.remove('open'); if (overlay) overlay.style.display = 'none'; }
  document.body.style.overflow = '';
}

function createOverlay() {
  let overlay = document.getElementById('mobile-overlay');
  if (!overlay) {
    overlay = document.createElement('div');
    overlay.id = 'mobile-overlay';
    overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);z-index:199;display:none;';
    document.body.appendChild(overlay);
    overlay.addEventListener('click', closeMobileMenus);
  }
  return overlay;
}

document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeMobileMenus(); });
window.addEventListener('resize', () => { if (window.innerWidth > 640) closeMobileMenus(); });

// ========== CAROUSEL ==========
function initHeroSlider() {
  if (typeof bootstrap !== 'undefined' && document.getElementById('heroSlider')) {
    try {
      new bootstrap.Carousel(document.getElementById('heroSlider'), { interval: 5000, pause: 'hover', wrap: true });
    } catch(e) { console.warn('Carousel init failed:', e); }
  }
}

// ========== CAMPAIGNS ==========
function renderCampaigns() {
  const container = document.getElementById('campaignsGrid');
  if (!container) return;

  let filtered = campaignsData.filter(c =>
    (currentFilter === 'all' || (c.category && c.category.toLowerCase() === currentFilter.toLowerCase())) &&
    (c.title.toLowerCase().includes(currentSearch.toLowerCase()) ||
     (c.description && c.description.toLowerCase().includes(currentSearch.toLowerCase())))
  );

  if (filtered.length === 0) {
    container.innerHTML = '<p style="text-align:center;color:var(--gray-500);padding:2rem;">No campaigns found.</p>';
    return;
  }

  const goalField = c => c.goalAmount || c.goal || 1;
  const currentField = c => c.currentAmount || c.current || 0;

  function getOrganizationNameForCampaign(c) {
    return (
      c.organizationName ||
      c.creator ||
      (c.organization && (c.organization.name || c.organization.title)) ||
      (c.organization && c.organizationId ? c.organization.name : null) ||
      (c.organizationId ? `Organization #${c.organizationId}` : '')
    );
  }

  container.innerHTML = filtered.map(c => {
    const orgName = getOrganizationNameForCampaign(c);
    const orgHtml = orgName ? `<div class="campaign-org" style="font-size:0.85rem;color:var(--gray-500);margin-top:0.25rem;">${escapeHtml(orgName)}</div>` : '';

    return `
      <div class="col-md-4 mb-4">
        <div class="campaign-card-modern" onclick="viewCampaign(${c.id})" style="cursor:pointer;">
          <div class="campaign-image" style="background-image:url('${c.image || 'https://images.unsplash.com/photo-1509062522246-3755977927d7?w=600'}')">
            <span class="campaign-category">${c.category || 'General'}</span>
          </div>
          <div class="campaign-content">
            <h3 class="campaign-title">${c.title}</h3>
            ${orgHtml}
            <p class="campaign-description">${(c.description || '').substring(0, 100)}...</p>
            <div class="progress-section">
              <div class="progress-bar">
                <div class="progress-fill" style="width:${Math.min((currentField(c) / goalField(c)) * 100, 100)}%"></div>
              </div>
              <div class="progress-stats">
                <span class="raised">$${currentField(c).toLocaleString()}</span>
                <span class="goal">of $${goalField(c).toLocaleString()}</span>
              </div>
            </div>
            <div class="campaign-actions-modern">
              <button class="btn btn-secondary btn-sm" onclick="event.stopPropagation();viewCampaign(${c.id})">View</button>
${campaignAllowsVolunteers(c) ? `<button class="btn btn-primary btn-sm" onclick="event.stopPropagation();window.location.href='volunteer.html?campaignId=${c.id}'">Volunteer</button>` : ''}
            </div>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function setupFilters() {
  const searchInput = document.getElementById('searchInput');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => { currentSearch = e.target.value; renderCampaigns(); });
  }

  const filterChips = document.querySelectorAll('.filter-chip');
  filterChips.forEach(chip => {
    chip.addEventListener('click', () => {
      filterChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentFilter = chip.dataset.category || 'all';
      renderCampaigns();
    });
  });
}

function campaignAllowsVolunteers(c) {
  if (!c) return false;
  return c.allowVolunteers === true || c.allowVolunteers === 'true';
}

function viewCampaign(id) {
  if (document.getElementById('campaignDetailContainer')) {
    const campaign = campaignsData.find(c => c.id === id);
    if (campaign) renderCampaignDetails(campaign);
  } else {
    localStorage.setItem('selectedCampaignId', id);
    window.location.href = 'campaign-details.html';
  }
}

function renderCampaignDetails(c) {
  const container = document.getElementById('campaignDetailContainer');
  if (!container) return;

  const goalField = c.goalAmount || c.goal || 1;
  const currentField = c.currentAmount || c.current || 0;
  const progress = Math.min((currentField / goalField) * 100, 100).toFixed(1);

  container.innerHTML = `
    <div class="campaign-detail-card">
      <div class="detail-hero" style="background-image:url('${c.image || 'https://images.unsplash.com/photo-1509062522246-3755977927d7?w=600'}')">
        <div class="detail-hero-overlay">
          <h1>${c.title}</h1>
          <p>${c.description || ''}</p>
        </div>
      </div>
      <div class="detail-content">
        <div class="progress-section">
          <div class="progress-bar">
            <div class="progress-fill" style="width:${progress}%"></div>
          </div>
          <div class="progress-stats">
            <span class="raised">$${currentField.toLocaleString()} raised</span>
            <span class="goal">Goal: $${goalField.toLocaleString()} (${progress}%)</span>
          </div>
        </div>
        <div class="creator-card">
          <i class="fas fa-user-circle fa-2x"></i>
          <div>
            <strong>${(
              c.organizationName ||
              c.organization?.name ||
              c.organization?.title ||
              c.creator ||
              (c.organizationId ? `Organization #${c.organizationId}` : 'Unknown')
            )}</strong>
            <span style="display:block;font-size:0.875rem;color:var(--gray-500);">Campaign Organizer</span>
          </div>
        </div>
        <div style="display:flex;gap:1rem;margin-top:2rem;flex-wrap:wrap;">
          <button class="btn-primary" onclick="openDonateModal(${c.id})">
            <i class="fas fa-heart"></i> Donate Now
          </button>
          ${campaignAllowsVolunteers(c) ? `
          <button class="btn-secondary" onclick="openVolunteerModal(${c.id})">
            <i class="fas fa-hands-helping"></i> Apply as Volunteer
          </button>` : `
          <p style="margin:0;color:var(--gray-500);font-size:0.9rem;align-self:center;">
            <i class="fas fa-info-circle"></i> Volunteering is not enabled for this campaign.
          </p>`}
        </div>
      </div>
    </div>
  `;
}

async function loadCampaignDetails() {
  const id = localStorage.getItem('selectedCampaignId');
  if (!id) return;
  try {
    const campaign = await apiGetCampaignById(id);
    campaignsData = [campaign];
    selectedCampaignId = Number(campaign.id);
    renderCampaignDetails(campaign);
    initVolunteerForms();
  } catch (e) {
    showToast('Failed to load campaign details', 'error');
  }
}

// ========== ADMIN DASHBOARD ==========
async function initAdminDashboard() {
  const user = guardPage(['ADMIN']);
  if (!user) return;

  const welcomeNameEl = document.getElementById('adminWelcomeName');
  if (welcomeNameEl && user.name) welcomeNameEl.textContent = user.name;

  const adminHeaderNameEl = document.getElementById('adminHeaderName');
  if (adminHeaderNameEl && user.name) adminHeaderNameEl.textContent = user.name;


  const navItems = document.querySelectorAll('.sidebar-nav .nav-item');
  const views = ['dashboard', 'campaigns', 'donations', 'volunteers', 'users'];

  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      const viewName = item.dataset.view;
      if (!viewName) return;
      navItems.forEach(nav => nav.classList.remove('active'));
      item.classList.add('active');
      views.forEach(v => {
        const el = document.getElementById(`${v}View`);
        if (el) el.classList.remove('active');
      });
      const activeView = document.getElementById(`${viewName}View`);
      if (activeView) activeView.classList.add('active');
    });
  });

  // Load stats from backend
  try {
    const stats = await apiGetDashboardStats();
    updateDashboardStats(stats);
  } catch (e) {
    console.warn('Could not load stats from backend, using defaults');
  }

  // Load campaigns table (pending + approved)
  try {
    const [pending, approved] = await Promise.all([
      apiGetAdminPendingCampaigns(),
      apiGetAdminApprovedCampaigns()
    ]);

    campaignsData = approved;
    renderAdminCampaignsTable(pending, approved);
    renderTopCampaigns(approved);
  } catch (e) {
    console.warn('Could not load campaigns');
  }


  // Load users table
  try {
    const users = await apiGetAllUsers();
    renderUsersTable(users);
  } catch (e) {
    console.warn('Could not load users');
  }

  // Load donations table
  try {
    const donations = await apiGetAdminDonations();
    renderAdminDonationsTable(donations);
  } catch (e) {
    console.warn('Could not load donations');
  }

  // Load volunteers table
  try {
    const volunteers = await apiGetAdminVolunteersForDashboard();
    renderAdminVolunteersTable(volunteers);
  } catch (e) {
    console.warn('Could not load volunteers');
  }

  generateChartBars();
  loadActivityTimeline();
}

function renderAdminDonationsTable(donations) {
  const table = document.getElementById('donationsTable');
  if (!table) return;

  if (!donations || donations.length === 0) {
    table.innerHTML = '<tr><td colspan="4" style="text-align:center;color:#9ca3af;">No donations yet.</td></tr>';
    return;
  }

  table.innerHTML = donations.map(d => `
    <tr>
      <td>${d.campaignTitle || (d.campaignId ? 'Campaign #' + d.campaignId : '-')}</td>
      <td><strong>$${(d.amount ?? 0).toLocaleString()}</strong></td>
      <td>${d.date || '-'}</td>
      <td><span class="status-badge ${d.status === 'COMPLETED' || d.status === 'SUCCESS' ? 'approved' : 'pending'}">${d.status || 'COMPLETED'}</span></td>
    </tr>
  `).join('');
}

function renderAdminVolunteersTable(volunteers) {
  const table = document.getElementById('volunteersTable');
  if (!table) return;

  if (!volunteers || volunteers.length === 0) {
    table.innerHTML = '<tr><td colspan="7" style="text-align:center;color:#9ca3af;">No volunteer applications yet.</td></tr>';
    return;
  }

  table.innerHTML = volunteers.map(v => `
    <tr>
      <td>${v.volunteerName || '-'}</td>
      <td>${v.email || '-'}${v.phone ? `<br><small style="color:#6b7280;">${v.phone}</small>` : ''}</td>
      <td>${v.campaignTitle || (v.campaignId ? 'Campaign #' + v.campaignId : '-')}</td>
      <td>${v.joinedDate || '-'}</td>
      <td>${v.hours || '-'}</td>
      <td><span class="status-badge ${v.status === 'APPROVED' || v.status === 'ACCEPTED' ? 'approved' : v.status === 'REJECTED' ? 'rejected' : 'pending'}">${v.status || 'PENDING'}</span></td>
      <td class="action-buttons">
        ${typeof v.id !== 'undefined' && v.id !== null ? `
          <button class="action-btn" style="color:var(--danger);" onclick="adminDeleteVolunteer(${v.id})">Delete</button>
        ` : '—'}
      </td>
    </tr>
  `).join('');
}


function updateDashboardStats(stats) {
  // Update stat cards if they have IDs
  const totalUsers = document.querySelector('.stat-card:nth-child(2) .stat-value');
  const totalVolunteers = document.querySelector('.stat-card:nth-child(3) .stat-value');
  const approvedCampaigns = document.querySelector('.stat-card:nth-child(4) .stat-value');
  if (totalUsers && stats.totalUsers) totalUsers.textContent = stats.totalUsers.toLocaleString();
  if (totalVolunteers && stats.totalVolunteers) totalVolunteers.textContent = stats.totalVolunteers.toLocaleString();
  if (approvedCampaigns && stats.approvedCampaigns) approvedCampaigns.textContent = stats.approvedCampaigns;
}

function renderAdminCampaignsTable(pendingCampaigns, approvedCampaigns) {
  const table = document.getElementById('campaignsTable');
  if (!table) return;

  const pending = pendingCampaigns || [];
  const approved = approvedCampaigns || [];

  const rows = [
    ...pending.map(c => ({ c, type: 'PENDING' })),
    ...approved.map(c => ({ c, type: 'APPROVED' }))
  ];

  table.innerHTML = rows.map(({ c, type }) => {
    const goal = c.goalAmount || c.goal || 1;
    const current = c.currentAmount || c.current || 0;
    const pct = Math.round((current / goal) * 100);
    const showApprove = type === 'PENDING';

    return `
      <tr>
        <td><strong>${c.title}</strong><br><small style="color:var(--gray-500);">${c.category || ''}</small></td>
        <td>${c.creator || c.organizationId || '-'}</td>
        <td>$${goal.toLocaleString()}</td>
        <td>$${current.toLocaleString()}</td>
        <td>
          <div class="progress-bar" style="width:100px;">
            <div class="progress-fill" style="width:${pct}%"></div>
          </div>
          <span style="font-size:0.75rem;">${pct}%</span>
        </td>
        <td><span class="status-badge ${(c.status === 'APPROVED' || c.status === 'ACTIVE') ? 'approved' : 'pending'}">${c.status || type}</span></td>
        <td class="action-buttons">
          ${showApprove ? `<button class="action-btn" onclick="adminApproveCampaign(${c.id})">Approve</button>` : ''}
          <button class="action-btn" style="color:var(--danger);" onclick="adminDeleteCampaign(${c.id})">Delete</button>
        </td>
      </tr>
    `;
  }).join('');
}


async function adminApproveCampaign(id) {
  try {
    const user = getCurrentUser();
    if (!user?.email) {
      showToast('Please log in as admin first', 'error');
      window.location.href = 'login.html';
      return;
    }

    // --- Diagnostic logs (approve flow) ---
    const adminHeaders = getAdminHeaders();
    console.log('[adminApproveCampaign] campaign id:', id);
    console.log('[adminApproveCampaign] currentUser:', user);
    console.log('[adminApproveCampaign] headers being sent:', adminHeaders);
    console.log('[adminApproveCampaign] X-User-Email:', adminHeaders['X-User-Email']);
    console.log('[adminApproveCampaign] Authorization present:', !!adminHeaders['Authorization']);

    const res = await fetch(`${API_BASE}/admin/campaigns/${id}/approve`, {
      method: 'PUT',
      headers: adminHeaders
    });

    console.log('[adminApproveCampaign] response status:', res.status);

    if (!res.ok) {
      const errMsg = await parseApiError(res, 'Failed to approve campaign');
      console.error('[adminApproveCampaign] backend error:', errMsg);
      throw new Error(errMsg);
    }

    showToast('Campaign approved!');

    // Refresh only campaigns section (pending + approved)
    const [pending, approved] = await Promise.all([
      apiGetAdminPendingCampaigns(),
      apiGetAdminApprovedCampaigns()
    ]);

    campaignsData = approved;
    renderAdminCampaignsTable(pending, approved);
    renderTopCampaigns(approved);
  } catch(e) {
    console.error('[adminApproveCampaign] caught error:', e);
    showToast(e.message || 'Failed to approve campaign', 'error');
  }
}


async function adminDeleteCampaign(id) {
  if (!confirm('Delete this campaign?')) return;
  try {
    await apiDeleteCampaign(id);
    showToast('Campaign deleted');
    initAdminDashboard();
  } catch(e) { showToast('Failed to delete campaign', 'error'); }
}

function renderUsersTable(users) {
  const table = document.getElementById('usersTable');
  if (!table) return;

  table.innerHTML = users.map(u => `
    <tr>
      <td>${u.name || u.fullName || '-'}</td>
      <td>${u.email}</td>
      <td><span class="status-badge ${u.role === 'ADMIN' ? 'pending' : 'approved'}">${u.role}</span></td>
      <td><span class="status-badge approved">Active</span></td>
      <td>${u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'}</td>
      <td class="action-buttons">
        <button class="action-btn" style="color:var(--danger);" onclick="adminDeleteUser(${u.id})">Delete</button>
      </td>
    </tr>
  `).join('');
}

async function adminDeleteUser(id) {
  if (!confirm('Delete this user?')) return;
  try {
    await apiDeleteUser(id);
    showToast('User deleted');
    initAdminDashboard();
  } catch(e) { showToast(e.message || 'Failed to delete user', 'error'); }
}

async function adminDeleteVolunteer(id) {
  if (!confirm('Delete this volunteer application?')) return;
  try {
    await apiDeleteVolunteer(id);
    showToast('Volunteer deleted');
    initAdminDashboard();
  } catch(e) { showToast(e.message || 'Failed to delete volunteer', 'error'); }
}


function renderTopCampaigns(campaigns) {
  const container = document.getElementById('topCampaigns');
  if (!container) return;
  const sorted = [...campaigns].sort((a, b) => (b.currentAmount || 0) - (a.currentAmount || 0)).slice(0, 5);
  container.innerHTML = sorted.map(c => `
    <div class="top-campaign-item">
      <div class="campaign-info">
        <h4>${c.title}</h4>
        <span>${c.category || ''}</span>
      </div>
      <div class="campaign-amount">$${(c.currentAmount || 0).toLocaleString()}</div>
    </div>
  `).join('');
}

function generateChartBars() {
  const container = document.getElementById('donationChart');
  if (!container) return;
  const data = [45, 62, 58, 78, 92, 108];
  const months = ['Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  container.innerHTML = data.map((v, i) => `
    <div class="bar-container">
      <div class="bar" style="height:${v}px;"></div>
      <span class="bar-label">${months[i]}</span>
    </div>
  `).join('');
}

function loadActivityTimeline() {
  const container = document.getElementById('activityTimeline');
  if (!container) return;
  container.innerHTML = `
    <div class="top-campaign-item">
      <div class="campaign-info">
        <strong>System</strong> ready — connected to backend
        <span style="display:block;font-size:0.75rem;">Just now</span>
      </div>
    </div>
  `;
}

// ========== VOLUNTEER FORM & WIZARD ==========
const VOLUNTEER_PHASE = { SELECT: 1, APPLICATION: 2, REVIEW: 3 };

const volunteerWizardState = {
  phase: VOLUNTEER_PHASE.SELECT,
  currentApplication: null,
  userApplications: []
};

function resolveVolunteerCampaignId() {
  if (selectedCampaignId) return Number(selectedCampaignId);
  const stored = localStorage.getItem('selectedCampaignId');
  if (stored) return parseInt(stored, 10);
  if (campaignsData && campaignsData.length === 1 && campaignsData[0]?.id) {
    return Number(campaignsData[0].id);
  }
  return null;
}

function normalizeVolunteerStatus(status) {
  const s = (status || 'PENDING').toUpperCase();
  if (s === 'ACCEPTED') return 'APPROVED';
  return s;
}

function findUserApplicationForCampaign(campaignId) {
  return volunteerWizardState.userApplications.find(
    v => Number(v.campaignId) === Number(campaignId)
  ) || null;
}

function getCampaignTitleById(campaignId) {
  const c = campaignsData.find(x => Number(x.id) === Number(campaignId));
  return c?.title || null;
}

function setVolunteerPhase(phase) {
  if (!document.getElementById('volunteerPhase1')) return;

  volunteerWizardState.phase = phase;

  document.querySelectorAll('.volunteer-phase').forEach(el => {
    const isActive = Number(el.dataset.phase) === phase;
    el.classList.toggle('active', isActive);
    el.hidden = !isActive;
  });

  document.querySelectorAll('#volunteerProgress .progress-step').forEach(step => {
    const stepNum = Number(step.dataset.step);
    step.classList.remove('active', 'completed');
    if (stepNum === phase) step.classList.add('active');
    else if (stepNum < phase) step.classList.add('completed');
  });

  const continueBtn = document.getElementById('volunteerContinueBtn');
  if (continueBtn) {
    // Enable only in phase 1 AND when a campaign is actually selected.
    continueBtn.disabled = (phase !== VOLUNTEER_PHASE.SELECT) || !getSelectedCampaignIdFromState();
  }

  // Ensure phase1 selection UI is in sync with button enabling.
  const listSelection = document.querySelector('.campaign-option.selected');
  if (listSelection && listSelection.dataset?.id) {
    const idNum = parseInt(listSelection.dataset.id, 10);
    if (!Number.isNaN(idNum)) selectedCampaignId = idNum;
  }
}

function getSelectedCampaignIdFromState() {
  if (selectedCampaignId) return Number(selectedCampaignId);
  const stored = localStorage.getItem('selectedCampaignId');
  if (stored) {
    const parsed = parseInt(stored, 10);
    return Number.isNaN(parsed) ? null : parsed;
  }
  const selectedEl = document.querySelector('.campaign-option.selected');
  if (selectedEl?.dataset?.id) {
    const parsed = parseInt(selectedEl.dataset.id, 10);
    return Number.isNaN(parsed) ? null : parsed;
  }
  return null;
}


function updateSelectedCampaignLabel() {
  const label = document.getElementById('selectedCampaignLabel');
  if (!label) return;
  label.textContent = getCampaignTitleById(selectedCampaignId) || '—';
}

function renderVolunteerReview(application) {
  const container = document.getElementById('volunteerReviewContent');
  if (!container || !application) return;

  const status = normalizeVolunteerStatus(application.status);
  const campaignTitle = application.campaignTitle || getCampaignTitleById(application.campaignId) || `Campaign #${application.campaignId}`;

  const statusMeta = {
    PENDING: {
      css: 'pending',
      icon: 'fa-clock',
      title: 'Application Under Review',
      message: 'Your application has been submitted. The organization will review it and update your status soon.'
    },
    APPROVED: {
      css: 'approved',
      icon: 'fa-check-circle',
      title: 'Application Approved',
      message: 'Congratulations! You have been approved to volunteer for this campaign.'
    },
    REJECTED: {
      css: 'rejected',
      icon: 'fa-times-circle',
      title: 'Application Not Approved',
      message: 'Unfortunately, your application was not accepted for this campaign. You may apply to another campaign.'
    }
  };

  const meta = statusMeta[status] || statusMeta.PENDING;

  container.innerHTML = `
    <div class="review-status-card ${meta.css}">
      <div class="review-icon"><i class="fas ${meta.icon}"></i></div>
      <h3>${meta.title}</h3>
      <p style="color:var(--gray-600);max-width:420px;margin:0 auto;">${meta.message}</p>
      <span class="status-badge ${meta.css}" style="margin-top:1rem;display:inline-block;">${status}</span>
    </div>
    <dl class="review-details">
      <dt>Campaign</dt><dd>${campaignTitle}</dd>
      <dt>Email</dt><dd>${application.email || '—'}</dd>
      <dt>Phone</dt><dd>${application.phone || '—'}</dd>
      <dt>Availability</dt><dd>${application.availability || '—'}</dd>
      <dt>Skills / Experience</dt><dd>${application.skills || application.experience || '—'}</dd>
      ${application.whyJoin ? `<dt>Why you want to volunteer</dt><dd>${application.whyJoin}</dd>` : ''}
    </dl>
  `;
}

async function refreshUserVolunteerApplications() {
  const user = getCurrentUser();
  if (!user?.id) {
    volunteerWizardState.userApplications = [];
    return;
  }
  try {
    volunteerWizardState.userApplications = await apiGetVolunteersByUser(user.id);
  } catch (e) {
    volunteerWizardState.userApplications = [];
  }
}

function prefillVolunteerApplicationForm() {
  const user = getCurrentUser();
  if (!user) return;
  const nameEl = document.getElementById('volName');
  const emailEl = document.getElementById('volEmail');
  if (nameEl && !nameEl.value) nameEl.value = user.name || '';
  if (emailEl && !emailEl.value) emailEl.value = user.email || '';
}

async function proceedAfterCampaignSelection() {
  if (!selectedCampaignId) {
    showToast('Please select a campaign first', 'error');
    return;
  }

  const existing = findUserApplicationForCampaign(selectedCampaignId);
  if (existing) {
    volunteerWizardState.currentApplication = existing;
    renderVolunteerReview(existing);
    setVolunteerPhase(VOLUNTEER_PHASE.REVIEW);
    return;
  }

  volunteerWizardState.currentApplication = null;
  updateSelectedCampaignLabel();
  prefillVolunteerApplicationForm();
  setVolunteerPhase(VOLUNTEER_PHASE.APPLICATION);
}

function volunteerSelectCampaign(id, element) {
  selectedCampaignId = id;
  localStorage.setItem('selectedCampaignId', String(id));
  document.querySelectorAll('.campaign-option').forEach(opt => opt.classList.remove('selected'));
  if (element) element.classList.add('selected');

  const continueBtn = document.getElementById('volunteerContinueBtn');
  if (continueBtn) continueBtn.disabled = false;

  const existing = findUserApplicationForCampaign(id);
  if (existing) {
    const badge = element?.querySelector('.vol-status-badge');
    if (!badge && element) {
      const tag = document.createElement('span');
      tag.className = 'vol-status-badge status-badge pending';
      tag.style.cssText = 'font-size:0.7rem;margin-top:0.35rem;display:inline-block;';
      tag.textContent = normalizeVolunteerStatus(existing.status);
      element.appendChild(tag);
    }
  }
}

function bindVolunteerForm(form) {
  if (!form || form.dataset.bound === 'true') return;

  form.dataset.bound = 'true';
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const user = getCurrentUser();
    if (!user) {
      showToast('Please login first', 'error');
      window.location.href = 'login.html';
      return;
    }

    const campaignId = resolveVolunteerCampaignId();
    if (!campaignId || Number.isNaN(campaignId)) {
      showToast('Please select a campaign first', 'error');
      return;
    }

    const existing = findUserApplicationForCampaign(campaignId);
    if (existing) {
      volunteerWizardState.currentApplication = existing;
      renderVolunteerReview(existing);
      if (document.getElementById('volunteerPhase1')) {
        setVolunteerPhase(VOLUNTEER_PHASE.REVIEW);
      } else {
        showToast('You already applied to this campaign', 'error');
      }
      return;
    }

    const whyJoin = document.getElementById('volMessage')?.value?.trim() || '';
    const availability = document.getElementById('volAvailability')?.value?.trim() || '';
    const experience = document.getElementById('volExperience')?.value?.trim() || '';
    const phone = document.getElementById('volPhone')?.value?.trim() || '';
    const skills = experience || whyJoin;

    if (!whyJoin) {
      showToast('Please write a reason for volunteering.', 'error');
      return;
    }

    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = 'Submitting...';
    }

    try {
      const result = await apiApplyVolunteer(user.id, campaignId, whyJoin, skills, availability, experience, phone);
      const enriched = {
        ...result,
        campaignTitle: result.campaignTitle || getCampaignTitleById(campaignId)
      };
      volunteerWizardState.currentApplication = enriched;
      await refreshUserVolunteerApplications();
      showToast('Volunteer application submitted successfully!');

      if (document.getElementById('volunteerPhase1')) {
        renderVolunteerReview(enriched);
        setVolunteerPhase(VOLUNTEER_PHASE.REVIEW);
        form.reset();
        prefillVolunteerApplicationForm();
      } else {
        closeModal('volunteerModal');
        form.reset();
      }
    } catch (err) {
      showToast(err.message || 'Application failed', 'error');
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Application';
      }
    }
  });
}

function initVolunteerForms() {
  bindVolunteerForm(document.getElementById('volunteerForm'));
  bindVolunteerForm(document.getElementById('volunteerApplyForm'));
}

async function initVolunteerWizard() {
  // Enable volunteer applications for NORMAL users.
  // Organizations are not allowed.
  const user = getCurrentUser();
  if (!user) {
    showToast('Please log in to apply as a volunteer', 'error');
    window.location.href = 'login.html';
    return;
  }

  if (user.role === 'ORGANIZATION') {
    showToast('Volunteer applications are not allowed for organization accounts', 'error');
    window.location.href = 'organization.html';
    return;
  }

  initVolunteerForms();
  await refreshUserVolunteerApplications();
  await loadCampaignSelectList();
  prefillVolunteerApplicationForm();

  // If user already applied for the selected campaign, sync UI immediately.
  // This ensures the wizard jumps straight to REVIEW when appropriate.
  await syncVolunteerWizardUIWithExistingApplication();

  // Default phase (selection)
  if (volunteerWizardState.phase !== VOLUNTEER_PHASE.REVIEW) {
    setVolunteerPhase(VOLUNTEER_PHASE.SELECT);
  }

  document.getElementById('volunteerContinueBtn')?.addEventListener('click', proceedAfterCampaignSelection);
  document.getElementById('volunteerBackToSelectBtn')?.addEventListener('click', () => setVolunteerPhase(VOLUNTEER_PHASE.SELECT));
  document.getElementById('volunteerBackFromReviewBtn')?.addEventListener('click', () => setVolunteerPhase(VOLUNTEER_PHASE.SELECT));

  const preselectIdRaw = new URLSearchParams(window.location.search).get('campaignId') || localStorage.getItem('selectedCampaignId');
  if (preselectIdRaw) {
    const id = parseInt(preselectIdRaw, 10);
    if (Number.isFinite(id)) {
      const option = document.querySelector(`.campaign-option[data-id="${id}"]`);
      if (option) volunteerSelectCampaign(id, option);
    }
  }
}


async function loadCampaignSelectList() {
  const container = document.getElementById('campaignSelectList');
  if (!container) return;
  try {
    const campaigns = await apiGetApprovedCampaigns();
    const open = (campaigns || []).filter(c => campaignAllowsVolunteers(c));
    campaignsData = open;
    if (open.length === 0) {
      container.innerHTML = '<p style="color:var(--gray-500);">No campaigns are accepting volunteers right now.</p>';
      return;
    }
    container.innerHTML = open.map(c => {
      const existing = findUserApplicationForCampaign(c.id);
      const statusTag = existing
        ? `<span class="status-badge ${normalizeVolunteerStatus(existing.status) === 'APPROVED' ? 'approved' : normalizeVolunteerStatus(existing.status) === 'REJECTED' ? 'rejected' : 'pending'}" style="font-size:0.7rem;margin-top:0.35rem;display:inline-block;">${normalizeVolunteerStatus(existing.status)}</span>`
        : '';
      return `
      <div class="campaign-option" data-id="${c.id}" onclick="volunteerSelectCampaign(${c.id}, this)">
        <strong>${c.title}</strong>
        <div style="font-size:0.875rem;color:var(--gray-500);">${c.category || ''}</div>
        ${statusTag}
      </div>
    `;
    }).join('');
  } catch (e) {
    container.innerHTML = '<p style="color:var(--gray-500);">Could not load campaigns</p>';
  }
}

function selectCampaign(id, element) {
  volunteerSelectCampaign(id, element);
}

async function syncVolunteerWizardUIWithExistingApplication() {
  // Determine which campaign to sync
  const preselectId = parseInt(new URLSearchParams(window.location.search).get('campaignId') || localStorage.getItem('selectedCampaignId') || '', 10);
  if (!preselectId || Number.isNaN(preselectId)) return;

  const existing = findUserApplicationForCampaign(preselectId);
  if (!existing) return;

  selectedCampaignId = preselectId;
  localStorage.setItem('selectedCampaignId', String(preselectId));

  // Mark selected campaign in the list (if list exists)
  const optionEl = document.querySelector(`.campaign-option[data-id="${preselectId}"]`);
  if (optionEl) {
    document.querySelectorAll('.campaign-option').forEach(opt => opt.classList.remove('selected'));
    optionEl.classList.add('selected');
  }

  volunteerWizardState.currentApplication = existing;
  renderVolunteerReview(existing);
  updateSelectedCampaignLabel();
  setVolunteerPhase(VOLUNTEER_PHASE.REVIEW);
}


// ========== MODALS ==========
function openVolunteerModal(campaignId) {
  const campaign = campaignsData.find(c => c.id === campaignId);
  if (campaign && !campaignAllowsVolunteers(campaign)) {
    showToast('This campaign is not accepting volunteers', 'error');
    return;
  }
  const modal = document.getElementById('volunteerModal');
  if (modal) {
    selectedCampaignId = campaignId;
    localStorage.setItem('selectedCampaignId', String(campaignId));
    modal.style.display = 'flex';
  }
}


function openDonateModal(campaignId) {
  const modal = document.getElementById('donateModal');
  if (modal) {
    selectedCampaignId = campaignId;
    modal.style.display = 'flex';
  }
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.style.display = 'none';
}

function closeSuccessModal() {
  const modal = document.getElementById('successModal');
  if (modal) modal.style.display = 'none';
  window.location.href = 'index.html';
}

// ========== DONATE MODAL SUBMIT ==========
// Works with the custom amount input (any value user types)
function bindDonationSubmit() {
  const btn = document.getElementById('donateSubmitBtn');
  if (!btn) return false;

  // Prevent double-binding
  if (btn.dataset.bound === 'true') return true;
  btn.dataset.bound = 'true';

  btn.addEventListener('click', async () => {
    const user = getCurrentUser();
    if (!user) { showToast('Please login to donate', 'error'); return; }
    if (!selectedCampaignId) { showToast('No campaign selected', 'error'); return; }

    const input = document.getElementById('donationAmount');
    const raw = input ? input.value : '';
    const amount = parseFloat(raw);

    if (!isFinite(amount) || amount <= 0) {
      showToast('Please enter a valid donation amount', 'error');
      if (input) input.focus();
      return;
    }

    try {
      await apiSubmitDonation(user.id, selectedCampaignId, amount);
      showToast(`Thank you! $${amount} donated successfully!`);
      closeModal('donateModal');

      if (input) input.value = '';
    } catch (err) {
      showToast(err.message || 'Donation failed', 'error');
    }
  });
  return true;
}

document.addEventListener('DOMContentLoaded', () => {
  bindDonationSubmit();
  // in case the script loads before DOM is ready
  setTimeout(bindDonationSubmit, 0);
});



// ========== AUTH FORMS ==========
function initAuthForms() {
  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = loginForm.querySelector('input[type="email"]').value;
      const password = loginForm.querySelector('input[type="password"]').value;
      try {
        const auth = await apiLogin(email, password);
        persistAuthResponse(auth);
        await refreshCurrentUserFromServer();
        showToast('Welcome back!');
        const role = getCurrentUser()?.role || 'USER';
        setTimeout(() => redirectByRole(role), 1000);
      } catch (err) {
        showToast(err.message || 'Login failed', 'error');
      }
    });
  }

  const registerForm = document.getElementById('registerForm');
  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const firstName = registerForm.querySelector('input[placeholder="John"]').value;
      const lastName = registerForm.querySelector('input[placeholder="Doe"]').value;
      const name = `${firstName} ${lastName}`;
      const email = registerForm.querySelector('input[type="email"]').value;
      const password = registerForm.querySelector('input[type="password"]').value;
      const roleInput = registerForm.querySelector('input[name="role"]:checked');
      const role = roleInput ? roleInput.value.toUpperCase() : 'USER';

      try {
        await apiRegister(name, email, password, role);
        showToast('Account created successfully!');
        setTimeout(() => window.location.href = 'login.html', 1500);
      } catch (err) {
        showToast(err.message || 'Registration failed', 'error');
      }
    });
  }
}

function togglePassword() {
  const input = document.getElementById('password');
  if (input) {
    input.type = input.type === 'password' ? 'text' : 'password';
    const icon = document.querySelector('.toggle-password');
    if (icon) { icon.classList.toggle('fa-eye-slash'); icon.classList.toggle('fa-eye'); }
  }
}

// ========== TOAST ==========
function showToast(message, type = 'success') {
  let container = document.getElementById('toastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toastContainer';
    container.style.cssText = 'position:fixed;bottom:20px;right:20px;z-index:9999;';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i><span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => { toast.style.animation = 'slideOutRight 0.3s forwards'; setTimeout(() => toast.remove(), 300); }, 3000);
}

// ========== SKELETON ==========
function showSkeleton() {
  const s = document.getElementById('skeletonLoader'); if (s) s.style.display = 'grid';
  const g = document.getElementById('campaignsGrid'); if (g) g.style.display = 'none';
}
function hideSkeleton() {
  const s = document.getElementById('skeletonLoader'); if (s) s.style.display = 'none';
  const g = document.getElementById('campaignsGrid'); if (g) g.style.display = 'flex';
}

// ========== PROFILE PAGE ==========
async function initProfilePage() {
  if (!document.querySelector('.profile-container')) return;

  updateNavForLoggedInUser();

  const user = getCurrentUser();
  if (!user) { window.location.href = 'login.html'; return; }

  // ✅ Setup tabs immediately so they work before async data loads
  setupProfileTabs();

  // Set header info
  const nameEl = document.getElementById('profileName');
  const emailEl = document.getElementById('profileEmail');
  const roleEl = document.getElementById('profileRole');
  const roleBadge = document.getElementById('profileRoleBadge');
  if (nameEl) nameEl.textContent = user.name;
  if (emailEl) emailEl.textContent = user.email;
  if (roleEl) roleEl.textContent = user.role;
  if (roleBadge) roleBadge.textContent = user.role;

  // Load donations
  let donations = [];
  try {
    donations = await apiGetDonationHistory(user.id);
    renderDonationHistory(donations);

    const totalDonated = donations.reduce((s, d) => s + (d.amount || 0), 0);
    const totalEl = document.getElementById('totalDonated');
    const campaignsEl = document.getElementById('campaignsJoined');
    if (totalEl) totalEl.textContent = '$' + totalDonated.toLocaleString();
    if (campaignsEl) campaignsEl.textContent = donations.length;
    if (donations.length > 0) {
      const badge = document.getElementById('donorBadge');
      if (badge) badge.style.display = 'inline-flex';
    }

    // Supported campaigns tab
    renderSupportedCampaigns(donations);
  } catch(e) { renderDonationHistory([]); }

  // Load volunteer history
  try {
    const allCampaigns = await apiGetCampaigns();
    let myVolunteers = [];
    for (const c of allCampaigns) {
      try {
        const vols = await apiGetVolunteersByCampaign(c.id);
        const mine = vols.filter(v => v.userId === user.id).map(v => ({ ...v, campaignTitle: c.title }));
        myVolunteers = myVolunteers.concat(mine);
      } catch(e) {}
    }
    const volEl = document.getElementById('volunteerCount');
    if (volEl) volEl.textContent = myVolunteers.length;
    if (myVolunteers.length > 0) {
      const badge = document.getElementById('volunteerBadge');
      if (badge) badge.style.display = 'inline-flex';
    }
    renderVolunteerHistory(myVolunteers);
  } catch(e) {}

  // Recent activity
  renderRecentActivity(donations);

  // Settings form
  const settingsForm = document.getElementById('profileSettingsForm');
  if (settingsForm) {
    const nameInput = document.getElementById('settingsName');
    const emailInput = document.getElementById('settingsEmail');
    if (nameInput) nameInput.value = user.name;
    if (emailInput) emailInput.value = user.email;

    settingsForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const newPass = document.getElementById('settingsPassword')?.value;
      const confirmPass = document.getElementById('settingsPasswordConfirm')?.value;
      if (newPass && newPass !== confirmPass) { showToast('Passwords do not match!', 'error'); return; }

      const updatedUser = getCurrentUser();
      if (nameInput?.value) updatedUser.name = nameInput.value;
      if (emailInput?.value) updatedUser.email = emailInput.value;
      saveCurrentUser(updatedUser);
      if (document.getElementById('profileName')) document.getElementById('profileName').textContent = updatedUser.name;
      showToast('Profile updated successfully!');
    });
  }

  // tabs already set up at top
}

function renderVolunteerHistory(volunteers) {
  const container = document.getElementById('volunteerHistoryList');
  if (!container) return;
  if (!volunteers || volunteers.length === 0) {
    container.innerHTML = '<p style="color:#9ca3af;padding:1rem 0;">No volunteer applications yet.</p>';
    return;
  }
  container.innerHTML = volunteers.map(v => `
    <div class="volunteer-card">
      <div class="vol-info">
        <h4>${v.campaignTitle || 'Campaign #' + v.campaignId}</h4>
        <p>${v.skills || '-'} · ${v.availability || '-'}</p>
        ${v.whyJoin ? `<p style="margin-top:0.25rem;color:#6b7280;font-size:0.8rem;font-style:italic;">"${v.whyJoin}"</p>` : ''}
      </div>
      <span class="status-badge ${v.status === 'APPROVED' ? 'approved' : v.status === 'REJECTED' ? 'rejected' : 'pending'}">${v.status || 'PENDING'}</span>
    </div>
  `).join('');
}

function renderSupportedCampaigns(donations) {
  const container = document.getElementById('supportedCampaignsList');
  if (!container) return;
  if (!donations || donations.length === 0) {
    container.innerHTML = '<p style="color:#9ca3af;padding:1rem 0;">No supported campaigns yet.</p>';
    return;
  }
  const unique = [...new Map(donations.map(d => [d.campaignId, d])).values()];
  container.innerHTML = unique.map(d => `
    <div class="campaign-simple-card">
      <div>
        <strong>Campaign #${d.campaignId}</strong>
        <div style="font-size:0.8rem;color:#6b7280;">${d.date || ''}</div>
      </div>
      <div style="display:flex;align-items:center;gap:1rem;">
        <span style="font-weight:700;color:#3B82F6;">$${d.amount}</span>
        <span class="status-badge approved">${d.status || 'COMPLETED'}</span>
      </div>
    </div>
  `).join('');
}

function renderRecentActivity(donations) {
  const container = document.getElementById('recentActivity');
  if (!container) return;
  if (!donations || donations.length === 0) {
    container.innerHTML = '<p style="color:#9ca3af;">No recent activity.</p>';
    return;
  }
  const recent = donations.slice(0, 5);
  container.innerHTML = recent.map(d => `
    <div class="activity-item">
      <div class="activity-icon donation"><i class="fas fa-heart"></i></div>
      <div>
        <strong>Donated $${d.amount}</strong> to Campaign #${d.campaignId}
        <div style="font-size:0.8rem;color:#9ca3af;">${d.date || 'Recently'}</div>
      </div>
    </div>
  `).join('');
}

function renderDonationHistory(donations) {
  const container = document.getElementById('donationsTable');
  if (!container) return;

  if (!donations || donations.length === 0) {
    container.innerHTML = '<p style="color:var(--gray-500);padding:1rem;">No donations yet.</p>';
    return;
  }

  container.innerHTML = `
    <table class="data-table">
      <thead><tr><th>Campaign</th><th>Amount</th><th>Date</th><th>Status</th></tr></thead>
      <tbody>
        ${donations.map(d => `
          <tr>
            <td>${d.campaignTitle ? escapeHtml(d.campaignTitle) : `Campaign #${d.campaignId}`}</td>
            <td><strong>$${d.amount}</strong></td>
            <td>${d.date || '-'}</td>
            <td><span class="status-badge approved">${d.status || 'COMPLETED'}</span></td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

function setupProfileTabs() {
  const tabBtns = document.querySelectorAll('.profile-tabs .tab-btn');
  if (tabBtns.length === 0) return;

  tabBtns.forEach(btn => {
    // Remove old listeners by cloning
    const newBtn = btn.cloneNode(true);
    btn.parentNode.replaceChild(newBtn, btn);
  });

  // Re-query after clone
  document.querySelectorAll('.profile-tabs .tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const tabId = btn.dataset.tab;
      document.querySelectorAll('.profile-tabs .tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      document.querySelectorAll('.profile-container .tab-content').forEach(t => t.classList.remove('active'));
      const activeTab = document.getElementById(tabId + 'Tab');
      if (activeTab) activeTab.classList.add('active');
    });
  });
}

// ========== ORGANIZATION PAGE ==========
async function initOrganizationPage() {
  if (!document.querySelector('.org-container')) return;
  const user = guardPage(['ORGANIZATION']);
  if (!user) return;
  await refreshCurrentUserFromServer();
  const orgUser = getCurrentUser();
  if (!orgUser?.id) {
    showToast('Organization profile is incomplete. Please log in again.', 'error');
    return;
  }
  updateNavForLoggedInUser();
  setupOrgTabs();
  setupOrgFilters();

  try {
    const campaigns = await apiGetCampaigns();
    const orgId = Number(orgUser.id);
    campaignsData = campaigns.filter(c => Number(c.organizationId) === orgId);
    renderOrgCampaigns();
  } catch(e) { console.warn('Could not load org campaigns'); }

  // New Campaign Form
  const newCampaignForm = document.getElementById('newCampaignForm');
  if (newCampaignForm) {
    newCampaignForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const currentOrg = getCurrentUser();
      if (!currentOrg?.id) { showToast('Please login as an organization', 'error'); return; }

      const title = document.getElementById('campaignTitle')?.value?.trim();
      const description = document.getElementById('campaignDescription')?.value?.trim();
      const category = document.getElementById('campaignCategory')?.value?.trim();
      const goalAmount = parseFloat(document.getElementById('campaignGoal')?.value);
      if (!title) { showToast('Campaign title is required', 'error'); return; }
      if (!category) { showToast('Category is required', 'error'); return; }
      if (!Number.isFinite(goalAmount) || goalAmount <= 0) {
        showToast('Enter a valid goal amount', 'error');
        return;
      }

      const deadline = new Date();
      deadline.setDate(deadline.getDate() + (parseInt(document.getElementById('campaignDuration')?.value, 10) || 30));

      try {
        const created = await apiCreateCampaign({
          title,
          description: description || title,
          category,
          goalAmount,
          deadline: deadline.toISOString().split('T')[0],
          organizationId: Number(currentOrg.id),
          allowVolunteers: document.getElementById('campaignAllowVolunteers')?.checked ?? true
        });
        const fileInput = document.getElementById('campaignImageFile');
        if (fileInput?.files?.[0] && created?.id && typeof apiUploadCampaignImage === 'function') {
          await apiUploadCampaignImage(created.id, fileInput.files[0]);
        }
        showToast('Campaign created successfully!');
        closeModal('createCampaignModal');
        newCampaignForm.reset();
        if (fileInput) fileInput.value = '';
        const campaigns = await apiGetCampaigns();
        const orgId = Number(currentOrg.id);
        campaignsData = campaigns.filter(c => Number(c.organizationId) === orgId);
        renderOrgCampaigns();
      } catch(err) { showToast(err.message || 'Failed to create campaign', 'error'); }
    });
  }

  const settingsForm = document.getElementById('orgSettingsForm');
  if (settingsForm) {
    settingsForm.addEventListener('submit', (e) => { e.preventDefault(); showToast('Settings saved!'); });
  }
}

function renderOrgCampaigns(filter = 'all', search = '') {
  const container = document.getElementById('orgCampaignsList');
  if (!container) return;

  let filtered = campaignsData.filter(c => filter === 'all' || (c.status && c.status.toLowerCase() === filter));
  if (search) filtered = filtered.filter(c => c.title.toLowerCase().includes(search.toLowerCase()));

  container.innerHTML = filtered.map(c => {
    const goal = c.goalAmount || c.goal || 1;
    const current = c.currentAmount || c.current || 0;
    return `
      <div class="org-campaign-card">
        <div class="org-campaign-image" style="background-image:url('${c.image || 'https://images.unsplash.com/photo-1509062522246-3755977927d7?w=400'}')">
          <span class="campaign-status status-${(c.status || 'pending').toLowerCase()}">${c.status || 'PENDING'}</span>
        </div>
        <div class="org-campaign-content">
          <h4>${c.title}</h4>
          <div class="progress-section">
            <div class="progress-bar">
              <div class="progress-fill" style="width:${Math.min((current / goal) * 100, 100)}%"></div>
            </div>
            <div class="campaign-stats">
              <span>$${current.toLocaleString()} raised</span>
              <span>of $${goal.toLocaleString()}</span>
            </div>
          </div>
          <div class="org-campaign-actions">
            <button class="btn-secondary btn-sm" onclick="editOrgCampaign(${c.id})">Edit</button>
            <button class="btn-outline btn-sm" onclick="viewCampaign(${c.id})">View</button>
          </div>
        </div>
      </div>
    `;
  }).join('') || '<p style="color:var(--gray-500);">No campaigns found.</p>';
}

function setupOrgTabs() {
  const tabBtns = document.querySelectorAll('.org-tabs .tab-btn');
  if (tabBtns.length === 0) return;

  tabBtns.forEach(btn => {
    const newBtn = btn.cloneNode(true);
    btn.parentNode.replaceChild(newBtn, btn);
  });

  document.querySelectorAll('.org-tabs .tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const tabId = btn.dataset.tab;
      document.querySelectorAll('.org-tabs .tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      document.querySelectorAll('.org-container .tab-content').forEach(t => t.classList.remove('active'));
      const activeTab = document.getElementById(tabId + 'Tab');
      if (activeTab) activeTab.classList.add('active');
    });
  });
}

function setupOrgFilters() {
  const searchInput = document.getElementById('orgCampaignSearch');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      const filter = document.getElementById('campaignStatusFilter')?.value || 'all';
      renderOrgCampaigns(filter, e.target.value);
    });
  }
  const statusFilter = document.getElementById('campaignStatusFilter');
  if (statusFilter) {
    statusFilter.addEventListener('change', (e) => {
      const search = document.getElementById('orgCampaignSearch')?.value || '';
      renderOrgCampaigns(e.target.value, search);
    });
  }
}

function editOrgCampaign(id) {
  const c = campaignsData.find(x => x.id === id);
  if (!c) return;

  const idField = document.getElementById('editCampaignId');
  const titleField = document.getElementById('editCampaignTitle');
  const goalField = document.getElementById('editCampaignGoal');
  const catField = document.getElementById('editCampaignCategory');
  const descField = document.getElementById('editCampaignDescription');

  if (idField) idField.value = c.id;
  if (titleField) titleField.value = c.title;
  if (goalField) goalField.value = c.goalAmount || c.goal || '';
  if (catField) catField.value = c.category || catField.value;
  if (descField) descField.value = c.description || '';

  // Prefill photo preview
  const previewEl = document.getElementById('editCampaignImagePreview');
  if (previewEl) previewEl.src = c.image || previewEl.src;
  const fileInputEl = document.getElementById('editCampaignImageFile');
  if (fileInputEl) fileInputEl.value = '';

  const modal = document.getElementById('editCampaignModal');
  if (modal) modal.style.display = 'flex';

  const editForm = document.getElementById('editCampaignForm');
  if (editForm) {
    editForm.onsubmit = async (e) => {
      e.preventDefault();
      try {
        // 1) Update text fields
        await apiUpdateCampaign(c.id, {
          title: titleField?.value,
          goalAmount: parseFloat(goalField?.value),
          category: document.getElementById('editCampaignCategory')?.value,
          description: document.getElementById('editCampaignDescription')?.value
        });

        // 2) Upload image if selected
        if (fileInputEl?.files?.[0]) {
          const formData = new FormData();
          formData.append('file', fileInputEl.files[0]);

          const uploadRes = await fetch(`${BACKEND_URL}/api/campaigns/${c.id}/image`, {
            method: 'POST',
            body: formData
          });

          if (!uploadRes.ok) {
            const err = await uploadRes.json().catch(() => ({}));
            throw new Error(err.error || err.message || 'Failed to upload campaign image');
          }
        }

        showToast('Campaign updated!');
        closeModal('editCampaignModal');
        const campaigns = await apiGetCampaigns();
        campaignsData = campaigns;
        renderOrgCampaigns();
      } catch(err) {
        showToast(err.message || 'Failed to update', 'error');
      }
    };
  }
}

async function deleteCampaign() {
  const id = document.getElementById('editCampaignId')?.value;
  if (!id || !confirm('Delete this campaign?')) return;
  try {
    await apiDeleteCampaign(id);
    showToast('Campaign deleted');
    closeModal('editCampaignModal');
    const campaigns = await apiGetCampaigns();
    campaignsData = campaigns;
    renderOrgCampaigns();
  } catch(e) { showToast('Failed to delete', 'error'); }
}

function createNewCampaign() {
  const modal = document.getElementById('createCampaignModal');
  if (modal) modal.style.display = 'flex';
}

// ========== MISC ==========
function editProfile() { showToast('Edit profile (coming soon)'); }
function shareProfile() { showToast('Profile link copied!'); }
function editOrganization() { showToast('Edit organization settings'); }
function inviteTeamMember() { showToast('Invitation sent!'); }

// ========== MAIN INIT ==========
document.addEventListener('DOMContentLoaded', async () => {
  // Always update nav header with logged-in user info on all pages
  updateNavForLoggedInUser();

  // Restore avatar/cover for Profile/Organization pages even after re-login
  try { loadSavedImages(); } catch (e) {}

  // Stats animation
  if (document.querySelector('.statistics')) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => { if (entry.isIntersecting) { animateStats(); observer.unobserve(entry.target); } });
    }, { threshold: 0.3 });
    const statsSection = document.querySelector('.statistics');
    if (statsSection) observer.observe(statsSection);
  }

  // Hero slider
  if (document.getElementById('heroSlider')) initHeroSlider();

// Home page campaigns from backend (ACTIVE ONLY)
  if (document.getElementById('campaignsGrid')) {
    showSkeleton();
    try {
      // Home page must show APPROVED campaigns
      campaignsData = await apiGetApprovedCampaigns();
    } catch(e) {
      console.warn('Backend not available, no campaigns loaded');
      campaignsData = [];
    }
    renderCampaigns();
    hideSkeleton();
    setupFilters();
  }


  // Admin dashboard
  if (document.querySelector('.dashboard-layout')) {
    await initAdminDashboard();
  }

  // Campaign details page
  if (document.getElementById('campaignDetailContainer')) {
    await loadCampaignDetails();
  }

  // Volunteer wizard (volunteer.html) or modal forms (campaign-details.html)
  if (document.getElementById('volunteerPhase1')) {
    await initVolunteerWizard();
  } else {
    initVolunteerForms();
  }

  // Auth forms
  initAuthForms();

  // Profile page
  await initProfilePage();

  // Organization page
  await initOrganizationPage();

  // Close modals on outside click
  window.onclick = (e) => {
    if (e.target.classList && e.target.classList.contains('modal-overlay')) {
      e.target.style.display = 'none';
    }
  };
});

// ========== GLOBAL EXPORTS ==========
window.viewCampaign = viewCampaign;
window.openVolunteerModal = openVolunteerModal;
window.openDonateModal = openDonateModal;
window.closeModal = closeModal;
window.closeSuccessModal = closeSuccessModal;
window.showToast = showToast;
window.togglePassword = togglePassword;
window.selectCampaign = selectCampaign;
window.createNewCampaign = createNewCampaign;
window.editOrgCampaign = editOrgCampaign;
window.deleteCampaign = deleteCampaign;
window.inviteTeamMember = inviteTeamMember;
window.editProfile = editProfile;
window.shareProfile = shareProfile;
window.editOrganization = editOrganization;
window.toggleMobileMenu = toggleMobileMenu;
window.animateStats = animateStats;
window.adminApproveCampaign = adminApproveCampaign;
window.adminDeleteCampaign = adminDeleteCampaign;
window.adminDeleteUser = adminDeleteUser;
window.adminDeleteVolunteer = adminDeleteVolunteer;
window.logout = logout;

window.guardPage = guardPage;
window.getAccessToken = getAccessToken;
window.saveAccessToken = saveAccessToken;
window.getAuthHeaders = getAuthHeaders;
window.persistAuthResponse = persistAuthResponse;
window.refreshCurrentUserFromServer = refreshCurrentUserFromServer;
window.redirectByRole = redirectByRole;
window.campaignAllowsVolunteers = campaignAllowsVolunteers;
window.volunteerSelectCampaign = volunteerSelectCampaign;
window.selectCampaign = selectCampaign;
