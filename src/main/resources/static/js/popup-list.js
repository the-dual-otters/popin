// 팝업 리스트 페이지 전용 모듈
class PopupListManager {
    constructor() {
        this.currentPage = 0;
        this.isFetching = false;
        this.hasMore = true;
        this.aiRecommendationsLoaded = false;

        // 필터 상태 관리
        this.currentFilterMode = 'latest';
        this.currentRegion = 'All';
        this.currentDateFilter = 'All';
        this.currentStatus = 'All';
        this.customStartDate = null;
        this.customEndDate = null;
        this.isCustomDateMode = false;

        // DOM 요소들
        this.grid = null;
        this.loadingIndicator = null;
        this.regionDateFilterContainer = null;
        this.statusFilterContainer = null;
        this.statusFilterSelect = null;
        this.aiMessage = null;

        // 캐시 관리
        this.aiCache = new AIRecommendationCache();
    }

    // ===== 초기화 메서드들 =====

    async initialize() {
        try {
            await this.renderHTML();
            this.setupElements();
            this.setupEventListeners();
            this.setInitialTabState();
            await this.loadInitialData();
        } catch (error) {
            console.error('팝업 리스트 페이지 초기화 실패:', error);
            this.showError('페이지를 불러오는 중 오류가 발생했습니다.');
        }
    }

    renderHTML() {
        document.getElementById('main-content').innerHTML = `
            <div class="announcement-banner">
                <span class="icon-speaker">📊</span>
                <p>새로운 팝업스토어가 매주 업데이트됩니다!</p>
            </div>

            <div class="filter-tabs">
                <button class="tab-item" data-mode="latest">All</button>
                <button class="tab-item ai-tab" data-mode="ai-recommended">AI 추천</button>
                <button class="tab-item" data-mode="popularity">인기 팝업</button>
                <button class="tab-item" data-mode="deadline">마감임박</button>
                <button class="tab-item" data-mode="region-date">지역/날짜</button>
            </div>

            <!-- AI 추천 메시지 -->
            <div id="ai-message" class="ai-message" style="display: none;">
                <!-- JavaScript에서 동적으로 채워짐 -->
            </div>

            <div id="status-filter-container" class="status-filter-container">
                <select id="status-filter-select" class="status-filter-select">
                    <option value="All">전체</option>
                    <option value="ONGOING">진행중</option>
                    <option value="PLANNED">오픈 예정</option>
                    <option value="ENDED">종료</option>
                </select>
            </div>

            <div id="region-date-filters" class="region-date-filters" style="display: none;">
                <div class="sub-filter-section">
                    <h4 class="sub-filter-title">지역</h4>
                    <div class="sub-filter-tabs" id="region-filter-tabs">
                        <button class="sub-tab-item active" data-region="All">All</button>
                        <button class="sub-tab-item" data-region="서울">서울</button>
                        <button class="sub-tab-item" data-region="경기">경기</button>
                        <button class="sub-tab-item" data-region="인천">인천</button>
                        <button class="sub-tab-item" data-region="부산">부산</button>
                        <button class="sub-tab-item" data-region="대구">대구</button>
                        <button class="sub-tab-item" data-region="대전">대전</button>
                        <button class="sub-tab-item" data-region="광주">광주</button>
                        <button class="sub-tab-item" data-region="울산">울산</button>
                        <button class="sub-tab-item" data-region="세종">세종</button>
                        <button class="sub-tab-item" data-region="강원">강원</button>
                        <button class="sub-tab-item" data-region="충북">충북</button>
                        <button class="sub-tab-item" data-region="충남">충남</button>
                        <button class="sub-tab-item" data-region="전북">전북</button>
                        <button class="sub-tab-item" data-region="전남">전남</button>
                        <button class="sub-tab-item" data-region="경북">경북</button>
                        <button class="sub-tab-item" data-region="경남">경남</button>
                        <button class="sub-tab-item" data-region="제주">제주</button>
                    </div>
                </div>

                <div class="sub-filter-section">
                    <h4 class="sub-filter-title">날짜</h4>
                    <div class="sub-filter-tabs" id="date-filter-tabs">
                        <button class="sub-tab-item active" data-date="All">All</button>
                        <button class="sub-tab-item" data-date="today">오늘</button>
                        <button class="sub-tab-item" data-date="7days">일주일</button>
                        <button class="sub-tab-item" data-date="14days">2주</button>
                        <button class="sub-tab-item" data-date="custom">직접 입력</button>
                    </div>

                    <div id="custom-date-inputs" class="custom-date-inputs" style="display: none;">
                        <input type="date" id="start-date" placeholder="시작일">
                        <input type="date" id="end-date" placeholder="종료일">
                        <button id="apply-custom-date" class="apply-date-btn">적용</button>
                    </div>
                </div>
            </div>

            <div id="popup-grid" class="popup-grid"></div>
            <div id="loading-indicator" class="loading-indicator" style="display: none;"></div>
        `;
    }

