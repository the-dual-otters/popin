// 공통 레이아웃 스크립트

// 페이지 로드 시 토큰을 쿠키에 동기화
(function syncTokenToCookie() {
    const token = localStorage.getItem('authToken') ||
        localStorage.getItem('accessToken') ||
        sessionStorage.getItem('authToken') ||
        sessionStorage.getItem('accessToken');

    if (token) {
        // 쿠키에 토큰이 없으면 동기화
        if (!document.cookie.includes('jwtToken=')) {
            document.cookie = `jwtToken=${token}; path=/; max-age=86400; SameSite=Lax`;
            console.log('✅ 토큰을 쿠키에 동기화했습니다.');
        }
    }
})();

// HTML 컴포넌트 로드 함수
async function loadComponents() {
    try {
        // 헤더 로드
        const headerHTML = await TemplateLoader.load('components/header');
        document.getElementById('header-container').innerHTML = headerHTML;

        // 푸터 로드
        document.getElementById('footer-container').innerHTML = createFooterByRole();

        // 컴포넌트 로드 완료 후 이벤트 설정
        setupComponentEvents();

    } catch (error) {
        console.error('컴포넌트 로드 실패:', error);
        createFallbackLayout();
    }
}

function setupAuthIcon() {
    const authIcon = document.getElementById("authIcon");
    if (!authIcon) return;

    const token = apiService.getStoredToken();

    if (token) {
        // 로그아웃 아이콘
        authIcon.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
        `;
        authIcon.onclick = async () => {
            try {
                await apiService.post("/auth/logout", {});
            } catch (e) {
                console.warn("서버 로그아웃 실패:", e);
            } finally {
                apiService.removeToken();
                localStorage.clear();
                sessionStorage.clear();
                window.location.href = "/auth/login?logout=true";
            }
        };
    } else {
        // 로그인 아이콘
        authIcon.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                <polyline points="10 17 15 12 10 7"></polyline>
                <line x1="15" y1="12" x2="3" y2="12"></line>
            </svg>
        `;
        authIcon.onclick = () => {
            window.location.href = "/auth/login";
        };
    }
}


// 컴포넌트 이벤트 설정
function setupComponentEvents() {
    // 푸터 네비게이션 이벤트
    setupFooterNavigation();

    // 현재 페이지에 맞는 활성 탭 설정
    setActiveFooterTab();

    // 헤더 로그인/로그아웃 버튼 이벤트
    setupAuthIcon();
}

// 푸터 네비게이션 이벤트 설정
function setupFooterNavigation() {
    const footerItems = document.querySelectorAll('.footer-item');
    footerItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();

            const page = this.getAttribute('data-page');

            if (page === 'popupList') {
                window.location.href = '/';
                return;
            }

            // 모든 탭에서 active 클래스 제거
            footerItems.forEach(tab => tab.classList.remove('active'));

            // 현재 탭에 active 클래스 추가
            this.classList.add('active');

            // 페이지 로드 (pages.js 사용)
            if (typeof Pages !== 'undefined' && Pages[page]) {
                Pages[page]();
            } else {
                console.error(`Page handler not found: ${page}`);
            }
        });
    });
}

// 현재 페이지에 맞는 푸터 탭 활성화 (새로운 URL 구조 적용)
function setActiveFooterTab() {
    const footerItems = document.querySelectorAll('.footer-item');
    const currentPath = window.location.pathname;

    // 모든 탭에서 active 클래스 제거
    footerItems.forEach(tab => tab.classList.remove('active'));

    // 현재 경로에 따라 활성 탭 설정
    let activeTab = null;

    if (currentPath === '/' || currentPath === '/index.html') {
        activeTab = document.querySelector('[data-page="popupList"]');
    } else if (currentPath.startsWith('/popup/search')) {
        activeTab = document.querySelector('[data-page="popupSearch"]');
    } else if (currentPath.startsWith('/popup/') && /\/popup\/\d+$/.test(currentPath)) {
        // 팝업 상세 페이지에서는 홈 탭 활성화
        activeTab = document.querySelector('[data-page="popupList"]');
    } else if (currentPath.startsWith('/map')) {
        activeTab = document.querySelector('[data-page="map"]');
    } else if (currentPath.includes('bookmark')) {
        activeTab = document.querySelector('[data-page="bookmark"]');
    } else if (currentPath.includes('space')) {
        activeTab = document.querySelector('[data-page="spaceList"]');
    }

    if (activeTab) {
        activeTab.classList.add('active');
    } else {
        // 기본값: 홈 탭 활성화
        const homeTab = document.querySelector('[data-page="popupList"]');
        if (homeTab) {
            homeTab.classList.add('active');
        }
    }
}

