// 간단한 API 통신 모듈
class SimpleApiService {
    constructor() {
        this.baseURL = '/api';
        this.token = this.getStoredToken();
    }

    // 로컬 스토리지에서 토큰 가져오기
    getStoredToken() {
        try {
            const raw =
                localStorage.getItem('accessToken') ||
                localStorage.getItem('authToken')   ||
                sessionStorage.getItem('accessToken') ||
                sessionStorage.getItem('authToken');
            return (raw || '').trim();
        } catch {
            return null;
        }
    }

    // 토큰 저장
    storeToken(token) {
        const clean = String(token || '').trim();
        try {
            localStorage.setItem('accessToken', clean);
            localStorage.setItem('authToken', clean);
        } catch {
            sessionStorage.setItem('accessToken', clean);
            sessionStorage.setItem('authToken', clean);
        }

        // 쿠키에도 토큰 저장
        document.cookie = `jwtToken=${clean}; path=/; max-age=86400; SameSite=Lax`;

        this.token = clean;
    }

    // 토큰 제거
    removeToken() {
        try {
            localStorage.removeItem('authToken');
            localStorage.removeItem('accessToken');
            sessionStorage.removeItem('authToken');
            sessionStorage.removeItem('accessToken');
        } catch (error) {
            console.warn('토큰 제거 실패');
        }

        // 쿠키에서도 토큰 제거
        document.cookie = 'jwtToken=; path=/; max-age=0';

        this.token = null;
    }

    // 공통 헤더 설정
    getHeaders() {
        const headers = {
            'Content-Type': 'application/json',
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        return headers;
    }

    // GET 요청
    async get(endpoint) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'GET',
                headers: this.getHeaders(),
                credentials: 'include'
            });

            if (response.status === 401) {
                this.removeToken();
                throw new Error('인증이 필요합니다.');
            }

            if (!response.ok) {
                if (options.silent) {
                    throw new Error(`HTTP ${response.status}`);
                }

                sessionStorage.setItem("errorCode", response.status);
                window.location.href = '/error/error.html';
            }

            return await response.json();
        } catch (error) {
            console.error('API GET Error:', error);

            if (!options.silent) {
                window.location.href = '/error/error.html';
            }

            throw error;
        }
    }

    // POST 요청
    async post(endpoint, data) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify(data),
                credentials: 'include'
            });

            if (response.status === 401) {
                this.removeToken();
                throw new Error('인증이 필요합니다.');
            }

            if (!response.ok) {
                sessionStorage.setItem("errorCode", response.status);
                window.location.href = '/error/error.html';
            }

            return await response.json();
        } catch (error) {
            console.error('API POST Error:', error);
            window.location.href = '/error/error.html';
            throw error;
        }
    }

    // PUT 요청
    async put(endpoint, data) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'PUT',
                headers: this.getHeaders(),
                body: JSON.stringify(data),
                credentials: 'include'
            });

            if (response.status === 401) {
                this.removeToken();
                throw new Error('인증이 필요합니다.');
            }

            if (!response.ok) {
                sessionStorage.setItem("errorCode", response.status);
                window.location.href = '/error/error.html';
            }

            // 응답이 비어있을 수 있음
            const text = await response.text();
            return text ? JSON.parse(text) : true;
        } catch (error) {
            console.error('API PUT Error:', error);
            window.location.href = '/error/error.html';
            throw error;
        }
    }

    // DELETE 요청
    async delete(endpoint) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'DELETE',
                headers: this.getHeaders(),
                credentials: 'include'
            });

            if (response.status === 401) {
                this.removeToken();
                throw new Error('인증이 필요합니다.');
            }

            if (!response.ok) {
                sessionStorage.setItem("errorCode", response.status);
                window.location.href = '/error/error.html';
            }

            // 보통 빈 응답이지만, 서버가 JSON을 주면 파싱
            const ct = response.headers.get('content-type') || '';
            return ct.includes('application/json') ? await response.json() : true;
        } catch (err) {
            console.error('API DELETE Error:', err);
            window.location.href = '/error/error.html';
            throw err;
        }
    }

    // PATCH 요청
    async patch(endpoint, data) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'PATCH',
                headers: this.getHeaders(),
                body: JSON.stringify(data),
                credentials: 'include'
            });

            if (response.status === 401) {
                this.removeToken();
                throw new Error('인증이 필요합니다.');
            }

            if (!response.ok) {
                sessionStorage.setItem("errorCode", response.status);
                window.location.href = '/error/error.html';
            }

            // 응답이 비어있을 수 있음
            const text = await response.text();
            return text ? JSON.parse(text) : true;
        } catch (error) {
            console.error('API PATCH Error:', error);
            window.location.href = '/error/error.html';
            throw error;
        }
    }

    // 현재 사용자 정보
    async getCurrentUser() {
        return await this.get('/users/me');
    }

    // 메인 페이지 데이터
    async getMainData() {
        return await this.get('/main');
    }
}

