/**
 * 회원 관리 컨트롤러 - popup-management와 동일한 구조로 수정
 */
class UserManagementController {
    constructor() {
        this.api = new UserManagementApi();
        this.ui = new UserManagementUI();
        this.currentPage = 1;
        this.currentFilters = {};
        this.selectedUserId = null;

        // 전역에서 접근 가능하도록 설정 (popup-management와 동일한 패턴)
        window.userController = this;

        this.init();
    }

    /**
     * 초기화
     */
    init() {
        console.log('UserManagementController 초기화 시작');
        this.checkAdminAuth();
        this.initEventListeners();
        this.loadInitialData();
        console.log('UserManagementController 초기화 완료');
    }

    /**
     * 관리자 권한 확인
     */
    checkAdminAuth() {
        const token = localStorage.getItem('authToken');
        const userRole = localStorage.getItem('userRole');

        if (!token || userRole !== 'ADMIN') {
            alert('관리자만 접근할 수 있습니다.');
            window.location.href = '/templates/pages/auth/login.html';
            return;
        }
    }

    /**
     * 이벤트 리스너 초기화 - popup-management의 initEventListeners와 동일한 구조
     */
    initEventListeners() {
        console.log('이벤트 바인딩 시작');

        // 검색 버튼
        document.getElementById('searchBtn')?.addEventListener('click', () => this.search());
        document.getElementById('resetBtn')?.addEventListener('click', () => this.reset());

        // 엔터키로 검색
        ['searchKeyword'].forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') this.search();
                });
            }
        });

        // 모달 이벤트 - popup-management와 동일한 구조
        const modalCloseBtn = document.getElementById('modalCloseBtn');
        const closeModalBtn = document.getElementById('closeModalBtn');

        if (modalCloseBtn) {
            modalCloseBtn.addEventListener('click', () => this.closeModal());
        }
        if (closeModalBtn) {
            closeModalBtn.addEventListener('click', () => this.closeModal());
        }

        // 상태 변경 관련 이벤트 - 안전하게 체크 후 바인딩
        const changeStatusBtn = document.getElementById('changeStatusBtn');
        const confirmStatusBtn = document.getElementById('confirmStatusBtn');
        const cancelStatusBtn = document.getElementById('cancelStatusBtn');
        const statusModalCloseBtn = document.getElementById('statusModalCloseBtn');

        if (changeStatusBtn) {
            changeStatusBtn.addEventListener('click', () => this.openStatusModal());
        }
        if (confirmStatusBtn) {
            confirmStatusBtn.addEventListener('click', () => this.confirmStatusChange());
        }
        if (cancelStatusBtn) {
            cancelStatusBtn.addEventListener('click', () => this.closeStatusModal());
        }
        if (statusModalCloseBtn) {
            statusModalCloseBtn.addEventListener('click', () => this.closeStatusModal());
        }

        // 모든 모달이 처음에는 숨겨져 있도록 확실히 설정
        this.ensureModalsAreClosed();

        console.log('이벤트 바인딩 완료');
    }

    /**
     * 모든 모달이 닫혀있도록 확실히 설정
     */
    ensureModalsAreClosed() {
        const detailModal = document.getElementById('detailModal');
        const statusSelectModal = document.getElementById('statusSelectModal');

        if (detailModal) {
            detailModal.style.display = 'none';
        }
        if (statusSelectModal) {
            statusSelectModal.style.display = 'none';
        }
    }

    /**
     * 초기 데이터 로드
     */
    async loadInitialData() {
        console.log('초기 데이터 로드 시작');
        try {
            // 계정 전환 요청 개수 로드
            const upgradeRequestCount = await this.api.getUpgradeRequestCount();
            this.ui.updateUpgradeRequestCount(upgradeRequestCount);

            // 초기 검색 (전체 목록)
            this.search();

        } catch (error) {
            console.error('초기 데이터 로드 실패:', error);
        }
    }

    /**
     * 검색 실행 - popup-management의 search와 동일한 구조
     */
    search() {
        this.currentFilters = this.ui.getSearchParams();
        this.currentPage = 1;
        this.loadUsers();
    }

    /**
     * 필터 초기화 - popup-management의 reset과 동일한 구조
     */
    reset() {
        this.ui.resetSearchForm();
        this.currentFilters = {};
        this.currentPage = 1;
        this.loadUsers();
    }

    /**
     * 페이지 이동 - popup-management의 goToPage와 동일한 구조
     */
    goToPage(page) {
        this.currentPage = page;
        this.loadUsers();
    }

    /**
     * 사용자 목록 로드 - popup-management의 loadPopups와 동일한 구조
     */
    async loadUsers() {
        try {
            this.ui.showLoading();

            const params = {
                ...this.currentFilters,
                page: this.currentPage - 1, // 백엔드는 0부터 시작
                size: 10
            };

            const data = await this.api.searchUsers(params);
            console.log('검색 결과:', data);

            this.ui.displaySearchResults(data);
            this.ui.setupPagination(data, this.currentPage, (newPage) => {
                this.goToPage(newPage);
            });

        } catch (error) {
            console.error('사용자 목록 로드 실패:', error);
            this.ui.showError('사용자 목록을 불러오는데 실패했습니다.');
        } finally {
            this.ui.hideLoading();
        }
    }

    /**
     * 사용자 상세 보기 - popup-management의 viewDetail과 정확히 동일한 구조
     */
    async viewDetail(userId) {
        this.selectedUserId = userId;

        try {
            const user = await this.api.getUserDetail(userId);
            console.log('사용자 상세 정보 조회 성공:', user);

            this.ui.renderDetailModal(user);

        } catch (error) {
            console.error('사용자 상세 정보 조회 실패:', error);
            this.ui.showError('사용자 정보를 불러오는데 실패했습니다: ' + error.message);
        }
    }

    /**
     * 상태 변경 모달 열기 - popup-management의 openStatusModal과 동일
     */
    openStatusModal() {
        this.ui.openStatusModal();
    }

    /**
     * 상태 변경 확인 - popup-management의 confirmStatusChange와 동일한 구조
     */
    async confirmStatusChange() {
        const newStatus = document.getElementById('newStatusSelect').value;

        if (!newStatus) {
            alert('변경할 상태를 선택해주세요.');
            return;
        }

        if (!confirm(`사용자 상태를 '${this.ui.getStatusText(newStatus)}'로 변경하시겠습니까?`)) {
            return;
        }

        try {
            const response = await this.api.updateUserStatus(this.selectedUserId, newStatus);
            console.log('사용자 상태 변경 성공:', response);

            alert('사용자 상태가 성공적으로 변경되었습니다.');

            this.closeStatusModal();
            this.closeModal();
            this.loadUsers();

        } catch (error) {
            console.error('사용자 상태 변경 실패:', error);
            alert('사용자 상태 변경에 실패했습니다: ' + error.message);
        }
    }

    /**
     * 상태 변경 모달 닫기 - popup-management의 closeStatusModal과 동일
     */
    closeStatusModal() {
        this.ui.closeStatusModal();
    }

    /**
     * 모달 닫기 - popup-management의 closeModal과 동일
     */
    closeModal() {
        this.ui.closeModal();
        this.selectedUserId = null;
    }

    /**
     * 페이지 새로고침
     */
    refresh() {
        this.loadInitialData();
    }

    /**
     * 통계 정보 조회
     */
    async getStatistics() {
        try {
            const [totalCount, roleStats] = await Promise.all([
                this.api.getTotalUserCount(),
                this.api.getUserCountByRole()
            ]);

            return {
                totalCount,
                roleStats
            };
        } catch (error) {
            console.error('통계 정보 조회 실패:', error);
            throw error;
        }
    }

    /**
     * 사용자 상태 토글 (기존 메서드 호환성을 위해 유지)
     * @param {string} userId 사용자 ID
     * @param {string} currentStatus 현재 상태
     */
    async toggleUserStatus(userId, currentStatus) {
        console.log('사용자 상태 변경 요청 - userId:', userId, 'currentStatus:', currentStatus);

        // 새로운 상태 결정
        const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        const action = newStatus === 'ACTIVE' ? '활성화' : '비활성화';

        // 확인 메시지
        if (!confirm(`이 사용자를 ${action}하시겠습니까?`)) {
            return;
        }

        try {
            // API 호출
            const response = await this.api.updateUserStatus(userId, newStatus);
            console.log('사용자 상태 변경 성공:', response);

            // 성공 메시지
            alert(`사용자가 ${action}되었습니다.`);

            // 모달 닫기
            this.closeModal();

            // 목록 새로고침
            this.loadUsers();

        } catch (error) {
            console.error('사용자 상태 변경 실패:', error);
            this.ui.showError(`사용자 ${action}에 실패했습니다: ` + error.message);
        }
    }

    /**
     * 사용자 상세 정보 표시 (기존 호환성을 위해 유지)
     * @param {string} userId 사용자 ID
     */
    async showUserDetail(userId) {
        return this.viewDetail(userId);
    }
}