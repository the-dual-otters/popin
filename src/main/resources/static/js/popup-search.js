// 팝업 검색 페이지 전용 모듈 (개선된 버전)
class PopupSearchManager {
    constructor() {
        this.searchInput = null;
        this.searchButton = null;
        this.relatedSearches = null;
        this.searchResults = null;
        this.searchLoading = null;

        // 상태 관리
        this.state = {
            currentQuery: '',
            isSearching: false,
            selectedIndex: -1,
            isLoadingSuggestions: false,
            isShowingAlert: false,
            isKeyboardNavigation: false,
            searchJustCompleted: false
        };

        // 자동완성 관련
        this.autocompleteItems = [];
        this.debounceTimeout = null;
        this.autocompleteCache = new Map();

        // 상수
        this.MIN_SEARCH_LENGTH = 2;
        this.MIN_AUTOCOMPLETE_LENGTH = 1;
        this.DEBOUNCE_DELAY = 300;
        this.MAX_CACHE_SIZE = 50;
    }

    // 페이지 초기화
    async initialize() {
        try {
            if (!this.checkExistingHTML()) {
                await this.renderHTML();
            }

            this.setupElements();
            this.setupEventListeners();
            this.hideAllResults();
        } catch (error) {
            console.error('팝업 검색 페이지 초기화 실패:', error);
            this.showError('페이지를 불러오는 중 오류가 발생했습니다.');
        }
    }

    // 기존 HTML 확인
    checkExistingHTML() {
        const searchInput = document.getElementById('popup-search-input');
        const searchContainer = document.querySelector('.popup-search-container');
        return searchInput && searchContainer;
    }

    // HTML 렌더링
    async renderHTML() {
        try {
            const html = await TemplateLoader.load('pages/popup-search');
            document.getElementById('main-content').innerHTML = html;
        } catch (error) {
            console.warn('템플릿 로드 실패, 폴백 HTML 사용:', error);
            document.getElementById('main-content').innerHTML = `
                <div class="popup-search-container">
                    <div class="search-area">
                        <div class="search-input-wrapper">
                            <input type="text" id="popup-search-input" class="search-input" 
                                   placeholder="검색어를 입력하세요" autocomplete="off">
                            <button class="search-button" id="popup-search-button">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <path d="m21 21-4.35-4.35"></path>
                                </svg>
                            </button>
                        </div>
                        <div class="related-searches" id="popup-related-searches"></div>
                    </div>
                    <div id="popup-search-results" class="search-results"></div>
                    <div id="popup-search-loading" class="loading-container" style="display: none;">
                        <div class="loading"></div>
                    </div>
                </div>`;
        }
    }