// 전역 API 서비스 인스턴스
const apiService = new SimpleApiService();

// === 공간대여 API ===

// 공간 목록 조회
apiService.listSpaces = async function(params = {}) {
    const sp = new URLSearchParams(params);
    const query = sp.toString() ? `?${sp.toString()}` : '';
    return await this.get(`/spaces${query}`);
};

// 내 공간 목록 조회
apiService.getMySpaces = async function() {
    return await this.get('/spaces/mine');
};

// 공간 상세 조회
apiService.getSpace = async function(spaceId) {
    return await this.get(`/spaces/${encodeURIComponent(spaceId)}`);
};

// 공간 등록
apiService.createSpace = async function(formData) {
    try {
        const response = await fetch(`${this.baseURL}/spaces`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${this.getStoredToken()}`
            },
            body: formData,
            credentials: 'include'
        });

        if (response.status === 401) {
            this.removeToken();
            throw new Error('인증이 필요합니다.');
        }

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('공간 등록 API Error:', error);
        throw error;
    }
};

// 공간 수정
apiService.updateSpace = async function(spaceId, formData) {
    try {
        const response = await fetch(`${this.baseURL}/spaces/${encodeURIComponent(spaceId)}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${this.getStoredToken()}`
            },
            body: formData,
            credentials: 'include'
        });

        if (response.status === 401) {
            this.removeToken();
            throw new Error('401');
        }
        if (!response.ok) {
            throw new Error(`${response.status}`);
        }

        const text = await response.text();
        if (!text || text.trim().length === 0) {
            return true; // 빈 응답도 성공으로 처리
        }
        try {
            return JSON.parse(text);
        } catch {
            return text; // JSON 아니면 그냥 텍스트 반환
        }
    } catch (error) {
        console.error('공간 수정 API Error:', error);
        throw error;
    }
};

// 공간 삭제
apiService.deleteSpace = async function(spaceId) {
    return await this.delete(`/spaces/${encodeURIComponent(spaceId)}`);
};

// 문의하기
apiService.inquireSpace = async function(spaceId) {
    return await this.post(`/spaces/${encodeURIComponent(spaceId)}/inquiries`, {});
};

// 신고하기
apiService.reportSpace = async function(spaceId) {
    return await this.post(`/spaces/${encodeURIComponent(spaceId)}/reports`, {});
};

// === 공간 예약 관련 (Provider 전용) ===

// 내 공간에 신청된 예약 목록 조회
apiService.getMyReservations = async function() {
    return await this.get('/space-reservations/my-spaces');
};

// 예약 상세 조회
apiService.getReservationDetail = async function(reservationId) {
    return await this.get(`/space-reservations/${encodeURIComponent(reservationId)}`);
};

// 예약 승인
apiService.acceptReservation = async function(reservationId) {
    return await this.put(`/space-reservations/${encodeURIComponent(reservationId)}/accept`, {});
};

// 예약 거절
apiService.rejectReservation = async function(reservationId) {
    return await this.put(`/space-reservations/${encodeURIComponent(reservationId)}/reject`, {});
};

// 예약 삭제
apiService.deleteReservation = async function(reservationId) {
    return await this.delete(`/space-reservations/${encodeURIComponent(reservationId)}/delete`);
};

// 예약 현황 통계
apiService.getReservationStats = async function() {
    try {
        const reservations = await this.getMyReservations();

        const stats = {
            pendingCount: 0,
            acceptedCount: 0,
            rejectedCount: 0,
            cancelledCount: 0,
            totalReservations: reservations.length
        };

        reservations.forEach(reservation => {
            switch(reservation.status) {
                case 'PENDING':
                    stats.pendingCount++;
                    break;
                case 'ACCEPTED':
                    stats.acceptedCount++;
                    break;
                case 'REJECTED':
                    stats.rejectedCount++;
                    break;
                case 'CANCELLED':
                    stats.cancelledCount++;
                    break;
            }
        });

        return stats;
    } catch (error) {
        console.error('통계 계산 오류:', error);
        return {
            pendingCount: 0,
            acceptedCount: 0,
            rejectedCount: 0,
            cancelledCount: 0,
            totalReservations: 0
        };
    }
};

// === 팝업 관련 API (수정됨) ===

// 헬퍼 함수
const createQueryString = (params) => {
    // null이나 undefined 값을 제외하고 URLSearchParams 생성
    const filteredParams = Object.entries(params).reduce((acc, [key, value]) => {
        if (value !== null && value !== undefined) {
            acc[key] = value;
        }
        return acc;
    }, {});

    const sp = new URLSearchParams(filteredParams);
    return sp.toString() ? `?${sp.toString()}` : '';
};

// 전체 팝업 목록 조회 (최신순)
apiService.getPopups = async function(params = {}) {
    const query = createQueryString(params);
    return await this.get(`/popups${query}`);
};

// 인기 팝업 조회
apiService.getPopularPopups = async function(params = {}) {
    const query = createQueryString(params);
    return await this.get(`/popups/popular${query}`);
};

// 마감임박 팝업 조회
apiService.getDeadlineSoonPopups = async function(params = {}) {
    const query = createQueryString(params);
    return await this.get(`/popups/deadline${query}`);
};

// 지역/날짜별 팝업 조회
apiService.getPopupsByRegionAndDate = async function(params = {}) {
    const query = createQueryString(params);
    return await this.get(`/popups/region-date${query}`);
};

// 팝업 상세 조회
apiService.getPopup = async function(popupId) {
    return await this.get(`/popups/${encodeURIComponent(popupId)}`);
};

// AI 추천 팝업 조회
apiService.getAIRecommendedPopups = async function(params = {}) {
    const query = createQueryString(params);
    return await this.get(`/popups/ai-recommended${query}`);
};

// 팝업 검색
apiService.searchPopups = async function(params = {}) {
    const sp = new URLSearchParams(params);
    const query = sp.toString() ? `?${sp.toString()}` : '';
    return await this.get(`/search/popups${query}`);
};

// 자동완성 제안 조회
apiService.getAutocompleteSuggestions = async function(query) {
    const q = String(query || '').trim();
    if (!q) {
        return { suggestions: [], query: '', totalCount: 0 };
    }

    try {
        const resp = await fetch(`${this.baseURL}/search/suggestions?q=${encodeURIComponent(q)}`, {
            method: 'GET',
            headers: this.getHeaders(),
            credentials: 'include'
        });

        if (resp.status === 401) {
            this.removeToken();
            return { suggestions: [], query: q, totalCount: 0 };
        }

        if (!resp.ok) {
            return { suggestions: [], query: q, totalCount: 0 };
        }

        const data = await resp.json();
        const suggestions = Array.isArray(data?.suggestions) ? data.suggestions : [];
        const totalCount = Number.isFinite(data?.totalCount) ? data.totalCount : suggestions.length;

        return { suggestions, query: q, totalCount };
    } catch (error) {
        console.warn('자동완성 제안 조회 실패:', error);
        return { suggestions: [], query: q, totalCount: 0 };
    }
};

// 지역 목록 조회
apiService.getMapRegions = async function() {
    return await this.get('/map/regions');
};

// 좌표 기반 지역 목록 조회
apiService.getMapRegionsWithCoordinates = async function() {
    return await this.get('/map/regions/coordinates');
};

// 지도용 팝업 목록 조회
apiService.getMapPopups = async function(params = {}) {
    const sp = new URLSearchParams(params);
    const query = sp.toString() ? `?${sp.toString()}` : '';
    return await this.get(`/map/popups${query}`);
};

// 위치 기반 유틸리티 메서드
apiService.getCurrentPosition = function() {
    return new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
            reject(new Error('Geolocation이 지원되지 않습니다.'));
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                resolve({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude
                });
            },
            (error) => {
                reject(new Error('위치 정보를 가져올 수 없습니다.'));
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 300000 // 5분
            }
        );
    });
};

// 특정 범위 내 팝업 조회
apiService.getPopupsInBounds = async function(southWestLat, southWestLng, northEastLat, northEastLng) {
    const params = new URLSearchParams({
        southWestLat,
        southWestLng,
        northEastLat,
        northEastLng
    });
    return await this.get(`/map/popups/bounds?${params}`);
};

// 주변 팝업 조회
apiService.getNearbyPopups = async function(lat, lng, radiusKm = 10) {
    const params = new URLSearchParams({
        lat,
        lng,
        radiusKm
    });
    return await this.get(`/map/popups/nearby?${params}`);
};

// 카테고리별 지도 팝업 통계
apiService.getMapPopupStatsByCategory = async function(region = null) {
    const params = region ? `?region=${encodeURIComponent(region)}` : '';
    return await this.get(`/map/popups/stats/category${params}`);
};

// 지역별 지도 팝업 통계
apiService.getMapPopupStatsByRegion = async function() {
    return await this.get('/map/popups/stats/region');
};

// 지도 팝업 검색
apiService.searchMapPopups = async function(params = {}) {
    const sp = new URLSearchParams(params);
    const query = sp.toString() ? `?${sp.toString()}` : '';
    return await this.get(`/map/popups/search${query}`);
};

// 유사한 팝업 조회
apiService.getSimilarPopups = async function(popupId, page = 0, size = 4) {
    try {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString()
        });
        return await this.get(`/popups/${encodeURIComponent(popupId)}/similar?${params}`);
    } catch (error) {
        console.error('유사한 팝업 조회 실패:', error);
        // 유사한 팝업이 없어도 에러가 아님
        return {
            popups: [],
            totalElements: 0,
            totalPages: 0,
            currentPage: 0,
            empty: true
        };
    }
};

// 카테고리별 팝업 조회 메서드 추가
apiService.getPopupsByCategory = async function(categoryName, page = 0, size = 20) {
    try {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString()
        });
        return await this.get(`/popups/category/${encodeURIComponent(categoryName)}?${params}`);
    } catch (error) {
        console.error('카테고리별 팝업 조회 실패:', error);
        return {
            popups: [],
            totalElements: 0,
            totalPages: 0,
            currentPage: 0,
            empty: true
        };
    }
};

// 지역별 팝업 조회 메서드 추가
apiService.getPopupsByRegion = async function(region) {
    try {
        return await this.get(`/popups/region/${encodeURIComponent(region)}`);
    } catch (error) {
        console.error('지역별 팝업 조회 실패:', error);
        return [];
    }
};

// 주소 복사 유틸리티 메서드 추가
apiService.copyToClipboard = async function(text) {
    try {
        if (navigator.clipboard && window.isSecureContext) {
            // Clipboard API 사용 (HTTPS 환경)
            await navigator.clipboard.writeText(text);
            return true;
        } else {
            // 폴백: 구형 브라우저나 HTTP 환경
            const textArea = document.createElement('textarea');
            textArea.value = text;
            textArea.style.position = 'fixed';
            textArea.style.left = '-999999px';
            textArea.style.top = '-999999px';
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();
            const success = document.execCommand('copy');
            document.body.removeChild(textArea);
            return success;
        }
    } catch (error) {
        console.error('클립보드 복사 실패:', error);
        return false;
    }
};

// === 팝업 제보 API ===

// 제보 생성
apiService.createPopupReport = async function(formData) {
    try {
        const response = await fetch(`${this.baseURL}/popups/reports`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${this.getStoredToken()}`
            },
            body: formData,
            credentials: 'include'
        });

        if (response.status === 401) {
            this.removeToken();
            throw new Error('인증이 필요합니다.');
        }

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('팝업 제보 API Error:', error);
        throw error;
    }
};

