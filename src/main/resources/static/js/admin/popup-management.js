// 팝업 관리 클래스
class PopupManagement {
    constructor() {
        this.currentPage = 1;
        this.pageSize = 10;
        this.currentFilters = {};
        this.selectedPopupId = null;
        this.categories = []; // 카테고리 목록

        this.initEventListeners();
        this.loadCategories();
        this.loadPopups();
    }

    // 이벤트 리스너 초기화
    initEventListeners() {
        // 검색 버튼
        document.getElementById('searchBtn').addEventListener('click', () => this.search());
        document.getElementById('resetBtn').addEventListener('click', () => this.reset());

        // 엔터키로 검색
        ['statusFilter', 'categoryFilter', 'searchKeyword'].forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') this.search();
                });
            }
        });

        // 모달 이벤트
        document.getElementById('closeModalBtn').addEventListener('click', () => this.closeModal());
        document.getElementById('rejectModalCloseBtn').addEventListener('click', () => this.closeRejectModal());
        document.getElementById('cancelRejectBtn').addEventListener('click', () => this.closeRejectModal());

        // 액션 버튼들
        document.getElementById('approveBtn').addEventListener('click', () => this.approvePopup());
        document.getElementById('rejectBtn').addEventListener('click', () => this.openRejectModal());
        document.getElementById('suspendBtn').addEventListener('click', () => this.suspendPopup());
        document.getElementById('confirmRejectBtn').addEventListener('click', () => this.confirmReject());

        // 상태 변경 버튼 이벤트 추가
        document.getElementById('changeStatusBtn')?.addEventListener('click', () => this.openStatusModal());
        document.getElementById('confirmStatusBtn')?.addEventListener('click', () => this.confirmStatusChange());
        document.getElementById('cancelStatusBtn')?.addEventListener('click', () => this.closeStatusModal());
        document.getElementById('statusModalCloseBtn')?.addEventListener('click', () => this.closeStatusModal());
    }

    // 인증 헤더 반환
    getAuthHeaders() {
        const token = localStorage.getItem('accessToken') || localStorage.getItem('authToken');
        return {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        };
    }

    // 카테고리 로드
    async loadCategories() {
        try {
            const response = await fetch('/api/categories', {
                headers: this.getAuthHeaders()
            });

            if (response.ok) {
                this.categories = await response.json();
                this.populateCategoryFilter(); // 필터 옵션 업데이트
            }
        } catch (error) {
            console.error('카테고리 로드 실패:', error);
        }
    }

    // 카테고리 필터 옵션 채우기
    populateCategoryFilter() {
        const categorySelect = document.getElementById('categoryFilter');
        if (!categorySelect || !this.categories) return;

        // 기존 옵션 유지하고 새 옵션 추가
        let html = '<option value="">전체</option>';
        this.categories.forEach(category => {
            html += `<option value="${category.slug}">${category.name}</option>`;
        });

        categorySelect.innerHTML = html;
    }

    // 검색 실행
    search() {
        this.currentFilters = {
            status: document.getElementById('statusFilter').value,
            category: document.getElementById('categoryFilter')?.value || '',
            keyword: document.getElementById('searchKeyword').value
        };
        this.currentPage = 1;
        this.loadPopups();
    }

    // 필터 초기화
    reset() {
        document.getElementById('statusFilter').value = '';
        document.getElementById('categoryFilter') && (document.getElementById('categoryFilter').value = '');
        document.getElementById('searchKeyword').value = '';
        this.currentFilters = {};
        this.currentPage = 1;
        this.loadPopups();
    }

    // 페이지 이동
    goToPage(page) {
        this.currentPage = page;
        this.loadPopups();
    }

    // 통계 로드
    async loadStats() {
        try {
            const response = await fetch('/api/admin/popups/stats', {
                headers: this.getAuthHeaders()
            });

            if (response.ok) {
                const stats = await response.json();
                document.getElementById('totalCount').textContent = stats.total || 0;
                document.getElementById('pendingCount').textContent = stats.pending || 0;
                document.getElementById('activeCount').textContent = stats.active || 0;
            }
        } catch (error) {
            console.error('통계 로드 실패:', error);
        }
    }

    async loadPopups() {
        try {
            this.showLoading();

            const params = new URLSearchParams({
                page: this.currentPage - 1,
                size: this.pageSize,
                ...this.currentFilters
            });

            const response = await fetch(`/api/admin/popups?${params}`, {
                headers: this.getAuthHeaders()
            });

            if (!response.ok) throw new Error('팝업 목록 로드 실패');

            const data = await response.json();
            this.renderTable(data.content);
            this.renderPagination(data);
            this.loadStats();

        } catch (error) {
            console.error('팝업 목록 로드 실패:', error);
            this.showError('팝업 목록을 불러오는데 실패했습니다.');
        }
    }

    showLoading() {
        document.getElementById('tableContainer').innerHTML =
            '<div class="loading">데이터를 불러오는 중...</div>';
    }

    showError(message) {
        document.getElementById('tableContainer').innerHTML =
            `<div class="no-data">${message}</div>`;
    }

    // 테이블 렌더링
    renderTable(popups) {
        if (!popups || popups.length === 0) {
            document.getElementById('tableContainer').innerHTML =
                '<div class="no-data">등록된 팝업이 없습니다.</div>';
            return;
        }

        const tableHTML = `
    <table class="popup-table">
        <thead>
            <tr>
                <th>ID</th><th>팝업명</th><th>브랜드</th><th>카테고리</th>
                <th>기간</th><th>상태</th><th>등록일</th><th>액션</th>
            </tr>
        </thead>
        <tbody>
            ${popups.map(popup => `
                <tr>
                    <td>${popup.id}</td>
                    <td>
                        <div style="font-weight: 500;">${popup.title}</div>
                        <div style="font-size: 12px; color: #666;">${popup.venueName || popup.venueAddress || ''}</div>
                    </td>
                    <td>${popup.brandName || '미등록'}</td>
                    <td><span class="category-badge">${popup.categoryName || this.getCategoryText(popup.category) || '미분류'}</span></td>
                    <td>
                        <div>${this.formatDate(popup.startDate)} ~</div>
                        <div>${this.formatDate(popup.endDate)}</div>
                    </td>
                    <td><span class="status-badge ${popup.status.toLowerCase()}">${this.getStatusText(popup.status)}</span></td>
                    <td>${this.formatDate(popup.createdAt)}</td>
                    <td>
                        <button class="button button-sm button-primary" onclick="popupManagement.viewDetail(${popup.id})">
                            상세보기
                        </button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    </table>
`;

        document.getElementById('tableContainer').innerHTML = tableHTML;
    }

    // 페이지네이션
    renderPagination(pageInfo) {
        const {number, totalPages, first, last} = pageInfo;

        if (totalPages <= 1) {
            document.getElementById('pagination').innerHTML = '';
            return;
        }

        const startPage = Math.max(0, number - 2);
        const endPage = Math.min(totalPages - 1, number + 2);

        let html = `<button class="page-button" ${first ? 'disabled' : ''} onclick="popupManagement.goToPage(${number})">이전</button>`;

        for (let i = startPage; i <= endPage; i++) {
            const activeClass = i === number ? 'active' : '';
            html += `<button class="page-button ${activeClass}" onclick="popupManagement.goToPage(${i + 1})">${i + 1}</button>`;
        }

        html += `<button class="page-button" ${last ? 'disabled' : ''} onclick="popupManagement.goToPage(${number + 2})">다음</button>`;

        document.getElementById('pagination').innerHTML = html;
    }

    // 팝업 상세 보기
    async viewDetail(popupId) {
        this.selectedPopupId = popupId;

        try {
            const response = await fetch(`/api/admin/popups/${popupId}`, {
                headers: this.getAuthHeaders()
            });

            if (!response.ok) throw new Error('팝업 상세 조회 실패');

            const popup = await response.json();
            this.renderDetailModal(popup);
            this.updateActionButtons(popup.status);
            document.getElementById('detailModal').style.display = 'block';

        } catch (error) {
            console.error('팝업 상세 조회 실패:', error);
            alert('팝업 상세 정보를 불러오는데 실패했습니다.');
        }
    }

    renderDetailModal(popup) {
        document.getElementById('modalBody').innerHTML = `
    <div class="detail-info">
        <div class="detail-section">
            <h3>기본 정보</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <span class="detail-label">팝업 ID:</span>
                    <span class="detail-value">${popup.id}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">팝업명:</span>
                    <span class="detail-value">${popup.title}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">브랜드:</span>
                    <span class="detail-value">${popup.brandName || '미등록'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">카테고리:</span>
                    <span class="detail-value">${popup.categoryName || this.getCategoryText(popup.category) || '미분류'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">상태:</span>
                    <span class="status-badge ${popup.status.toLowerCase()}">${this.getStatusText(popup.status)}</span>
                </div>
            </div>
        </div>

        <div class="detail-section">
            <h3>운영 기간</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <span class="detail-label">시작일:</span>
                    <span class="detail-value">${this.formatDate(popup.startDate)}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">종료일:</span>
                    <span class="detail-value">${this.formatDate(popup.endDate)}</span>
                </div>
            </div>
        </div>

        <div class="detail-section">
            <h3>장소 정보</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <span class="detail-label">장소명:</span>
                    <span class="detail-value">${popup.venueName || '-'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">주소:</span>
                    <span class="detail-value">${popup.venueAddress || '-'}</span>
                </div>
            </div>
        </div>

        <div class="detail-section">
            <h3>등록 정보</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <span class="detail-label">등록일:</span>
                    <span class="detail-value">${this.formatDate(popup.createdAt)}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">최종 수정:</span>
                    <span class="detail-value">${this.formatDate(popup.updatedAt)}</span>
                </div>
            </div>
        </div>

        ${popup.description ? `
            <div class="detail-section">
                <h3>설명</h3>
                <div class="detail-value description">
                    ${popup.description}
                </div>
            </div>
        ` : ''}

        ${popup.rejectReason ? `
            <div class="detail-section">
                <h3>거부 사유</h3>
                <div class="detail-value reject-reason">
                    ${popup.rejectReason}
                </div>
            </div>
        ` : ''}
    </div>
`;
    }

    updateActionButtons(status) {
        const buttons = {
            approveBtn: document.getElementById('approveBtn'),
            rejectBtn: document.getElementById('rejectBtn'),
            suspendBtn: document.getElementById('suspendBtn'),
            changeStatusBtn: document.getElementById('changeStatusBtn')
        };

        // 모든 버튼 숨김
        Object.values(buttons).forEach(btn => {
            if (btn) btn.style.display = 'none';
        });

        // 상태별 버튼 표시
        if (status === 'PENDING') {
            buttons.approveBtn && (buttons.approveBtn.style.display = 'block');
            buttons.rejectBtn && (buttons.rejectBtn.style.display = 'block');
        } else if (['APPROVED', 'ACTIVE', 'ONGOING'].includes(status)) {
            buttons.suspendBtn && (buttons.suspendBtn.style.display = 'block');
        }

        // 상태 변경 버튼은 항상 표시 (PENDING과 REJECTED 제외)
        if (!['PENDING'].includes(status) && buttons.changeStatusBtn) {
            buttons.changeStatusBtn.style.display = 'block';
        }
    }

    // 팝업 관리 액션
    async approvePopup() {
        if (!this.selectedPopupId) return;
        if (!confirm('이 팝업을 승인하시겠습니까?')) return;

        try {
            const response = await fetch(`/api/admin/popups/${this.selectedPopupId}/approve`, {
                method: 'POST',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) throw new Error('팝업 승인 실패');

            alert('팝업이 승인되었습니다.');
            this.closeModal();
            this.loadPopups();

        } catch (error) {
            console.error('팝업 승인 실패:', error);
            alert('팝업 승인에 실패했습니다.');
        }
    }

    openRejectModal() {
        document.getElementById('rejectReason').value = '';
        document.getElementById('rejectModal').style.display = 'block';
    }

    async confirmReject() {
        const reason = document.getElementById('rejectReason').value.trim();

        if (!reason) {
            alert('거부 사유를 입력해주세요.');
            return;
        }

        try {
            const response = await fetch(`/api/admin/popups/${this.selectedPopupId}/reject`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({reason})
            });

            if (!response.ok) throw new Error('팝업 거부 실패');

            alert('팝업이 거부되었습니다.');
            this.closeRejectModal();
            this.closeModal();
            this.loadPopups();

        } catch (error) {
            console.error('팝업 거부 실패:', error);
            alert('팝업 거부에 실패했습니다.');
        }
    }

    async suspendPopup() {
        if (!this.selectedPopupId) return;
        if (!confirm('이 팝업을 정지하시겠습니까?')) return;

        try {
            const response = await fetch(`/api/admin/popups/${this.selectedPopupId}/suspend`, {
                method: 'POST',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) throw new Error('팝업 정지 실패');

            alert('팝업이 정지되었습니다.');
            this.closeModal();
            this.loadPopups();

        } catch (error) {
            console.error('팝업 정지 실패:', error);
            alert('팝업 정지에 실패했습니다.');
        }
    }

    // === 새로 추가된 상태 변경 기능 ===
    openStatusModal() {
        // 상태 선택 모달 표시
        document.getElementById('statusSelectModal').style.display = 'block';
        this.populateStatusOptions();
    }

    populateStatusOptions() {
        const statusSelect = document.getElementById('newStatusSelect');
        if (!statusSelect) return;

        // PopupStatus enum에 맞는 4개 상태만
        const statusOptions = [
            { value: 'PLANNED', text: '계획중' },
            { value: 'ONGOING', text: '진행중' },
            { value: 'ENDED', text: '종료됨' },
            { value: 'HIDDEN', text: '숨김' }
        ];

        statusSelect.innerHTML = '<option value="">상태를 선택하세요</option>';
        statusOptions.forEach(option => {
            statusSelect.innerHTML += `<option value="${option.value}">${option.text}</option>`;
        });
    }

    async confirmStatusChange() {
        const newStatus = document.getElementById('newStatusSelect').value;

        if (!newStatus) {
            alert('변경할 상태를 선택해주세요.');
            return;
        }

        if (!confirm(`팝업 상태를 '${this.getStatusText(newStatus)}'로 변경하시겠습니까?`)) {
            return;
        }

        try {
            const response = await fetch(`/api/admin/popups/${this.selectedPopupId}/status`, {
                method: 'PUT',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({status: newStatus})
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '상태 변경 실패');
            }

            const result = await response.json();
            alert('팝업 상태가 성공적으로 변경되었습니다.');

            this.closeStatusModal();
            this.closeModal();
            this.loadPopups();

        } catch (error) {
            console.error('팝업 상태 변경 실패:', error);
            alert('팝업 상태 변경에 실패했습니다: ' + error.message);
        }
    }

    closeStatusModal() {
        document.getElementById('statusSelectModal').style.display = 'none';
        document.getElementById('newStatusSelect').value = '';
    }

    // 모달 닫기
    closeModal() {
        document.getElementById('detailModal').style.display = 'none';
        this.selectedPopupId = null;
    }

    closeRejectModal() {
        document.getElementById('rejectModal').style.display = 'none';
    }

    // 유틸리티 함수
    getStatusText(status) {
        const statusMap = {
            'PLANNED': '계획중',
            'ONGOING': '진행중',
            'ENDED': '종료됨',
            'HIDDEN': '숨김'
        };
        return statusMap[status] || status;
    }

    getCategoryText(category) {
        // 실제 카테고리 데이터에서 찾기
        const found = this.categories.find(cat =>
            cat.slug === category || cat.name === category
        );

        if (found) return found.name;

        // 카테고리를 찾지 못한 경우 slug 그대로 반환
        return category || '미분류';
    }

    formatDate(dateString) {
        if (!dateString) return '-';

        return new Date(dateString).toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    }
}

// 전역 변수로 인스턴스 생성
let popupManagement;

// DOM 로드 완료 후 초기화
document.addEventListener('DOMContentLoaded', async function () {
    popupManagement = new PopupManagement();
});