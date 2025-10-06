function pickSpaceId(space) {
    return space?.id ?? space?.spaceId ?? space?.space_id ?? null;
}

const IMG_PLACEHOLDER =
    'data:image/svg+xml;utf8,' +
    encodeURIComponent(
        `<svg xmlns="http://www.w3.org/2000/svg" width="160" height="120">
       <rect width="100%" height="100%" fill="#f2f2f2"/>
       <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
             fill="#888" font-size="14">no image</text>
     </svg>`
    );

class SpaceListManager {
    constructor() {
        this.allSpaces = [];
        this.currentSpaces = [];
        this.isSearchMode = false;
    }

    async initialize() {
        try {
            this.showLoading();
            const spaces = (await apiService.listSpaces()).content || await apiService.listSpaces();
            this.allSpaces = spaces;
            this.currentSpaces = spaces;
            this.renderSpaces(spaces);
            this.initializeSearch();
        } catch (error) {
            console.error('Space List page initialization failed:', error);
            this.showError('공간 목록을 불러오는데 실패했습니다.');
        }
    }

    // 검색 기능 초기화
    initializeSearch() {
        const searchInput = document.getElementById('searchKeyword');
        const searchBtn = document.getElementById('searchBtn');
        const resetBtn = document.getElementById('resetBtn');
        const applyFilterBtn = document.getElementById('applyFilter');
        const clearSearchBtn = document.getElementById('clearSearch');
        const resetSearchBtn = document.getElementById('resetSearch');

        searchBtn?.addEventListener('click', () => this.performSearch());
        searchInput?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.performSearch();
        });
        applyFilterBtn?.addEventListener('click', () => this.performSearch());
        resetBtn?.addEventListener('click', () => this.resetFilters());
        clearSearchBtn?.addEventListener('click', () => this.clearSearch());
        resetSearchBtn?.addEventListener('click', () => this.clearSearch());
    }

    // 검색 실행
    async performSearch() {
        const keyword = document.getElementById('searchKeyword')?.value?.trim();
        const location = document.getElementById('locationFilter')?.value?.trim();
        const minArea = document.getElementById('minArea')?.value;
        const maxArea = document.getElementById('maxArea')?.value;

        const hasSearchCondition = keyword || location || minArea || maxArea;
        if (!hasSearchCondition) {
            alert('검색 조건을 하나 이상 입력해주세요.');
            return;
        }

        try {
            this.showLoading();

            const searchParams = new URLSearchParams();
            if (keyword) searchParams.append('keyword', keyword);
            if (location) searchParams.append('location', location);
            if (minArea) searchParams.append('minArea', minArea);
            if (maxArea) searchParams.append('maxArea', maxArea);

            const searchResults = await apiService.get(`/spaces/search?${searchParams.toString()}`);
            this.currentSpaces = searchResults;
            this.isSearchMode = true;
            this.renderSpaces(searchResults);
            this.showSearchInfo(searchResults.length);
        } catch (error) {
            console.error('검색 실패:', error);
            this.showError('검색 중 오류가 발생했습니다.');
        }
    }

    // 필터 초기화
    resetFilters() {
        document.getElementById('searchKeyword').value = '';
        document.getElementById('locationFilter').value = '';
        document.getElementById('minArea').value = '';
        document.getElementById('maxArea').value = '';
    }

    // 검색 해제 (전체 목록으로 돌아가기)
    clearSearch() {
        this.resetFilters();
        this.isSearchMode = false;
        this.currentSpaces = this.allSpaces;
        this.renderSpaces(this.allSpaces);
        this.hideSearchInfo();
    }

    // 검색 정보 표시
    showSearchInfo(count) {
        const searchInfo = document.getElementById('searchInfo');
        const resultCount = document.getElementById('resultCount');
        if (searchInfo && resultCount) {
            resultCount.textContent = count;
            searchInfo.style.display = 'flex';
        }
    }

    // 검색 정보 숨기기
    hideSearchInfo() {
        const searchInfo = document.getElementById('searchInfo');
        if (searchInfo) searchInfo.style.display = 'none';
    }

    showLoading() {
        document.getElementById('loading').style.display = 'block';
        document.getElementById('spaceList').style.display = 'none';
        document.getElementById('emptyState').style.display = 'none';
        document.getElementById('noSearchResult').style.display = 'none';
    }

    hideLoading() {
        document.getElementById('loading').style.display = 'none';
    }

    renderSpaces(spaces) {
        this.hideLoading();

        const spaceListEl = document.getElementById('spaceList');
        const emptyStateEl = document.getElementById('emptyState');
        const noSearchResultEl = document.getElementById('noSearchResult');

        if (this.isSearchMode && (!spaces || spaces.length === 0)) {
            spaceListEl.style.display = 'none';
            emptyStateEl.style.display = 'none';
            noSearchResultEl.style.display = 'block';
            return;
        }

        if (!spaces || spaces.length === 0) {
            spaceListEl.style.display = 'none';
            emptyStateEl.style.display = 'block';
            noSearchResultEl.style.display = 'none';
            return;
        }

        spaceListEl.style.display = 'block';
        spaceListEl.innerHTML = '';
        spaces.forEach(space => {
            const spaceCard = this.createSpaceCard(space);
            spaceListEl.appendChild(spaceCard);
        });

        emptyStateEl.style.display = 'none';
        noSearchResultEl.style.display = 'none';
    }

    createSpaceCard(space) {
        const id = pickSpaceId(space);
        const card = document.createElement('div');
        card.className = 'space-card';

        const imageUrl = this.getThumbUrl(space);

        card.innerHTML = `
      <div class="space-title">${space.title || '(제목 없음)'}</div>
      
      <div class="space-body">
        <img class="thumb" src="${imageUrl}" alt="썸네일">
        <div class="space-details">
          <div>등록자: ${space.ownerName || '-'}</div>
          <div>임대료: ${this.formatRentalFee(space.rentalFee)}</div>
          <div>주소: ${space.address || '-'}</div>
          <div>면적: ${space.areaSize || '-'} m²</div>
          <div class="actions-inline">
            <button class="link" data-act="detail" data-id="${id}">상세정보</button>
            <button class="link" data-act="report" data-id="${id}">신고</button>
          </div>
        </div>
      </div>
      
      <div class="space-meta">
        <span>등록일: ${this.formatDate(space.createdAt)}</span>
        <div class="space-actions">
          ${space.mine ? `
            <button class="action-btn edit" data-act="edit" data-id="${id}">수정</button>
            <button class="action-btn delete" data-act="delete" data-id="${id}">삭제</button>
          ` : ''}
        </div>
      </div>
    `;

        // 이미지 에러 처리
        const imgEl = card.querySelector('.thumb');
        if (imgEl) {
            imgEl.onerror = function () {
                this.onerror = null;
                this.src = IMG_PLACEHOLDER;
            };
        }

        // 이벤트 처리
        card.addEventListener('click', (e) => {
            const btn = e.target.closest('[data-act]');
            if (!btn) return;

            const act = btn.getAttribute('data-act');
            const targetId = btn.getAttribute('data-id');

            switch (act) {
                case 'detail':
                    Pages.spaceDetail(targetId);
                    break;
                case 'edit':
                    Pages.spaceEdit(targetId);
                    break;
                case 'delete':
                    this.deleteSpace(targetId);
                    break;
                case 'inquire':
                    this.inquireSpace(targetId);
                    break;
                case 'report':
                    this.reportSpace(targetId);
                    break;
            }
        });

        return card;
    }

    getThumbUrl(space) {
        if (space.coverImageUrl) return `${window.location.origin}${space.coverImageUrl}`;
        if (space.coverImage) return `${window.location.origin}${space.coverImage}`;
        return IMG_PLACEHOLDER;
    }

    // API
    async deleteSpace(spaceId) {
        if (!confirm('정말로 이 공간을 삭제하시겠습니까?')) return;
        try {
            await apiService.deleteSpace(spaceId);
            alert('공간이 삭제되었습니다.');
            location.reload();
        } catch (error) {
            console.error('공간 삭제 실패:', error);
            alert('삭제에 실패했습니다.');
        }
    }

    async inquireSpace(spaceId) {
        try {
            await apiService.inquireSpace(spaceId);
        } catch (error) {
            console.error('문의 실패:', error);
            alert('문의 중 오류가 발생했습니다.');
        }
    }

    async reportSpace(spaceId) {
        try {
            await apiService.reportSpace(spaceId);
        } catch (error) {
            console.error('신고 실패:', error);
            alert('신고 중 오류가 발생했습니다.');
        }
    }

    // 메서드들
    formatDate(dateString) {
        if (!dateString) return '날짜 정보 없음';
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR');
    }

    formatRentalFee(amount) {
        if (!amount && amount !== 0) return '-';
        return `${amount} 원 / 일`;
    }

    showError(message) {
        this.hideLoading();
        const spaceListEl = document.getElementById('spaceList');
        const emptyStateEl = document.getElementById('emptyState');
        if (spaceListEl) {
            spaceListEl.style.display = 'block';
            spaceListEl.innerHTML = `<div class="error-state"><p>${message}</p></div>`;
        }
        if (emptyStateEl) emptyStateEl.style.display = 'none';
    }
}

window.SpaceListManager = SpaceListManager;
