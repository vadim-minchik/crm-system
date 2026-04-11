(function () {
	var sb = document.getElementById('appSidebar');
	var bd = document.getElementById('sidebarBackdrop');
	var btn = document.getElementById('sidebarOpenBtn');
	if (!sb || !bd || !btn) return;

	function isNarrow() {
		return window.matchMedia('(max-width: 991.98px)').matches;
	}

	function openDrawer() {
		sb.classList.add('sidebar--open');
		bd.classList.add('sidebar-backdrop--visible');
		document.body.classList.add('crm-drawer-open');
		btn.setAttribute('aria-expanded', 'true');
	}

	function closeDrawer() {
		sb.classList.remove('sidebar--open');
		bd.classList.remove('sidebar-backdrop--visible');
		document.body.classList.remove('crm-drawer-open');
		btn.setAttribute('aria-expanded', 'false');
	}

	btn.addEventListener('click', function () {
		if (!isNarrow()) return;
		if (sb.classList.contains('sidebar--open')) closeDrawer();
		else openDrawer();
	});

	bd.addEventListener('click', closeDrawer);

	sb.querySelectorAll('a.nav-link').forEach(function (a) {
		a.addEventListener('click', function () {
			if (isNarrow()) closeDrawer();
		});
	});

	var logoutForm = sb.querySelector('form[action*="logout"]');
	if (logoutForm) logoutForm.addEventListener('submit', closeDrawer);

	window.addEventListener('resize', function () {
		if (!isNarrow()) closeDrawer();
	});

	document.addEventListener('keydown', function (e) {
		if (e.key === 'Escape' && isNarrow()) closeDrawer();
	});
})();
