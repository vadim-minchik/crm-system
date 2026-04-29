(function () {
	var SCROLL_KEY = 'crm-reload-scroll';
	var INTERVAL_MS = 40000;

	function isScrollable(el) {
		if (!el) return false;
		return (
			(el.scrollHeight || 0) > (el.clientHeight || 0) + 2 ||
			(el.scrollWidth || 0) > (el.clientWidth || 0) + 2
		);
	}

	function buildSelector(el) {
		if (!el || el.nodeType !== 1) return null;
		if (el.id) return '#' + CSS.escape(el.id);

		var parts = [];
		var cur = el;
		while (cur && cur.nodeType === 1 && cur !== document.body) {
			var tag = (cur.tagName || '').toLowerCase();
			if (!tag) return null;

			var parent = cur.parentElement;
			if (!parent) return null;

			var idx = 1;
			var sib = cur;
			while ((sib = sib.previousElementSibling)) {
				if ((sib.tagName || '').toLowerCase() === tag) idx++;
			}
			parts.unshift(tag + ':nth-of-type(' + idx + ')');
			cur = parent;
		}

		return parts.length ? 'body > ' + parts.join(' > ') : null;
	}

	function collectInnerScrollState() {
		var out = [];
		var all = document.querySelectorAll('*');
		for (var i = 0; i < all.length; i++) {
			var el = all[i];
			if (!isScrollable(el)) continue;
			if ((el.scrollTop || 0) === 0 && (el.scrollLeft || 0) === 0) continue;

			var sel = buildSelector(el);
			if (!sel) continue;

			out.push({
				selector: sel,
				top: el.scrollTop || 0,
				left: el.scrollLeft || 0
			});
		}
		return out;
	}

	function restoreInnerScrollState(items) {
		if (!Array.isArray(items) || !items.length) return;
		items.forEach(function (item) {
			if (!item || !item.selector) return;
			var el = document.querySelector(item.selector);
			if (!el) return;
			if (typeof item.top === 'number') el.scrollTop = item.top;
			if (typeof item.left === 'number') el.scrollLeft = item.left;
		});
	}

	try {
		var raw = sessionStorage.getItem(SCROLL_KEY);
		if (raw) {
			sessionStorage.removeItem(SCROLL_KEY);
			var o = JSON.parse(raw);
			var here = location.pathname + location.search;
			if (o && o.path === here && typeof o.y === 'number') {
				window.addEventListener('load', function () {
					var tries = 0;
					var maxTries = 20;
					var t = setInterval(function () {
						tries++;
						window.scrollTo(o.x || 0, o.y || 0);
						restoreInnerScrollState(o.inner);
						if (tries >= maxTries) clearInterval(t);
					}, 100);
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
						y: window.scrollY || 0,
						inner: collectInnerScrollState()
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