// 컴포넌트 로드 실패 시 기본 레이아웃 생성
function createFallbackLayout() {
    // 기본 헤더 생성
    document.getElementById('header-container').innerHTML = `
        <header class="header">
            <div class="header-left"></div>
            <div class="logo">
                <div class="logo-bubble">
                    <span class="logo-text">POPIN</span>
                </div>
            </div>
            <div class="header-right">
                <div class="icon-btn" onclick="showNotifications()">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                        <path d="m13.73 21a2 2 0 0 1-3.46 0"></path>
                    </svg>
                </div>
                <div class="icon-btn" onclick="goToProfile()">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                        <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                </div>
            </div>
        </header>
    `;

    // 동적 푸터 생성
    document.getElementById('footer-container').innerHTML = createFooterByRole();

    // 폴백 이벤트 설정
    setupComponentEvents();
}

// 레이아웃 초기화
function initializeLayout() {
    // 스크롤 위치 초기화
    const mainContent = document.querySelector('.main-content');
    if (mainContent) {
        mainContent.scrollTop = 0;
    }
}

// 프로필 선택 시 이동
async function goToProfile() {
    try {
        const role = await getUserRole();

        if (role === 'USER') {
            window.location.href = '/users/mypage';
        }
        else if (role === 'PROVIDER') {
            window.location.href = '/mypage/provider';
        } else if (role === 'HOST') {
            window.location.href = '/mypage/host';
        } else {
            alert('로그인이 필요합니다.');
            location.assign('/auth/login');
        }
    } catch (err) {
        console.error('프로필 이동 실패:', err);
        alert('로그인이 필요합니다.');
        location.assign('/auth/login');
    }
}

// === 사용자 정보 가져오기 ===
function getUserInfo() {
    try {
        const storage = localStorage.getItem('userId') ? localStorage : sessionStorage;

        return {
            userId: storage.getItem('userId'),
            email: storage.getItem('userEmail'),
            name: storage.getItem('userName'),
            role: storage.getItem('userRole'),
            loginTime: storage.getItem('loginTime')
        };
    } catch (error) {
        console.warn('사용자 정보 조회 실패:', error);
        return {};
    }
}

// === 로그아웃 관련 ===
async function performLogout() {
    try {
        showAlert('로그아웃 중...', 'info');

        // LogoutController 사용
        const success = await logoutController.performLogout({
            showConfirm: false,  // 이미 확인했으므로
            redirectUrl: '/auth/login?logout=true'
        });

        if (!success) {
            throw new Error('로그아웃 처리 실패');
        }

    } catch (error) {
        console.error('로그아웃 실패:', error);
        showAlert('로그아웃 중 오류가 발생했습니다.', 'error');

        // 오류가 발생해도 강제로 로그인 페이지로 이동
        setTimeout(() => {
            window.location.href = '/auth/login';
        }, 2000);
    }
}

// === 인증 체크 ===
function checkAuthOnPageLoad() {
    const currentPath = window.location.pathname;

    // 공개 페이지는 인증 체크 하지 않음
    const publicPaths = [
        '/',
        '/index.html',
        '/main',
        '/popup/',
        '/map',
        '/reviews/',
        '/space',
    ];

    // 현재 경로가 공개 페이지인지 확인
    const isPublicPage = publicPaths.some(path => currentPath.startsWith(path) || currentPath === path);

    if (isPublicPage) {
        console.log('공개 페이지 - 인증 체크 건너뜀');
        return true;
    }

    // 비공개 페이지(마이페이지, 북마크 등)만 토큰 체크
    const token = apiService.getStoredToken();
    if (!token || isTokenExpired(token)) {
        console.log('인증 필요한 페이지 - 로그인 페이지로 리다이렉트');
        const redirectUrl = encodeURIComponent(currentPath);
        window.location.href = `/auth/login?redirect=${redirectUrl}`;
        return false;
    }

    return true;
}

