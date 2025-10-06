// 팝업 상세 페이지 매니저
class PopupDetailManager {
    constructor(popupId) {
        this.popupId = popupId;
        this.popupData = null;
        this.isBookmarked = false;
        this.reviewManager = null;
        this.shareModal = null;
    }

    // 페이지 초기화
    async initialize() {
        try {
            if (!document.getElementById('popup-detail-content')) {
                await this.renderHTML();
            }
            this.setupEventListeners();
            await this.loadPopupData();

            // 공유 모달 초기화
            this.initializeShareModal();

            // 리뷰 매니저 초기화
            this.reviewManager = new ReviewManager(this.popupId);
            await this.reviewManager.initialize();
        } catch (error) {
            console.error('팝업 상세 페이지 초기화 실패:', error);
            this.showError();
        }
    }

    // 공유 모달 초기화
    initializeShareModal() {
        this.shareModal = new ShareModal(this.getShareData.bind(this));
    }

    // HTML 렌더링
    async renderHTML() {
        const template = await TemplateLoader.load('pages/popup/popup-detail');
        document.getElementById('main-content').innerHTML = template;
        document.getElementById('page-title').textContent = 'POPIN - 팝업 상세';
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        // 공유 버튼
        const shareBtn = document.getElementById('share-btn');
        if (shareBtn) {
            shareBtn.addEventListener('click', () => this.showShareModal());
        }

        // 북마크 버튼
        const bookmarkBtn = document.getElementById('bookmark-btn');
        if (bookmarkBtn) {
            bookmarkBtn.addEventListener('click', () => this.handleBookmark());
        }

        // 예약하기 버튼
        const reservationBtn = document.getElementById('reservation-btn');
        if (reservationBtn) {
            reservationBtn.addEventListener('click', () => this.handleReservation());
        }

        // 주소 복사 버튼
        const copyAddressBtn = document.getElementById('copy-address-btn');
        if (copyAddressBtn) {
            copyAddressBtn.addEventListener('click', () => this.handleCopyAddress());
        }

        // 리뷰 작성 버튼
        const writeReviewBtn = document.querySelector('.write-review-btn');
        if (writeReviewBtn) {
            writeReviewBtn.addEventListener('click', () => this.handleWriteReview());
        }

        // 더보기 버튼
        const loadMoreBtn = document.querySelector('.load-more-btn');
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener('click', () => this.handleLoadMoreReviews());
        }

