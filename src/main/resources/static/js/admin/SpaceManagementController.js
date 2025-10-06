/**
 * 장소 관리 컨트롤러 클래스
 */
class SpaceManagementController {
    constructor() {
        this.api = new SpaceManagementApi();
        this.ui = new SpaceManagementUI();
        this.currentPage = 0;
        this.pageSize = 20;
        this.currentFilters = {};

        this.init();
    }

    /**
     * 초기화
     */
    async init() {
        this.setupEventListeners();
        await this.loadInitialData();
    }

    /**
     * 이벤트 리스너 설정
     */
    setupEventListeners() {
        // 검색 버튼
        if (this.ui.elements.searchBtn) {
            this.ui.elements.searchBtn.addEventListener('click', () => {
                this.search();
            });
        }

        // 초기화 버튼
        if (this.ui.elements.resetBtn) {
            this.ui.elements.resetBtn.addEventListener('click', () => {
                this.reset();
            });
        }

        // 필터 입력 시 엔터키로 검색
        const filterInputs = [
            this.ui.elements.ownerFilter,
            this.ui.elements.titleFilter
        ];

        filterInputs.forEach(input => {
            if (input) {
                input.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') {
                        this.search();
                    }
                });
            }
        });

        // 모달 닫기 이벤트
        if (this.ui.elements.closeModal) {
            this.ui.elements.closeModal.addEventListener('click', () => {
                this.ui.closeModal();
            });
        }

        if (this.ui.elements.confirmModalClose) {
            this.ui.elements.confirmModalClose.addEventListener('click', () => {
                this.ui.closeModal();
            });
        }

        if (this.ui.elements.cancelBtn) {
            this.ui.elements.cancelBtn.addEventListener('click', () => {
                this.ui.closeModal();
            });
        }

        if (this.ui.elements.confirmBtn) {
            this.ui.elements.confirmBtn.addEventListener('click', () => {
                this.ui.executeConfirmedAction();
            });
        }

        // 모달 배경 클릭 시 닫기
        [this.ui.elements.spaceDetailModal, this.ui.elements.confirmModal].forEach(modal => {
            if (modal) {
                modal.addEventListener('click', (e) => {
                    if (e.target === modal) {
                        this.ui.closeModal();
                    }
                });
            }
        });

        // ★ 테이블 버튼 이벤트 처리 (이벤트 위임 방식) - 수정된 부분
        if (this.ui.elements.spacesTableBody) {
            this.ui.elements.spacesTableBody.addEventListener('click', (e) => {
                const target = e.target;
                const spaceId = target.getAttribute('data-space-id');

                if (!spaceId) return;

                // 상세보기 버튼
                if (target.classList.contains('detail-button')) {
                    this.showSpaceDetail(spaceId);
                }
                // 상태 토글 버튼 - 새로 수정된 부분
                else if (target.classList.contains('toggle-visibility-button')) {
                    const isHidden = target.getAttribute('data-is-hidden') === 'true';
                    this.toggleSpaceVisibility(spaceId, isHidden);
                }
            });
        }
    }

    /**
     * 초기 데이터 로드
     */
    async loadInitialData() {
        try {
            // 통계 로드
            const stats = await this.api.getSpaceStats();
            this.ui.updateStats(stats);

            // 장소 목록 로드
            await this.loadSpaces();
        } catch (error) {
            console.error('초기 데이터 로드 실패:', error);
            this.ui.showError('데이터를 불러오는데 실패했습니다.');
        }
    }

    /**
     * 장소 목록 로드
     */
    async loadSpaces() {
        try {
            this.ui.showLoading(true);

            const params = {
                page: this.currentPage,
                size: this.pageSize,
                ...this.getFilters()
            };

            console.log('🔍 장소 목록 로드 요청:', params);
            const data = await this.api.getSpaces(params);
            console.log('📦 API 응답 데이터:', data);

            this.ui.renderSpacesTable(data);

            this.ui.showLoading(false);
        } catch (error) {
            console.error('장소 목록 로드 실패:', error);
            this.ui.showLoading(false);
            this.api.handleError(error);
        }
    }

    /**
     * ★ 장소 상세 정보 표시
     */
    async showSpaceDetail(spaceId) {
        try {
            console.log('🔍 장소 상세 정보 로드 시작:', spaceId);

            const space = await this.api.getSpaceDetail(spaceId);
            console.log('✅ 장소 상세 정보 로드 완료:', space);

            this.ui.showSpaceDetail(space);
        } catch (error) {
            console.error('장소 상세 정보 로드 실패:', error);
            this.api.handleError(error);
        }
    }

    /**
     * 검색
     */
    search() {
        this.currentPage = 0;
        this.loadSpaces();
    }

    /**
     * 초기화 - isPublic → isHidden으로 변경
     */
    reset() {
        // 필터 초기화
        if (this.ui.elements.ownerFilter) this.ui.elements.ownerFilter.value = '';
        if (this.ui.elements.titleFilter) this.ui.elements.titleFilter.value = '';
        if (this.ui.elements.isHiddenFilter) this.ui.elements.isHiddenFilter.value = '';

        this.currentPage = 0;
        this.loadSpaces();
    }

    /**
     * 페이지 로드
     */
    loadPage(page) {
        this.currentPage = page;
        this.loadSpaces();
    }

    /**
     * 필터 값 가져오기 - isPublic → isHidden으로 변경
     */
    getFilters() {
        const filters = {};

        if (this.ui.elements.ownerFilter?.value) {
            filters.owner = this.ui.elements.ownerFilter.value;
        }

        if (this.ui.elements.titleFilter?.value) {
            filters.title = this.ui.elements.titleFilter.value;
        }

        // ★ isPublic → isHidden 로직 변경
        if (this.ui.elements.isHiddenFilter?.value) {
            filters.isHidden = this.ui.elements.isHiddenFilter.value === 'true';
        }

        return filters;
    }

    /**
     * 장소 상태 토글 (활성화/비활성화) - 새로 추가된 메소드
     */
    async toggleSpaceVisibility(spaceId, currentlyHidden) {
        const action = currentlyHidden ? '활성화' : '비활성화';
        const message = `이 장소를 ${action}하시겠습니까?`;

        this.ui.showConfirm(
            `장소 ${action}`,
            message,
            () => this.executeToggleVisibility(spaceId, currentlyHidden)
        );
    }

    /**
     * 장소 상태 토글 실행 - 새로 추가된 메소드
     */
    async executeToggleVisibility(spaceId, currentlyHidden) {
        try {
            await this.api.toggleSpaceVisibility(spaceId);

            const action = currentlyHidden ? '활성화' : '비활성화';
            this.ui.showSuccess(`장소가 ${action}되었습니다.`);

            // 목록 새로고침
            await this.loadSpaces();
        } catch (error) {
            console.error('장소 상태 변경 실패:', error);
            this.api.handleError(error);
        }
    }
}

// 전역 변수로 컨트롤러 인스턴스 생성
let spaceManagementController;

// DOM 로드 완료 시 초기화
document.addEventListener('DOMContentLoaded', () => {
    spaceManagementController = new SpaceManagementController();
});