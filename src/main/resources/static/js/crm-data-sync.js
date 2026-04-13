(function () {
	var SCROLL_KEY = 'crm-reload-scroll';
	var INTERVAL_MS = 40000;

	try {
		var raw = sessionStorage.getItem(SCROLL_KEY);
		if (raw) {
			sessionStorage.removeItem(SCROLL_KEY);
			var o = JSON.parse(raw);
			var here = location.pathname + location.search;
			if (o && o.path === here && typeof o.y === 'number') {
				window.addEventListener('load', function () {
					requestAnimationFrame(function () {
						requestAnimationFrame(function () {
							window.scrollTo(o.x || 0, o.y || 0);
						});
					});
				});
			}
		}
	} catch (e) {
	}

	var dirtyForms = new Set();

	function markFormDirty(form) {
		if (form && form.tagName === 'FORM') dirtyForms.add(form);
	}

	function pruneDirtyForms() {
		dirtyForms.forEach(function (f) {
			if (!document.body || !document.body.contains(f)) dirtyForms.delete(f);
		});
	}

	document.addEventListener(
			'input',
			function (e) {
				var f = e.target && e.target.closest && e.target.closest('form');
				if (f) markFormDirty(f);
			},
			true);

	document.addEventListener(
			'change',
			function (e) {
				var f = e.target && e.target.closest && e.target.closest('form');
				if (f) markFormDirty(f);
			},
			true);

	document.addEventListener(
			'submit',
			function (e) {
				if (e.target && e.target.tagName === 'FORM') dirtyForms.delete(e.target);
			},
			true);

	document.addEventListener(
			'reset',
			function (e) {
				if (e.target && e.target.tagName === 'FORM') dirtyForms.delete(e.target);
			},
			true);

	function isBootstrapModalOpen() {
		return !!document.querySelector('.modal.show');
	}

	function isTextLikeFocus() {
		var el = document.activeElement;
		if (!el || el === document.body) return false;
		var tag = el.tagName;
		if (tag === 'TEXTAREA') return true;
		if (tag === 'SELECT') return true;
		if (tag === 'INPUT') {
			var t = (el.type || '').toLowerCase();
			if (t === 'button' || t === 'submit' || t === 'reset' || t === 'hidden') return false;
			if (t === 'checkbox' || t === 'radio' || t === 'file' || t === 'range' || t === 'color') return false;
			return true;
		}
		if (el.isContentEditable) return true;
		return false;
	}

	function shouldDeferReload() {
		if (document.visibilityState === 'hidden') return true;
		if (isBootstrapModalOpen()) return true;
		if (isTextLikeFocus()) return true;
		pruneDirtyForms();
		return dirtyForms.size > 0;
	}

	function saveScrollAndReload() {
		try {
			sessionStorage.setItem(
					SCROLL_KEY,
					JSON.stringify({
						path: location.pathname + location.search,
						x: window.scrollX || 0,
						y: window.scrollY || 0
					}));
		} catch (e2) {
		}
		window.location.reload();
	}

	setInterval(function () {
		if (shouldDeferReload()) return;
		saveScrollAndReload();
	}, INTERVAL_MS);
})();