        // 유사한 팝업 클릭 이벤트
        const similarGrid = document.getElementById('similar-popups-grid');
        if (similarGrid) {
            this.initializeDragScroll(similarGrid);

            similarGrid.addEventListener('click', (e) => {
                const card = e.target.closest('.similar-popup-card');
                if (card && card.dataset.id) {
                    goToPopupDetail(card.dataset.id);
                }
            });
        }
    }

    // 팝업 데이터 로드
    async loadPopupData() {
        this.showLoading();

        try {
            this.popupData = await apiService.getPopup(this.popupId);
            await this.checkBookmarkStatus();
            this.renderPopupInfo();
            this.updateReservationButtonState(); // 예약 버튼 상태 업데이트 추가
            this.renderOperatingHours();
            this.renderLocationInfo();
            this.renderDescriptionInfo();
            await this.loadSimilarPopups();
            this.updateMetaTags();
            this.showContent();
        } catch (error) {
            console.error('팝업 데이터 로드 실패:', error);
            this.showError();
        }
    }

    // ===== 예약 버튼 상태 관리 =====

    // 예약 버튼 상태 업데이트
    updateReservationButtonState() {
        const reservationBtn = document.getElementById('reservation-btn');
        if (!reservationBtn || !this.popupData) return;

        // reservationAvailable 체크 - false면 버튼 숨김
        if (!this.popupData.reservationAvailable) {
            reservationBtn.style.display = 'none';
            return;
        }

        // reservationAvailable이 true일 때만 버튼 표시
        reservationBtn.style.display = 'block';

        // 팝업 상태에 따른 버튼 활성화/비활성화
        const popupStatus = this.popupData.status;
        const isStatusAvailable = this.isReservationAvailable(popupStatus);

        // 버튼 클래스 초기화
        reservationBtn.className = 'reservation-btn';

        if (isStatusAvailable) {
            // 예약 가능한 상태: 버튼 활성화
            reservationBtn.disabled = false;
            reservationBtn.style.cursor = 'pointer';

            // 상태별 스타일 적용
            if (popupStatus === 'PLANNED') {
                reservationBtn.classList.add('planned');
                reservationBtn.textContent = '사전 예약하기';
            } else if (popupStatus === 'ONGOING') {
                reservationBtn.classList.add('ongoing');
                reservationBtn.textContent = '예약하기';
            }
        } else {
            // 예약 불가능한 상태: 버튼 비활성화
            reservationBtn.disabled = true;
            reservationBtn.classList.add('disabled');
            reservationBtn.style.cursor = 'not-allowed';

            if (popupStatus === 'ENDED') {
                reservationBtn.textContent = '종료된 팝업';
                reservationBtn.classList.add('ended');
            } else {
                reservationBtn.textContent = '예약 불가';
            }
        }
    }

    // 팝업 상태에 따른 예약 가능 여부 확인
    isReservationAvailable(status) {
        return status === 'PLANNED' || status === 'ONGOING';
    }

    // 상태 텍스트 변환 헬퍼 메서드
    getStatusText(status) {
        const statusMap = {
            'PLANNED': '예정',
            'ONGOING': '진행중',
            'ENDED': '종료'
        };
        return statusMap[status] || status;
    }

    // ===== 기존 렌더링 메서드들 =====

    // 북마크 상태 확인 함수 추가
    async checkBookmarkStatus() {
        try {
            const result = await apiService.checkBookmark(this.popupId);
            this.isBookmarked = result.bookmarked || false;
            this.updateBookmarkButton();
        } catch (error) {
            console.debug('북마크 상태 확인 실패:', error);
            this.isBookmarked = false; // 기본값으로 설정
            this.updateBookmarkButton();
        }
    }

    // 팝업 정보 렌더링
    renderPopupInfo() {
        if (!this.popupData) return;

        // 메인 이미지
        const mainImg = document.getElementById('popup-main-img');
        if (mainImg) {
            const defaultImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAwIiBoZWlnaHQ9IjMwMCIgdmlld0JveD0iMCAwIDYwMCAzMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSI2MDAiIGhlaWdodD0iMzAwIiBmaWxsPSIjNEI1QUU0Ii8+Cjx0ZXh0IHg9IjMwMCIgeT0iMTUwIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiIgZm9udC1zaXplPSI0OCIgZmlsbD0id2hpdGUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGRvbWluYW50LWJhc2VsaW5lPSJjZW50cmFsIj7wn46qPC90ZXh0Pgo8L3N2Zz4=';

            mainImg.src = this.popupData.mainImageUrl || defaultImage;
            mainImg.alt = this.popupData.title;
        }

        // 제목
        const titleEl = document.getElementById('popup-title');
        if (titleEl) {
            titleEl.textContent = this.popupData.title;
        }

        // 기간 정보
        this.renderScheduleInfo();

        // 태그와 상태 표시
        this.renderTagsAndStatus();
    }

    // 일정 정보 렌더링
    renderScheduleInfo() {
        // 기간 표시
        const periodEl = document.getElementById('popup-period');
        if (periodEl && this.popupData) {
            // 기간 텍스트 생성
            let periodText;
            if (this.popupData.periodText) {
                periodText = this.popupData.periodText;
            } else {
                const startDate = new Date(this.popupData.startDate).toLocaleDateString('ko-KR', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit'
                }).replace(/\. /g, '.').replace(/\.$/, '');

                const endDate = new Date(this.popupData.endDate).toLocaleDateString('ko-KR', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit'
                }).replace(/\. /g, '.').replace(/\.$/, '');

                periodText = `${startDate} - ${endDate}`;
            }

            // 상태 배지 생성
            const statusBadge = this.createInlineStatusBadge(this.popupData.status);

            // 기간과 상태 배지를 함께 표시
            periodEl.innerHTML = `
            <span class="period-text">${periodText}</span>
            ${statusBadge}
        `;
        }
    }

    // 인라인 상태 배지 생성 메서드
    createInlineStatusBadge(status) {
        const statusInfo = this.getStatusInfo(status);
        return `<span class="status-badge-inline ${statusInfo.className}">${statusInfo.text}</span>`;
    }

    // 상태 정보 반환 메서드
    getStatusInfo(status) {
        const statusMap = {
            'PLANNED': {
                text: '오픈예정',
                className: 'planned'
            },
            'ONGOING': {
                text: '진행중',
                className: 'ongoing'
            },
            'ENDED': {
                text: '종료',
                className: 'ended'
            }
        };

        return statusMap[status] || {
            text: status,
            className: 'ongoing'
        };
    }

    // 태그와 상태 표시 렌더링
    renderTagsAndStatus() {
        const tagsEl = document.getElementById('popup-tags');
        if (tagsEl) {
            tagsEl.innerHTML = '';

            // 기존 태그들 추가
            if (this.popupData.tags && this.popupData.tags.length > 0) {
                this.popupData.tags.forEach(tag => {
                    const span = document.createElement('span');
                    span.className = 'tag';
                    span.textContent = `#${tag}`;
                    span.addEventListener('click', () => searchByTag(String(tag)));
                    tagsEl.appendChild(span);
                });
            }
        }
    }

    // 운영시간 렌더링
    renderOperatingHours() {
        if (!this.popupData || !this.popupData.hours || this.popupData.hours.length === 0) {
            return;
        }

        const operatingHoursSection = document.getElementById('operating-hours-section');
        const hoursEl = document.getElementById('popup-hours');

        if (!operatingHoursSection || !hoursEl) return;

        // 운영시간 섹션 표시
        operatingHoursSection.style.display = 'block';

        const hours = this.popupData.hours;

        // 요일별 운영시간 상세 표시
        const detailedHours = hours.map(hour => {
            const dayText = hour.dayOfWeekText;
            const timeText = hour.timeRangeText.replace(' - ', ' - ');

            // 특별 운영시간이나 휴무일 체크
            let timeDisplay = timeText;
            if (hour.note) {
                timeDisplay += ` (${hour.note})`;
            }

            return `${dayText} : ${timeDisplay}`;
        }).join('\n');

        // 운영시간 내용 표시
        hoursEl.innerHTML = `
            <div class="operating-hours-details">${detailedHours.split('\n').map(line =>
            `<div class="hours-line">${line}</div>`
        ).join('')}</div>
        `;

        hoursEl.style.display = 'block';
    }

    // 위치 정보 렌더링 메서드
    renderLocationInfo() {
        if (!this.popupData) return;

        const hasLocation = this.popupData.latitude && this.popupData.longitude;
        const hasVenue = this.popupData.venueName || this.popupData.venueAddress;

        if (!hasLocation && !hasVenue) return;

        const locationSection = document.getElementById('location-section');
        if (locationSection) {
            locationSection.style.display = 'block';
        }

        const venueNameEl = document.getElementById('venue-name');
        if (venueNameEl) {
            venueNameEl.textContent = this.popupData.venueName || '장소 정보 없음';
        }

        const venueAddressEl = document.getElementById('venue-address');
        if (venueAddressEl) {
            venueAddressEl.textContent = this.popupData.venueAddress || '주소 정보 없음';
        }

        if (hasLocation) {
            setTimeout(() => {
                this.initializeLocationMap();
            }, 0);
        } else {
            const mapContainer = document.querySelector('.map-container');
            if (mapContainer) {
                mapContainer.style.display = 'none';
            }
        }
    }

    // 상세설명 렌더링
    renderDescriptionInfo() {
        if (!this.popupData) return;

        const hasDescription = this.popupData.summary || this.popupData.description;
        if (!hasDescription) return;

        const descriptionSection = document.getElementById('description-section');
        if (descriptionSection) {
            descriptionSection.style.display = 'block';
        }

        // 요약 정보
        const summaryEl = document.getElementById('popup-summary');
        if (summaryEl && this.popupData.summary) {
            summaryEl.innerHTML = `
                <p class="description-text">${this.escapeHtml(this.popupData.summary)}</p>
            `;
        }

        // 상세 설명
        const descriptionEl = document.getElementById('popup-description');
        if (descriptionEl && this.popupData.description) {
            descriptionEl.innerHTML = `
                <div class="description-text">${this.formatDescription(this.popupData.description)}</div>
            `;
        }
    }

    // 설명 텍스트 포맷팅
    formatDescription(description) {
        if (!description) return '';

        // 줄바꿈을 <br>로 변환하고 HTML 이스케이프
        return this.escapeHtml(description)
            .replace(/\n/g, '<br>')
            .replace(/\r\n/g, '<br>');
    }

    // ===== 이벤트 핸들러들 =====

    // 예약하기 처리
    handleReservation() {
        if (!this.popupData) {
            alert('팝업 정보를 불러오는 중입니다.');
            return;
        }

        // 버튼이 비활성화된 상태면 클릭 무시
        const reservationBtn = document.getElementById('reservation-btn');
        if (reservationBtn && reservationBtn.disabled) {
            return;
        }

        // 예약 가능 상태 확인
        if (!this.isReservationAvailable(this.popupData.status)) {
            alert('현재 예약할 수 없는 팝업입니다.');
            return;
        }

        // 외부 링크가 있으면 외부 링크로 이동, 없으면 내부 예약 페이지로 이동
        if (this.popupData.reservationLink) {
            window.open(this.popupData.reservationLink, '_blank');
        } else {
            // 내부 예약 페이지로 이동
            window.location.href = `/popup/${this.popupId}/reservation`;
        }
    }

