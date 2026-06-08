/* Bento masonry + lightbox for the wabi-sabi gallery. Progressive: the grid
 * already looks right with CSS alone; this just tightens the packing and adds
 * the full-screen view. */
(function () {
  'use strict';

  const grid = document.querySelector('.bento');

  /* ---------- masonry: size each tile to its content height ---------- */
  function readMetric(el, prop, fallback) {
    const v = parseFloat(getComputedStyle(el)[prop]);
    return Number.isFinite(v) ? v : fallback;
  }

  function layoutTile(tile, rowHeight, gap) {
    const inner = tile.querySelector('.tile__inner') || tile;
    const height = inner.getBoundingClientRect().height;
    const span = Math.max(1, Math.ceil((height + gap) / (rowHeight + gap)));
    tile.style.gridRowEnd = 'span ' + span;
  }

  function layoutAll() {
    if (!grid) return;
    const rowHeight = readMetric(grid, 'gridAutoRows', 8);
    const gap = readMetric(grid, 'rowGap', 20);
    grid.querySelectorAll('.tile').forEach((t) => layoutTile(t, rowHeight, gap));
  }

  if (grid) {
    const tiles = Array.from(grid.querySelectorAll('.tile'));

    // A deterministic handmade rhythm: every so often a tile spans two columns.
    tiles.forEach((tile, i) => {
      if (i % 6 === 2 || i % 11 === 7) tile.classList.add('tile--wide');
    });

    // Re-pack as each image arrives (heights are unknown until then).
    tiles.forEach((tile) => {
      const img = tile.querySelector('img');
      if (!img) return;
      if (img.complete) {
        requestAnimationFrame(layoutAll);
      } else {
        img.addEventListener('load', layoutAll);
        img.addEventListener('error', layoutAll);
      }
    });

    layoutAll();
    let resizeTimer;
    window.addEventListener('resize', () => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(layoutAll, 120);
    });
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(layoutAll);
  }

  /* ---------------------------- lightbox ---------------------------- */
  const lightbox = document.getElementById('lightbox');
  if (lightbox) {
    const lbImg = lightbox.querySelector('.lightbox__img');
    const lbCaption = lightbox.querySelector('.lightbox__caption');

    function open(src, alt, caption) {
      lbImg.src = src;
      lbImg.alt = alt || '';
      lbCaption.textContent = caption || '';
      lbCaption.style.display = caption ? 'block' : 'none';
      lightbox.classList.add('is-open');
      document.body.style.overflow = 'hidden';
    }
    function close() {
      lightbox.classList.remove('is-open');
      lbImg.removeAttribute('src');
      document.body.style.overflow = '';
    }

    document.querySelectorAll('.tile__open').forEach((btn) => {
      btn.addEventListener('click', () => {
        const fig = btn.closest('.tile');
        const img = btn.querySelector('img');
        open(fig.dataset.full || img.src, img.alt, fig.dataset.desc || '');
      });
    });

    lightbox.addEventListener('click', (e) => {
      if (e.target === lightbox || e.target.classList.contains('lightbox__close')) close();
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && lightbox.classList.contains('is-open')) close();
    });
  }

  /* ------------------- upload dropzone preview ---------------------- */
  const dropzone = document.querySelector('.dropzone');
  if (dropzone) {
    const input = dropzone.querySelector('input[type=file]');
    const nameEl = dropzone.querySelector('.dropzone__name');
    const hintEl = dropzone.querySelector('.dropzone__hint');
    const markEl = dropzone.querySelector('.dropzone__mark');
    let preview = null;

    function show(file) {
      if (!file) return;
      if (nameEl) nameEl.textContent = file.name;
      if (file.type && file.type.startsWith('image/')) {
        if (!preview) {
          preview = document.createElement('img');
          preview.className = 'dropzone__preview';
          dropzone.appendChild(preview);
        }
        preview.src = URL.createObjectURL(file);
        if (markEl) markEl.style.display = 'none';
        if (hintEl) hintEl.style.display = 'none';
      }
    }

    input.addEventListener('change', () => show(input.files[0]));
    ['dragenter', 'dragover'].forEach((ev) =>
      dropzone.addEventListener(ev, (e) => { e.preventDefault(); dropzone.classList.add('is-dragover'); }));
    ['dragleave', 'drop'].forEach((ev) =>
      dropzone.addEventListener(ev, () => dropzone.classList.remove('is-dragover')));
    dropzone.addEventListener('drop', (e) => {
      e.preventDefault();
      if (e.dataTransfer.files.length) {
        input.files = e.dataTransfer.files;
        show(input.files[0]);
      }
    });
  }
})();
