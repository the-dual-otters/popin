/**
 * 회원 관리 UI 클래스 - popup-management와 정확히 동일한 구조로 수정
 */
class UserManagementUI {
    constructor() {
        this.elements = {
            searchType: document.getElementById('searchType'),
            searchKeyword: document.getElementById('searchKeyword'),
            roleFilter: document.getElementById('roleFilter'),
            searchBtn: document.getElementById('searchBtn'),
            resetBtn: document.getElementById('resetBtn'),
            userTableBody: document.getElementById('userTableBody'),
            totalCount: document.getElementById('totalCount'),
            pagination: document.getElementById('pagination'),
            searchLoading: document.getElementById('searchLoading'),
            noResults: document.getElementById('noResults'),
            searchResults: document.querySelector('.search-results'),
            // popup-management와 동일한 모달 구조
            detailModal: document.getElementById('detailModal'),
            modalBody: document.getElementById('modalBody'),
            modalCloseBtn: document.getElementById('modalCloseBtn'),
            closeModalBtn: document.getElementById('closeModalBtn'),
            statusSelectModal: document.getElementById('statusSelectModal'),
            upgradeRequestCount: document.getElementById('upgradeRequestCount')
        };

        // 선택된 사용자 ID (popup-management와 동일한 패턴)
        this.selectedUserId = null;

        // 모든 모달을 확실히 닫아놓기
        this.initializeModals();
    }

    /**
     * 모달들을 초기 상태로 설정
     */
    initializeModals() {
        // 모든 모달이 처음에는 숨겨져 있도록 확실히 설정
        if (this.elements.detailModal) {
            this.elements.detailModal.style.display = 'none';
        }
        if (this.elements.statusSelectModal) {
            this.elements.statusSelectModal.style.display = 'none';
        }

        console.log('모달 초기화 완료');
    }

    /**
     * 검색 결과 표시
     */
    displaySearchResults(data) {
        this.elements.totalCount.textContent = data.totalElements;

        if (data.content && data.content.length > 0) {
            console.log('사용자 데이터 구조:', data.content[0]);

            this.elements.userTableBody.innerHTML = data.content.map(user => {
                const userId = user.id || user.userId || user.userNumber || user.seq;
                console.log('사용자:', user.name, 'ID:', userId);

                return `
                <tr>
                    <td>${this.escapeHtml(user.name)}</td>
                    <td>${this.escapeHtml(user.nickname)}</td>
                    <td>${this.escapeHtml(user.email)}</td>
                    <td>${this.escapeHtml(user.phone || '-')}</td>
                    <td><span class="role-badge ${user.role.toLowerCase()}">${this.getRoleText(user.role)}</span></td>
                    <td>${this.formatDate(user.createdAt)}</td>
                    <td><span class="status-badge ${user.status === 'ACTIVE' ? 'active' : 'inactive'}">${user.status === 'ACTIVE' ? '활성' : '비활성'}</span></td>
                    <td>
                        <button type="button" class="button button-sm button-primary detail-button" 
                                data-user-id="${userId}" 
                                onclick="userController.viewDetail('${userId}');">
                            상세보기
                        </button>
                    </td>
                </tr>
            `;
            }).join('');

            this.showSearchResults();
            console.log('테이블 렌더링 완료, 사용자 수:', data.content.length);
        } else {
            this.showNoResults();
        }
    }

    /**
     * 사용자 상세보기 모달 표시 - popup-management의 viewDetail과 정확히 동일한 구조
     */
    viewDetail(userId) {
        this.selectedUserId = userId;
        // 여기서는 컨트롤러가 API 호출 후 renderDetailModal을 호출함
    }

