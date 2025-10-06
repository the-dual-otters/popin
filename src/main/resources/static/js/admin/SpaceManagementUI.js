/**
 * 장소 관리 UI 클래스
 */
class SpaceManagementUI {
    constructor() {
        this.elements = {
            // 통계
            totalSpaces: document.getElementById('totalSpaces'),
            publicSpaces: document.getElementById('publicSpaces'),
            privateSpaces: document.getElementById('privateSpaces'),

            ownerFilter: document.getElementById('ownerFilter'),
            titleFilter: document.getElementById('titleFilter'),
            isHiddenFilter: document.getElementById('isHiddenFilter'),  // ★ 변경된 부분
            searchBtn: document.getElementById('searchBtn'),
            resetBtn: document.getElementById('resetBtn'),

            // 테이블
            spacesTableBody: document.getElementById('spacesTableBody'),
            pagination: document.getElementById('pagination'),
            searchLoading: document.getElementById('searchLoading'),
            noResults: document.getElementById('noResults'),

            // 모달
            spaceDetailModal: document.getElementById('spaceDetailModal'),
            spaceDetailContent: document.getElementById('spaceDetailContent'),
            closeModal: document.getElementById('closeModal'),

            confirmModal: document.getElementById('confirmModal'),
            confirmTitle: document.getElementById('confirmTitle'),
            confirmMessage: document.getElementById('confirmMessage'),
            confirmBtn: document.getElementById('confirmBtn'),
            cancelBtn: document.getElementById('cancelBtn'),
            confirmModalClose: document.getElementById('confirmModalClose')
        };

        this.currentAction = null;
        this.currentSpaceId = null;

        console.log('🎨 SpaceManagementUI 초기화 완료');
        console.log('📋 Elements:', this.elements);
    }

    /**
     * 통계 업데이트
     */
    updateStats(stats) {
        console.log('📊 통계 업데이트:', stats);
        if (this.elements.totalSpaces) {
            this.elements.totalSpaces.textContent = stats.totalSpaces || 0;
        }
        if (this.elements.publicSpaces) {
            this.elements.publicSpaces.textContent = stats.publicSpaces || 0;
        }
        if (this.elements.privateSpaces) {
            this.elements.privateSpaces.textContent = stats.privateSpaces || 0;
        }
    }

    /**
     * 장소 테이블 렌더링
     */
    renderSpacesTable(spacesData) {
        console.log('🏠 장소 테이블 렌더링 시작:', spacesData);

        const tbody = this.elements.spacesTableBody;
        if (!tbody) {
            console.error('❌ spacesTableBody 요소를 찾을 수 없습니다');
            return;
        }

        // 데이터 검증
        if (!spacesData || !spacesData.content || spacesData.content.length === 0) {
            console.log('🔭 데이터가 없어서 "결과 없음" 표시');
            this.showNoResults();
            return;
        }

        console.log(`✅ ${spacesData.content.length}개의 장소 데이터 렌더링`);
        this.hideNoResults();

        tbody.innerHTML = '';

        spacesData.content.forEach((space, index) => {
            console.log(`🏠 장소 ${index + 1}:`, space);
            const row = this.createSpaceRow(space);
            tbody.appendChild(row);
        });

        // 페이지네이션 렌더링
        this.renderPagination(spacesData);
    }

    /**
     * 장소 행 생성 - ★ 버튼 로직 수정
     */
    createSpaceRow(space) {
        const row = document.createElement('tr');

        const formatDate = (dateString) => {
            if (!dateString) return '-';
            return new Date(dateString).toLocaleDateString('ko-KR');
        };

        const formatPrice = (price) => {
            if (!price) return '-';
            return new Intl.NumberFormat('ko-KR').format(price) + '원';
        };

        const getOwnerName = (space) => {
            return space.ownerName || '-';
        };

        const getLocation = (space) => {
            return space.address || '-';
        };

        const getStatusBadge = (space) => {
            const isHidden = space.isHidden || false;
            return {
                class: isHidden ? 'private' : 'public',
                text: isHidden ? '숨김' : '공개'
            };
        };

        const status = getStatusBadge(space);

        row.innerHTML = `
        <td>${space.id}</td>
        <td>${space.title || '-'}</td>
        <td>${getOwnerName(space)}</td>
        <td>${getLocation(space)}</td>
        <td>${formatPrice(space.rentalFee)}</td>
        <td>
            <span class="status-badge ${status.class}">
                ${status.text}
            </span>
        </td>
        <td>${formatDate(space.createdAt)}</td>
        <td>
            <div class="action-buttons">
                <button class="button button-sm button-primary detail-button" data-space-id="${space.id}">
                    상세보기
                </button>
                <button class="button button-success toggle-visibility-button" 
                        data-space-id="${space.id}" data-is-hidden="${space.isHidden}">
                    ${space.isHidden ? '활성화' : '비활성화'}
                </button>
            </div>
        </td>
    `;

        return row;
    }