// 북마크 처리
    async handleBookmark() {
        // 로그인 체크
        const token = apiService.getStoredToken();
        if (!token) {
            alert('로그인 후 이용 가능합니다.');
            return;
        }

        try {
            if (this.isBookmarked) {
                await apiService.removeBookmark(this.popupId);
                this.isBookmarked = false;
                this.updateBookmarkButton();
                alert('북마크가 해제되었습니다.');
            } else {
                await apiService.addBookmark(this.popupId);
                this.isBookmarked = true;
                this.updateBookmarkButton();
                alert('북마크에 추가되었습니다.');
            }
        } catch (error) {
            console.error('북마크 처리 실패:', error);
            alert('북마크 처리 중 오류가 발생했습니다.');
        }
    }

    // 북마크 버튼 업데이트
    updateBookmarkButton() {
        const bookmarkBtn = document.getElementById('bookmark-btn');
        if (bookmarkBtn) {
            const svg = bookmarkBtn.querySelector('svg');
            if (this.isBookmarked) {
                svg.setAttribute('fill', 'currentColor');
                bookmarkBtn.style.color = '#4B5AE4';
            } else {
                svg.setAttribute('fill', 'none');
                bookmarkBtn.style.color = '';
            }
        }
    }

    // 주소 복사
    async handleCopyAddress() {
        if (!this.popupData.venueAddress) {
            this.showToast('복사할 주소가 없습니다.');
            return;
        }

        const success = await apiService.copyToClipboard(this.popupData.venueAddress);

        if (success) {
            this.showToast('주소가 클립보드에 복사되었습니다.');

            const copyBtn = document.getElementById('copy-address-btn');
            if (copyBtn) {
                copyBtn.classList.add('copied');
                setTimeout(() => copyBtn.classList.remove('copied'), 2000);
            }
        } else {
            this.showToast('주소 복사에 실패했습니다.');
        }
    }

    // 리뷰 작성 처리
    async handleWriteReview() {
        // 사용자 로그인 체크
        const userId = await this.getOrFetchUserId();
        if (!userId) {
            alert('로그인이 필요한 서비스입니다.');
            window.location.href = '/login';
            return;
        }

        // 리뷰 작성 페이지로 이동
        window.location.href = `/reviews/popup/${this.popupId}/create`;
    }

    // 더 많은 리뷰 로드
    handleLoadMoreReviews() {
        // 전체 리뷰 목록 페이지로 이동
        window.location.href = `/reviews/popup/${this.popupId}`;
    }

    // ===== 지도 관련 메서드들 =====

    // 지도 초기화
    initializeLocationMap() {
        console.log('[지도 초기화] 시작');
        const startTime = performance.now();

        const mapContainer = document.getElementById('popup-location-map');
        if (!mapContainer) {
            console.error('[지도 초기화] 맵 컨테이너를 찾을 수 없음');
            return;
        }

        console.log('[지도 초기화] 맵 컨테이너 발견:', mapContainer);

        // 카카오맵 API 로드 확인
        if (typeof kakao === 'undefined') {
            console.error('[지도 초기화] kakao 객체가 정의되지 않음');
            this.handleMapLoadError(mapContainer, '카카오맵 스크립트가 로드되지 않았습니다.');
            return;
        }

        if (!kakao.maps) {
            console.error('[지도 초기화] kakao.maps 객체가 정의되지 않음');
            this.handleMapLoadError(mapContainer, '카카오맵 API가 제대로 로드되지 않았습니다.');
            return;
        }

        console.log('[지도 초기화] 카카오맵 API 로드 확인됨');
        console.log('[지도 초기화] 좌표:', this.popupData.latitude, this.popupData.longitude);

        try {
            // 지도 옵션 설정
            const lat = Number(this.popupData.latitude);
            const lng = Number(this.popupData.longitude);
            if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
                this.handleMapLoadError(mapContainer, '올바르지 않은 좌표값입니다.');
                return;
            }
            const mapOption = { center: new kakao.maps.LatLng(lat, lng), level: 3 };

            console.log('[지도 초기화] 지도 옵션:', mapOption);

            // 지도 생성 시작
            const mapCreateStart = performance.now();
            console.log('[지도 초기화] 지도 생성 시작');

            this.locationMap = new kakao.maps.Map(mapContainer, mapOption);

            const mapCreateEnd = performance.now();
            console.log(`[지도 초기화] 지도 생성 완료 (소요시간: ${mapCreateEnd - mapCreateStart}ms)`);

            // 마커 생성
            const markerCreateStart = performance.now();
            console.log('[지도 초기화] 마커 생성 시작');

            const marker = new kakao.maps.Marker({ position: new kakao.maps.LatLng(lat, lng) });

            marker.setMap(this.locationMap);

            const markerCreateEnd = performance.now();
            console.log(`[지도 초기화] 마커 생성 완료 (소요시간: ${markerCreateEnd - markerCreateStart}ms)`);

            const totalTime = performance.now() - startTime;
            console.log(`[지도 초기화] 전체 완료 (총 소요시간: ${totalTime}ms)`);

            // 성능 임계치 확인
            if (totalTime > 3000) {
                console.warn(`[지도 초기화] 성능 주의: ${totalTime}ms 소요됨 (권장: 3초 미만)`);
            }

        } catch (error) {
            console.error('[지도 초기화] 오류 발생:', error);
            this.handleMapLoadError(mapContainer, `지도 생성 중 오류: ${error.message}`);
        }
    }

    // 지도 로드 실패 처리
    handleMapLoadError(container, message) {
        console.warn('[지도 초기화] fallback:', message);
        if (container) container.style.display = 'none';
        this.showToast(message);
    }

    // ===== 유틸리티 메서드들 =====

    showToast(message) {
        let toast = document.getElementById('toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'toast';
            toast.className = 'toast';
            document.body.appendChild(toast);
        }
        toast.textContent = message;
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), 3000);
    }

    // 유사한 팝업 로드
    async loadSimilarPopups() {
        try {
            // 카테고리를 기반으로 유사한 팝업 검색 (최대 4개)
            const similarPopups = await apiService.getSimilarPopups(this.popupId, 0, 4);
            this.renderSimilarPopups(similarPopups.popups || similarPopups);
        } catch (error) {
            console.warn('유사한 팝업 로드 실패:', error);
            const gridEl = document.getElementById('similar-popups-grid');
            if (gridEl) {
                gridEl.innerHTML = '<p style="text-align: center; color: #6B7280; padding: 20px;">유사한 팝업을 불러올 수 없습니다.</p>';
            }
        }
    }

    // 유사한 팝업 렌더링
    renderSimilarPopups(popups) {
        const gridEl = document.getElementById('similar-popups-grid');
        if (!gridEl || !popups || popups.length === 0) {
            if (gridEl) {
                gridEl.innerHTML = '<p style="text-align: center; color: #6B7280; padding: 20px;">유사한 팝업이 없습니다.</p>';
            }
            return;
        }

        gridEl.innerHTML = popups.map(popup => {
            const title = this.escapeHtml(popup.title ?? '');
            const thumb = (popup.thumbnailUrl && /^https?:/i.test(popup.thumbnailUrl))
                ? popup.thumbnailUrl
                : 'https://via.placeholder.com/200x150/4B5AE4/ffffff?text=%F0%9F%8E%AA';
            return `
              <div class="similar-popup-card" data-id="${popup.id}">
                <img src="${thumb}" alt="${title}" class="similar-popup-image">
                <div class="similar-popup-info">
                  <h3 class="similar-popup-title">${title}</h3>
                  <p class="similar-popup-period">${this.formatDateRange(popup.startDate, popup.endDate)}</p>
                </div>
              </div>`;
        }).join('');
    }

    // 날짜 범위 포맷
    formatDateRange(startDate, endDate) {
        const start = new Date(startDate).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
        const end = new Date(endDate).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
        return `${start} ~ ${end}`;
    }

    // 공유 데이터 생성
    getShareData() {
        if (!this.popupData) {
            return {
                title: '팝업 스토어',
                description: 'POPIN에서 확인하세요!',
                url: window.location.href,
                hashtags: ['POPIN', '팝업스토어']
            };
        }

        const formatDate = (dateStr) => {
            const date = new Date(dateStr);
            return date.toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            }).replace(/\. /g, '.').replace(/\.$/, '');
        };

        const period = `${formatDate(this.popupData.startDate)}-${formatDate(this.popupData.endDate)}`;
        const sanitizeHashtag = (tag) => String(tag ?? '').replace(/^#/, '').replace(/\s+/g, '');
        const hashtags = ['POPIN', '팝업스토어', ...((this.popupData.tags || []).map(sanitizeHashtag))];

        return {
            title: this.popupData.title || '팝업 스토어',
            description: `✨ ${this.popupData.title} ✨\n📅 ${period}\n📍 ${this.popupData.venueAddress || ''}\n\nPOPIN에서 확인하세요!`,
            url: window.location.href,
            address: this.popupData.venueAddress || '',
            hashtags,
            image: this.getPopupImageUrl()
        };
    }

    // 메타 태그 업데이트
    updateMetaTags() {
        if (!this.popupData) return;

        // 기본 title 업데이트
        document.title = `${this.popupData.title} - POPIN`;

        // Open Graph 메타 태그 업데이트
        this.updateMetaTag('og:title', `${this.popupData.title} - POPIN`);
        this.updateMetaTag('og:description', this.createMetaDescription());
        this.updateMetaTag('og:image', this.getPopupImageUrl());
        this.updateMetaTag('og:url', window.location.href);

        // Twitter 카드 업데이트
        this.updateMetaTag('twitter:title', `${this.popupData.title} - POPIN`);
        this.updateMetaTag('twitter:description', this.createMetaDescription());
        this.updateMetaTag('twitter:image', this.getPopupImageUrl());

        console.log('메타 태그 업데이트 완료');
    }

    // 메타 태그 업데이트 헬퍼
    updateMetaTag(property, content) {
        // property 속성으로 찾기 (og:* 태그용)
        let meta = document.querySelector(`meta[property="${property}"]`);

        // name 속성으로 찾기 (twitter:* 태그용)
        if (!meta) {
            meta = document.querySelector(`meta[name="${property}"]`);
        }

        if (meta) {
            meta.setAttribute('content', content);
        } else {
            // 메타 태그가 없으면 생성
            meta = document.createElement('meta');
            if (property.startsWith('og:')) {
                meta.setAttribute('property', property);
            } else {
                meta.setAttribute('name', property);
            }
            meta.setAttribute('content', content);
            document.head.appendChild(meta);
        }
    }

    // 메타 설명 생성
    createMetaDescription() {
        if (!this.popupData) return 'POPIN에서 다양한 팝업스토어 정보를 확인하세요!';

        let description = '';

        // 요약이 있으면 요약 사용
        if (this.popupData.summary && this.popupData.summary.trim()) {
            description = this.popupData.summary.trim();
        } else {
            // 요약이 없으면 기본 정보로 구성
            description = `${this.popupData.title}`;

            // 기간 정보 추가
            const period = this.popupData.periodText || this.createPeriodText();
            if (period && period !== '기간 미정') {
                description += ` | ${period}`;
            }

            // 장소 정보 추가
            const address = this.popupData.venueAddress;
            if (address && address.trim()) {
                // 주소가 너무 길면 첫 부분만 사용
                const shortAddress = address.length > 30 ? address.substring(0, 30) + '...' : address;
                description += ` | ${shortAddress}`;
            }
        }

        description += ' | POPIN에서 확인하세요!';

        // 메타 설명은 160자 이하로 제한
        if (description.length > 160) {
            description = description.substring(0, 157) + '...';
        }

        return description;
    }

    // 기간 텍스트 생성 (popupData에 periodText가 없는 경우)
    createPeriodText() {
        if (!this.popupData.startDate && !this.popupData.endDate) {
            return '기간 미정';
        }

        const formatDate = (dateStr) => {
            const date = new Date(dateStr);
            return date.toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            }).replace(/\. /g, '.').replace(/\.$/, '');
        };

        if (this.popupData.startDate && this.popupData.endDate) {
            if (this.popupData.startDate === this.popupData.endDate) {
                return formatDate(this.popupData.startDate);
            }
            return `${formatDate(this.popupData.startDate)} - ${formatDate(this.popupData.endDate)}`;
        } else if (this.popupData.startDate) {
            return `${formatDate(this.popupData.startDate)} -`;
        } else {
            return `- ${formatDate(this.popupData.endDate)}`;
        }
    }

    // 팝업 이미지 URL 가져오기
    getPopupImageUrl() {
        if (this.popupData.mainImageUrl && this.popupData.mainImageUrl.trim()) {
            // 절대 URL인지 확인
            if (this.popupData.mainImageUrl.startsWith('http')) {
                return this.popupData.mainImageUrl;
            } else {
                // 상대 URL인 경우 절대 URL로 변환
                return window.location.origin + this.popupData.mainImageUrl;
            }
        }

        // 기본 이미지
        return window.location.origin + '/images/default-popup.png';
    }

    // 사용자 ID 확보
    async getOrFetchUserId() {
        try {
            const cached = localStorage.getItem('userId') || sessionStorage.getItem('userId');
            const parsed = cached ? parseInt(cached, 10) : NaN;
            if (!Number.isNaN(parsed)) return parsed;
        } catch (e) {
            console.warn('userId 캐시 확인 실패:', e);
        }

        if (!apiService.getStoredToken()) return null;
        try {
            const userInfo = await apiService.getCurrentUser();
            if (userInfo && userInfo.id) {
                try { localStorage.setItem('userId', String(userInfo.id)); }
                catch { sessionStorage.setItem('userId', String(userInfo.id)); }
                return userInfo.id;
            }
        } catch (e) {
            console.warn('사용자 정보 가져오기 실패:', e);
        }

        return null;
    }

    // 공유 모달 표시
    showShareModal() {
        if (this.shareModal) {
            this.shareModal.show();
        }
    }

    // HTML 이스케이프
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = String(text ?? '');
        return div.innerHTML;
    }

    // ===== 화면 상태 관리 =====

    // 로딩 표시
    showLoading() {
        document.getElementById('popup-detail-loading').style.display = 'flex';
        document.getElementById('popup-detail-content').style.display = 'none';
        if (document.getElementById('popup-detail-error')) {
            document.getElementById('popup-detail-error').style.display = 'none';
        }
    }

    // 콘텐츠 표시
    showContent() {
        document.getElementById('popup-detail-loading').style.display = 'none';
        document.getElementById('popup-detail-content').style.display = 'block';
        if (document.getElementById('popup-detail-error')) {
            document.getElementById('popup-detail-error').style.display = 'none';
        }

        if (this.locationMap) {
            this.locationMap.relayout();

            const correctPosition = new kakao.maps.LatLng(this.popupData.latitude, this.popupData.longitude);
            this.locationMap.setCenter(correctPosition);
        }
    }

    // 에러 표시
    showError() {
        document.getElementById('popup-detail-loading').style.display = 'none';
        document.getElementById('popup-detail-content').style.display = 'none';
        if (document.getElementById('popup-detail-error')) {
            document.getElementById('popup-detail-error').style.display = 'flex';
        }
    }

    // 드래그 스크롤 기능 초기화
    initializeDragScroll(element) {
        let isDown = false;
        let startX;
        let scrollLeft;

        element.addEventListener('mousedown', (e) => {
            isDown = true;
            element.classList.add('active-drag');
            startX = e.pageX - element.offsetLeft;
            scrollLeft = element.scrollLeft;
        });

        element.addEventListener('mouseleave', () => {
            isDown = false;
            element.classList.remove('active-drag');
        });

        element.addEventListener('mouseup', () => {
            isDown = false;
            element.classList.remove('active-drag');
        });

        element.addEventListener('mousemove', (e) => {
            if (!isDown) return;
            e.preventDefault();
            const x = e.pageX - element.offsetLeft;
            const walk = (x - startX) * 2; // 스크롤 속도 조절
            element.scrollLeft = scrollLeft - walk;
        });

        // 터치 이벤트 (모바일)
        element.addEventListener('touchstart', (e) => {
            startX = e.touches[0].pageX - element.offsetLeft;
            scrollLeft = element.scrollLeft;
        });

        element.addEventListener('touchmove', (e) => {
            if (!startX) return;
            const x = e.touches[0].pageX - element.offsetLeft;
            const walk = (x - startX) * 2;
            element.scrollLeft = scrollLeft - walk;
        });
    }

    // 컴포넌트 정리
    cleanup() {
        if (this.reviewManager) {
            this.reviewManager.cleanup();
        }
    }
}