// === 토큰 만료 체크 ===
function isTokenExpired(token) {
    if (!token) return true;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return Date.now() >= payload.exp * 1000;
    } catch (error) {
        return true;
    }
}

// === 자동 로그아웃 설정 ===
function setupAutoLogout() {
    // 5분마다 토큰 체크
    setInterval(() => {
        const token = apiService.getStoredToken();
        if (token && isTokenExpired(token) && !window.location.pathname.includes('/auth/')) {
            showAlert('세션이 만료되었습니다. 로그인 페이지로 이동합니다.', 'warning');

            // LogoutController를 통한 자동 로그아웃
            logoutController.autoLogout('세션이 만료되었습니다.', '/auth/login?expired=true');
        }
    }, 5 * 60 * 1000);

    // 페이지 포커스 시 토큰 체크
    window.addEventListener('focus', () => {
        const token = apiService.getStoredToken();
        if (token && isTokenExpired(token) && !window.location.pathname.includes('/auth/')) {
            logoutController.autoLogout('세션이 만료되었습니다.', '/auth/login?expired=true');
        }
    });
}

// 알림 메시지 표시
function showAlert(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;

    const mainContent = document.querySelector('.main-content');
    if (mainContent) {
        mainContent.insertBefore(alertDiv, mainContent.firstChild);

        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.parentNode.removeChild(alertDiv);
            }
        }, 3000);
    }
}

// 푸터용 사용자 역할 반환
function getStoredUserRole() {
    try {
        const storage = localStorage.getItem('userId') ? localStorage : sessionStorage;
        return storage.getItem('userRole') || 'GUEST';
    } catch (e) {
        return 'GUEST';
    }
}

// 역할에 따른 푸터 생성 함수
function createFooterByRole() {
    const userRole = getStoredUserRole();

    let navItems = `
        <a href="#" class="footer-item active" data-page="popupList">
            <svg class="footer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                <polyline points="9,22 9,12 15,12 15,22"></polyline>
            </svg>
            <span class="footer-text">홈</span>
        </a>
        <a href="#" class="footer-item" data-page="popupSearch">
            <svg class="footer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <circle cx="11" cy="11" r="8"></circle>
                <path d="m21 21-4.35-4.35"></path>
            </svg>
            <span class="footer-text">검색</span>
        </a>
        <a href="#" class="footer-item" data-page="bookmark">
            <svg class="footer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M17 3H7a2 2 0 0 0-2 2v16l7-3 7 3V5a2 2 0 0 0-2-2z"></path>
            </svg>
            <span class="footer-text">북마크</span>
        </a>`;

    // PROVIDER나 HOST일 경우 공간대여 메뉴 추가
    if (userRole === 'PROVIDER' || userRole === 'HOST') {
        navItems += `
        <a href="#" class="footer-item" data-page="spaceList">
            <svg class="footer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M3 9.75L12 4l9 5.75V20a1 1 0 01-1 1h-5v-6H9v6H4a1 1 0 01-1-1V9.75z"></path>
            </svg>
            <span class="footer-text">공간대여</span>
        </a>`;
    }

    navItems += `
        <a href="#" class="footer-item" data-page="map">
            <svg class="footer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                <circle cx="12" cy="10" r="3"></circle>
            </svg>
            <span class="footer-text">지도</span>
        </a>`;

    return `
        <footer class="footer">
            ${navItems}
        </footer>
    `;
}


// 현재 사용자 ROLE 반환
async function getUserRole() {
    try {
        const user = await apiService.getCurrentUser();
        return user?.role || 'GUEST';  // 로그인 안됐으면 GUEST
    } catch (e) {
        console.warn('사용자 정보를 가져올 수 없음:', e);
        return 'GUEST'; // 기본값
    }
}

