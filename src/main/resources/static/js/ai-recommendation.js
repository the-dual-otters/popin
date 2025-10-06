/**
 * AI 추천 팝업 관리 클래스
 */
class AIRecommendationManager {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.isLoading = false;
        this.hasMore = true;
        this.recommendations = [];

        // DOM 요소들
        this.container = null;
        this.loadingEl = null;
        this.emptyEl = null;
        this.errorEl = null;
        this.loadMoreBtn = null;

        this.initializeElements();
        this.bindEvents();
    }

    /**
     * DOM 요소 초기화
     */
    initializeElements() {
        this.container = document.getElementById('ai-recommendation-list');
        this.loadingEl = document.getElementById('ai-loading');
        this.emptyEl = document.getElementById('ai-empty');
        this.errorEl = document.getElementById('ai-error');
        this.loadMoreBtn = document.getElementById('ai-load-more');

        // 요소가 없으면 생성
        if (!this.container) {
            console.warn('AI 추천 컨테이너를 찾을 수 없습니다.');
            return;
        }
    }

    /**
     * 이벤트 바인딩
     */
    bindEvents() {
        if (this.loadMoreBtn) {
            this.loadMoreBtn.addEventListener('click', () => this.loadMore());
        }

        // 새로고침 버튼이 있다면
        const refreshBtn = document.getElementById('ai-refresh');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => this.refresh());
        }
    }

    /**
     * 초기 로드
     */
    async initialize() {
        console.log('AI 추천 매니저 초기화 시작');
        await this.loadRecommendations(true);
    }

    /**
     * AI 추천 팝업 로드
     */
    async loadRecommendations(reset = false) {
        if (this.isLoading) return;

        try {
            this.isLoading = true;
            this.showLoading(true);

            if (reset) {
                this.currentPage = 0;
                this.recommendations = [];
                this.hasMore = true;
            }

            const params = {
                page: this.currentPage,
                size: this.pageSize
            };

            console.log('AI 추천 로드 시작 - 페이지:', this.currentPage);

            const response = await apiService.getAIRecommendedPopups(params);

            if (!response || !Array.isArray(response.content)) {
                throw new Error('잘못된 응답 형식');
            }

            const newRecommendations = response.content;

            if (reset) {
                this.recommendations = newRecommendations;
                this.clearContainer();
            } else {
                this.recommendations.push(...newRecommendations);
            }

            // 더 로드할 데이터가 있는지 확인
            this.hasMore = !response.last && newRecommendations.length === this.pageSize;

            // UI 업데이트
            this.renderRecommendations(newRecommendations, reset);
            this.updateLoadMoreButton();
            this.updateEmptyState();

            this.currentPage++;

            console.log(`AI 추천 로드 완료 - ${newRecommendations.length}개 로드됨`);

        } catch (error) {
            console.error('AI 추천 로드 실패:', error);
            this.handleError(error);
        } finally {
            this.isLoading = false;
            this.showLoading(false);
        }
    }

    /**
     * 더보기
     */
    async loadMore() {
        if (!this.hasMore || this.isLoading) return;
        await this.loadRecommendations(false);
    }

    /**
     * 새로고침
     */
    async refresh() {
        console.log('AI 추천 새로고침');
        await this.loadRecommendations(true);
    }

    /**
     * 추천 목록 렌더링
     */
    renderRecommendations(recommendations, reset = false) {
        if (!this.container) return;

        const fragment = document.createDocumentFragment();

        recommendations.forEach(popup => {
            const card = this.createPopupCard(popup);
            fragment.appendChild(card);
        });

        if (reset) {
            this.container.innerHTML = '';
        }

        this.container.appendChild(fragment);
    }

    /**
     * 팝업 카드 생성
     */
    createPopupCard(popup) {
        const card = document.createElement('div');
        card.className = 'popup-card ai-recommended';
        card.onclick = () => goToPopupDetail(popup.id);

        // 이미지 URL 처리
        const imageUrl = popup.thumbnailUrl || '/images/popup-placeholder.jpg';

        // 날짜 포맷팅
        const dateRange = this.formatDateRange(popup.startDate, popup.endDate);

        // 상태 표시
        const statusBadge = this.getStatusBadge(popup.status);

        card.innerHTML = `
            <div class="popup-image">
                <img src="${imageUrl}" alt="${popup.title}" onerror="this.src='/images/popup-placeholder.jpg'">
                <div class="ai-badge">
                    <i class="icon-ai"></i>
                    AI 추천
                </div>
                ${statusBadge}
            </div>
            <div class="popup-info">
                <h3 class="popup-title">${popup.title}</h3>
                <p class="popup-venue">
                    <i class="icon-location"></i>
                    ${popup.venueName}
                </p>
                <p class="popup-date">
                    <i class="icon-calendar"></i>
                    ${dateRange}
                </p>
                <div class="popup-stats">
                    <span class="view-count">
                        <i class="icon-eye"></i>
                        ${this.formatNumber(popup.viewCount)}
                    </span>
                    ${popup.categoryName ? `<span class="category">${popup.categoryName}</span>` : ''}
                </div>
            </div>
        `;

        return card;
    }

    /**
     * 상태 배지 생성
     */
    getStatusBadge(status) {
        const statusMap = {
            'ONGOING': { text: '진행중', class: 'ongoing' },
            'PLANNED': { text: '예정', class: 'planned' },
            'ENDED': { text: '종료', class: 'ended' }
        };

        const statusInfo = statusMap[status] || { text: status, class: 'default' };
        return `<div class="status-badge ${statusInfo.class}">${statusInfo.text}</div>`;
    }

    /**
     * 날짜 범위 포맷팅
     */
    formatDateRange(startDate, endDate) {
        if (!startDate || !endDate) return '날짜 미정';

        const start = new Date(startDate);
        const end = new Date(endDate);

        const formatDate = (date) => {
            return `${date.getMonth() + 1}.${date.getDate()}`;
        };

        if (start.getTime() === end.getTime()) {
            return formatDate(start);
        }

        return `${formatDate(start)} ~ ${formatDate(end)}`;
    }

    /**
     * 숫자 포맷팅
     */
    formatNumber(num) {
        if (!num) return '0';
        if (num >= 1000) {
            return `${Math.floor(num / 1000)}k`;
        }
        return num.toString();
    }

    /**
     * 컨테이너 초기화
     */
    clearContainer() {
        if (this.container) {
            this.container.innerHTML = '';
        }
    }

    /**
     * 로딩 상태 표시
     */
    showLoading(show) {
        if (this.loadingEl) {
            this.loadingEl.style.display = show ? 'flex' : 'none';
        }
    }

    /**
     * 빈 상태 업데이트
     */
    updateEmptyState() {
        if (!this.emptyEl) return;

        const isEmpty = this.recommendations.length === 0;
        this.emptyEl.style.display = isEmpty ? 'flex' : 'none';

        if (isEmpty) {
            // 로그인 상태에 따른 메시지 변경
            const isLoggedIn = apiService.isLoggedIn();
            this.emptyEl.innerHTML = isLoggedIn
                ? `
                    <div class="empty-content">
                        <i class="icon-ai-large"></i>
                        <h3>추천할 팝업이 없습니다</h3>
                        <p>더 많은 팝업을 둘러보고 관심사를 설정해보세요</p>
                        <button onclick="location.href='/mypage/user/preferences'" class="btn-primary">관심사 설정하기</button>
                    </div>
                `
                : `
                    <div class="empty-content">
                        <i class="icon-login"></i>
                        <h3>로그인하고 맞춤 추천을 받아보세요</h3>
                        <p>당신의 취향에 맞는 특별한 팝업을 AI가 추천해드립니다</p>
                        <button onclick="location.href='/auth/login'" class="btn-primary">로그인하기</button>
                    </div>
                `;
        }
    }

    /**
     * 더보기 버튼 상태 업데이트
     */
    updateLoadMoreButton() {
        if (!this.loadMoreBtn) return;

        if (this.hasMore && this.recommendations.length > 0) {
            this.loadMoreBtn.style.display = 'block';
            this.loadMoreBtn.disabled = this.isLoading;
            this.loadMoreBtn.textContent = this.isLoading ? '로딩 중...' : '더 보기';
        } else {
            this.loadMoreBtn.style.display = 'none';
        }
    }

    /**
     * 에러 처리
     */
    handleError(error) {
        console.error('AI 추천 에러:', error);

        if (this.errorEl) {
            this.errorEl.style.display = 'flex';
            this.errorEl.innerHTML = `
                <div class="error-content">
                    <i class="icon-error"></i>
                    <h3>추천을 불러올 수 없습니다</h3>
                    <p>${error.message || '네트워크 오류가 발생했습니다'}</p>
                    <button onclick="aiRecommendationManager.refresh()" class="btn-secondary">다시 시도</button>
                </div>
            `;
        } else {
            // Fallback: 컨테이너에 직접 에러 메시지 표시
            if (this.container && this.recommendations.length === 0) {
                this.container.innerHTML = `
                    <div class="error-fallback">
                        <p>추천을 불러오는데 실패했습니다.</p>
                        <button onclick="aiRecommendationManager.refresh()">다시 시도</button>
                    </div>
                `;
            }
        }
    }
}

// 전역 인스턴스 생성
let aiRecommendationManager;

// DOM 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
    // AI 추천 탭이 활성화된 경우에만 초기화
    const aiTab = document.querySelector('[data-tab="ai-recommended"]');
    if (aiTab && aiTab.classList.contains('active')) {
        aiRecommendationManager = new AIRecommendationManager();
        aiRecommendationManager.initialize();
    }
});

// 탭 전환 시 초기화 (탭 시스템이 있는 경우)
document.addEventListener('tabChange', (event) => {
    if (event.detail.tabId === 'ai-recommended') {
        if (!aiRecommendationManager) {
            aiRecommendationManager = new AIRecommendationManager();
        }
        aiRecommendationManager.initialize();
    }
});

// 전역 접근을 위한 export
window.AIRecommendationManager = AIRecommendationManager;
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AIRecommendationManager;
}