// 제보 단건 조회
apiService.getPopupReport = async function(reportId) {
    return await this.get(`/popups/reports/${encodeURIComponent(reportId)}`);
};

// 상태별 제보 목록 (관리자)
apiService.listPopupReportsByStatus = async function(status, page = 0, size = 20) {
    const params = new URLSearchParams({ status, page, size });
    return await this.get(`/popups/reports?${params}`);
};

// 내 제보 목록
apiService.getMyPopupReports = async function(page = 0, size = 20) {
    const params = new URLSearchParams({ page, size });
    return await this.get(`/popups/reports/me?${params}`);
};

// 제보 승인 (관리자)
apiService.approvePopupReport = async function(reportId) {
    return await this.put(`/popups/reports/${encodeURIComponent(reportId)}/approve`, {});
};

// 제보 반려 (관리자)
apiService.rejectPopupReport = async function(reportId) {
    return await this.put(`/popups/reports/${encodeURIComponent(reportId)}/reject`, {});
};

// === 알림 관련 API ===
apiService.getNotifications = async function() {
    return await this.get('/notifications/me');
};

apiService.markNotificationAsRead = async function(notificationId) {
    return await this.post(`/notifications/${encodeURIComponent(notificationId)}/read`);
};

apiService.createReservationNotification = async function(userId) {
    return await this.post('/notifications/reservation', null, {
        params: { userId }
    });
};

