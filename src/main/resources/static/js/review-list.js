// 리뷰 목록 페이지 매니저
class ReviewListManager {
    constructor(popupId) {
        this.popupId = popupId;
        this.popupData = null;
        this.currentPage = 0;
        this.pageSize = 10;
        this.currentSort = 'latest';
        this.hasMore = true;
        this.loading = false;
    }

    // 초기화
    async initialize() {
        try {
            this.setupEventListeners();
            await this.loadPopupInfo();
            await this.loadReviewStats();
            await this.loadReviews(true); // 첫 페이지 로드
        } catch (error) {
            console.error('리뷰 목록 페이지 초기화 실패:', error);
            this.showError();
        }
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        // 리뷰 작성 버튼
        const writeReviewBtn = document.getElementById('writeReviewBtn');
        const writeFirstReviewBtn = document.getElementById('writeFirstReviewBtn');

        if (writeReviewBtn) {
            writeReviewBtn.addEventListener('click', () => this.handleWriteReview());
        }

        if (writeFirstReviewBtn) {
            writeFirstReviewBtn.addEventListener('click', () => this.handleWriteReview());
        }

        // 정렬 필터 버튼
        const filterBtns = document.querySelectorAll('.filter-btn');
        filterBtns.forEach(btn => {
            btn.addEventListener('click', (e) => this.handleSortChange(e));
        });

        // 더보기 버튼
        const loadMoreBtn = document.getElementById('loadMoreBtn');
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener('click', () => this.loadMoreReviews());
        }
    }

    // 팝업 정보 로드
    async loadPopupInfo() {
        try {
            const response = await fetch(`/api/popups/${this.popupId}`);
            if (!response.ok) throw new Error('팝업 정보 로드 실패');

            this.popupData = await response.json();
            this.renderPopupHeader();
        } catch (error) {
            console.error('팝업 정보 로드 실패:', error);
            this.showPopupError();
        }
    }

    // 리뷰 통계 로드
    async loadReviewStats() {
        try {
            const response = await fetch(`/api/reviews/popup/${this.popupId}/stats`);
            if (!response.ok) throw new Error('리뷰 통계 로드 실패');

            const stats = await response.json();
            this.renderReviewStats(stats);
        } catch (error) {
            console.error('리뷰 통계 로드 실패:', error);
            this.renderReviewStats({ averageRating: 0, totalReviews: 0 });
        }
    }

    // 리뷰 목록 로드
    async loadReviews(reset = false) {
        if (this.loading) return;

        this.loading = true;

        if (reset) {
            this.currentPage = 0;
            this.hasMore = true;
        }

        try {
            const sortParam = this.getSortParam();
            const response = await fetch(
                `/api/reviews/popup/${this.popupId}?page=${this.currentPage}&size=${this.pageSize}&sort=${sortParam}`
            );

            if (!response.ok) throw new Error('리뷰 로드 실패');

            const result = await response.json();

            if (reset) {
                this.renderReviews(result.content);
            } else {
                this.appendReviews(result.content);
            }

            this.hasMore = !result.last;
            this.updatePagination();

            // 리뷰가 없는 경우 빈 상태 표시
            if (result.totalElements === 0) {
                this.showEmptyState();
            } else {
                this.hideEmptyState();
            }

        } catch (error) {
            console.error('리뷰 로드 실패:', error);
            if (reset) {
                this.showEmptyState();
            }
        } finally {
            this.loading = false;
            this.hideReviewsLoading();
        }
    }

    // 팝업 헤더 렌더링
    renderPopupHeader() {
        if (!this.popupData) return;

        const headerEl = document.getElementById('popupHeader');
        if (!headerEl) return;

        const startDate = new Date(this.popupData.startDate).toLocaleDateString('ko-KR');
        const endDate = new Date(this.popupData.endDate).toLocaleDateString('ko-KR');
        const now = new Date();
        const endDateTime = new Date(this.popupData.endDate);
        const isActive = now <= endDateTime;

        // 안전한 기본 이미지 사용
        const defaultImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiByeD0iMTIiIGZpbGw9IiM2MzY2RjEiLz4KPHRleHQgeD0iNDAiIHk9IjUwIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiIgZm9udC1zaXplPSIyNCIgZmlsbD0id2hpdGUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGRvbWluYW50LWJhc2VsaW5lPSJjZW50cmFsIj7wn46qPC90ZXh0Pgo8L3N2Zz4=';

        headerEl.innerHTML = `
        <div class="popup-header-content">
            <img src="${this.escapeAttr(this.popupData.thumbnailUrl || defaultImage)}" 
                 alt="${this.popupData.title}" 
                 class="popup-thumbnail">
            <div class="popup-info">
                <h1>${this.escapeHtml(this.popupData.title)}</h1>
                <p class="popup-period">${startDate} ~ ${endDate}</p>
                <span class="popup-status ${isActive ? 'active' : 'ended'}">
                    ${isActive ? '운영중' : '종료'}
                </span>
            </div>
        </div>
    `;
    }

    // 리뷰 통계 렌더링
    renderReviewStats(stats) {
        const statsEl = document.getElementById('reviewStats');
        if (!statsEl) return;

        statsEl.innerHTML = `
            <div class="rating-display">
                <div class="stars">
                    ${this.renderStars(stats.averageRating || 0)}
                </div>
                <span class="rating-text">${(stats.averageRating || 0).toFixed(1)}</span>
            </div>
            <span class="review-count">리뷰 ${stats.totalReviews || 0}개</span>
        `;
    }

    // 리뷰 목록 렌더링
    renderReviews(reviews) {
        const listEl = document.getElementById('reviewsList');
        if (!listEl) return;

        listEl.innerHTML = reviews.map(review => this.renderReviewItem(review)).join('');
    }

    // 리뷰 목록 추가 렌더링
    appendReviews(reviews) {
        const listEl = document.getElementById('reviewsList');
        if (!listEl) return;

        const newItems = reviews.map(review => this.renderReviewItem(review)).join('');
        listEl.insertAdjacentHTML('beforeend', newItems);
    }

    // 리뷰 아이템 렌더링
    renderReviewItem(review) {
        const createdDate = new Date(review.createdAt).toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });

        return `
            <div class="review-item">
                <div class="review-header">
                    <div class="review-rating">
                        ${this.renderReviewStars(review.rating)}
                    </div>
                    <span class="review-date">${createdDate}</span>
                </div>
                <p class="review-content">${this.escapeHtml(review.content)}</p>
                <div class="reviewer-info">
                    <div class="reviewer-details">
                        <p class="reviewer-name">${this.escapeHtml(review.userName || '익명')}</p>
                    </div>
                </div>
            </div>
        `;
    }

    // 별점 렌더링 (통계용)
    renderStars(rating) {
        let stars = '';
        for (let i = 1; i <= 5; i++) {
            if (i <= rating) {
                stars += '<span class="star">★</span>';
            } else {
                stars += '<span class="star empty">★</span>';
            }
        }
        return stars;
    }

    // 별점 렌더링 (리뷰용)
    renderReviewStars(rating) {
        let stars = '';
        for (let i = 1; i <= 5; i++) {
            if (i <= rating) {
                stars += '<span class="review-star">★</span>';
            } else {
                stars += '<span class="review-star empty">★</span>';
            }
        }
        return stars;
    }

    // 정렬 변경 처리
    handleSortChange(e) {
        const newSort = e.target.dataset.sort;
        if (newSort === this.currentSort) return;

        // 버튼 UI 업데이트
        document.querySelectorAll('.filter-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        e.target.classList.add('active');

        // 상태 업데이트 및 데이터 다시 로드
        this.currentSort = newSort;
        this.loadReviews(true);
    }

    // 더보기 처리
    async loadMoreReviews() {
        if (!this.hasMore || this.loading) return;

        this.currentPage++;
        await this.loadReviews(false);
    }

    // 리뷰 작성 처리
    async handleWriteReview() {
        // 사용자 로그인 체크
        const token = apiService.getStoredToken && apiService.getStoredToken();
        const userId = this.getCurrentUserId();
        if (!token) {
            alert('로그인이 필요한 서비스입니다.');
            window.location.href = '/login';
            return;
        }

        // userId 미캐시 시 한 번 갱신 시도
        if (!userId) {
            try { await this.refreshUserIdAsync(); } catch {}
        }

        const cached = (localStorage.getItem('userId') || sessionStorage.getItem('userId'));
        if (!cached) {
            alert('세션이 만료되었습니다. 다시 로그인해주세요.');
            this.clearUserData();
            window.location.href = '/login';
            return;
        }

        // 리뷰 작성 페이지로 이동
        window.location.href = `/reviews/popup/${this.popupId}/create`;
    }

    // 정렬 파라미터 변환
    getSortParam() {
        switch (this.currentSort) {
            case 'latest':
                return 'createdAt,desc';
            case 'rating_high':
                return 'rating,desc';
            case 'rating_low':
                return 'rating,asc';
            default:
                return 'createdAt,desc';
        }
    }

    // 페이지네이션 업데이트
    updatePagination() {
        const container = document.getElementById('paginationContainer');
        const loadMoreBtn = document.getElementById('loadMoreBtn');

        if (!container || !loadMoreBtn) return;

        if (this.hasMore) {
            container.style.display = 'block';
            loadMoreBtn.disabled = this.loading;
            loadMoreBtn.textContent = this.loading ? '로딩 중...' : '더 많은 리뷰 보기';
        } else {
            container.style.display = 'none';
        }
    }

    // 빈 상태 표시
    showEmptyState() {
        const emptyState = document.getElementById('emptyState');
        const reviewsContainer = document.querySelector('.reviews-container');

        if (emptyState) emptyState.style.display = 'block';
        if (reviewsContainer) reviewsContainer.style.display = 'none';
    }

    // 빈 상태 숨김
    hideEmptyState() {
        const emptyState = document.getElementById('emptyState');
        const reviewsContainer = document.querySelector('.reviews-container');

        if (emptyState) emptyState.style.display = 'none';
        if (reviewsContainer) reviewsContainer.style.display = 'block';
    }

    // 리뷰 로딩 숨김
    hideReviewsLoading() {
        const loading = document.querySelector('.reviews-loading');
        if (loading) loading.style.display = 'none';
    }

    // 팝업 정보 에러 표시
    showPopupError() {
        const headerEl = document.getElementById('popupHeader');
        if (headerEl) {
            headerEl.innerHTML = `
                <div class="popup-header-content">
                    <div class="error-message">
                        <p>팝업 정보를 불러오는 중 오류가 발생했습니다.</p>
                    </div>
                </div>
            `;
        }
    }

    // 전체 에러 표시
    showError() {
        const container = document.querySelector('.review-list-container');
        if (container) {
            container.innerHTML = `
                <div class="error-container">
                    <p>페이지를 불러오는 중 오류가 발생했습니다.</p>
                    <button onclick="window.history.back()" class="btn btn-primary">돌아가기</button>
                </div>
            `;
        }
    }

    // 현재 사용자 ID 가져오기
    getCurrentUserId() {
        // 캐시된 userId 확인
        try {
            const cachedUserId = localStorage.getItem('userId') || sessionStorage.getItem('userId');
            if (cachedUserId && cachedUserId !== 'null') {
                return parseInt(cachedUserId);
            }
        } catch (error) {
            console.warn('캐시된 userId 확인 실패:', error);
        }

        // 토큰이 있는지 확인
        if (!apiService.getStoredToken()) {
            return null;
        }

        this.refreshUserIdAsync();

        return null; // 첫 번째 호출에서는 null 반환, 이후 캐시된 값 사용
    }

    // 비동기로 사용자 ID 새로고침
    async refreshUserIdAsync() {
        try {
            const userInfo = await apiService.getCurrentUser();
            if (userInfo && userInfo.id) {
                // 사용자 ID를 캐시에 저장
                try {
                    localStorage.setItem('userId', userInfo.id.toString());
                } catch {
                    sessionStorage.setItem('userId', userInfo.id.toString());
                }
            }
        } catch (error) {
            console.warn('사용자 정보 가져오기 실패:', error);
            // 토큰이 만료된 경우 정리
            if (error.message.includes('401') || error.message.includes('인증')) {
                this.clearUserData();
            }
        }
    }

    // 인증 정보 초기화
    clearUserData() {
        try {
            localStorage.removeItem('userId');
            sessionStorage.removeItem('userId');
        } catch {}

        if (apiService.clearStoredToken) {
            try { apiService.clearStoredToken(); } catch {}
        }
    }


    // HTML 이스케이프
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    // Attribute 이스케이프
    escapeAttr(value) {
        const s = value == null ? '' : String(value);
        return s
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }
}

// 전역 등록
window.ReviewListManager = ReviewListManager;