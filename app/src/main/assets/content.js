const DEFAULTS = {
  enabled: true, dark: false, bg: '',
  glass: false, opacity: 55, blur: 10,
  font: '', color: '', colorLinks: false,
  contrast: true, loginCenter: true,
  hdr: true, hdrC1: '#2563eb', hdrC2: '#7c3aed', hdrC3: '#db2777', hdrAnim: true,
  particles: true
};

// Selector Moodle (tema Boost) di lms.warmadewa.ac.id.
// Jika ada area yang masih tertutup, tambahkan selector-nya di sini (F12 > Inspect).
const WRAPPERS = '#page, #page-wrapper, #page-content, #region-main-box, .main-inner, #topofscroll, .drawers';
const PANELS = '#region-main, .navbar, .secondary-navigation, .drawer, .courseindex, .login-container, #page-footer, .card';
const INNER = [
  '#region-main .card', '#region-main .block', '#region-main .list-group-item', '#region-main .activity-item',
  '.drawer .card', '.drawer .block', '.drawer .list-group-item',
  '.card .card', '.card .list-group-item', '.navbar .primary-navigation'
].join(', ');

const ICON = 'i, .icon, [class^="fa"], [class*=" fa"]';
const MEDIA = 'img, picture, video, canvas, iframe, [style*="background-image"], [data-lmsc-img]';
const FLT = 'invert(1) hue-rotate(180deg)';

// Dark mode memakai filter, jadi warna pilihan user harus "dibalik" dulu
// supaya yang tampil tetap sesuai pilihan.
function compensate(hex) {
  const n = parseInt(hex.slice(1), 16);
  const r = ((n >> 16) & 255) / 255, g = ((n >> 8) & 255) / 255, b = (n & 255) / 255;
  const cl = (v) => Math.min(1, Math.max(0, v));
  const out = [
    cl(-0.574 * r + 1.430 * g + 0.144 * b),
    cl(0.426 * r + 0.430 * g + 0.144 * b),
    cl(0.426 * r + 1.430 * g - 0.856 * b)
  ].map((v) => Math.round((1 - v) * 255).toString(16).padStart(2, '0'));
  return '#' + out.join('');
}