    setupElements() {
        this.grid = document.getElementById('popup-grid');
        this.loadingIndicator = document.getElementById('loading-indicator');
        this.regionDateFilterContainer = document.getElementById('region-date-filters');
        this.statusFilterContainer = document.getElementById('status-filter-container');
        this.statusFilterSelect = document.getElementById('status-filter-select');
        this.aiMessage = document.getElementById('ai-message');
    }

    setupEventListeners() {
        // 메인 필터 탭 이벤트
        document.querySelector('.filter-tabs').addEventListener('click', (e) => {
            this.handleFilterClick(e);
        });

        // 상태 필터 드롭다운 이벤트
        this.statusFilterSelect.addEventListener('change', (e) => {
            this.handleStatusChange(e);
        });

        // 지역/날짜 서브 필터 이벤트
        this.regionDateFilterContainer.addEventListener('click', (e) => {
            this.handleSubFilterClick(e);
        });

        // 지역 필터 드래그 스크롤 기능
        const regionFilterTabs = document.getElementById('region-filter-tabs');
        if (regionFilterTabs) {
            let isDown = false, startX, scrollLeft;
            regionFilterTabs.addEventListener('mousedown', (e) => {
                isDown = true;
                regionFilterTabs.classList.add('active-drag');
                startX = e.pageX - regionFilterTabs.offsetLeft;
                scrollLeft = regionFilterTabs.scrollLeft;
            });
            regionFilterTabs.addEventListener('mouseleave', () => {
                isDown = false;
                regionFilterTabs.classList.remove('active-drag');
            });
            regionFilterTabs.addEventListener('mouseup', () => {
                isDown = false;
                regionFilterTabs.classList.remove('active-drag');
            });
            regionFilterTabs.addEventListener('mousemove', (e) => {
                if (!isDown) return;
                e.preventDefault();
                const x = e.pageX - regionFilterTabs.offsetLeft;
                const walk = (x - startX) * 2;
                regionFilterTabs.scrollLeft = scrollLeft - walk;
            });
        }

        // 커스텀 날짜 선택기 이벤트
        document.getElementById('apply-custom-date')?.addEventListener('click', () => {
            this.applyCustomDate();
        });

        // 무한 스크롤 이벤트
        this._onScroll = () => {
            if (this.isFetching || !this.hasMore || this.currentFilterMode === 'ai-recommended') return;
            this.handlePageScroll();
        };
        window.addEventListener('scroll', this._onScroll, {passive: true});

        // 카드 클릭 위임
        this.grid.addEventListener('click', (e) => {
            const card = e.target.closest('.popup-card');
            if (card && card.dataset.id) {
                goToPopupDetail(card.dataset.id);
            }
        });

        // 이미지 로딩 실패 처리
        this.grid.addEventListener('error', (e) => {
            const img = e.target;
            if (img && img.matches('.card-image') && !img.dataset.errorHandled) {
                img.dataset.errorHandled = 'true';
                img.onerror = null;
                const fallback = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTUwIiBoZWlnaHQ9IjE1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjNjY3ZWVhIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMTYiIGZpbGw9IndoaXRlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+Tm8gSW1hZ2U8L3RleHQ+PC9zdmc+';
                img.src = fallback;
            }
        }, true);
    }

