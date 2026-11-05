// Tambahan khusus aplikasi HP: tab Pengingat, tombol kembali, mode layar sentuh.
(function () {
  if (!window.AndroidBridge) return;
  var B = window.AndroidBridge;
  document.body.classList.add('app');

  function cfg() { try { return JSON.parse(B.getAll() || '{}'); } catch (e) { return {}; } }

  $('backBtn').onclick = function () { B.close(); };
  $('calConnect').onclick = function () { B.connectCalendar(); };
  $('calSync').onclick = function () { say('Menyinkronkan kalender… 🔄'); B.syncNow(); };
  $('testNotif').onclick = function () { B.testNotification(); say('Notifikasi tes dikirim 🔔'); };
  $('calUrl').onchange = function (e) {
    var v = e.target.value.trim();
    if (!v) return;
    save({ calUrl: v });
    say('Link disimpan, menyinkronkan… 🔄');
    setTimeout(refresh, 3000);
  };

  $('remind').checked = cfg().remind !== false;
  $('remind').onchange = function (e) {
    save({ remind: e.target.checked });
    say(e.target.checked ? 'Pengingat aktif 🔔' : 'Pengingat dimatikan 🔕');
  };

  var ABSEN = [[60, '1 jam'], [30, '30 menit'], [10, '10 menit'], [0, 'Saat mulai']];
  var TUGAS = [[4320, '3 hari'], [1440, '1 hari'], [180, '3 jam'], [60, '1 jam']];

  function chips(boxId, key, options, fallback) {
    var box = $(boxId);
    box.innerHTML = '';
    var cur = {};
    String(cfg()[key] !== undefined ? cfg()[key] : fallback).split(',').forEach(function (x) {
      if (x !== '') cur[Number(x)] = true;
    });
    options.forEach(function (o) {
      var b = document.createElement('button');
      b.className = 'chip' + (cur[o[0]] ? ' on' : '');
      b.textContent = o[1];
      b.onclick = function () {
        b.classList.toggle('on');
        var sel = [];
        options.forEach(function (x, i) { if (box.children[i].classList.contains('on')) sel.push(x[0]); });
        save((function () { var p = {}; p[key] = sel.join(','); return p; })());
        say('Waktu pengingat diperbarui 🔔');
      };
      box.appendChild(b);
    });
  }
  chips('absenLead', 'absenLead', ABSEN, '30,10');
  chips('tugasLead', 'tugasLead', TUGAS, '1440,180');

  function refresh() {
    var st = {};
    try { st = JSON.parse(B.status() || '{}'); } catch (e) {}
    var box = $('calStatus');
    if (st.connected) {
      var t = st.lastSync ? new Date(st.lastSync).toLocaleString('id-ID', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : 'belum pernah';
      box.className = 'statusCard ok';
      box.innerHTML = '✅ <b>Kalender terhubung</b><small></small>';
      box.querySelector('small').textContent = 'Sinkron terakhir: ' + t + ' · ' + (st.count || 0) + ' acara';
    } else {
      box.className = 'statusCard warn';
      box.innerHTML = '⚠️ <b>Belum terhubung</b><small>Ketuk “Hubungkan kalender LMS”, masuk ke akun, lalu tunggu pesan hijau.</small>';
    }
    var list = [];
    try { list = JSON.parse(B.upcoming() || '[]'); } catch (e) {}
    var up = $('upcoming');
    up.innerHTML = '';
    if (!list.length) { up.innerHTML = '<div class="empty">Belum ada jadwal. Hubungkan kalender dulu ya 🐱</div>'; return; }
    list.forEach(function (e) {
      var d = document.createElement('div');
      d.className = 'evt';
      var icon = e.type === 0 ? '📍' : e.type === 1 ? '📝' : '📅';
      var when = new Date(e.start).toLocaleString('id-ID', { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
      d.innerHTML = icon + ' <b></b><small></small>';
      d.querySelector('b').textContent = e.title;
      d.querySelector('small').textContent = (e.course ? e.course + ' · ' : '') + when;
      up.appendChild(d);
    });
  }
  window.__onSync = function (ok) { refresh(); say(ok ? 'Kalender diperbarui ✅' : 'Gagal sinkron. Cek link kalender atau koneksi internet.'); };
  refresh();
})();
