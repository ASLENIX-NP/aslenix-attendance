/**
 * ASLENIX Mobile Responsive Navigation & Drawer
 * Automatically binds the mobile sidebar drawer and header hamburger button.
 */
(function () {
    'use strict';

    function initMobileSidebar() {
        const sidebar = document.querySelector('.sidebar');
        const header = document.querySelector('.top-header');

        if (!sidebar || !header) return;

        // 1. Ensure backdrop exists
        let backdrop = document.getElementById('sidebarBackdrop');
        if (!backdrop) {
            backdrop = document.createElement('div');
            backdrop.id = 'sidebarBackdrop';
            backdrop.className = 'sidebar-backdrop';
            document.body.appendChild(backdrop);
        }

        // 2. Ensure mobile toggle button exists on the left side of the header
        let toggleBtn = document.getElementById('mobileSidebarToggle');
        if (!toggleBtn) {
            toggleBtn = document.createElement('button');
            toggleBtn.type = 'button';
            toggleBtn.id = 'mobileSidebarToggle';
            toggleBtn.className = 'mobile-sidebar-toggle';
            toggleBtn.setAttribute('aria-label', 'Open navigation menu');
            toggleBtn.setAttribute('aria-expanded', 'false');
            toggleBtn.innerHTML = `
                <svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="3" y1="6" x2="21" y2="6"></line>
                    <line x1="3" y1="12" x2="21" y2="12"></line>
                    <line x1="3" y1="18" x2="21" y2="18"></line>
                </svg>
            `;
            // Prepend as the first child of the header (left side)
            header.insertBefore(toggleBtn, header.firstChild);
        }

        // 3. Ensure close button exists in sidebar for easy dismissal on mobile
        let closeBtn = sidebar.querySelector('.sidebar-close-btn');
        if (!closeBtn) {
            closeBtn = document.createElement('button');
            closeBtn.type = 'button';
            closeBtn.className = 'sidebar-close-btn';
            closeBtn.setAttribute('aria-label', 'Close navigation menu');
            closeBtn.innerHTML = `
                <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            `;

            // Place close button nicely inside the first header/title in the sidebar
            const titleContainer = sidebar.querySelector('.sidebar-section-title') ||
                                   sidebar.querySelector('.sidebar-title') ||
                                   sidebar.querySelector('.sidebar-brand') ||
                                   sidebar.querySelector('.brand');
            if (titleContainer) {
                titleContainer.classList.add('sidebar-title-with-close');
                titleContainer.appendChild(closeBtn);
            } else {
                sidebar.insertBefore(closeBtn, sidebar.firstChild);
            }
        }

        function openSidebar() {
            sidebar.classList.add('mobile-open');
            backdrop.classList.add('active');
            toggleBtn.setAttribute('aria-expanded', 'true');
            document.body.classList.add('sidebar-open');
        }

        function closeSidebar() {
            sidebar.classList.remove('mobile-open');
            backdrop.classList.remove('active');
            toggleBtn.setAttribute('aria-expanded', 'false');
            document.body.classList.remove('sidebar-open');
        }

        function toggleSidebar(e) {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }
            if (sidebar.classList.contains('mobile-open')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        }

        // Event listeners
        toggleBtn.addEventListener('click', toggleSidebar);
        closeBtn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            closeSidebar();
        });
        backdrop.addEventListener('click', closeSidebar);

        // Close on ESC key
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && sidebar.classList.contains('mobile-open')) {
                closeSidebar();
            }
        });

        // Close when navigation links inside the drawer are tapped on mobile
        const navLinks = sidebar.querySelectorAll('.sidebar-link, a[href]');
        navLinks.forEach(function (link) {
            link.addEventListener('click', function () {
                if (window.innerWidth <= 900) {
                    closeSidebar();
                }
            });
        });

        // Touch swipe gesture: swipe left on sidebar closes it
        let startX = 0;
        let startY = 0;

        sidebar.addEventListener('touchstart', function (e) {
            if (e.touches.length === 1) {
                startX = e.touches[0].clientX;
                startY = e.touches[0].clientY;
            }
        }, { passive: true });

        sidebar.addEventListener('touchend', function (e) {
            if (e.changedTouches.length === 1) {
                const diffX = startX - e.changedTouches[0].clientX;
                const diffY = Math.abs(startY - e.changedTouches[0].clientY);
                // Swiped left by at least 45px and mostly horizontal
                if (diffX > 45 && diffY < 80) {
                    closeSidebar();
                }
            }
        }, { passive: true });

        // Auto close when resized beyond mobile breakpoint
        window.addEventListener('resize', function () {
            if (window.innerWidth > 900 && sidebar.classList.contains('mobile-open')) {
                closeSidebar();
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initMobileSidebar);
    } else {
        initMobileSidebar();
    }
})();