// 리뷰 관리 클래스
class ReviewManager {
    constructor(popupId) {
        this.popupId = popupId;
        this.currentRating = 0;
        this.currentPage = 0;
        this.hasMore = true;
    }

    // 초기화
    async initialize() {
        this.setupEventListeners();
        await this.loadReviewStats();
        await this.loadRecentReviews();
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
    }

    // 리뷰 통계 로드
    async loadReviewStats() {
        try {
            const response = await fetch(`/api/reviews/popup/${this.popupId}/stats`);
            if (!response.ok) throw new Error('Failed to load review stats');

            const stats = await response.json();
            this.renderReviewStats(stats);
        } catch (error) {
            console.error('리뷰 통계 로드 실패:', error);
            // 기본값으로 설정
            this.renderReviewStats({ averageRating: 0, totalReviews: 0 });
        } finally {
            // 로딩 스피너 강제 숨김
            this.hideStatsLoading();
        }
    }

    // 최근 리뷰 로드 (최대 2개)
    async loadRecentReviews() {
        try {
            const response = await fetch(`/api/reviews/popup/${this.popupId}/recent?limit=2`);
            if (!response.ok) throw new Error('Failed to load reviews');

            const reviews = await response.json();
            this.renderRecentReviews(reviews);

            // 더보기 버튼 표시 여부 결정
            const statsResponse = await fetch(`/api/reviews/popup/${this.popupId}/stats`);
            if (statsResponse.ok) {
                const stats = await statsResponse.json();
                const loadMoreBtn = document.getElementById('loadMoreBtn') || document.querySelector('.load-more-btn');
                if (loadMoreBtn && stats.totalReviews > 2) {
                    loadMoreBtn.style.display = 'block';
                }
            }
        } catch (error) {
            console.error('리뷰 로드 실패:', error);
            this.renderNoReviews();
        } finally {
            // 로딩 스피너 강제 숨김
            this.hideReviewsLoading();
        }
    }