// === 초기화 ===
document.addEventListener('DOMContentLoaded', async () => {
    // 인증 체크
    if (checkAuthOnPageLoad()) {

        // 토큰이 있는 사용자만 자동 로그아웃 설정 및 알림 체크
        const token = apiService.getStoredToken();

        if (token) {
            setupAutoLogout();

            // 로그인된 사용자면 알림 상태 초기화
            const userInfo = getUserInfo();
            if (userInfo?.userId) {
                try {
                    // 초기 알림 목록 조회 → 뱃지 상태 업데이트
                    const notifications = await apiService.getNotifications();
                    const hasUnread = notifications.some(n => !n.read);
                    updateNotificationBadge(hasUnread);
                } catch (err) {
                    console.warn("초기 알림 조회 실패 (무시):", err);
                    // 에러 무시 - 토큰이 만료되었거나 권한이 없을 수 있음
                }
            }
        }
    }
});

// === 알림 목록 불러오기 ===
async function showNotifications() {
    const dropdown = document.getElementById("notificationDropdown");
    dropdown.classList.toggle("hidden");

    if (!dropdown.classList.contains("hidden")) {
        const listEl = document.getElementById("notificationList");

        // 로그인 여부 체크
        const token = apiService.getStoredToken();

        if (!token) {
            // 로그인 안한 상태 - 로그인 안내 표시
            listEl.innerHTML = "<li class='no-data'>로그인 후 알림을 확인할 수 있습니다.</li>";
            updateNotificationBadge(false);
            return;
        }

        try {
            const notifications = await apiService.getNotifications();
            listEl.innerHTML = "";

            if (!notifications || notifications.length === 0) {
                listEl.innerHTML = "<li class='no-data'>알림이 없습니다.</li>";
                updateNotificationBadge(false);
                return;
            }

            notifications.forEach(n => {
                const li = buildNotificationItem(n);
                listEl.appendChild(li);
            });

            // 뱃지 상태 갱신
            const hasUnread = notifications.some(n => !n.read);
            updateNotificationBadge(hasUnread);

        } catch (err) {
            console.error("알림 불러오기 실패:", err);
            listEl.innerHTML =
                "<li class='error'>알림을 불러오는 중 오류가 발생했습니다.</li>";
        }
    }
}

// === 알림 아이템 HTML 생성 ===
function buildNotificationItem(n) {
    const li = document.createElement("li");
    li.className = n.read ? "notification-item read" : "notification-item unread";

    // 왼쪽 내용
    const contentDiv = document.createElement("div");
    contentDiv.className = "notification-content";
    contentDiv.innerHTML = `
        <div class="notification-title">${n.title}</div>
        <div class="notification-message">${n.message}</div>
    `;

    li.appendChild(contentDiv);

    // 읽지 않은 경우 → 읽음 버튼 추가
    if (!n.read) {
        const btn = document.createElement("button");
        btn.className = "mark-read-btn";
        btn.textContent = "읽음";

        btn.addEventListener("click", async (e) => {
            e.stopPropagation();

            li.classList.remove("unread");
            li.classList.add("read");
            btn.remove();
            updateNotificationBadge(false);

            await apiService.markNotificationRead(n.id);
        });

        li.appendChild(btn);
    }

    // 알림 전체 클릭 → 읽음 처리 + 링크 이동
    li.addEventListener("click", async () => {
        if (!li.classList.contains("read")) {
            li.classList.remove("unread");
            li.classList.add("read");
            const btn = li.querySelector(".mark-read-btn");
            if (btn) btn.remove();

            try {
                await apiService.markNotificationRead(n.id);
            } catch (err) {
                console.error("알림 읽음 처리 실패:", err);
            }
        }

        if (n.link) {
            window.location.href = n.link;
        }
    });

    return li;
}

// === 알림 아이콘 뱃지 표시 ===
function updateNotificationBadge(show) {
    const iconBtn = document.getElementById("notificationIcon");
    if (!iconBtn) return;

    let badge = iconBtn.querySelector(".notification-badge");

    if (show) {
        if (!badge) {
            badge = document.createElement("span");
            badge.className = "notification-badge";
            badge.style.cssText = `
                position:absolute; top:2px; right:2px;
                width:8px; height:8px;
                background:red; border-radius:50%;
            `;
            iconBtn.style.position = "relative";
            iconBtn.appendChild(badge);
        }
    } else {
        if (badge) badge.remove();
    }
}

// === 실행 ===
document.addEventListener("DOMContentLoaded", async () => {
    await loadComponents();
    initializeLayout();
});
