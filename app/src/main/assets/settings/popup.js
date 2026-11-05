const $ = (id) => document.getElementById(id);

const DEFAULTS = {
  enabled: true, dark: false, bg: '',
  glass: false, opacity: 55, blur: 10,
  font: '', color: '', colorLinks: false,
  contrast: true, loginCenter: true,
  hdr: true, hdrC1: '#2563eb', hdrC2: '#7c3aed', hdrC3: '#db2777', hdrAnim: true,
  particles: true
};

const FONTS = [
  ['', 'Bawaan situs'],
  ['Arial, sans-serif', 'Arial'],
  ['Verdana, sans-serif', 'Verdana'],
  ["'Trebuchet MS', sans-serif", 'Trebuchet'],
  ['Georgia, serif', 'Georgia'],
  ["'Times New Roman', serif", 'Times New Roman'],
  ["'Courier New', monospace", 'Courier New'],
  ["'Comic Sans MS', cursive", 'Comic Sans']
];

const SWATCHES = [
  ['#000000', 'Hitam'], ['#ffffff', 'Putih'], ['#f5c518', 'Emas'], ['#1747a6', 'Biru Warmadewa'],
  ['#0f766e', 'Hijau'], ['#db2777', 'Pink'], ['#7c3aed', 'Ungu'], ['#ea580c', 'Oranye']
];

const PRESETS = [
  { name: 'Warmadewa', emoji: '🏛️', desc: 'Biru & emas khas kampus',
    grad: 'linear-gradient(90deg,#1747a6,#2e7de0,#f5b82e)',
    v: { dark: false, hdr: true, hdrC1: '#1747a6', hdrC2: '#2e7de0', hdrC3: '#f5b82e', hdrAnim: true, particles: true, glass: false, contrast: true, color: '' } },
  { name: 'Malam', emoji: '🌙', desc: 'Gelap, nyaman di mata',
    grad: 'linear-gradient(90deg,#0f172a,#4338ca,#a21caf)',
    v: { dark: true, hdr: true, hdrC1: '#0f172a', hdrC2: '#4338ca', hdrC3: '#a21caf', hdrAnim: true, particles: true, glass: true, opacity: 45, blur: 8, contrast: true, color: '' } },
  { name: 'Santai', emoji: '🍃', desc: 'Kaca lembut, segar',
    grad: 'linear-gradient(90deg,#0ea5a4,#22c55e,#facc15)',
    v: { dark: false, hdr: true, hdrC1: '#0ea5a4', hdrC2: '#22c55e', hdrC3: '#facc15', hdrAnim: true, particles: true, glass: true, opacity: 60, blur: 12, contrast: true, color: '' } },
  { name: 'Fokus', emoji: '🎯', desc: 'Polos, tegas, tanpa animasi',
    grad: 'linear-gradient(90deg,#1f2937,#374151,#4b5563)',
    v: { dark: false, hdr: false, hdrAnim: false, particles: false, glass: false, contrast: true, color: '' } }
];

const SWITCHES = {
  enabled: ['Ekstensi aktif ✅', 'Ekstensi dimatikan 💤'],
  dark: ['Mode malam aktif 🌙', 'Kembali terang ☀️'],
  loginCenter: ['Halaman login dirapikan 🚪', 'Login kembali seperti semula'],
  contrast: ['Teks jadi lebih tegas 🔆', 'Kontras kembali normal'],
  glass: ['Efek kaca menyala 🪟✨', 'Panel kembali solid'],
  hdr: ['Header gradient aktif 🌈', 'Header kembali ke warna bawaan'],
  hdrAnim: ['Gradient mengalir 🌊', 'Gradient diam'],
  particles: ['Partikel beterbangan ✨', 'Partikel dimatikan'],
  colorLinks: ['Link ikut berubah warna 🔗', 'Link kembali normal']
};

const TIPS = [
  'Coba tema “Malam” untuk belajar larut 🌙',
  'Seret gambar atau GIF ke tab Latar 🖼️',
  'Geser kursor di header, partikelnya ikut! ✨',
  'Teks pucat? Nyalakan Kontras tinggi 🔆',
  'Panel terlalu bening? Naikkan kepekatan 🪟',
  'Semangat kuliahnya! 💛 Om Santih, Santih, Santih'
];

let state = { ...DEFAULTS };
let savedTimer = null;

// ---------- util ----------
function say(msg) { $('bubble').textContent = msg; }

function flashSaved() {
  const el = $('saved');
  el.classList.add('show');
  clearTimeout(savedTimer);
  savedTimer = setTimeout(() => el.classList.remove('show'), 1200);
}

function save(obj) {
  Object.assign(state, obj);
  chrome.storage.local.set(obj, flashSaved);
}

