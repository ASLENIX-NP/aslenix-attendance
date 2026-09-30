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

    /* =========================================================
       NOTIFICATION SYSTEM: REDIRECT ON CLICK & VIEWPORT CONTAINMENT
    ========================================================= */

    window.getNotificationRedirectUrl = function (notification) {
        if (notification && notification.targetUrl) {
            return notification.targetUrl;
        }
        const type = ((notification && notification.type) || '').toUpperCase();
        const text = (((notification && notification.title) || '') + ' ' + ((notification && notification.message) || '')).toLowerCase();
        const isAdmin = window.location.pathname.startsWith('/admin');

        if (isAdmin) {
            if (type.includes('ATTENDANCE') || text.includes('attendance')) return '/admin/attendance';
            if (type.includes('LEAVE') || text.includes('leave')) return '/admin/leave';
            if (type.includes('TASK') || text.includes('task') || text.includes('subtask') || text.includes('assignment')) return '/admin/tasks';
            if (type.includes('EMPLOYEE_OF_THE_MONTH') || type.includes('PERFORMANCE') || text.includes('performance') || text.includes('employee of the month')) return '/admin/performance';
            if (type.includes('EMPLOYEE') || text.includes('employee')) return '/admin/employees';
            return '/admin/dashboard';
        } else {
            if (type.includes('ATTENDANCE') || text.includes('attendance')) return '/employee/attendance';
            if (type.includes('LEAVE') || text.includes('leave')) return '/employee/leave';
            if (type.includes('TASK') || text.includes('task') || text.includes('subtask') || text.includes('assignment')) return '/employee/tasks';
            if (type.includes('EMPLOYEE_OF_THE_MONTH') || type.includes('PROFILE') || text.includes('profile') || text.includes('employee of the month')) return '/employee/profile';
            return '/employee/dashboard';
        }
    };

    window.handleNotificationClick = async function (notification, itemElement) {
        if (!notification) return;
        const targetUrl = notification.targetUrl || window.getNotificationRedirectUrl(notification);

        if (!notification.read && notification.id) {
            try {
                if (typeof markNotificationAsRead === 'function') {
                    await markNotificationAsRead(notification.id, itemElement);
                } else {
                    const csrfMeta = document.querySelector('meta[name="_csrf"]');
                    const csrfToken = csrfMeta ? csrfMeta.getAttribute('content') : '';
                    const csrfHeader = (document.querySelector('meta[name="_csrf_header"]') || {}).content || 'X-CSRF-TOKEN';
                    const endpoint = window.location.pathname.startsWith('/admin')
                        ? '/api/notifications/' + encodeURIComponent(notification.id) + '/read'
                        : '/employee/notifications/' + encodeURIComponent(notification.id) + '/read';
                    const headers = { 'Accept': 'application/json' };
                    if (csrfToken) headers[csrfHeader] = csrfToken;
                    await fetch(endpoint, {
                        method: 'POST',
                        headers: headers,
                        credentials: 'same-origin',
                        keepalive: true
                    });
                }
            } catch (e) {
                console.error('Error marking notification as read before redirect:', e);
            }
        }

        if (targetUrl) {
            window.location.href = targetUrl;
        }
    };

    function initMobileNotifications() {
        const notifDropdown = document.getElementById('notificationDropdown') || document.querySelector('.notification-dropdown');
        if (!notifDropdown) return;

        function adjustDropdownPosition() {
            if (window.innerWidth <= 900 && (notifDropdown.classList.contains('show') || notifDropdown.classList.contains('open') || window.getComputedStyle(notifDropdown).display !== 'none')) {
                const rect = notifDropdown.getBoundingClientRect();
                if (rect.left < 8 || rect.right > window.innerWidth - 8) {
                    notifDropdown.style.left = '10px';
                    notifDropdown.style.right = '10px';
                    notifDropdown.style.width = 'auto';
                    notifDropdown.style.maxWidth = 'calc(100vw - 20px)';
                }
            }
        }

        const observer = new MutationObserver(adjustDropdownPosition);
        observer.observe(notifDropdown, { attributes: true, attributeFilter: ['class', 'style'] });
        window.addEventListener('resize', adjustDropdownPosition);
    }

    function initAll() {
        initMobileSidebar();
        initMobileNotifications();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll);
    } else {
        initAll();
    }
})();
