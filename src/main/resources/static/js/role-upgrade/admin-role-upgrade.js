// /js/role-upgrade/admin-role-upgrade.js

class AdminRoleUpgradeManager {
    constructor() {
        this.baseURL = '/api/admin/role-upgrade';  // 정확한 API 경로로 수정
        this.currentRequestId = null;
        this.currentPage = 0;
        this.pageSize = 20;

        this.init();
    }

    // 초기화
    init() {
        this.loadStats();
        this.loadRequests();
        this.setupEventListeners();
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        // 검색 버튼
        const searchBtn = document.getElementById('searchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => {
                this.handleSearch();
            });
        }

        // 초기화 버튼
        const resetBtn = document.getElementById('resetBtn');
        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                this.resetFilters();
            });
        }

        // 엔터키로 검색
        const searchKeyword = document.getElementById('searchKeyword');
        if (searchKeyword) {
            searchKeyword.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.handleSearch();
                }
            });
        }

        // 모달 닫기
        const modalCloseBtn = document.getElementById('modalCloseBtn');
        if (modalCloseBtn) {
            modalCloseBtn.addEventListener('click', () => {
                this.closeModal();
            });
        }

        // 모달 배경 클릭시 닫기
        const detailModal = document.getElementById('detailModal');
        if (detailModal) {
            detailModal.addEventListener('click', (e) => {
                if (e.target.id === 'detailModal') {
                    this.closeModal();
                }
            });
        }
    }

    // JWT 토큰 가져오기 - authToken으로 통일
    getStoredToken() {
        return localStorage.getItem('authToken') ||
            localStorage.getItem('accessToken') ||
            localStorage.getItem('token') ||
            sessionStorage.getItem('accessToken') ||
            sessionStorage.getItem('token');
    }

    // API 호출 헬퍼
    async apiCall(url, options = {}) {
        const token = this.getStoredToken();

        const defaultHeaders = {
            'Content-Type': 'application/json'
        };

        if (token) {
            defaultHeaders['Authorization'] = `Bearer ${token}`;
        }

        const finalOptions = {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, finalOptions);

            if (!response.ok) {
                if (response.status === 401) {
                    alert('로그인이 필요합니다.');
                    window.location.href = '/templates/pages/auth/login.html';
                    return;
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            return data;

        } catch (error) {
            console.error('API 호출 에러:', error);
            throw error;
        }
    }

    // 통계 로드
    async loadStats() {
        try {
            const data = await this.apiCall(`${this.baseURL}/pending-count`);
            const pendingElement = document.getElementById('pendingCount');
            if (pendingElement) {
                pendingElement.textContent = data || 0;
            }
        } catch (error) {
            console.error('통계 로드 실패:', error);
        }
    }

    // 요청 목록 로드
    async loadRequests() {
        try {
            const statusFilter = document.getElementById('statusFilter');
            const roleFilter = document.getElementById('roleFilter');
            const searchKeyword = document.getElementById('searchKeyword');

            const status = statusFilter ? statusFilter.value : '';
            const role = roleFilter ? roleFilter.value : '';
            const keyword = searchKeyword ? searchKeyword.value : '';

            let url = `${this.baseURL}/requests?page=${this.currentPage}&size=${this.pageSize}`;

            if (status) url += `&status=${status}`;
            if (role) url += `&role=${role}`;
            if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

            const data = await this.apiCall(url);
            this.renderTable(data);

        } catch (error) {
            console.error('요청 목록 로드 실패:', error);
            const tableContainer = document.getElementById('tableContainer');
            if (tableContainer) {
                tableContainer.innerHTML = '<div class="error-message">데이터를 불러오는데 실패했습니다.</div>';
            }
        }
    }

    renderTable(data) {
        const container = document.getElementById('tableContainer');
        if (!container) return;

        if (!data.content || data.content.length === 0) {
            container.innerHTML = '<div class="no-data">데이터가 없습니다.</div>';
            return;
        }

        const rowsHtml = data.content.map(request => {
            const statusClass = request.status ? request.status.toLowerCase() : '';
            const roleClass = request.requestedRole ? request.requestedRole.toLowerCase() : '';

            // payload 파싱
            let payload = {};
            try {
                if (request.payload && typeof request.payload === 'string') {
                    payload = JSON.parse(request.payload);
                } else if (request.payload && typeof request.payload === 'object') {
                    payload = request.payload;
                }
            } catch (e) {
                console.warn('Payload 파싱 실패 for', request.email, ':', e);
                payload = {};
            }

            return `
      <tr>
        <td>${this.escapeHtml(request.email)}</td>
        <td><span class="role-badge ${roleClass}">${this.getRoleText(request.requestedRole)}</span></td>
        <td>${this.escapeHtml(payload.companyName || '-')}</td>
        <td><span class="status-badge ${statusClass}">${this.getStatusText(request.status)}</span></td>
        <td>${this.formatDate(request.createdAt)}</td>
        <td>${this.formatDate(request.updatedAt)}</td>
        <td>
          <button class="button button-sm button-primary" onclick="adminManager.showDetail(${request.id})">
            상세보기
          </button>
        </td>
      </tr>
    `;
        }).join('');

        container.innerHTML = `
    <table class="requests-table">
      <thead>
        <tr>
          <th>이메일</th>
          <th>요청 역할</th>
          <th>업체명</th>
          <th>상태</th>
          <th>요청일</th>
          <th>처리일</th>
          <th>처리</th>
        </tr>
      </thead>
      <tbody>
        ${rowsHtml}
      </tbody>
    </table>
  `;

        // 페이지네이션
        this.renderPagination(data);
    }

    // 페이지네이션 렌더링
    renderPagination(data) {
        const paginationContainer = document.getElementById('pagination');
        if (!paginationContainer) return;

        if (data.totalPages <= 1) {
            paginationContainer.innerHTML = '';
            return;
        }

        let html = '<div class="pagination-wrapper">';

        // 이전 페이지
        if (data.first) {
            html += '<button class="page-button" disabled>이전</button>';
        } else {
            html += `<button class="page-button" onclick="adminManager.changePage(${data.number - 1})">이전</button>`;
        }

        // 페이지 번호들
        const startPage = Math.max(0, data.number - 2);
        const endPage = Math.min(data.totalPages - 1, data.number + 2);

        for (let i = startPage; i <= endPage; i++) {
            const activeClass = i === data.number ? 'active' : '';
            html += `<button class="page-button ${activeClass}" onclick="adminManager.changePage(${i})">${i + 1}</button>`;
        }

        // 다음 페이지
        if (data.last) {
            html += '<button class="page-button" disabled>다음</button>';
        } else {
            html += `<button class="page-button" onclick="adminManager.changePage(${data.number + 1})">다음</button>`;
        }

        html += '</div>';
        paginationContainer.innerHTML = html;
    }

    // 페이지 변경
    changePage(page) {
        this.currentPage = page;
        this.loadRequests();
    }

    // 상세보기 표시
    async showDetail(requestId) {
        try {
            this.currentRequestId = requestId;
            const request = await this.apiCall(`${this.baseURL}/requests/${requestId}`);

            const modal = document.getElementById('detailModal');
            const modalBody = document.getElementById('modalBody');

            if (modalBody) {
                modalBody.innerHTML = this.generateDetailHTML(request);
            }
            if (modal) {
                modal.style.display = 'block';
            }

        } catch (error) {
            console.error('상세 조회 실패:', error);
            alert('상세 정보를 불러오는데 실패했습니다: ' + error.message);
        }
    }

    // 상세보기 HTML 생성
    generateDetailHTML(request) {
        let payload = {};
        try {
            if (request.payload) {
                payload = JSON.parse(request.payload);
            }
        } catch (e) {
            console.warn('Payload 파싱 실패:', e);
        }

        const canProcess = request.status === 'PENDING';

        return `
            <div class="request-detail">
                <div class="detail-section">
                    <h3>기본 정보</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>이메일</label>
                            <span>${this.escapeHtml(request.email)}</span>
                        </div>
                        <div class="detail-item">
                            <label>요청 역할</label>
                            <span class="role-badge ${request.requestedRole ? request.requestedRole.toLowerCase() : ''}">${this.getRoleText(request.requestedRole)}</span>
                        </div>
                        <div class="detail-item">
                            <label>상태</label>
                            <span class="status-badge ${request.status ? request.status.toLowerCase() : ''}">${this.getStatusText(request.status)}</span>
                        </div>
                        <div class="detail-item">
                            <label>요청일</label>
                            <span>${this.formatDate(request.createdAt)}</span>
                        </div>
                        ${request.updatedAt !== request.createdAt ? `
                            <div class="detail-item">
                                <label>처리일</label>
                                <span>${this.formatDate(request.updatedAt)}</span>
                            </div>
                        ` : ''}
                    </div>
                </div>

                <div class="detail-section">
                    <h3>사업자 정보</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>업체명</label>
                            <span>${this.escapeHtml(payload.companyName || '-')}</span>
                        </div>
                        <div class="detail-item">
                            <label>사업자등록번호</label>
                            <span>${this.escapeHtml(payload.businessNumber || '-')}</span>
                        </div>
                        ${payload.description ? `
                            <div class="detail-item full-width">
                                <label>추가 설명</label>
                                <span>${this.escapeHtml(payload.description)}</span>
                            </div>
                        ` : ''}
                    </div>
                </div>

                ${request.documents && request.documents.length > 0 ? `
                    <div class="detail-section">
                        <h3>첨부 문서</h3>
                        <div class="documents-list">
                            ${request.documents.map(doc => `
                                <div class="document-item">
                                    <span>${doc.docType === 'BUSINESS_LICENSE' ? '사업자등록증' : '기타'}</span>
                                    ${doc.businessNumber ? `<span>사업자번호: ${doc.businessNumber}</span>` : ''}
                                    ${doc.fileUrl ? `<a href="${doc.fileUrl}" target="_blank" class="download-link">파일 보기</a>` : ''}  
                                </div>
                            `).join('')}
                        </div>
                    </div>
                ` : ''}

                ${request.status === 'REJECTED' && request.rejectReason ? `
                    <div class="detail-section">
                        <h3>반려 사유</h3>
                        <div class="reason-text">${this.escapeHtml(request.rejectReason)}</div>
                    </div>
                ` : ''}

                <div class="detail-section">
                    <h3>처리</h3>
                    <textarea id="adminComment" placeholder="처리 사유나 메모를 입력하세요..." 
                              style="width: 100%; min-height: 100px; padding: 10px; border: 1px solid #ddd; border-radius: 4px; resize: vertical; font-family: inherit;"></textarea>
                </div>

                <div class="action-buttons">
                    ${canProcess ? `
                        <button class="button button-success" onclick="adminManager.processRequest(true)">
                            승인
                        </button>
                        <button class="button button-danger" onclick="adminManager.processRequest(false)">
                            반려
                        </button>
                    ` : ''}
                    <button class="button button-secondary" onclick="adminManager.closeModal()">
                        닫기
                    </button>
                </div>
            </div>
        `;
    }

    // 검색 처리
    handleSearch() {
        this.currentPage = 0;
        this.loadRequests();
    }

    // 필터 초기화
    resetFilters() {
        const statusFilter = document.getElementById('statusFilter');
        const roleFilter = document.getElementById('roleFilter');
        const searchKeyword = document.getElementById('searchKeyword');

        if (statusFilter) statusFilter.value = '';
        if (roleFilter) roleFilter.value = '';
        if (searchKeyword) searchKeyword.value = '';

        this.currentPage = 0;
        this.loadRequests();
    }

    // 요청 처리 (승인/반려) - 수정된 부분
    async processRequest(approve) {
        const adminComment = document.getElementById('adminComment')?.value || '';

        if (!approve && !adminComment.trim()) {
            alert('반려 시에는 반려 사유를 입력해주세요.');
            return;
        }

        if (!confirm(`정말로 이 요청을 ${approve ? '승인' : '반려'}하시겠습니까?`)) {
            return;
        }

        try {
            // AdminUpdateRequest DTO에 맞게 데이터 구성
            const requestData = {
                approve: approve
            };

            // 반려인 경우에만 rejectReason 추가
            if (!approve) {
                requestData.rejectReason = adminComment.trim();
            }

            await this.apiCall(`${this.baseURL}/requests/${this.currentRequestId}/process`, {
                method: 'PUT',
                body: JSON.stringify(requestData)
            });

            alert(approve ? '요청이 승인되었습니다.' : '요청이 반려되었습니다.');
            this.closeModal();
            this.loadRequests();
            this.loadStats();

        } catch (error) {
            console.error('요청 처리 실패:', error);
            alert('요청 처리에 실패했습니다: ' + error.message);
        }
    }

    // 모달 닫기
    closeModal() {
        const modal = document.getElementById('detailModal');
        if (modal) {
            modal.style.display = 'none';
        }
        this.currentRequestId = null;
    }

    // 유틸리티 함수들
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // 날짜 포맷팅
    formatDate(dateString) {
        if (!dateString) return '-';
        try {
            const date = new Date(dateString);
            return date.toLocaleString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (error) {
            return dateString;
        }
    }

    // 상태 텍스트 변환
    getStatusText(status) {
        switch (status) {
            case 'PENDING': return '대기중';
            case 'APPROVED': return '승인됨';
            case 'REJECTED': return '반려됨';
            default: return status || '-';
        }
    }

    // 역할 텍스트 변환
    getRoleText(role) {
        switch (role) {
            case 'HOST': return '호스트';
            case 'PROVIDER': return '공간 제공자';
            default: return role || '-';
        }
    }
}

// 전역 변수로 인스턴스 생성
let adminManager;

// DOM 로드 완료 후 초기화
document.addEventListener('DOMContentLoaded', function() {
    adminManager = new AdminRoleUpgradeManager();
});