    hideStatsLoading() {
        const statsLoading = document.querySelector('.stats-loading');
        if (statsLoading) {
            statsLoading.style.display = 'none';
        }
    }

    hideReviewsLoading() {
        const reviewsLoading = document.getElementById('reviewsLoading') || document.querySelector('.reviews-loading');
        if (reviewsLoading) {
            reviewsLoading.style.display = 'none';
        }
    }

    // 리뷰 통계 렌더링
    renderReviewStats(stats) {
        const statsContainer = document.getElementById('reviewStats');
        if (!statsContainer) {
            console.warn('reviewStats 요소를 찾을 수 없습니다.');
            return;
        }

        // 로딩 숨김
        const loadingEl = statsContainer.querySelector('.stats-loading');
        if (loadingEl) {
            loadingEl.style.display = 'none';
        }

        // 통계 HTML 생성
        const rating = stats.averageRating || 0;
        const count = stats.totalReviews || 0;

        statsContainer.innerHTML = `
        <div class="rating-display">
            <div class="stars">
                ${this.renderStars(rating)}
            </div>
            <span class="rating-text">${rating.toFixed(1)}</span>
        </div>
        <span class="review-count">(${count})</span>
    `;
    }

    // 최근 리뷰 렌더링
    renderRecentReviews(reviews) {
        const listEl = document.getElementById('reviewsList') || document.querySelector('.reviews-list');
        const loadingEl = document.getElementById('reviewsLoading') || document.querySelector('.loading-spinner');

        if (loadingEl) {
            loadingEl.style.display = 'none';
        }

        if (!reviews || reviews.length === 0) {
            this.renderNoReviews();
            return;
        }

        if (listEl) {
            listEl.innerHTML = reviews.map(review => this.renderReviewItem(review)).join('');
        }
    }

