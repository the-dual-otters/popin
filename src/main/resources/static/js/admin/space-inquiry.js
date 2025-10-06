// /js/admin/space-inquiry.js

class SpaceInquiryManager {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 20;
        this.currentStatus = '';
        this.currentSearch = '';
        this.targetType = 'SPACE';

        this.init();
    }

    init() {
        this.bindEvents();
        this.loadInquiries();
        this.loadStats();
    }

    bindEvents() {
        // 검색 버튼
        document.getElementById('searchBtn').addEventListener('click', () => {
            this.handleSearch();
        });

        // 초기화 버튼
        document.getElementById('resetBtn').addEventListener('click', () => {
            this.handleReset();
        });

        // 엔터키 검색
        document.getElementById('searchKeyword').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.handleSearch();
            }
        });

        // 상태 필터 변경
        document.getElementById('statusFilter').addEventListener('change', (e) => {
            this.currentStatus = e.target.value;
            this.currentPage = 0;
            this.loadInquiries();
        });

        // 모달 닫기
        document.getElementById('modalCloseBtn').addEventListener('click', () => {
            this.closeModal();
        });

        // 모달 외부 클릭 시 닫기
        document.getElementById('detailModal').addEventListener('click', (e) => {
            if (e.target.id === 'detailModal') {
                this.closeModal();
            }
        });
    }

    async loadStats() {
        try {
            const response = await fetch('/api/admin/inquiries/counts', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('통계 로딩 실패');
            }

            const data = await response.json();
            document.getElementById('totalCount').textContent = data.space || 0;
            document.getElementById('pendingCount').textContent = data.spacePending || 0;
        } catch (error) {
            console.error('통계 로딩 실패:', error);
            document.getElementById('totalCount').textContent = '0';
            document.getElementById('pendingCount').textContent = '0';
        }
    }

    async loadInquiries() {
        const tableContainer = document.getElementById('tableContainer');
        tableContainer.innerHTML = '<div class="loading">데이터를 불러오는 중...</div>';

        try {
            const params = new URLSearchParams({
                targetType: this.targetType,
                page: this.currentPage.toString(),
                size: this.pageSize.toString()
            });

            if (this.currentStatus) {
                params.append('status', this.currentStatus);
            }

            const response = await fetch(`/api/admin/inquiries?${params}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('데이터 로딩 실패');
            }

            const data = await response.json();
            this.renderTable(data.content);
            this.renderPagination(data);

        } catch (error) {
            console.error('신고 목록 로딩 실패:', error);
            tableContainer.innerHTML = '<div class="error-message">데이터를 불러오는데 실패했습니다.</div>';
        }
    }

    renderTable(inquiries) {
        const tableContainer = document.getElementById('tableContainer');

        if (!inquiries || inquiries.length === 0) {
            tableContainer.innerHTML = '<div class="no-data">신고가 없습니다.</div>';
            return;
        }

        const table = document.createElement('table');
        table.className = 'inquiry-table';

        // 테이블 헤더
        table.innerHTML = `
            <thead>
                <tr>
                    <th>신고일</th>
                    <th>신고자</th>
                    <th>대상 장소</th>
                    <th>제목</th>
                    <th>상태</th>
                    <th>관리</th>
                </tr>
            </thead>
            <tbody></tbody>
        `;

        const tbody = table.querySelector('tbody');

        inquiries.forEach(inquiry => {
            const row = document.createElement('tr');
            row.onclick = () => this.showDetail(inquiry.id);

            row.innerHTML = `
                <td>${this.formatDate(inquiry.createdAt)}</td>
                <td>${inquiry.email}</td>
                <td>${inquiry.targetTitle || '삭제된 장소'}</td>
                <td>${inquiry.inquiryType}</td>
                <td><span class="status-badge ${inquiry.status.toLowerCase()}">${this.getStatusText(inquiry.status)}</span></td>
                <td><button class="button button-sm button-primary" onclick="event.stopPropagation(); spaceInquiryManager.showDetail(${inquiry.id})">상세보기</button></td>
            `;

            tbody.appendChild(row);
        });

        tableContainer.innerHTML = '';
        tableContainer.appendChild(table);
    }

    renderPagination(pageData) {
        const paginationContainer = document.getElementById('pagination');
        paginationContainer.innerHTML = '';

        const totalPages = pageData.totalPages;
        const currentPage = pageData.number;

        // 이전 버튼
        const prevBtn = document.createElement('button');
        prevBtn.className = `page-button ${currentPage === 0 ? 'disabled' : ''}`;
        prevBtn.innerHTML = '이전';
        prevBtn.onclick = () => {
            if (currentPage > 0) {
                this.currentPage = currentPage - 1;
                this.loadInquiries();
            }
        };
        paginationContainer.appendChild(prevBtn);

        // 페이지 번호들
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.className = `page-button ${i === currentPage ? 'active' : ''}`;
            pageBtn.textContent = i + 1;
            pageBtn.onclick = () => {
                this.currentPage = i;
                this.loadInquiries();
            };
            paginationContainer.appendChild(pageBtn);
        }

        // 다음 버튼
        const nextBtn = document.createElement('button');
        nextBtn.className = `page-button ${currentPage >= totalPages - 1 ? 'disabled' : ''}`;
        nextBtn.innerHTML = '다음';
        nextBtn.onclick = () => {
            if (currentPage < totalPages - 1) {
                this.currentPage = currentPage + 1;
                this.loadInquiries();
            }
        };
        paginationContainer.appendChild(nextBtn);
    }

    async showDetail(inquiryId) {
        try {
            const response = await fetch(`/api/admin/inquiries/${inquiryId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('상세 정보 로딩 실패');
            }

            const inquiry = await response.json();
            this.renderDetailModal(inquiry);
            this.openModal();

        } catch (error) {
            console.error('상세 정보 로딩 실패:', error);
            alert('상세 정보를 불러오는데 실패했습니다.');
        }
    }

    renderDetailModal(inquiry) {
        const modalBody = document.getElementById('modalBody');

        modalBody.innerHTML = `
            <div class="detail-section">
                <h3>신고 정보</h3>
                <div class="detail-info">
                    <div class="info-item">
                        <span class="info-label">신고 ID</span>
                        <span class="info-value">${inquiry.id}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">신고일</span>
                        <span class="info-value">${this.formatDate(inquiry.createdAt)}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">신고자</span>
                        <span class="info-value">${inquiry.email || 'N/A'}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">현재 상태</span>
                        <span class="info-value">
                            <span class="status-badge ${inquiry.status.toLowerCase()}">${this.getStatusText(inquiry.status)}</span>
                        </span>
                    </div>
                </div>
            </div>

            <div class="detail-section">
                <h3>대상 정보</h3>
                <div class="detail-info">
                    <div class="info-item">
                        <span class="info-label">장소명</span>
                        <span class="info-value">${inquiry.targetTitle || '삭제된 장소'}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">장소 ID</span>
                        <span class="info-value">${inquiry.targetId}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">신고 유형</span>
                        <span class="info-value">${inquiry.inquiryType}</span>
                    </div>
                </div>
            </div>

            <div class="detail-section">
                <h3>신고 내용</h3>
                <div class="content-area">${inquiry.content || '내용이 없습니다.'}</div>
            </div>

            <div class="status-update-section">
                <h4>상태 변경</h4>
                <div class="status-select-group">
                    <label for="statusSelect">새 상태:</label>
                    <select id="statusSelect">
                        <option value="OPEN" ${inquiry.status === 'OPEN' ? 'selected' : ''}>대기중</option>
                        <option value="IN_PROGRESS" ${inquiry.status === 'IN_PROGRESS' ? 'selected' : ''}>처리중</option>
                        <option value="CLOSED" ${inquiry.status === 'CLOSED' ? 'selected' : ''}>완료</option>
                    </select>
                </div>
            </div>

            <div class="action-buttons">
                <button class="button button-success" onclick="spaceInquiryManager.updateStatus(${inquiry.id})">상태 변경</button>
                <button class="button button-secondary" onclick="spaceInquiryManager.closeModal()">닫기</button>
            </div>
        `;
    }

    async updateStatus(inquiryId) {
        const statusSelect = document.getElementById('statusSelect');
        const newStatus = statusSelect.value;

        try {
            const response = await fetch(`/api/admin/inquiries/${inquiryId}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getToken()}`
                },
                body: JSON.stringify({ status: newStatus })
            });

            if (!response.ok) {
                throw new Error('상태 변경 실패');
            }

            alert('상태가 변경되었습니다.');
            this.closeModal();
            this.loadInquiries();
            this.loadStats();

        } catch (error) {
            console.error('상태 변경 실패:', error);
            alert('상태 변경에 실패했습니다.');
        }
    }

    handleSearch() {
        const keyword = document.getElementById('searchKeyword').value.trim();
        this.currentSearch = keyword;
        this.currentPage = 0;

        if (keyword) {
            this.searchByEmail(keyword);
        } else {
            this.loadInquiries();
        }
    }

    async searchByEmail(email) {
        const tableContainer = document.getElementById('tableContainer');
        tableContainer.innerHTML = '<div class="loading">검색 중...</div>';

        try {
            const params = new URLSearchParams({
                email: email,
                page: this.currentPage.toString(),
                size: this.pageSize.toString()
            });

            const response = await fetch(`/api/admin/inquiries/by-email?${params}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('검색 실패');
            }

            const data = await response.json();
            this.renderTable(data.content);
            this.renderPagination(data);

        } catch (error) {
            console.error('이메일 검색 실패:', error);
            tableContainer.innerHTML = '<div class="error-message">검색에 실패했습니다.</div>';
        }
    }

    handleReset() {
        document.getElementById('statusFilter').value = '';
        document.getElementById('searchKeyword').value = '';
        this.currentStatus = '';
        this.currentSearch = '';
        this.currentPage = 0;
        this.loadInquiries();
    }

    openModal() {
        document.getElementById('detailModal').style.display = 'block';
    }

    closeModal() {
        document.getElementById('detailModal').style.display = 'none';
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR') + ' ' + date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
    }

    getStatusText(status) {
        const statusMap = {
            'OPEN': '대기중',
            'IN_PROGRESS': '처리중',
            'CLOSED': '완료'
        };
        return statusMap[status] || status;
    }

    getToken() {
        return localStorage.getItem('accessToken') || '';
    }
}

// 뒤로가기 함수
function goBack() {
    window.history.back();
}

// 전역 변수로 인스턴스 생성
let spaceInquiryManager;

// DOM이 로드되면 초기화
document.addEventListener('DOMContentLoaded', function() {
    spaceInquiryManager = new SpaceInquiryManager();
});