    setInitialTabState() {
        // 모든 탭에서 active 클래스 제거
        document.querySelectorAll('.filter-tabs .tab-item').forEach(tab => {
            tab.classList.remove('active');
        });

        const latestTab = document.querySelector('.filter-tabs .tab-item[data-mode="latest"]');
        if (latestTab) {
            latestTab.classList.add('active');
        }

        // 필터 모드를 latest로 설정
        this.currentFilterMode = 'latest';

        console.log('초기 탭 상태 설정 완료 - latest 모드 활성화');
    }

    async refreshAIRecommendations() {
        console.log('AI 추천 강제 새로고침');
        this.aiCache.invalidateCache();
        await this.loadAIRecommendations();
    }

    // ===== 이벤트 핸들러들 =====

    async handleFilterClick(e) {
        const selectedTab = e.target.closest('.tab-item');
        if (!selectedTab || this.isFetching) return;

        const newMode = selectedTab.dataset.mode;
        const previousMode = this.currentFilterMode;

        // 같은 탭 클릭시 리턴 (지역/날짜 제외)
        if (this.currentFilterMode === newMode && newMode !== 'region-date') return;

        // 활성 탭 UI 변경
        document.querySelectorAll('.filter-tabs .tab-item').forEach(tab => {
            tab.classList.remove('active');
        });
        selectedTab.classList.add('active');

        this.currentFilterMode = newMode;

        // AI 추천 탭 선택시
        if (newMode === 'ai-recommended') {
            console.log('AI 추천 탭 선택됨');
            await this.showAIMessage();
            this.statusFilterContainer.style.display = 'none';
            this.regionDateFilterContainer.style.display = 'none';
            await this.loadAIRecommendations();
        }
        // 다른 탭들
        else {
            this.hideAIMessage();
            this.aiRecommendationsLoaded = false;

            if (newMode === 'latest') {
                this.statusFilterContainer.style.display = 'block';
                this.regionDateFilterContainer.style.display = 'none';
                this.resetAndLoad();
            } else if (newMode === 'region-date') {
                this.statusFilterContainer.style.display = 'none';
                this.regionDateFilterContainer.style.display = 'block';
                if (previousMode !== 'region-date') {
                    this.resetRegionDateFilters();
                    this.resetAndLoad();
                }
            } else {
                this.statusFilterContainer.style.display = 'none';
                this.regionDateFilterContainer.style.display = 'none';
                this.resetAndLoad();
            }
        }
    }

    handleStatusChange(e) {
        if(this.isFetching) return;
        this.currentStatus = e.target.value;
        this.resetAndLoad();
    }

    handleSubFilterClick(e) {
        const selectedSubTab = e.target.closest('.sub-tab-item');
        if (!selectedSubTab || this.isFetching) return;

        const region = selectedSubTab.dataset.region;
        const date = selectedSubTab.dataset.date;

        if (region) {
            this.currentRegion = region;
            document.querySelectorAll('#region-filter-tabs .sub-tab-item').forEach(tab =>
                tab.classList.toggle('active', tab.dataset.region === region)
            );
            this.resetAndLoad();
        }

        if (date) {
            if (date === 'custom') {
                this.showCustomDatePicker();
                return;
            }

            this.currentDateFilter = date;
            this.isCustomDateMode = false;
            this.customStartDate = null;
            this.customEndDate = null;

            document.querySelectorAll('#date-filter-tabs .sub-tab-item').forEach(tab =>
                tab.classList.toggle('active', tab.dataset.date === date)
            );

            this.hideCustomDatePicker();
            this.hideSelectedDateRange();
            this.resetAndLoad();
        }
    }

    handlePageScroll() {
        if (this.currentFilterMode === 'ai-recommended') return;

        const { scrollTop, scrollHeight, clientHeight } = document.documentElement;
        if (scrollHeight - scrollTop - clientHeight < 200) {
            this.loadMore();
        }
    }

    // ===== AI 관련 메서드들 =====