apiService.markNotificationRead = async function(id) {
    return await this.post(`/notifications/${id}/read`);
};

// === 마이페이지 USER api ===
apiService.getNotificationSettings = async function() {
    return await this.get("/notifications/settings/me");
};

apiService.updateNotificationSetting = async function(type, enabled) {
    return await this.patch(`/notifications/settings/${type}?enabled=${enabled}`);
};

// === 마이페이지 HOST api ===
// 팝업 등록
apiService.createPopup = async function(data) {
    return await this.post('/hosts/popups', data);
};

// 내 팝업 목록 조회
apiService.getMyPopups = async function() {
    return await this.get('/hosts/popups');
};

// 내 팝업 상세 조회
apiService.getMyPopupDetail = async function(popupId) {
    return await this.get(`/hosts/popups/${encodeURIComponent(popupId)}`);
};

// 팝업 수정
apiService.updatePopup = async function(popupId, data) {
    return await this.put(`/hosts/popups/${encodeURIComponent(popupId)}`, data);
};

// 팝업 삭제
apiService.deletePopup = async function(popupId) {
    return await this.delete(`/hosts/popups/${encodeURIComponent(popupId)}`);
};

// 호스트 프로필 조회
apiService.getMyHostProfile = async function() {
    return await this.get('/hosts/me');
};
// 팝업 통계
apiService.getPopupStats = async function(popupId, params = {}) {
    const sp = new URLSearchParams(params);
    const query = sp.toString() ? `?${sp.toString()}` : '';
    return await this.get(`/host/popups/${encodeURIComponent(popupId)}/stats${query}`);
};

