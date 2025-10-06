// 리뷰 작성 페이지 매니저
class ReviewCreateManager {
    constructor(popupId) {
        this.popupId = popupId;
        this.currentRating = 0;
        this.popupData = null;
    }

    // 초기화
    async initialize() {
        try {
            this.setupEventListeners();
            await this.loadPopupInfo();
            await this.checkExistingReview();
        } catch (error) {
            console.error('리뷰 작성 페이지 초기화 실패:', error);
            this.showError('페이지를 불러오는 중 오류가 발생했습니다.');
        }
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        // 평점 선택
        const ratingStars = document.querySelectorAll('#ratingInput .rating-star');
        ratingStars.forEach(star => {
            star.addEventListener('click', (e) => this.handleRatingClick(e));
            star.addEventListener('mouseover', (e) => this.handleRatingHover(e));
            star.addEventListener('mouseleave', () => this.handleRatingLeave());
        });

        // 텍스트 입력
        const textarea = document.getElementById('reviewContent');
        if (textarea) {
            textarea.addEventListener('input', () => this.handleTextInput());
        }

        // 폼 제출
        const form = document.getElementById('reviewForm');
        if (form) {
            form.addEventListener('submit', (e) => this.handleSubmit(e));
        }

        // 취소 버튼
        const cancelBtn = document.getElementById('cancelBtn');
        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => this.handleCancel());
        }
    }

    // 팝업 정보 로드
    async loadPopupInfo() {
        try {
            const response = await fetch(`/api/popups/${this.popupId}`);
            if (!response.ok) throw new Error('팝업 정보를 불러올 수 없습니다.');

            this.popupData = await response.json();
            this.renderPopupInfo();
        } catch (error) {
            console.error('팝업 정보 로드 실패:', error);
            this.showError('팝업 정보를 불러오는 중 오류가 발생했습니다.');
        }
    }

    // 기존 리뷰 체크
    async checkExistingReview() {
        try {
            const response = await apiService.checkUserReview(this.popupId);
            if (response.hasReviewed) {
                alert('이미 이 팝업에 리뷰를 작성하셨습니다.');
                window.location.href = `/popup/${this.popupId}`;
                return;
            }
        } catch (error) {
            console.warn('리뷰 중복 체크 실패:', error);
            // 에러가 발생해도 계속 진행
        }
    }

    // 팝업 정보 렌더링
    renderPopupInfo() {
        if (!this.popupData) return;

        const popupInfoCard = document.getElementById('popupInfoCard');
        if (!popupInfoCard) return;

        const startDate = new Date(this.popupData.startDate).toLocaleDateString('ko-KR');
        const endDate = new Date(this.popupData.endDate).toLocaleDateString('ko-KR');

        // 안전한 이미지 URL 생성
        const imageUrl = this.popupData.thumbnailUrl ||
            'https://via.placeholder.com/60x60/4B5AE4/ffffff?text=🎪';

        popupInfoCard.innerHTML = `
        <div class="popup-info-content">
            <img src="${imageUrl}" 
                 alt="${this.escapeHtml(this.popupData.title)}" 
                 class="popup-thumbnail"
                 onload="this.style.opacity='1'"
                 style="opacity:0;transition:opacity 0.3s;">
            <div class="popup-details">
                <h3>${this.escapeHtml(this.popupData.title)}</h3>
                <p class="popup-period">${startDate} ~ ${endDate}</p>
            </div>
        </div>
    `;
    }

    // 평점 클릭 처리
    handleRatingClick(e) {
        const rating = parseInt(e.target.dataset.rating);
        this.currentRating = rating;
        this.updateRatingDisplay(rating);
        this.updateRatingLabel(rating);
        this.validateForm();
    }

    // 평점 호버 처리
    handleRatingHover(e) {
        const rating = parseInt(e.target.dataset.rating);
        this.updateRatingDisplay(rating);
        this.updateRatingLabel(rating);
    }

    // 평점 호버 해제 처리
    handleRatingLeave() {
        this.updateRatingDisplay(this.currentRating);
        this.updateRatingLabel(this.currentRating);
    }

    // 평점 표시 업데이트
    updateRatingDisplay(rating) {
        const stars = document.querySelectorAll('#ratingInput .rating-star');
        stars.forEach((star, index) => {
            if (index < rating) {
                star.classList.add('active');
            } else {
                star.classList.remove('active');
            }
        });
    }

    // 평점 라벨 업데이트
    updateRatingLabel(rating) {
        const labels = document.querySelectorAll('.rating-label');
        labels.forEach((label, index) => {
            if (index + 1 === rating) {
                label.classList.add('active');
            } else {
                label.classList.remove('active');
            }
        });
    }

    // 텍스트 입력 처리
    handleTextInput() {
        const textarea = document.getElementById('reviewContent');
        const charCount = document.getElementById('charCount');

        const length = textarea.value.length;
        charCount.textContent = length;

        // 글자 수 제한 표시
        if (length > 1000) {
            charCount.classList.add('over-limit');
        } else {
            charCount.classList.remove('over-limit');
        }

        this.validateForm();
    }

    // 폼 유효성 검사
    validateForm() {
        const submitBtn = document.getElementById('submitBtn');
        const content = document.getElementById('reviewContent').value.trim();

        const isValid = this.currentRating > 0 &&
            content.length >= 10 &&
            content.length <= 1000;

        submitBtn.disabled = !isValid;
    }

    // 폼 제출 처리
    async handleSubmit(e) {
        e.preventDefault();

        const content = document.getElementById('reviewContent').value.trim();

        if (this.currentRating === 0) {
            alert('평점을 선택해주세요.');
            return;
        }

        if (content.length < 10) {
            alert('리뷰 내용을 10자 이상 입력해주세요.');
            return;
        }

        if (content.length > 1000) {
            alert('리뷰 내용은 1000자를 초과할 수 없습니다.');
            return;
        }

        this.showLoading();

        try {
            // apiService 사용으로 변경
            const response = await apiService.createReview({
                popupId: this.popupId,
                content: content,
                rating: this.currentRating
            });

            this.hideLoading();
            alert('리뷰가 성공적으로 등록되었습니다.');
            // 팝업 상세 페이지로 이동
            window.location.href = `/popup/${this.popupId}`;
        } catch (error) {
            this.hideLoading();
            console.error('리뷰 제출 실패:', error);

            if (error.message.includes('500')) {
                alert('서버 오류가 발생했습니다. 이미 리뷰를 작성하셨거나 일시적인 문제일 수 있습니다.');
            } else if (error.message.includes('401')) {
                alert('로그인이 필요합니다.');
                window.location.href = '/login';
            } else {
                alert(error.message || '리뷰 등록 중 오류가 발생했습니다.');
            }
        }
    }

    // 취소 처리
    handleCancel() {
        const hasContent = this.currentRating > 0 ||
            document.getElementById('reviewContent').value.trim().length > 0;

        if (hasContent) {
            const confirm = window.confirm('작성 중인 내용이 있습니다. 정말 취소하시겠습니까?');
            if (!confirm) return;
        }

        // 이전 페이지로 이동 (팝업 상세 페이지)
        window.location.href = `/popup/${this.popupId}`;
    }

    // 로딩 표시
    showLoading() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.style.display = 'flex';
        }
    }

    // 로딩 숨김
    hideLoading() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.style.display = 'none';
        }
    }

    // 에러 표시
    showError(message) {
        this.hideLoading();
        alert(message);
    }

    // HTML 이스케이프
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// 전역 등록
window.ReviewCreateManager = ReviewCreateManager;