    // DOM 요소 설정
    setupElements() {
        this.searchInput = document.getElementById('popup-search-input');
        this.searchButton = document.getElementById('popup-search-button');
        this.relatedSearches = document.getElementById('popup-related-searches');
        this.searchResults = document.getElementById('popup-search-results');
        this.searchLoading = document.getElementById('popup-search-loading');

        if (!this.searchInput || !this.searchButton) {
            throw new Error('필수 DOM 요소를 찾을 수 없습니다.');
        }
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        // 검색 버튼 클릭
        this.searchButton.addEventListener('click', () => {
            if (!this.state.isShowingAlert) {
                this.performSearch();
            }
        });

        // 키보드 이벤트
        this.searchInput.addEventListener('keydown', this.handleKeyDown.bind(this));

        // 입력 이벤트 (디바운싱)
        this.searchInput.addEventListener('input', this.handleInput.bind(this));

        // 포커스 이벤트
        this.searchInput.addEventListener('focus', () => {
            const query = this.searchInput.value.trim();
            if (query.length >= this.MIN_AUTOCOMPLETE_LENGTH) {
                this.loadAutocompleteSuggestions(query);
            }
        });

        // 블러 이벤트
        this.searchInput.addEventListener('blur', (e) => {
            setTimeout(() => {
                if (!e.relatedTarget || !e.relatedTarget.closest('.related-searches')) {
                    this.hideAutocomplete();
                }
            }, 150);
        });

        // 외부 클릭 시 자동완성 숨김
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.search-area')) {
                this.hideAutocomplete();
            }
        });

        // 자동완성 항목 클릭
        if (this.relatedSearches) {
            this.relatedSearches.addEventListener('click', (e) => {
                if (this.state.isShowingAlert) return;

                const item = e.target.closest('.autocomplete-item');
                if (item) {
                    const suggestionText = item.dataset.suggestion;
                    this.searchInput.value = suggestionText;
                    this.hideAutocomplete();
                    this.performSearchFromAutocomplete(suggestionText);
                }
            });
        }

        // 검색 결과 클릭
        if (this.searchResults) {
            this.searchResults.addEventListener('click', (e) => {
                const card = e.target.closest('.popup-card');
                if (card) {
                    const popupId = card.dataset.popupId;
                    if (popupId) goToPopupDetail(popupId);
                }
            });
        }
    }

    // 입력 처리
    handleInput() {
        // 키보드 네비게이션 중이거나 검색 완료 직후에는 자동완성 로드 안함
        if (this.state.isKeyboardNavigation || this.state.searchJustCompleted) return;

        clearTimeout(this.debounceTimeout);
        this.debounceTimeout = setTimeout(() => {
            const query = this.searchInput.value.trim();

            // 검색 중이면 자동완성 로드 안함
            if (this.state.isSearching) return;

            if (query && query.length >= this.MIN_AUTOCOMPLETE_LENGTH) {
                this.loadAutocompleteSuggestions(query);
            } else {
                this.hideAutocomplete();
                this.hideSearchResults();
            }
        }, this.DEBOUNCE_DELAY);
    }

    // 자동완성 제안 로드
    async loadAutocompleteSuggestions(query) {
        if (this.state.isLoadingSuggestions) return;

        try {
            this.state.isLoadingSuggestions = true;

            // 캐시 확인
            if (this.autocompleteCache.has(query)) {
                const cachedSuggestions = this.autocompleteCache.get(query);
                this.displayAutocompleteSuggestions(cachedSuggestions, query);
                return;
            }

            // API 호출
            const response = await apiService.getAutocompleteSuggestions(query);

            // 응답 구조 확인 및 처리
            const suggestions = response?.suggestions || [];

            // 캐시 저장
            if (this.autocompleteCache.size >= this.MAX_CACHE_SIZE) {
                const firstKey = this.autocompleteCache.keys().next().value;
                this.autocompleteCache.delete(firstKey);
            }
            this.autocompleteCache.set(query, suggestions);

            // 결과 표시
            this.displayAutocompleteSuggestions(suggestions, query);

        } catch (error) {
            console.error('자동완성 제안 로드 실패:', error);
            this.hideAutocomplete();
        } finally {
            this.state.isLoadingSuggestions = false;
        }
    }

    // 자동완성 제안 표시
    displayAutocompleteSuggestions(suggestions, query) {
        if (!this.relatedSearches || !suggestions || suggestions.length === 0) {
            this.hideAutocomplete();
            return;
        }

        this.searchInput.closest('.search-input-wrapper').classList.add('autocomplete-active');
        this.searchInput.closest('.search-area').classList.add('active');

        this.relatedSearches.innerHTML = suggestions.map(suggestion => {
            const text = typeof suggestion === 'string' ? suggestion : suggestion.text;
            return `
                <div class="autocomplete-item" data-suggestion="${this.escapeHtml(text)}">
                    <svg class="autocomplete-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="11" cy="11" r="8"></circle>
                        <path d="m21 21-4.35-4.35"></path>
                    </svg>
                    <div class="autocomplete-text">${this.escapeHtml(text)}</div>
                </div>`;
        }).join('');

        this.autocompleteItems = this.relatedSearches.querySelectorAll('.autocomplete-item');
        this.relatedSearches.classList.add('show');

        // 키보드 네비게이션 중이 아닐 때만 selectedIndex 리셋
        if (!this.state.isKeyboardNavigation) {
            this.state.selectedIndex = -1;
        }
    }

    // 키보드 이벤트 처리
    handleKeyDown(e) {
        if (this.state.isShowingAlert) return;

        const isAutocompleteVisible = this.relatedSearches && this.relatedSearches.classList.contains('show');

        switch (e.key) {
            case 'Enter':
                e.preventDefault();
                if (this.state.selectedIndex > -1 && this.autocompleteItems[this.state.selectedIndex]) {
                    // 자동완성에서 선택한 경우
                    const suggestion = this.autocompleteItems[this.state.selectedIndex].dataset.suggestion;
                    this.searchInput.value = suggestion;
                    this.clearAutocompleteState();
                    this.performSearchFromAutocomplete(suggestion);
                } else {
                    // 직접 입력한 경우
                    this.clearAutocompleteState();
                    this.performSearch();
                }
                break;
            case 'ArrowDown':
                if (isAutocompleteVisible && this.autocompleteItems.length > 0) {
                    e.preventDefault();
                    this.navigateAutocomplete(1);
                }
                break;
            case 'ArrowUp':
                if (isAutocompleteVisible && this.autocompleteItems.length > 0) {
                    e.preventDefault();
                    this.navigateAutocomplete(-1);
                }
                break;
            case 'Escape':
                this.clearAutocompleteState();
                break;
        }
    }

    // 자동완성 네비게이션
    navigateAutocomplete(direction) {
        if (this.autocompleteItems.length === 0) return;

        this.state.isKeyboardNavigation = true;

        // 기존 선택 제거
        if (this.state.selectedIndex >= 0) {
            this.autocompleteItems[this.state.selectedIndex]?.classList.remove('selected');
        }

        // 새로운 인덱스 계산
        if (this.state.selectedIndex === -1) {
            // 처음 선택하는 경우
            this.state.selectedIndex = direction === 1 ? 0 : this.autocompleteItems.length - 1;
        } else {
            // 다음/이전 항목으로 이동
            this.state.selectedIndex = (this.state.selectedIndex + direction + this.autocompleteItems.length) % this.autocompleteItems.length;
        }

        // 새로운 선택 적용
        this.autocompleteItems[this.state.selectedIndex].classList.add('selected');
        this.autocompleteItems[this.state.selectedIndex].scrollIntoView({ block: 'nearest' });

        // 키보드 네비게이션 상태를 잠시 후 해제
        setTimeout(() => {
            this.state.isKeyboardNavigation = false;
        }, 100);
    }

    // 자동완성에서 선택했을 때의 검색 (길이 제한 없음)
    async performSearchFromAutocomplete(searchQuery) {
        if (!searchQuery || this.state.isSearching || this.state.isShowingAlert) return;

        this.state.currentQuery = searchQuery;
        this.state.isSearching = true;

        this.hideAutocomplete();
        this.showLoading();
        this.hideSearchResults();

        try {
            const params = { query: searchQuery, page: 0, size: 20 };
            const response = await apiService.searchPopups(params);
            this.displaySearchResults(response);
        } catch (error) {
            console.error('자동완성 검색 실패:', error);
            this.showSearchError();
        } finally {
            this.state.isSearching = false;
            this.hideLoading();
        }
    }

    // 일반 검색 수행 (길이 체크 포함)
    async performSearch(searchParams = {}) {
        const searchQuery = searchParams.query || this.searchInput.value.trim();

        // 검색어 길이 체크
        if (!this.validateSearchQuery(searchQuery)) return;
        if (this.state.isSearching) return;

        this.state.currentQuery = searchQuery;
        this.state.isSearching = true;

        this.hideAutocomplete();
        this.showLoading();
        this.hideSearchResults();

        try {
            const params = {
                query: searchQuery,
                page: searchParams.page || 0,
                size: searchParams.size || 20
            };
            const response = await apiService.searchPopups(params);
            this.displaySearchResults(response);
        } catch (error) {
            console.error('팝업 검색 실패:', error);
            this.showSearchError();
        } finally {
            this.state.isSearching = false;
            this.hideLoading();
        }
    }

    // 검색어 유효성 검사
    validateSearchQuery(query) {
        if (!query || query.length < this.MIN_SEARCH_LENGTH) {
            if (query && query.length === 1) {
                this.showLengthAlert();
            }
            return false;
        }
        return true;
    }

    // 길이 경고 알림
    showLengthAlert() {
        if (this.state.isShowingAlert) return;

        this.state.isShowingAlert = true;
        alert(`검색어를 ${this.MIN_SEARCH_LENGTH}글자 이상 입력해주세요.`);

        setTimeout(() => {
            this.state.isShowingAlert = false;
            this.searchInput.focus();
        }, 200);
    }

    // 검색 결과 표시
    displaySearchResults(response) {
        if (!this.searchResults) return;

        // 응답 구조 확인
        const popups = response?.popups || [];
        const totalElements = response?.totalElements || 0;

        if (popups.length === 0) {
            this.showNoResults();
            return;
        }

        this.searchResults.innerHTML = `
            <div class="search-results-title">'${this.escapeHtml(this.state.currentQuery)}' 검색 결과 (${totalElements}개)</div>
            <div class="search-results-grid">
                ${popups.map(popup => this.createSearchResultCard(popup)).join('')}
            </div>`;
        this.showSearchResults();
    }

    // 검색 결과 카드 생성
    createSearchResultCard(popup) {
        const safeTitle = this.escapeHtml(popup?.title ?? '');
        const safeRegion = this.escapeHtml(popup?.region ?? '장소 미정');
        const safePeriod = this.escapeHtml(popup?.period ?? '');
        const imgSrc = this.escapeHtml(popup?.mainImageUrl || 'https://via.placeholder.com/150');
        const popupId = encodeURIComponent(String(popup?.id ?? ''));

        return `
            <div class="popup-card" data-popup-id="${popupId}">
                <div class="card-image-wrapper">
                    <img src="${imgSrc}" alt="${safeTitle}" class="card-image" loading="lazy">
                </div>
                <div class="card-content">
                    <h3 class="card-title">${safeTitle}</h3>
                    <p class="card-info location">${safeRegion}</p>
                    <p class="card-info">${safePeriod}</p>
                </div>
            </div>`;
    }

    // 검색 결과 없음 표시
    showNoResults() {
        if (!this.searchResults) return;
        this.searchResults.innerHTML = `
            <div class="no-results">
                <div class="no-results-icon">🔍</div>
                <h3>검색 결과가 없습니다</h3>
                <p>'${this.escapeHtml(this.state.currentQuery)}'에 대한 검색 결과를 찾을 수 없습니다.</p>
                <p>다른 검색어로 다시 시도해보세요.</p>
            </div>`;
        this.showSearchResults();
    }

    // 검색 오류 표시
    showSearchError() {
        if (!this.searchResults) return;
        this.searchResults.innerHTML = `
            <div class="search-error">
                <div class="error-icon">⚠️</div>
                <h3>검색 중 오류가 발생했습니다</h3>
                <p>잠시 후 다시 시도해주세요.</p>
            </div>`;
        this.showSearchResults();
    }

    // 자동완성 상태 완전 정리
    clearAutocompleteState() {
        clearTimeout(this.debounceTimeout);
        this.hideAutocomplete();
        this.state.isLoadingSuggestions = false;
        this.state.isKeyboardNavigation = false;
        this.state.searchJustCompleted = true;

        // 1초 후 검색 완료 상태 해제
        setTimeout(() => {
            this.state.searchJustCompleted = false;
        }, 1000);
    }

    // 자동완성 숨김
    hideAutocomplete() {
        if (this.relatedSearches) {
            this.relatedSearches.classList.remove('show');
            this.searchInput.closest('.search-input-wrapper').classList.remove('autocomplete-active');
            this.searchInput.closest('.search-area').classList.remove('active');
        }
        this.autocompleteItems = [];
        this.state.selectedIndex = -1;
    }

    // UI 표시/숨김 메서드들
    showSearchResults() {
        if (this.searchResults) this.searchResults.classList.add('show');
    }

    hideSearchResults() {
        if (this.searchResults) this.searchResults.classList.remove('show');
    }

    hideAllResults() {
        this.hideAutocomplete();
        this.hideSearchResults();
    }

    showLoading() {
        if (this.searchLoading) this.searchLoading.style.display = 'flex';
    }

    hideLoading() {
        if (this.searchLoading) this.searchLoading.style.display = 'none';
    }

    showError(message) {
        const mainContent = document.getElementById('main-content');
        if (mainContent) {
            const div = document.createElement('div');
            div.className = 'alert alert-error';
            div.textContent = String(message);
            mainContent.innerHTML = '';
            mainContent.appendChild(div);
        }
    }

    // HTML 이스케이프
    escapeHtml(text) {
        if (typeof text !== 'string') return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, (m) => map[m]);
    }
}

// 전역 인스턴스
window.PopupSearchManager = PopupSearchManager;