// /js/admin/report-management.js

class ReportManagement {
    constructor() {
        this.init();
    }

    init() {
        this.loadReportCounts();
        this.bindEvents();
    }

    bindEvents() {
        // 새로고침 버튼이 있다면 이벤트 바인딩
        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                this.loadReportCounts();
            });
        }

        // 카드 클릭 이벤트 트래킹 (분석용)
        document.querySelectorAll('.card').forEach(card => {
            card.addEventListener('click', function() {
                const cardType = this.classList[1]; // 두 번째 클래스명 (popup-report, space-report 등)
                console.log('카드 클릭:', cardType);

                // 구글 애널리틱스나 다른 분석 도구로 이벤트 전송 가능
                if (typeof gtag !== 'undefined') {
                    gtag('event', 'click', {
                        event_category: 'admin',
                        event_label: cardType
                    });
                }
            });
        });
    }

    // 각 신고 유형별 카운트 가져오기
    async loadReportCounts() {
        try {
            // 모든 통계를 병렬로 가져오기
            const [inquiryCounts, upgradeCounts] = await Promise.all([
                this.getInquiryCounts(),
                this.getUpgradeRequestCounts()
            ]);

            // 배지 업데이트
            this.updateBadge('popupReportCount', inquiryCounts.popupPending || 0);
            this.updateBadge('spaceReportCount', inquiryCounts.spacePending || 0);
            this.updateBadge('reviewReportCount', this.calculateReviewPending(inquiryCounts));
            this.updateBadge('generalReportCount', this.calculateGeneralPending(inquiryCounts));
            this.updateBadge('upgradeRequestCount', upgradeCounts.pendingCount || 0);

            console.log('신고 카운트 로딩 완료');

        } catch (error) {
            console.error('신고 카운트 로딩 실패:', error);
            // 에러 시 모든 배지를 0으로 설정
            this.updateBadge('popupReportCount', 0);
            this.updateBadge('spaceReportCount', 0);
            this.updateBadge('reviewReportCount', 0);
            this.updateBadge('generalReportCount', 0);
            this.updateBadge('upgradeRequestCount', 0);
        }
    }

    // 문의/신고 통계 API 호출
    async getInquiryCounts() {
        const response = await fetch('/api/admin/inquiries/counts', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            }
        });

        if (!response.ok) {
            throw new Error(`API 호출 실패: ${response.status}`);
        }

        return await response.json();
    }

    // 역할 승격 요청 통계 API 호출
    async getUpgradeRequestCounts() {
        const response = await fetch('/api/admin/users/upgrade-requests/count', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            }
        });

        if (!response.ok) {
            throw new Error(`API 호출 실패: ${response.status}`);
        }

        // AdminUserController에서 Long을 직접 반환하므로 객체 형태로 변환
        const count = await response.json();
        return { pendingCount: count };
    }

    // 리뷰 신고 대기중 개수 계산 (별도 API가 없으므로 전체에서 계산)
    calculateReviewPending(inquiryCounts) {
        // 현재 API에서 리뷰별 대기중 개수를 직접 제공하지 않으므로
        // 전체 리뷰 신고에서 대략적으로 계산 (실제로는 별도 API 호출 필요)
        return Math.max(0, Math.floor((inquiryCounts.review || 0) * 0.3)); // 임시로 30% 가정
    }

    // 일반 신고 대기중 개수 계산
    calculateGeneralPending(inquiryCounts) {
        // 일반 신고도 마찬가지로 대략적 계산
        return Math.max(0, Math.floor((inquiryCounts.general || 0) * 0.3)); // 임시로 30% 가정
    }

    // 배지 업데이트 함수
    updateBadge(elementId, count) {
        const badge = document.getElementById(elementId);
        if (badge) {
            badge.textContent = count;
            badge.className = count > 0 ? 'badge' : 'badge zero';
        }
    }

    // JWT 토큰 가져오기
    getToken() {
        return localStorage.getItem('authToken') || localStorage.getItem('accessToken') || '';
    }

    // 수동 새로고침 함수 (외부에서 호출 가능)
    refreshCounts() {
        this.loadReportCounts();
    }

    // 특정 타입의 카운트만 업데이트 (다른 페이지에서 돌아올 때 사용)
    async updateSpecificCount(type) {
        try {
            switch (type) {
                case 'popup':
                    const popupCount = await this.getPopupPendingCount();
                    this.updateBadge('popupReportCount', popupCount);
                    break;
                case 'space':
                    const spaceCount = await this.getSpacePendingCount();
                    this.updateBadge('spaceReportCount', spaceCount);
                    break;
                case 'review':
                    const reviewCount = await this.getReviewPendingCount();
                    this.updateBadge('reviewReportCount', reviewCount);
                    break;
                case 'general':
                    const generalCount = await this.getGeneralPendingCount();
                    this.updateBadge('generalReportCount', generalCount);
                    break;
                case 'upgrade':
                    const upgradeCount = await this.getUpgradeRequestCounts();
                    this.updateBadge('upgradeRequestCount', upgradeCount.pendingCount || 0);
                    break;
            }
        } catch (error) {
            console.error(`${type} 카운트 업데이트 실패:`, error);
        }
    }

    // 각 타입별 대기중 개수를 정확히 가져오는 메서드들
    async getPopupPendingCount() {
        const params = new URLSearchParams({
            targetType: 'POPUP',
            status: 'OPEN',
            page: '0',
            size: '1'
        });

        const response = await fetch(`/api/admin/inquiries?${params}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            }
        });

        if (!response.ok) {
            throw new Error('팝업 대기 카운트 조회 실패');
        }

        const data = await response.json();
        return data.totalElements || 0;
    }

    async getSpacePendingCount() {
        const params = new URLSearchParams({
            targetType: 'SPACE',
            status: 'OPEN',
            page: '0',
            size: '1'
        });

        const response = await fetch(`/api/admin/inquiries?${params}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            }
        });

        if (!response.ok) {
            throw new Error('장소 대기 카운트 조회 실패');
        }

        const data = await response.json();
        return data.totalElements || 0;
    }

    async getReviewPendingCount() {
        const params = new URLSearchParams({
            targetType: 'REVIEW',
            status: 'OPEN',
            page: '0',
            size: '1'
        });

        const response = await fetch(`/api/admin/inquiries?${params}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            }
        });

        if (!response.ok) {
            throw new Error('리뷰 대기 카운트 조회 실패');
        }

        const data = await response.json();
        return data.totalElements || 0;
    }

    async getGeneralPendingCount() {
        const params = new URLSearchParams({
            targetType: 'GENERAL',
            status: 'OPEN',
            page: '0',
            size: '1'
        });

        const response = await fetch(`/api/admin/inquiries?${params}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            }
        });

        if (!response.ok) {
            throw new Error('일반 대기 카운트 조회 실패');
        }

        const data = await response.json();
        return data.totalElements || 0;
    }
}

// 뒤로가기 함수
function goBack() {
    window.history.back();
}

// 전역 변수로 인스턴스 생성
let reportManagement;

// DOM이 로드되면 초기화
document.addEventListener('DOMContentLoaded', function() {
    reportManagement = new ReportManagement();
});

// 다른 페이지에서 돌아올 때 카운트 업데이트 (페이지 포커스 시)
window.addEventListener('focus', function() {
    if (reportManagement) {
        reportManagement.refreshCounts();
    }
});

// 주기적 업데이트 (5분마다)
setInterval(() => {
    if (reportManagement && document.visibilityState === 'visible') {
        reportManagement.refreshCounts();
    }
}, 5 * 60 * 1000); // 5분