// === 채팅 api ===
apiService.getChatMessages = async function(reservationId) {
    return await this.get(`/chat/${encodeURIComponent(reservationId)}/messages`);
};

// 채팅자 정보 조회 함수 추가
apiService.getChatContext = async function(reservationId) {
    return await this.get(`/chat/${encodeURIComponent(reservationId)}/context`);
};

// === 리뷰 관련 API ===

// 리뷰 작성
apiService.createReview = async function(reviewData) {
    return await this.post('/reviews', reviewData);
};

// 리뷰 수정
apiService.updateReview = async function(reviewId, reviewData) {
    return await this.put(`/reviews/${encodeURIComponent(reviewId)}`, reviewData);
};

// 리뷰 삭제
apiService.deleteReview = async function(reviewId) {
    return await this.delete(`/reviews/${encodeURIComponent(reviewId)}`);
};

// 특정 팝업의 최근 리뷰 조회
apiService.getRecentReviews = async function(popupId, limit = 2) {
    return await this.get(`/reviews/popup/${encodeURIComponent(popupId)}/recent?limit=${limit}`);
};

// 특정 팝업의 전체 리뷰 조회 (페이징)
apiService.getReviewsByPopup = async function(popupId, page = 0, size = 10, sort = 'createdAt,desc') {
    const params = new URLSearchParams({ page, size, sort });
    return await this.get(`/reviews/popup/${encodeURIComponent(popupId)}?${params}`);
};