    // 리뷰 아이템 렌더링
    renderReviewItem(review) {
        const createdDate = new Date(review.createdAt).toLocaleDateString('ko-KR');

        return `
            <div class="review-item">
                <div class="review-header">
                    <div class="review-stars">
                        ${this.renderStars(review.rating)}
                    </div>
                    <span class="review-date">${createdDate}</span>
                </div>
                <p class="review-content">${this.escapeHtml(review.content)}</p>
                <div class="reviewer-info">
                    <span class="reviewer-name">${this.escapeHtml(review.userName || '익명')}</span>
                </div>
            </div>
        `;
    }

    // 별점 렌더링
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

    // 리뷰 없을 때 렌더링
    renderNoReviews() {
        const listEl = document.getElementById('reviewsList') || document.querySelector('.reviews-list');
        if (listEl) {
            listEl.innerHTML = `
                <div class="no-reviews">
                    <div class="no-reviews-icon">📝</div>
                    <p>아직 작성된 리뷰가 없습니다.<br>첫 번째 리뷰를 작성해보세요!</p>
                </div>
            `;
        }
    }

    // HTML 이스케이프
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = String(text ?? '');
        return div.innerHTML;
    }

    // 정리
    cleanup() {
    }
}

// 태그로 검색하는 함수
function searchByTag(tag) {
    console.log(`"${tag}" 태그로 검색`);

    // 태그에서 # 제거
    const cleanTag = tag.startsWith('#') ? tag.substring(1) : tag;

    window.location.href = `/popup/search?query=${encodeURIComponent(cleanTag)}`;
}