    async showAIMessage() {
        if (!this.aiMessage) return;

        const isLoggedIn = this.checkLoginStatus();

        if (isLoggedIn) {
            const userName = await this.getCurrentUserName();
            this.aiMessage.innerHTML = `
                 <h3>${this.escapeHtml(userName)}님을 위한 맞춤 추천</h3>
                <p>취향과 관심사를 분석해서 딱 맞는 팝업스토어를 추천해드릴게요</p>
            `;
        } else {
            this.aiMessage.innerHTML = `
                <h3>로그인하고 나에게 딱 맞는 팝업을 찾아보세요</h3>
                <p>AI가 당신의 취향을 분석해서 완벽한 팝업스토어를 추천해드립니다</p>
                <a href="/auth/login" class="login-btn">로그인하러 가기</a>
            `;
        }

        this.aiMessage.style.display = 'block';
    }

    hideAIMessage() {
        if (this.aiMessage) {
            this.aiMessage.style.display = 'none';
        }
    }

    async loadAIRecommendations() {
        // 중복 호출 방지
        if (this.isFetching) {
            console.log('AI 추천 이미 로딩 중');
            return;
        }

        console.log('AI 추천 로드 시작');

        if (this.aiCache.isCacheValid()) {
            console.log('캐시된 AI 추천 데이터 사용');
            const cachedData = this.aiCache.getCachedData();

            if (cachedData && cachedData.length > 0) {
                this.clearGrid();
                this.renderAIRecommendations(cachedData);
                this.aiRecommendationsLoaded = true;
                this.hasMore = false;
                return;
            }
        }

        // 캐시가 없거나 만료된 경우 API 호출
        this.isFetching = true;
        this.clearGrid();
        this.showLoading();

        try {
            const params = {
                page: 0,
                size: 20
            };

            console.log('AI 추천 API 호출');
            const response = await apiService.getAIRecommendedPopups(params);
            console.log('AI 추천 API 응답:', response);

            if (!response) {
                console.warn('AI 추천 API 응답이 null');
                this.showNoAIResults();
                return;
            }

            let recommendations = [];
            if (response.content && Array.isArray(response.content)) {
                recommendations = response.content;
            } else if (response.popups && Array.isArray(response.popups)) {
                recommendations = response.popups;
            } else if (Array.isArray(response)) {
                recommendations = response;
            } else {
                console.warn('예상치 못한 응답 형식:', response);
                this.showNoAIResults();
                return;
            }

            if (recommendations.length > 0) {
                // 캐시에 저장
                this.aiCache.setCacheData(recommendations);

                // 화면에 렌더링
                this.renderAIRecommendations(recommendations);
                console.log(`AI 추천 ${recommendations.length}개 로드 및 캐시 저장 완료`);
            } else {
                console.log('AI 추천 결과 없음');
                this.showNoAIResults();
            }

            this.aiRecommendationsLoaded = true;
            this.hasMore = false;

        } catch (error) {
            console.error('AI 추천 로드 실패:', error);
            this.showAIError();
            this.aiCache.invalidateCache();
        } finally {
            this.isFetching = false;
            this.hideLoading();
        }
    }

    renderAIRecommendations(recommendations) {
        const fragment = document.createDocumentFragment();

        recommendations.forEach(popup => {
            const card = this.createAIPopupCard(popup);
            fragment.appendChild(card);
        });

        this.grid.appendChild(fragment);
    }

    createAIPopupCard(popup) {
        const card = document.createElement('div');
        card.className = 'popup-card ai-recommended';
        const popupId = encodeURIComponent(String(popup?.id ?? ''));
        card.dataset.id = popupId;

        const fallbackImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTUwIiBoZWlnaHQ9IjE1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjNjY3ZWVhIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMTYiIGZpbGw9IndoaXRlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+Tm8gSW1hZ2U8L3RleHQ+PC9zdmc+';
        const imageUrl = popup.thumbnailUrl || popup.mainImageUrl || fallbackImage;
        const safeImageUrl = this.isSafeUrl(imageUrl) ? imageUrl : fallbackImage;

        // 날짜 포맷팅
        const dateRange = popup.period || this.formatDateRange(popup.startDate, popup.endDate);

        // 지역 정보
        const region = popup.region || (popup.venue ? popup.venue.region : '') || '장소 미정';

        card.innerHTML = `
            <div class="card-image-wrapper">
                <img src="${safeImageUrl}" 
                     alt="${this.escapeHtml(popup.title)}" 
                     class="card-image"
                     loading="lazy"
                     onerror="this.onerror=null; this.src='${fallbackImage}'; this.dataset.errorHandled='true';">
                <div class="ai-badge">AI 추천</div>
            </div>
            <div class="card-content">
                <h3 class="card-title">${this.escapeHtml(popup.title)}</h3>
                <p class="card-info">${this.escapeHtml(dateRange)}</p>
                <p class="card-info location">${this.escapeHtml(region)}</p>
            </div>
        `;

        return card;
    }