// 팝업 리뷰 통계 조회
apiService.getReviewStats = async function(popupId) {
    return await this.get(`/reviews/popup/${encodeURIComponent(popupId)}/stats`);
};

// 내 리뷰 목록 조회
apiService.getMyReviews = async function(page = 0, size = 10, sort = 'createdAt,desc') {
    const params = new URLSearchParams({ page, size, sort });
    return await this.get(`/reviews/me?${params}`);
};

// 리뷰 단건 조회
apiService.getReview = async function(reviewId) {
    return await this.get(`/reviews/${encodeURIComponent(reviewId)}`);
};

// 사용자가 특정 팝업에 리뷰 작성 여부 확인
apiService.checkUserReview = async function(popupId) {
    return await this.get(`/reviews/popup/${encodeURIComponent(popupId)}/check`);
};

// 사용자의 특정 팝업 리뷰 조회
apiService.getUserReviewForPopup = async function(popupId) {
    return await this.get(`/reviews/popup/${encodeURIComponent(popupId)}/me`);
};

// 팝업별 리뷰 통계와 최근 리뷰 통합 조회
apiService.getPopupReviewSummary = async function(popupId) {
    return await this.get(`/reviews/popup/${encodeURIComponent(popupId)}/summary`);
};

// === 북마크 관련 API ===

// 북마크 추가
apiService.addBookmark = async function(popupId) {
    return await this.post('/bookmarks', { popupId });
};

// 북마크 제거
apiService.removeBookmark = async function(popupId) {
    return await this.delete(`/bookmarks/${encodeURIComponent(popupId)}`);
};

// 북마크 상태 확인
apiService.checkBookmark = async function(popupId) {
    try {
        return await this.get(`/bookmarks/check/${encodeURIComponent(popupId)}`, { silent: true });
    } catch (error) {
        return { isBookmarked: false };
    }
};

// 내 북마크 목록 조회
apiService.getMyBookmarks = async function(page = 0, size = 10) {
    const params = new URLSearchParams({ page, size });
    return await this.get(`/bookmarks/me?${params}`);
};

// === 유틸리티 메서드 ===

// 현재 사용자 ID 가져오기
apiService.getCurrentUserId = function() {
    try {
        const userId = localStorage.getItem('userId') ||
            sessionStorage.getItem('userId');
        return userId ? parseInt(userId) : null;
    } catch (error) {
        return null;
    }
};

// 로그인 상태 확인
apiService.isLoggedIn = function() {
    const token = this.getStoredToken();
    const userId = this.getCurrentUserId();
    return !!(token && userId);
};

// 에러 핸들링 (기존 메서드가 없다면 추가)
apiService.handleApiError = function(error) {
    console.error('API Error:', error);

    if (error.message.includes('401') || error.message.includes('인증이 필요합니다')) {
        this.removeToken();
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
    }

    if (error.message.includes('403')) {
        alert('접근 권한이 없습니다.');
        return;
    }

    if (error.message.includes('404')) {
        alert('요청하신 정보를 찾을 수 없습니다.');
        return;
    }

    if (error.message.includes('500')) {
        alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        return;
    }

    // 기타 오류
    alert(error.message || '알 수 없는 오류가 발생했습니다.');
};

// === 관리자용 미션셋 / 미션 API ===

// 미션셋 목록 (페이지네이션 지원)
apiService.getMissionSets = async function(params = {}) {
    const sp = new URLSearchParams(params);
    const query = sp.toString() ? `?${sp.toString()}` : '';
    return await this.get(`/admin/mission-sets${query}`);
};

// 미션셋 상세
apiService.getMissionSetDetail = async function(setId) {
    return await this.get(`/admin/mission-sets/${encodeURIComponent(setId)}`);
};

// 미션셋 생성
apiService.createMissionSet = async function(data) {
    return await this.post(`/admin/mission-sets`, data);
};

// 미션셋 삭제
apiService.deleteMissionSet = async function(setId) {
    return await this.delete(`/admin/mission-sets/${encodeURIComponent(setId)}`);
};

