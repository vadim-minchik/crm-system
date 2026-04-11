(function () {
	var KEY = 'crm-theme';
	var COOKIE_MAX_AGE = 365 * 24 * 60 * 60;

	function persist(theme) {
		try {
			localStorage.setItem(KEY, theme);
		} catch (e) {}
		try {
			var secure = typeof location !== 'undefined' && location.protocol === 'https:' ? ';Secure' : '';
			document.cookie = KEY + '=' + encodeURIComponent(theme) + ';path=/;max-age=' + COOKIE_MAX_AGE + ';SameSite=Lax' + secure;
		} catch (e2) {}
	}

	function apply(theme) {
		var root = document.documentElement;
		if (theme === 'dark') {
			root.setAttribute('data-theme', 'dark');
			root.setAttribute('data-bs-theme', 'dark');
		} else {
			root.removeAttribute('data-theme');
			root.setAttribute('data-bs-theme', 'light');
		}
		persist(theme);
		syncUi(theme);
	}

	function syncUi(theme) {
		var isDark = theme === 'dark';
		document.querySelectorAll('#crmThemeToggle, #crmThemeToggleLogin').forEach(function (btn) {
			if (!btn) return;
			btn.setAttribute('aria-pressed', isDark ? 'true' : 'false');
			var t = btn.querySelector('.theme-toggle-text');
			if (t) t.textContent = isDark ? 'Светлая тема' : 'Тёмная тема';
			var whenLight = btn.querySelectorAll('.theme-ico-when-light');
			var whenDark = btn.querySelectorAll('.theme-ico-when-dark');
			whenLight.forEach(function (el) {
				el.classList.toggle('d-none', isDark);
			});
			whenDark.forEach(function (el) {
				el.classList.toggle('d-none', !isDark);
			});
		});
		var meta = document.querySelector('meta[name="theme-color"]');
		if (meta) meta.setAttribute('content', isDark ? '#08080a' : '#ffffff');
	}

	function readSaved() {
		try {
			var t = localStorage.getItem(KEY);
			if (t === 'dark' || t === 'light') return t;
		} catch (e) {}
		try {
			var m = document.cookie.match(new RegExp('(?:^|;\\s*)' + KEY + '=([^;]*)'));
			if (m) {
				var v = decodeURIComponent(m[1].trim());
				if (v === 'dark' || v === 'light') return v;
			}
		} catch (e2) {}
		return null;
	}

	function bind(btn) {
		if (!btn || btn.dataset.themeBound) return;
		btn.dataset.themeBound = '1';
		btn.addEventListener('click', function () {
			var next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
			apply(next);
		});
	}

	document.addEventListener('DOMContentLoaded', function () {
		var saved = readSaved();
		if (saved === 'dark' || saved === 'light') {
			apply(saved);
		} else {
			syncUi(document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light');
		}
		bind(document.getElementById('crmThemeToggle'));
		bind(document.getElementById('crmThemeToggleLogin'));
	});
})();