// 공유 모달 클래스
class ShareModal {
    constructor(getShareDataCallback) {
        this.getShareData = getShareDataCallback;
        this.modal = null;
        this.initialize();
    }

    initialize() {
        this.createModalHTML();
        this.setupEventListeners();
    }

    createModalHTML() {
        // 이미 모달이 존재하면 제거
        const existingModal = document.getElementById('share-modal-overlay');
        if (existingModal) {
            existingModal.remove();
        }

        const modalHTML = `
            <div id="share-modal-overlay" class="share-modal-overlay">
                <div class="share-modal">
                    <div class="share-modal-header">
                        <h3 class="share-modal-title">공유하기</h3>
                        <button class="share-modal-close" id="share-modal-close">×</button>
                    </div>

                    <div class="share-options">
                        <button class="share-option" data-share-type="kakaotalk">
                            <div class="share-option-icon kakaotalk">
                                <img src="/images/icon_kakotalk.png" alt="카카오톡">
                            </div>
                            <p class="share-option-label">카카오톡</p>
                        </button>
            
                        <button class="share-option" data-share-type="twitter">
                            <div class="share-option-icon twitter">
                                <img src="/images/icon-x.png" alt="X">
                            </div>
                            <p class="share-option-label">X</p>
                        </button>
            
                        <button class="share-option" data-share-type="url">
                            <div class="share-option-icon url">🔗</div>
                            <p class="share-option-label">URL 복사</p>
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);
        this.modal = document.getElementById('share-modal-overlay');
    }

    setupEventListeners() {
        // 모달 닫기 버튼
        document.getElementById('share-modal-close').addEventListener('click', () => {
            this.hide();
        });

        // 배경 클릭 시 닫기
        this.modal.addEventListener('click', (e) => {
            if (e.target === this.modal) {
                this.hide();
            }
        });

        // 공유 옵션 클릭
        document.querySelectorAll('.share-option').forEach(option => {
            option.addEventListener('click', () => {
                const shareType = option.dataset.shareType;
                this.handleShare(shareType);
            });
        });
    }

    show() {
        this.modal.classList.add('show');
        document.body.style.overflow = 'hidden';
    }

    hide() {
        this.modal.classList.remove('show');
        document.body.style.overflow = '';
    }

    async handleShare(shareType) {
        const shareData = this.getShareData();

        try {
            switch (shareType) {
                case 'kakaotalk':
                    this.shareToKakaoTalk(shareData);
                    break;
                case 'twitter':
                    this.shareToTwitter(shareData);
                    break;
                case 'url':
                    await this.copyUrl(shareData);
                    break;
            }
        } catch (error) {
            console.error(`${shareType} 공유 실패:`, error);
            this.showToast('공유 중 오류가 발생했습니다.');
        }

        this.hide();
    }

    shareToKakaoTalk(data) {
        if (typeof Kakao !== 'undefined' && Kakao.Share) {
            Kakao.Share.sendDefault({
                objectType: 'location',
                address: data.address || '',
                addressTitle: data.title,
                content: {
                    title: data.title,
                    description: data.description,
                    imageUrl: data.image,
                    link: {
                        mobileWebUrl: data.url,
                        webUrl: data.url
                    }
                },
                buttons: [{
                    title: '자세히 보기',
                    link: {
                        mobileWebUrl: data.url,
                        webUrl: data.url
                    }
                }]
            });
        } else {
            const kakaoUrl = `https://sharer.kakao.com/talk/friends/picker/link?url=${encodeURIComponent(data.url)}&text=${encodeURIComponent(data.description)}`;
            window.open(kakaoUrl, '_blank', 'width=500,height=600');
        }
    }

    shareToTwitter(data) {
        const twitterText = `${data.title}\n\n${data.description}\n\n${data.hashtags.map(tag => `#${tag}`).join(' ')}`;
        const twitterUrl = `https://twitter.com/intent/tweet?text=${encodeURIComponent(twitterText)}&url=${encodeURIComponent(data.url)}`;
        window.open(twitterUrl, '_blank', 'width=550,height=420');
    }

    async copyUrl(data) {
        try {
            await this.copyToClipboard(data.url);
            this.showToast('링크가 클립보드에 복사되었습니다.');
        } catch (error) {
            console.error('URL 복사 실패:', error);
            this.showToast('링크 복사에 실패했습니다.');
        }
    }

    async copyToClipboard(text) {
        if (navigator.clipboard) {
            await navigator.clipboard.writeText(text);
        } else {
            const textarea = document.createElement('textarea');
            textarea.value = text;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
        }
    }

    showToast(message) {
        // 기존 showToast 메서드 활용하거나 새로 생성
        let toast = document.getElementById('share-toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'share-toast';
            toast.className = 'share-toast';
            document.body.appendChild(toast);
        }

        toast.textContent = message;
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 3000);
    }

    isMobile() {
        return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    }
}

// 전역 등록
window.PopupDetailManager = PopupDetailManager;
window.ReviewManager = ReviewManager;
window.searchByTag = searchByTag;