// 특정 미션셋에 미션 추가
apiService.addMission = async function(setId, data) {
    return await this.post(`/admin/mission/${encodeURIComponent(setId)}`, data);
};

// 미션 삭제
apiService.deleteMission = async function(missionId) {
    return await this.delete(`/admin/mission/${encodeURIComponent(missionId)}`);
};

apiService.listAdminPopups = async function ({ page = 0, size = 500 } = {}) {
    const sp = new URLSearchParams({ page, size });
    return await this.get(`/admin/popups?${sp.toString()}`);
};

apiService.updateMissionSet = async function(setId, data) {
    return await this.put(`/admin/mission-sets/${encodeURIComponent(setId)}`, data);
};

apiService.getMission = async function(missionId) {
    return await this.get(`/missions/${encodeURIComponent(missionId)}`);
};

apiService.listMissions = async function(params = {}) {
    let url = '/missions';
    if (params.missionSetId != null) {
        url += `?missionSetId=${encodeURIComponent(params.missionSetId)}`;
    }
    return await this.get(url);
};

apiService.getMissionSet = async function(missionSetId) {
    return await this.get(`/mission-sets/${encodeURIComponent(missionSetId)}`);
};

apiService.submitMissionAnswer = async function(missionId, answer) {
    return await this.post(`/user-missions/${encodeURIComponent(missionId)}/submit-answer`, { answer });
};

// === 리워드 관련 API ===
apiService.getMyReward = async function(missionSetId) {
    return await this.get(`/rewards/my/${encodeURIComponent(missionSetId)}`);
};

apiService.redeemReward = async function(missionSetId, staffPin) {
    return await this.post(`/rewards/redeem`, { missionSetId, staffPin });
};

// 닉네임 중복 확인
apiService.checkNicknameDuplicate = async function(nickname) {
    return await this.get(`/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`);
};

// === 팝업 예약 관련 API ===

// 특정 팝업의 예약 목록 조회
apiService.getPopupReservations = async function(popupId) {
    return await this.get(`/reservations/popups/${encodeURIComponent(popupId)}`);
};

// 예약 취소
apiService.cancelReservation = async function(reservationId) {
    return await this.put(`/reservations/${encodeURIComponent(reservationId)}/cancel`, {});
};

// 예약 방문 완료
apiService.visitReservation = async function(reservationId) {
    return await this.put(`/reservations/${encodeURIComponent(reservationId)}/visit`, {});
};

// 예약 기본 설정 조회
apiService.getPopupBasicSettings = async function(popupId) {
    return await this.get(`/reservations/settings/popups/${encodeURIComponent(popupId)}/basic`);
};

// 예약 기본 설정 수정
apiService.updatePopupBasicSettings = async function(popupId, settings) {
    return await this.put(`/reservations/settings/popups/${encodeURIComponent(popupId)}/basic`, settings);
};

// 예약 가능 시간대 조회
apiService.getAvailableTimeSlots = async function(popupId, date) {
    return await this.get(`/reservations/popups/${encodeURIComponent(popupId)}/available-slots?date=${encodeURIComponent(date)}`);
};
// 남은 예약 좌석 수 조회
apiService.getAvailableSlotsWithCapacity = async function(popupId, date) {
    return await this.get(`/reservations/popups/${encodeURIComponent(popupId)}/available-slots/with-capacity?date=${encodeURIComponent(date)}`);
};

// ===== 리워드 =====
apiService.getRewards = async function(missionSetId) {
    return await this.get(`/admin/mission-sets/${encodeURIComponent(missionSetId)}/rewards`);
};

// 리워드 생성
apiService.createReward = async function(missionSetId, payload) {
    return await this.post(`/admin/mission-sets/${encodeURIComponent(missionSetId)}/rewards`, payload);
};

// 리워드 수정
apiService.updateReward = async function(missionSetId, rewardId, payload) {
    return await this.put(`/admin/mission-sets/${encodeURIComponent(missionSetId)}/rewards/${encodeURIComponent(rewardId)}`, payload);
};

// 리워드 삭제
apiService.deleteReward = async function(missionSetId, rewardId) {
    return await this.delete(`/admin/mission-sets/${encodeURIComponent(missionSetId)}/rewards/${encodeURIComponent(rewardId)}`);
};