    showNoAIResults() {
        const isLoggedIn = this.checkLoginStatus();

        if (isLoggedIn) {
            this.grid.innerHTML = `
                <div style="grid-column: 1 / -1; text-align: center; padding: 40px 20px;">
                    <h3>추천할 팝업이 준비중입니다</h3>
                    <p>더 많은 팝업을 둘러보시면 더 정확한 추천이 가능해요</p>
                </div>
            `;
        } else {
            this.grid.innerHTML = `
                <div style="grid-column: 1 / -1; text-align: center; padding: 40px 20px;">
                    <h3>로그인하고 나에게 딱 맞는 팝업을 찾아보세요</h3>
                    <p>AI가 당신의 취향을 분석해서 완벽한 팝업스토어를 추천해드립니다</p>
                    <button onclick="location.href='/auth/login'" 
                            style="margin-top: 16px; padding: 12px 24px; background: #4B5AE4; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px;">
                        로그인하기
                    </button>
                </div>
            `;
        }
    }

    showAIError() {
        this.grid.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 40px 20px;">
                <h3>추천을 불러올 수 없습니다</h3>
                <p>네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.</p>
                <button onclick="location.reload()" 
                        style="margin-top: 16px; padding: 12px 24px; background: #6B7280; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px;">
                    새로고침
                </button>
            </div>
        `;
    }

    // ===== 일반 팝업 로드 관련 메서드들 =====

    async loadInitialData() {
        await this.fetchAndDisplayPopups(false);
    }

    async loadMore() {
        await this.fetchAndDisplayPopups(true);
    }

    async resetAndLoad() {
        this.currentPage = 0;
        this.hasMore = true;
        this.aiRecommendationsLoaded = false;
        window.scrollTo({ top: 0, behavior: 'smooth' });
        await this.fetchAndDisplayPopups(false);
    }

    async fetchAndDisplayPopups(isLoadMore = false) {
        if (this.isFetching || !this.hasMore) return;

        // AI 추천 모드일 때는 일반 팝업 로드하지 않음
        if (this.currentFilterMode === 'ai-recommended') {
            return;
        }

        this.isFetching = true;

        if (!isLoadMore) {
            this.grid.innerHTML = '';
            this.currentPage = 0;
            this.hasMore = true;
        }

        this.showLoading();

        try {
            const params = {
                page: this.currentPage,
                size: 10
            };
            let response;

            switch (this.currentFilterMode) {
                case 'latest': {
                    const latestParams = { ...params };
                    if (this.currentStatus !== 'All') {
                        latestParams.status = this.currentStatus;
                    }
                    response = await apiService.getPopups(latestParams);
                    break;
                }
                case 'popularity': {
                    response = await apiService.getPopularPopups(params);
                    break;
                }
                case 'deadline': {
                    response = await apiService.getDeadlineSoonPopups(params);
                    break;
                }
                case 'region-date': {
                    const regionDateParams = { ...params };
                    if (this.currentRegion !== 'All') {
                        regionDateParams.region = this.currentRegion;
                    }
                    if (this.currentDateFilter !== 'All') {
                        if (this.isCustomDateMode) {
                            regionDateParams.startDate = this.customStartDate;
                            regionDateParams.endDate = this.customEndDate;
                        } else {
                            regionDateParams.dateFilter = this.currentDateFilter;
                        }
                    }
                    response = await apiService.getPopupsByRegionAndDate(regionDateParams);
                    break;
                }
                default: {
                    response = await apiService.getPopups(params);
                }
            }

            if (response.popups && response.popups.length > 0) {
                this.renderPopups(response.popups);
                this.currentPage++;
                this.hasMore = !response.last;
            } else {
                this.hasMore = false;
                if (!isLoadMore) {
                    this.showNoResults();
                }
            }
        } catch (error) {
            console.error('팝업 로드 실패:', error);
            if (!isLoadMore) {
                this.showError('팝업을 불러오는 중 오류가 발생했습니다.');
            }
        } finally {
            this.isFetching = false;
            this.hideLoading();
        }
    }

    renderPopups(popups) {
        const cardsHTML = popups.map(popup => this.createPopupCard(popup)).join('');
        this.grid.insertAdjacentHTML('beforeend', cardsHTML);
    }

    createPopupCard(popup) {
        const fallbackImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTUwIiBoZWlnaHQ9IjE1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjNjY3ZWVhIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMTYiIGZpbGw9IndoaXRlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+Tm8gSW1hZ2U8L3RleHQ+PC9zdmc+';
        const safeSrc = this.isSafeUrl(popup.mainImageUrl) ? popup.mainImageUrl : fallbackImage;
        const popupId = encodeURIComponent(String(popup?.id ?? ''));

        return `
            <div class="popup-card" data-id="${popupId}">
                <div class="card-image-wrapper">
                    <img src="${safeSrc}"
                         alt="${this.escapeHtml(popup.title)}"
                         class="card-image"
                         loading="lazy"
                         decoding="async"
                         onerror="if(!this.dataset.errorHandled){this.onerror=null; this.src='${fallbackImage}'; this.dataset.errorHandled='true';}">
                </div>
                <div class="card-content">
                    <h3 class="card-title">${this.escapeHtml(popup.title)}</h3>
                    <p class="card-info">${this.escapeHtml(popup.period)}</p>
                    <p class="card-info location">${this.escapeHtml(popup.region || '장소 미정')}</p>
                </div>
            </div>
        `;
    }

    // ===== 날짜 필터 관련 메서드들 =====

    showCustomDatePicker() {
        document.querySelectorAll('#date-filter-tabs .sub-tab-item').forEach(tab =>
            tab.classList.toggle('active', tab.dataset.date === 'custom')
        );

        document.getElementById('custom-date-inputs').style.display = 'block';

        const today = new Date().toISOString().split('T')[0];
        document.getElementById('start-date').value = today;
        document.getElementById('end-date').value = today;
    }

    hideCustomDatePicker() {
        document.getElementById('custom-date-inputs').style.display = 'none';
    }

    applyCustomDate() {
        const startDate = document.getElementById('start-date').value;
        const endDate = document.getElementById('end-date').value;

        if (!startDate || !endDate) {
            alert('시작일과 마감일을 모두 선택해주세요.');
            return;
        }

        if (new Date(startDate) > new Date(endDate)) {
            alert('시작일은 마감일보다 이전이어야 합니다.');
            return;
        }

        this.customStartDate = startDate;
        this.customEndDate = endDate;
        this.isCustomDateMode = true;
        this.currentDateFilter = 'custom';

        this.displaySelectedDateRange(startDate, endDate);
        this.hideCustomDatePicker();
        this.resetAndLoad();
    }

    displaySelectedDateRange(startDate, endDate) {
        const dateRangeElement = document.getElementById('date-range-text');
        const selectedDateDisplay = document.getElementById('selected-date-display');

        if (dateRangeElement && selectedDateDisplay) {
            const formattedStartDate = startDate.replace(/-/g, '.');
            const formattedEndDate = endDate.replace(/-/g, '.');

            if (startDate === endDate) {
                dateRangeElement.textContent = formattedStartDate;
            } else {
                dateRangeElement.textContent = `${formattedStartDate} - ${formattedEndDate}`;
            }

            selectedDateDisplay.style.display = 'block';
        }
    }

    hideSelectedDateRange() {
        const selectedDateDisplay = document.getElementById('selected-date-display');
        if (selectedDateDisplay) {
            selectedDateDisplay.style.display = 'none';
        }
    }

    resetRegionDateFilters() {
        this.currentRegion = 'All';
        this.currentDateFilter = 'All';
        this.isCustomDateMode = false;
        this.customStartDate = null;
        this.customEndDate = null;

        document.querySelectorAll('#region-filter-tabs .sub-tab-item').forEach(tab =>
            tab.classList.toggle('active', tab.dataset.region === 'All')
        );
        document.querySelectorAll('#date-filter-tabs .sub-tab-item').forEach(tab =>
            tab.classList.toggle('active', tab.dataset.date === 'All')
        );

        this.hideCustomDatePicker();
        this.hideSelectedDateRange();
    }

    // ===== UI 관련 유틸리티 메서드들 =====

    clearGrid() {
        if (this.grid) {
            this.grid.innerHTML = '';
        }
    }

    showLoading() {
        if (this.loadingIndicator) {
            this.loadingIndicator.style.display = 'flex';
        }
    }

    hideLoading() {
        if (this.loadingIndicator) {
            this.loadingIndicator.style.display = 'none';
        }
    }

    showNoResults() {
        this.grid.innerHTML = '<p style="grid-column: 1 / -1; text-align: center; padding: 20px; color: #666;">표시할 팝업이 없습니다.</p>';
    }

    showError(message) {
        this.grid.innerHTML = `<p style="grid-column: 1 / -1; text-align: center; padding: 20px; color: #ef4444;">${message}</p>`;
    }

    // ===== 기타 유틸리티 메서드들 =====

    checkLoginStatus() {
        try {
            // 여러 방법으로 로그인 상태 확인
            const token = localStorage.getItem('accessToken') ||
                localStorage.getItem('authToken') ||
                sessionStorage.getItem('accessToken') ||
                sessionStorage.getItem('authToken');

            const userId = localStorage.getItem('userId') || sessionStorage.getItem('userId');

            // API 서비스 체크
            if (apiService && typeof apiService.isLoggedIn === 'function') {
                return apiService.isLoggedIn();
            }

            // 토큰이나 userId 존재 여부로 판단
            return !!(token || userId);
        } catch (error) {
            console.warn('로그인 상태 확인 실패:', error);
            return false;
        }
    }

    async getCurrentUserName() {
        try {
            console.log('API를 통한 사용자 정보 조회 시작');

            if (typeof apiService !== 'undefined' && apiService.getCurrentUser) {
                const user = await apiService.getCurrentUser();
                console.log('API에서 가져온 사용자 정보:', user);

                if (user && user.nickname) {
                    return user.nickname;
                }
            }

            console.log('API에서 사용자 정보를 가져올 수 없음, localStorage 확인');

            // 백업: localStorage에서 확인
            const userInfo = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
            if (userInfo) {
                try {
                    const parsed = JSON.parse(userInfo);
                    console.log('저장된 사용자 정보:', parsed);
                    // 여기도 닉네임을 먼저 확인
                    if (parsed.nickname) return parsed.nickname;
                } catch (e) {
                    console.warn('사용자 정보 파싱 실패:', e);
                }
            }

            return '회원';

        } catch (error) {
            console.error('사용자 정보 조회 실패:', error);
            return '회원';
        }
    }

    formatDateRange(startDate, endDate) {
        if (!startDate || !endDate) return '날짜 미정';

        const start = new Date(startDate);
        const end = new Date(endDate);

        const formatDate = (date) => {
            return `${date.getMonth() + 1}.${date.getDate()}`;
        };

        if (start.getTime() === end.getTime()) {
            return formatDate(start);
        }

        return `${formatDate(start)} ~ ${formatDate(end)}`;
    }

    escapeHtml(text) {
        if (!text) return '';
        return String(text).replace(/[&<>"']/g, function(match) {
            const escapeMap = {
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#39;'
            };
            return escapeMap[match];
        });
    }

    isSafeUrl(url) {
        try {
            const u = new URL(url, window.location.origin);
            return u.protocol === 'http:' || u.protocol === 'https:';
        } catch {
            return false;
        }
    }

    cleanup() {
        if (this._onScroll) {
            window.removeEventListener('scroll', this._onScroll);
            this._onScroll = null;
        }
    }
}

window.PopupListManager = PopupListManager;