// ---------- render ----------
function render() {
  Object.keys(SWITCHES).forEach((id) => { $(id).checked = !!state[id]; });
  $('opacity').value = state.opacity; $('opacityVal').textContent = state.opacity + '%';
  $('blur').value = state.blur; $('blurVal').textContent = state.blur + 'px';
  ['hdrC1', 'hdrC2', 'hdrC3'].forEach((id) => { $(id).value = state[id]; });
  $('gradBar').style.backgroundImage = `linear-gradient(120deg, ${state.hdrC1}, ${state.hdrC2}, ${state.hdrC3})`;
  $('bgUrl').value = state.bg && !state.bg.startsWith('data:') ? state.bg : '';
  const th = $('thumb');
  if (state.bg) { th.style.backgroundImage = `url("${state.bg.replace(/"/g, '%22')}")`; th.textContent = ''; }
  else { th.style.backgroundImage = ''; th.textContent = '🖼️'; }
  document.querySelectorAll('.chip').forEach((c) => c.classList.toggle('on', c.dataset.v === state.font));
  document.querySelectorAll('.sw').forEach((c) => c.classList.toggle('on', c.dataset.c === state.color));
  if (state.color) $('color').value = state.color;
  $('colorNow').textContent = state.color ? 'Aktif: ' + state.color : 'Aktif: bawaan';
}

// ---------- build dynamic bits ----------
PRESETS.forEach((p) => {
  const b = document.createElement('button');
  b.className = 'preset';
  b.innerHTML = `<div class="strip" style="background:${p.grad}"></div>
    <div class="pbody"><span class="pn">${p.emoji} ${p.name}</span><span class="pd">${p.desc}</span></div>`;
  b.onclick = () => { save(p.v); render(); say(`Tema ${p.name} dipasang! ${p.emoji}`); };
  $('presets').appendChild(b);
});

FONTS.forEach(([v, label]) => {
  const b = document.createElement('button');
  b.className = 'chip';
  b.dataset.v = v;
  b.textContent = label;
  if (v) b.style.fontFamily = v;
  b.onclick = () => { save({ font: v }); render(); say(v ? `Font ${label} dipakai 🔤` : 'Font kembali bawaan'); };
  $('fonts').appendChild(b);
});

SWATCHES.forEach(([c, label]) => {
  const b = document.createElement('button');
  b.className = 'sw';
  b.dataset.c = c;
  b.title = label;
  b.style.background = c;
  b.onclick = () => { save({ color: c }); render(); say(`Warna teks: ${label} 🎨`); };
  $('swatches').appendChild(b);
});

// ---------- tabs ----------
$('tabs').addEventListener('click', (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  document.querySelectorAll('.tabs button').forEach((b) => b.classList.toggle('on', b === btn));
  document.querySelectorAll('.panel').forEach((p) => p.classList.toggle('on', p.id === 'tab-' + btn.dataset.tab));
});

// ---------- switches ----------
Object.keys(SWITCHES).forEach((id) => {
  $(id).onchange = (e) => {
    save({ [id]: e.target.checked });
    say(SWITCHES[id][e.target.checked ? 0 : 1]);
  };
});

// ---------- sliders ----------
$('opacity').oninput = (e) => { $('opacityVal').textContent = e.target.value + '%'; save({ opacity: +e.target.value }); };
$('blur').oninput = (e) => { $('blurVal').textContent = e.target.value + 'px'; save({ blur: +e.target.value }); };

// ---------- header colors ----------
['hdrC1', 'hdrC2', 'hdrC3'].forEach((id) => {
  $(id).oninput = (e) => { save({ [id]: e.target.value }); render(); };
});

// ---------- text color ----------
$('color').oninput = (e) => { save({ color: e.target.value }); render(); };
$('colorClear').onclick = () => { save({ color: '' }); render(); say('Warna teks kembali bawaan'); };

// ---------- background ----------
function loadFile(file) {
  if (!file || !file.type.startsWith('image/')) { say('Itu bukan gambar 🙈'); return; }
  const r = new FileReader();
  r.onload = () => { save({ bg: r.result }); render(); say('Latar baru terpasang! 🖼️'); };
  r.readAsDataURL(file);
}
$('pick').onclick = () => $('bgFile').click();
$('bgFile').onchange = (e) => loadFile(e.target.files[0]);
$('bgUrl').onchange = (e) => { save({ bg: e.target.value.trim() }); render(); };
$('bgClear').onclick = () => { save({ bg: '' }); render(); say('Latar dihapus'); };
const drop = $('drop');
['dragenter', 'dragover'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.add('over'); }));
['dragleave', 'drop'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.remove('over'); }));
drop.addEventListener('drop', (e) => loadFile(e.dataTransfer.files[0]));

// ---------- mascot & reset ----------
$('cat').onclick = () => say(TIPS[Math.floor(Math.random() * TIPS.length)]);

let resetArmed = false;
$('resetAll').onclick = () => {
  const btn = $('resetAll');
  if (!resetArmed) {
    resetArmed = true; btn.textContent = 'Yakin? Klik lagi';
    setTimeout(() => { resetArmed = false; btn.textContent = '↺ Reset semua'; }, 3000);
    return;
  }
  resetArmed = false; btn.textContent = '↺ Reset semua';
  chrome.storage.local.clear(() => { state = { ...DEFAULTS }; render(); flashSaved(); say('Semua kembali ke awal 🔄'); });
};

// ---------- init ----------
chrome.storage.local.get(DEFAULTS, (s) => { state = s; render(); });