function buildCSS(s) {
  if (!s.enabled) return '';
  let css = '';
  if (s.loginCenter) {
    css += `
      [data-lmsc-hide] { display: none !important; }
      [data-lmsc-wide] { width: 100% !important; max-width: none !important; flex: 1 1 100% !important;
        margin-left: 0 !important; margin-right: 0 !important; left: 0 !important; float: none !important; }`;
  }
  const userColor = s.color ? (s.dark ? compensate(s.color) : s.color) : '';
  // Kontras tinggi: teks pakai hitam murni (di dark mode otomatis jadi putih murni)
  const textColor = userColor || (s.contrast ? '#000000' : '');

  // ---- DARK MODE (invert filter, gambar/video dikembalikan ke warna asli)
  if (s.dark) {
    css += `
      html { filter: ${FLT} !important; background: #fff; }
      ${MEDIA} { filter: ${FLT}; }
      :is(${MEDIA}) :is(img, picture, video, canvas, iframe) { filter: none; }
      #lmsc-bg { filter: ${FLT}; }`;
  }

  // ---- BACKGROUND + PANEL (solid / translucent)
  if (s.bg) {
    const url = s.bg.replace(/"/g, '%22');
    const alpha = s.glass ? s.opacity / 100 : 0.92;
    const blur = s.glass
      ? `backdrop-filter: blur(${s.blur}px) saturate(1.2) !important; -webkit-backdrop-filter: blur(${s.blur}px) saturate(1.2) !important;`
      : '';
    const navColor = textColor || '#111111';
    css += `
      html, body { background: transparent !important; }
      ${WRAPPERS} { background: transparent !important; }
      ${PANELS} { background: rgba(255,255,255,${alpha}) !important; ${blur} border-color: rgba(128,128,128,.3) !important; }
      .drawer { background: rgba(255,255,255,${Math.max(alpha, 0.7)}) !important; }
      body #page-footer, body #page-footer *:not([class*="btn"]):not([class*="btn"] *) { color: ${textColor || '#111111'} !important; }
      ${INNER} { background: transparent !important; box-shadow: none !important; backdrop-filter: none !important; border-color: rgba(128,128,128,.3) !important; }
      .navbar, .navbar *:not(.badge):not(.count-container) { color: ${navColor} !important; }
      #lmsc-bg { position: fixed; inset: 0; z-index: -1; background: url("${url}") center / cover no-repeat; }`;
  }

  // ---- FONT
  if (s.font) css += `:where(*:not(${ICON})) { font-family: ${s.font} !important; }`;

  // ---- WARNA FONT (hanya teks umum; warna khusus seperti merah/peringatan tetap)
  if (textColor) {
    css += `
      :root { --bs-body-color: ${textColor}; --bs-heading-color: ${textColor}; }
      body, :where(p, li, td, th, label, dt, dd, h1, h2, h3, h4, h5, h6, .card-title, .card-text, .list-group-item) { color: ${textColor} !important; }`;
    if (s.colorLinks) css += `a:not(.btn), .nav-link { color: ${textColor} !important; }`;
  }

  // ---- KONTRAS TINGGI: teks pucat dibuat tegas
  if (s.contrast) {
    css += `
      body { font-weight: 500; }
      body :is(.text-muted, .text-secondary, .dimmed_text, .dimmed) { color: ${textColor} !important; opacity: 1 !important; }
      body :is(.btn-outline-secondary, .dropdown-toggle, .form-control, .form-select, .custom-select):not(.btn-primary):not(.btn-danger) {
        color: ${textColor} !important; opacity: 1 !important; border-color: rgba(0,0,0,.55) !important;
      }
      ::placeholder { color: ${textColor} !important; opacity: .65 !important; }`;
    if (s.bg) css += `body * { text-shadow: 0 0 3px rgba(255,255,255,.85), 0 0 1px rgba(255,255,255,.9); }`;
  }
  // ---- HEADER: gradient (opsional animasi) + canvas partikel
  if (s.hdr || s.particles) {
    css += `
      nav.navbar { isolation: isolate; }
      #lmsc-particles { position: absolute; inset: 0; width: 100%; height: 100%; z-index: -1; pointer-events: none; }`;
  }
  if (s.hdr) {
    const comp = (h) => (s.dark ? compensate(h) : h);
    css += `
      @keyframes lmscGrad { 0% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } }
      nav.navbar, .navbar.fixed-top {
        background: linear-gradient(120deg, ${comp(s.hdrC1)}, ${comp(s.hdrC2)}, ${comp(s.hdrC3)}) !important;
        background-size: ${s.hdrAnim ? '300% 300%' : '100% 100%'} !important;
        ${s.hdrAnim ? 'animation: lmscGrad 14s ease infinite !important;' : ''}
        backdrop-filter: none !important;
        border-bottom: 1px solid rgba(255,255,255,.3) !important;
        box-shadow: 0 2px 14px rgba(0,0,0,.25) !important;
      }
      nav.navbar .primary-navigation { background: transparent !important; }
      body nav.navbar *:not(.badge):not(.count-container):not(.dropdown-menu):not(.dropdown-menu *) {
        color: ${comp('#ffffff')} !important; text-shadow: 0 1px 3px rgba(0,0,0,.35) !important;
      }`;
  }
  return css;
}

let styleEl = null;
let bgEl = null;
let current = DEFAULTS;
let domHooked = false;

// ---------- Halaman login: sembunyikan banner, form jadi di tengah ----------
const KEEP = 'nav, header, footer, script, style, link, .navbar, #page-footer, .drawer';
const BANNER_RE = /banner|slider|carousel|hero|cover|illustration|promo|gallery|login-?(image|img|bg|left|side|visual|art)|loginbg/i;
let touched = [];

const isLoginPage = () => !!document.body &&
  (/pagelayout-login|path-login/.test(document.body.className) || /^page-login/.test(document.body.id));

function looksLikeBanner(el) {
  if (el.matches(KEEP) || el.querySelector('form, input, button, select')) return false;
  if (el.offsetWidth < 150 || el.offsetHeight < 150) return false;
  const cls = (el.getAttribute('class') || '') + ' ' + el.id;
  return !!el.querySelector('img, picture, video, canvas') ||
    getComputedStyle(el).backgroundImage !== 'none' || BANNER_RE.test(cls);
}

function undoLogin() {
  touched.forEach((el) => { el.removeAttribute('data-lmsc-hide'); el.removeAttribute('data-lmsc-wide'); });
  touched = [];
}

function centerLogin(on) {
  undoLogin();
  if (!on || !isLoginPage()) return;
  const anchor = document.querySelector(
    'form#login, .loginform, .login-container, #region-main, [role="main"], form[action*="login"], form[action*="logout"], .card');
  if (!anchor) return;
  let node = anchor;
  while (node.parentElement && node.parentElement !== document.documentElement) {
    const parent = node.parentElement;
    let found = false;
    for (const sib of parent.children) {
      if (sib === node || sib.contains(anchor)) continue;
      if (looksLikeBanner(sib)) {
        sib.setAttribute('data-lmsc-hide', '');
        touched.push(sib);
        found = true;
        console.log('[LMSC] banner disembunyikan:', sib);
      }
    }
    if (found) { node.setAttribute('data-lmsc-wide', ''); touched.push(node); }
    if (parent === document.body) break;
    node = parent;
  }
}

// ---------- Dark mode: gambar latar (CSS background) dikembalikan ke warna asli ----------
let checked = new WeakSet();

function markBgImages(on) {
  if (!document.body) return;
  if (!on) {
    document.querySelectorAll('[data-lmsc-img]').forEach((e) => e.removeAttribute('data-lmsc-img'));
    checked = new WeakSet();
    return;
  }
  document.body.querySelectorAll('*').forEach((el) => {
    if (checked.has(el)) return;
    checked.add(el);
    const bi = getComputedStyle(el).backgroundImage;
    if (bi && bi.includes('url(') && !el.textContent.trim()) el.setAttribute('data-lmsc-img', '');
  });
}


// ---------- Partikel di header ----------
let pfx = null;

function stopParticles() {
  if (!pfx) return;
  cancelAnimationFrame(pfx.raf);
  pfx.ro.disconnect();
  pfx.nav.removeEventListener('mousemove', pfx.mm);
  pfx.nav.removeEventListener('mouseleave', pfx.ml);
  pfx.nav.removeEventListener('touchstart', pfx.tm);
  pfx.nav.removeEventListener('touchmove', pfx.tm);
  pfx.nav.removeEventListener('touchend', pfx.te);
  pfx.canvas.remove();
  pfx = null;
}

function startParticles() {
  if (pfx) return;
  const nav = document.querySelector('nav.navbar, .navbar.fixed-top');
  if (!nav) return;
  if (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  if (getComputedStyle(nav).position === 'static') nav.style.position = 'relative';

  const canvas = document.createElement('canvas');
  canvas.id = 'lmsc-particles';
  nav.insertBefore(canvas, nav.firstChild);
  const ctx = canvas.getContext('2d');
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const mouse = { x: null, y: null };
  let w = 0, h = 0, ps = [];

  function resize() {
    const r = nav.getBoundingClientRect();
    w = r.width; h = r.height;
    canvas.width = w * dpr; canvas.height = h * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    const n = Math.max(18, Math.min(90, Math.round(w / 18)));
    while (ps.length < n) ps.push({
      x: Math.random() * w, y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.5, vy: (Math.random() - 0.5) * 0.5,
      r: Math.random() * 1.6 + 0.8
    });
    ps.length = n;
  }

  function frame() {
    if (!document.hidden) {
      ctx.clearRect(0, 0, w, h);
      for (const p of ps) {
        p.x += p.vx; p.y += p.vy;
        if (p.x < 0 || p.x > w) p.vx *= -1;
        if (p.y < 0 || p.y > h) p.vy *= -1;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(255,255,255,.8)';
        ctx.fill();
      }
      ctx.lineWidth = 1;
      for (let i = 0; i < ps.length; i++) {
        for (let j = i + 1; j < ps.length; j++) {
          const dx = ps[i].x - ps[j].x, dy = ps[i].y - ps[j].y;
          const d = Math.sqrt(dx * dx + dy * dy);
          if (d < 90) {
            ctx.strokeStyle = `rgba(255,255,255,${(1 - d / 90) * 0.35})`;
            ctx.beginPath(); ctx.moveTo(ps[i].x, ps[i].y); ctx.lineTo(ps[j].x, ps[j].y); ctx.stroke();
          }
        }
        if (mouse.x !== null) {
          const dx = ps[i].x - mouse.x, dy = ps[i].y - mouse.y;
          const d = Math.sqrt(dx * dx + dy * dy);
          if (d < 120) {
            ctx.strokeStyle = `rgba(255,255,255,${(1 - d / 120) * 0.6})`;
            ctx.beginPath(); ctx.moveTo(ps[i].x, ps[i].y); ctx.lineTo(mouse.x, mouse.y); ctx.stroke();
          }
        }
      }
    }
    pfx.raf = requestAnimationFrame(frame);
  }

  const mm = (e) => { const r = nav.getBoundingClientRect(); mouse.x = e.clientX - r.left; mouse.y = e.clientY - r.top; };
  const ml = () => { mouse.x = null; mouse.y = null; };
  nav.addEventListener('mousemove', mm);
  nav.addEventListener('mouseleave', ml);
  const tm = (e) => {
    const t = e.touches && e.touches[0];
    if (!t) return;
    const r = nav.getBoundingClientRect();
    mouse.x = t.clientX - r.left; mouse.y = t.clientY - r.top;
  };
  const te = () => { mouse.x = null; mouse.y = null; };
  nav.addEventListener('touchstart', tm, { passive: true });
  nav.addEventListener('touchmove', tm, { passive: true });
  nav.addEventListener('touchend', te);
  const ro = new ResizeObserver(resize);
  ro.observe(nav);
  pfx = { nav, canvas, ro, mm, ml, tm, te, raf: 0 };
  resize();
  frame();
}

function domTasks() {
  centerLogin(current.enabled && current.loginCenter);
  markBgImages(current.enabled && current.dark);
  if (current.enabled && current.particles) startParticles(); else stopParticles();
}

function hookDom() {
  if (domHooked) return;
  domHooked = true;
  document.addEventListener('DOMContentLoaded', domTasks);
  window.addEventListener('load', () => { domTasks(); setTimeout(domTasks, 1000); });
  let t = null;
  new MutationObserver(() => {
    if (!current.enabled || !current.dark) return;
    clearTimeout(t);
    t = setTimeout(() => markBgImages(true), 800);
  }).observe(document.documentElement, { childList: true, subtree: true });
}

function apply(s) {
  current = s;
  if (!styleEl) {
    styleEl = document.createElement('style');
    styleEl.id = 'lmsc-style';
    document.documentElement.appendChild(styleEl);
  }
  styleEl.textContent = buildCSS(s);

  if (s.enabled && s.bg) {
    if (!bgEl) {
      bgEl = document.createElement('div');
      bgEl.id = 'lmsc-bg';
      document.documentElement.appendChild(bgEl);
    }
  } else if (bgEl) {
    bgEl.remove();
    bgEl = null;
  }

  hookDom();
  if (document.body) domTasks();
}

const load = () => chrome.storage.local.get(DEFAULTS, apply);
load();
chrome.storage.onChanged.addListener(load);