    /**
     * 페이지네이션 렌더링
     */
    renderPagination(data) {
        const pagination = this.elements.pagination;
        if (!pagination) return;

        const { number: currentPage, totalPages, first, last } = data;

        if (totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        let paginationHTML = '';

        // 이전 페이지 버튼
        if (!first) {
            paginationHTML += `
                <button onclick="spaceManagementController.loadPage(${currentPage - 1})" ${first ? 'disabled' : ''}>
                    이전
                </button>
            `;
        }

        // 페이지 번호들
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <button onclick="spaceManagementController.loadPage(${i})" 
                        ${i === currentPage ? 'class="active"' : ''}>
                    ${i + 1}
                </button>
            `;
        }

        // 다음 페이지 버튼
        if (!last) {
            paginationHTML += `
                <button onclick="spaceManagementController.loadPage(${currentPage + 1})" ${last ? 'disabled' : ''}>
                    다음
                </button>
            `;
        }

        pagination.innerHTML = paginationHTML;
    }

    /**
     * 장소 상세 정보 표시 - ★ isHidden 로직 변경
     */
    showSpaceDetail(space) {
        console.log('🔍 장소 상세 정보 표시:', space);

        const modal = this.elements.spaceDetailModal;
        const content = this.elements.spaceDetailContent;

        if (!modal || !content) {
            console.error('모달 요소를 찾을 수 없습니다');
            return;
        }

        const formatDate = (dateString) => {
            if (!dateString) return '-';
            return new Date(dateString).toLocaleDateString('ko-KR');
        };

        const formatPrice = (price) => {
            if (!price) return '-';
            return new Intl.NumberFormat('ko-KR').format(price) + '원';
        };

        const statusText = space.isHidden ? '숨김' : '공개';
        const statusClass = space.isHidden ? 'private' : 'public';

        content.innerHTML = `
            <div class="space-detail">
                <div class="detail-section">
                    <h4>기본 정보</h4>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>장소명:</label>
                            <span>${space.title || '-'}</span>
                        </div>
                        <div class="detail-item">
                            <label>소유자:</label>
                            <span>${space.owner?.name || '-'} (${space.owner?.email || '-'})</span>
                        </div>
                        <div class="detail-item">
                            <label>면적:</label>
                            <span>${space.areaSize || '-'}㎡</span>
                        </div>
                        <div class="detail-item">
                            <label>임대료:</label>
                            <span>${formatPrice(space.rentalFee)}</span>
                        </div>
                        <div class="detail-item">
                            <label>연락처:</label>
                            <span>${space.contactPhone || '-'}</span>
                        </div>
                        <div class="detail-item">
                            <label>상태:</label>
                            <span class="status-badge ${statusClass}">${statusText}</span>
                        </div>
                    </div>
                </div>

                <div class="detail-section">
                    <h4>위치 정보</h4>
                    <div class="detail-item">
                        <label>주소:</label>
                        <span>${space.address || '-'}</span>
                    </div>
                </div>

                <div class="detail-section">
                    <h4>운영 기간</h4>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>시작일:</label>
                            <span>${formatDate(space.startDate)}</span>
                        </div>
                        <div class="detail-item">
                            <label>종료일:</label>
                            <span>${formatDate(space.endDate)}</span>
                        </div>
                    </div>
                </div>

                <div class="detail-section">
                    <h4>설명</h4>
                    <div class="detail-description">
                        ${space.description || '설명이 없습니다.'}
                    </div>
                </div>

                <div class="detail-section">
                    <h4>등록 정보</h4>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>등록일:</label>
                            <span>${formatDate(space.createdAt)}</span>
                        </div>
                        <div class="detail-item">
                            <label>수정일:</label>
                            <span>${formatDate(space.updatedAt)}</span>
                        </div>
                    </div>
                </div>

                ${space.coverImageUrl ? `
                <div class="detail-section">
                    <h4>대표 이미지</h4>
                    <img src="${space.coverImageUrl}" alt="장소 이미지" style="max-width: 100%; height: auto;">
                </div>
                ` : ''}
            </div>
        `;

        modal.style.display = 'flex';
    }

    /**
     * 로딩 표시
     */
    showLoading(show = true) {
        const loading = this.elements.searchLoading;
        const tableContainer = document.getElementById('tableContainer');

        if (loading) {
            loading.style.display = show ? 'block' : 'none';
        }

        if (tableContainer) {
            const table = tableContainer.querySelector('.data-table');
            if (table) {
                table.style.display = show ? 'none' : 'table';
            }
        }
    }

    /**
     * 결과 없음 표시
     */
    showNoResults() {
        if (this.elements.noResults) {
            this.elements.noResults.style.display = 'block';
        }
        const tableContainer = document.getElementById('tableContainer');
        if (tableContainer) {
            const table = tableContainer.querySelector('.data-table');
            if (table) {
                table.style.display = 'none';
            }
        }
    }

    /**
     * 결과 없음 숨기기
     */
    hideNoResults() {
        if (this.elements.noResults) {
            this.elements.noResults.style.display = 'none';
        }
        const tableContainer = document.getElementById('tableContainer');
        if (tableContainer) {
            const table = tableContainer.querySelector('.data-table');
            if (table) {
                table.style.display = 'table';
            }
        }
    }

    /**
     * 확인 대화상자 표시
     */
    showConfirm(title, message, action) {
        const modal = this.elements.confirmModal;
        const titleEl = this.elements.confirmTitle;
        const messageEl = this.elements.confirmMessage;

        if (titleEl) titleEl.textContent = title;
        if (messageEl) messageEl.textContent = message;

        this.currentAction = action;
        if (modal) modal.style.display = 'flex';
    }

    /**
     * 성공 메시지 표시
     */
    showSuccess(message) {
        alert(message); // 간단한 구현
    }

    /**
     * 에러 메시지 표시
     */
    showError(message) {
        alert(message); // 간단한 구현
    }

    /**
     * 모달 닫기
     */
    closeModal() {
        [this.elements.spaceDetailModal, this.elements.confirmModal].forEach(modal => {
            if (modal) modal.style.display = 'none';
        });
        this.currentAction = null;
        this.currentSpaceId = null;
    }

    /**
     * 확인된 액션 실행
     */
    executeConfirmedAction() {
        if (this.currentAction && typeof this.currentAction === 'function') {
            this.currentAction();
        }
        this.closeModal();
    }
}