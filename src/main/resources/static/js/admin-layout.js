// === 관리자 레이아웃 로드 ===
async function loadAdminComponents() {
    try {
        // === 헤더 로드 ===
        const headerRes = await fetch("/templates/components/admin-header.html");
        if (!headerRes.ok) throw new Error("헤더 로드 실패");
        const headerHTML = await headerRes.text();
        document.getElementById("admin-header-container").innerHTML = headerHTML;

        // === 푸터 로드 ===
        const footerRes = await fetch("/templates/components/admin-footer.html");
        if (!footerRes.ok) throw new Error("푸터 로드 실패");
        const footerHTML = await footerRes.text();
        document.getElementById("admin-footer-container").innerHTML = footerHTML;

        // 이벤트 바인딩
        setupAdminEvents();

    } catch (error) {
        console.error("관리자 레이아웃 로드 실패:", error);
        createAdminFallbackLayout();
    }
}

// === 이벤트 설정 ===
function setupAdminEvents() {
    // 로그아웃 버튼 이벤트
    const logoutBtn = document.getElementById("adminLogoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", async () => {
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
        });
    }

    // 푸터 네비게이션 active 처리만 담당
    const navItems = document.querySelectorAll(".footer-item");
    const currentPath = window.location.pathname;

    navItems.forEach(item => {
        const href = item.getAttribute("href");
        if (href && currentPath.startsWith(href)) {
            item.classList.add("active");
        } else {
            item.classList.remove("active");
        }
    });
}


// === 기본 폴백 ===
function createAdminFallbackLayout() {
    document.getElementById("admin-header-container").innerHTML = `
        <header class="admin-header">
            <div class="logo">Admin</div>
            <button id="adminLogoutBtn">로그아웃</button>
        </header>
    `;
    document.getElementById("admin-footer-container").innerHTML = `
        <footer class="admin-footer">
            <p>관리자 페이지</p>
        </footer>
    `;
    setupAdminEvents();
}

// === 공통 레이아웃 초기화 ===
function initializeLayout() {
    const mainContent = document.querySelector(".admin-container");
    if (mainContent) {
        mainContent.scrollTop = 0;
    }
}

// === 실행 ===
document.addEventListener("DOMContentLoaded", async () => {
    await loadAdminComponents();
    initializeLayout();
});