    /**
     * 상세 모달 렌더링 - popup-management의 renderDetailModal과 정확히 동일한 구조
     */
    renderDetailModal(user) {
        console.log('사용자 상세 모달 표시:', user);

        const isAdmin = user.role === 'ADMIN';
        const userId = user.id || user.userId || user.userNumber || user.seq;

        // popup-management의 renderDetailModal과 정확히 동일한 구조
        this.elements.modalBody.innerHTML = `
            <div class="detail-info">
                <div class="detail-section">
                    <h3>기본 정보</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <span class="detail-label">이름:</span>
                            <span class="detail-value">${this.escapeHtml(user.name)}</span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">닉네임:</span>
                            <span class="detail-value">${this.escapeHtml(user.nickname)}</span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">이메일:</span>
                            <span class="detail-value">${this.escapeHtml(user.email)}</span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">전화번호:</span>
                            <span class="detail-value">${this.escapeHtml(user.phone || '-')}</span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">역할:</span>
                            <span class="detail-value">
                                <span class="role-badge ${user.role.toLowerCase()}">${this.getRoleText(user.role)}</span>
                            </span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">상태:</span>
                            <span class="detail-value">
                                <span class="status-badge ${user.status === 'ACTIVE' ? 'active' : 'inactive'}">${user.status === 'ACTIVE' ? '활성' : '비활성'}</span>
                            </span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">가입일:</span>
                            <span class="detail-value">${this.formatDate(user.createdAt)}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // popup-management와 동일한 방식으로 액션 버튼 업데이트
        this.updateActionButtons(user.status, isAdmin, userId);

        // 모달 표시
        this.elements.detailModal.style.display = 'block';
    }

    /**
     * popup-management의 updateActionButtons와 정확히 동일한 방식
     */
    updateActionButtons(status, isAdmin, userId) {
        const buttons = {
            changeStatusBtn: document.getElementById('changeStatusBtn'),
            closeModalBtn: document.getElementById('closeModalBtn')
        };

        // 모든 버튼 숨김
        Object.values(buttons).forEach(btn => {
            if (btn) btn.style.display = 'none';
        });

        // 관리자가 아닌 경우에만 상태 변경 버튼 표시
        if (!isAdmin && buttons.changeStatusBtn) {
            buttons.changeStatusBtn.style.display = 'block';
        }

        // 닫기 버튼은 항상 표시
        if (buttons.closeModalBtn) {
            buttons.closeModalBtn.style.display = 'block';
        }
    }

    /**
     * 상태 변경 모달 열기 - popup-management의 openStatusModal과 동일
     */
    openStatusModal() {
        document.getElementById('statusSelectModal').style.display = 'block';
        this.populateStatusOptions();
    }

    /**
     * 상태 옵션 채우기 - popup-management의 populateStatusOptions과 유사
     */
    populateStatusOptions() {
        const statusSelect = document.getElementById('newStatusSelect');
        if (!statusSelect) return;

        // 사용자 상태 옵션 (ACTIVE/INACTIVE만)
        const statusOptions = [
            { value: 'ACTIVE', text: '활성' },
            { value: 'INACTIVE', text: '비활성' }
        ];

        statusSelect.innerHTML = '<option value="">상태를 선택하세요</option>';
        statusOptions.forEach(option => {
            statusSelect.innerHTML += `<option value="${option.value}">${option.text}</option>`;
        });
    }

    /**
     * 상태 변경 모달 닫기 - popup-management의 closeStatusModal과 동일
     */
    closeStatusModal() {
        document.getElementById('statusSelectModal').style.display = 'none';
        document.getElementById('newStatusSelect').value = '';
    }

    /**
     * 페이징 설정
     */
    setupPagination(data, currentPage, onPageClick) {
        const totalPages = data.totalPages;

        if (totalPages <= 1) {
            this.elements.pagination.innerHTML = '';
            return;
        }

        let paginationHtml = '';

        // 이전 페이지
        if (currentPage > 1) {
            paginationHtml += `<button type="button" class="page-button" data-page="${currentPage - 1}">이전</button>`;
        }

        // 페이지 번호들
        const startPage = Math.max(1, currentPage - 2);
        const endPage = Math.min(totalPages, currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            paginationHtml += `<button type="button" class="page-button ${i === currentPage ? 'active' : ''}" data-page="${i}">${i}</button>`;
        }

        // 다음 페이지
        if (currentPage < totalPages) {
            paginationHtml += `<button type="button" class="page-button" data-page="${currentPage + 1}">다음</button>`;
        }

        this.elements.pagination.innerHTML = paginationHtml;

        // 페이지 버튼 클릭 이벤트
        this.elements.pagination.addEventListener('click', (e) => {
            if (e.target.classList.contains('page-button')) {
                const page = parseInt(e.target.dataset.page);
                if (page && page !== currentPage) {
                    onPageClick(page);
                }
            }
        });
    }

    /**
     * 모달 닫기 - popup-management의 closeModal과 동일
     */
    closeModal() {
        console.log('모달 닫기 실행');
        this.elements.detailModal.style.display = 'none';
        this.selectedUserId = null;
    }

    /**
     * 로딩 표시
     */
    showLoading() {
        this.elements.searchLoading.style.display = 'block';
        this.elements.noResults.style.display = 'none';
        this.elements.searchResults.style.display = 'none';
    }

    /**
     * 로딩 숨김
     */
    hideLoading() {
        this.elements.searchLoading.style.display = 'none';
    }

    /**
     * 검색 결과 표시
     */
    showSearchResults() {
        this.elements.searchResults.style.display = 'block';
        this.elements.noResults.style.display = 'none';
    }

    /**
     * 결과 없음 표시
     */
    showNoResults() {
        this.elements.noResults.style.display = 'block';
        this.elements.searchResults.style.display = 'none';
    }

    /**
     * 검색 폼 리셋
     */
    resetSearchForm() {
        this.elements.searchType.value = '';
        this.elements.searchKeyword.value = '';
        this.elements.roleFilter.value = '';
    }

    /**
     * 계정 전환 요청 수 업데이트
     */
    updateUpgradeRequestCount(count) {
        if (this.elements.upgradeRequestCount) {
            this.elements.upgradeRequestCount.textContent = count;
        }
    }

    /**
     * 에러 메시지 표시
     */
    showError(message) {
        alert(message);
    }

    /**
     * 검색 파라미터 가져오기
     */
    getSearchParams() {
        return {
            searchType: this.elements.searchType.value,
            keyword: this.elements.searchKeyword.value.trim(),
            role: this.elements.roleFilter.value
        };
    }

    /**
     * 상태 텍스트 반환 - popup-management의 getStatusText와 유사
     */
    getStatusText(status) {
        const statusMap = {
            'ACTIVE': '활성',
            'INACTIVE': '비활성'
        };
        return statusMap[status] || status;
    }

    // 유틸리티 메서드들
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    getRoleText(role) {
        const roleTexts = {
            'USER': '일반사용자',
            'PROVIDER': '제공자',
            'HOST': '호스트',
            'ADMIN': '관리자'
        };
        return roleTexts[role] || role;
    }

    formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR') + ' ' + date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
    }
}