// 지도 페이지 전용 모듈 (컴팩트 버전)
class MapPageManager {
    constructor() {
        this.map = null;
        this.markers = [];
        this.overlays = [];
        this.popups = [];
        this.userLocation = null;
        this.currentLocationMarker = null;
        this._reqSeq = 0;
        this._debouncedLoadInBounds = null;
    }

    // 페이지 초기화
    async initialize() {
        try {
            await this.renderHTML();
            await this.wait(100);
            await this.initializeMap();
            this.setupEventListeners();
            await this.findMyLocation();
        } catch (error) {
            console.error('지도 페이지 초기화 실패:', error);
        }
    }

    // HTML 렌더링
    async renderHTML() {
        const mainContent = document.getElementById('main-content');
        if (mainContent) {
            mainContent.innerHTML = `
                <div class="map-container">
                    <div id="kakao-map" class="kakao-map"></div>
                </div>
                <div id="selected-popup-section" class="selected-popup-section"></div>
                <div class="nearby-section">
                    <h2 class="section-title">내 근처 팝업</h2>
                    <div class="category-filters" id="category-filters">
                        <button class="category-btn active" data-category="">전체</button>
                        <button class="category-btn" data-category="패션">패션</button>
                        <button class="category-btn" data-category="반려동물">반려동물</button>
                        <button class="category-btn" data-category="게임">게임</button>
                        <button class="category-btn" data-category="캐릭터/IP">캐릭터/IP</button>
                        <button class="category-btn" data-category="문화/콘텐츠">문화/콘텐츠</button>
                        <button class="category-btn" data-category="연예">연예</button>
                        <button class="category-btn" data-category="여행/레저/스포츠">여행/레저/스포츠</button>
                    </div>
                    <div class="popup-list" id="popup-list"></div>
                </div>
            `;
        }
    }

    // 카카오맵 초기화
    async initializeMap() {
        const container = document.getElementById('kakao-map');
        if (!container || !window.kakao || !window.kakao.maps) return;
        const options = { center: new window.kakao.maps.LatLng(37.5665, 126.9780), level: 5 };
        this.map = new window.kakao.maps.Map(container, options);
        this.addLocationButton();
        this._debouncedLoadInBounds = this.debounce(() => this.loadMapPopupsInBounds(), 250);
        window.kakao.maps.event.addListener(this.map, 'idle', this._debouncedLoadInBounds);
    }

    // 현재 위치 버튼
    addLocationButton() {
        const mapContainer = document.getElementById('kakao-map');
        const btn = document.createElement('button');
        btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/><circle cx="12" cy="12" r="3" fill="currentColor"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4" stroke="currentColor" stroke-width="1.5"/></svg>`;
        btn.style.cssText = `position: absolute; bottom: 20px; right: 8px; width: 36px; height: 36px; border-radius: 50%; background: white; border: 1px solid #ddd; box-shadow: 0 2px 8px rgba(0,0,0,0.2); cursor: pointer; z-index: 1000; display: flex; align-items: center; justify-content: center; color: #333;`;
        btn.onclick = () => this.findMyLocation();
        mapContainer.appendChild(btn);
    }

    // 현재 위치 찾기
    async findMyLocation() {
        if (!navigator.geolocation) return;
        navigator.geolocation.getCurrentPosition(
            (position) => {
                this.userLocation = { lat: position.coords.latitude, lng: position.coords.longitude };

                if (!this.map) {
                    console.warn('카카오맵이 초기화되지 않아 위치 설정을 건너뜁니다.');
                    return;
                }

                const center = new window.kakao.maps.LatLng(this.userLocation.lat, this.userLocation.lng);
                this.map.setCenter(center);
                this.map.setLevel(4);
                this.showCurrentLocationMarker();
            },
            () => {
                console.log('위치 정보를 가져올 수 없습니다. 기본 위치의 팝업을 로드합니다.');
                this.loadMapPopupsInBounds();
            }
        );
    }

    // 현재 위치 마커 표시
    showCurrentLocationMarker() {
        if (!this.userLocation) return;
        if (this.currentLocationMarker) this.currentLocationMarker.setMap(null);
        const position = new window.kakao.maps.LatLng(this.userLocation.lat, this.userLocation.lng);
        const svgString = '<svg width="24" height="24" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><circle cx="12" cy="12" r="11" fill="#FF4444" stroke="white" stroke-width="2"/><circle cx="12" cy="12" r="4" fill="white"/></svg>';
        const markerImage = new window.kakao.maps.MarkerImage('data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svgString), new window.kakao.maps.Size(24, 24), { offset: new window.kakao.maps.Point(12, 12) });
        this.currentLocationMarker = new window.kakao.maps.Marker({ position, image: markerImage });
        this.currentLocationMarker.setMap(this.map);
    }

    // 이벤트 리스너
    setupEventListeners() {
        const categoryFilters = document.getElementById('category-filters');
        if (!categoryFilters) return;
        categoryFilters.addEventListener('click', (e) => {
            if (e.target.classList.contains('category-btn')) this.handleCategoryFilter(e.target);
        });
        let isDown = false, startX, scrollLeft;
        categoryFilters.addEventListener('mousedown', (e) => {
            isDown = true;
            categoryFilters.classList.add('active-drag');
            startX = e.pageX - categoryFilters.offsetLeft;
            scrollLeft = categoryFilters.scrollLeft;
        });
        categoryFilters.addEventListener('mouseleave', () => { isDown = false; categoryFilters.classList.remove('active-drag'); });
        categoryFilters.addEventListener('mouseup', () => { isDown = false; categoryFilters.classList.remove('active-drag'); });
        categoryFilters.addEventListener('mousemove', (e) => {
            if (!isDown) return;
            e.preventDefault();
            const x = e.pageX - categoryFilters.offsetLeft;
            const walk = (x - startX) * 2;
            categoryFilters.scrollLeft = scrollLeft - walk;
        });
    }

    // 지도 범위 내 팝업 로드
    async loadMapPopupsInBounds() {
        if (!this.map) return;
        const reqId = ++this._reqSeq;

        const popupList = document.getElementById('popup-list');
        if (popupList) {
            // API 요청 전에 로딩 인디케이터를 먼저 표시하여 즉각적인 피드백 제공
            popupList.innerHTML = '<div class="loading-indicator"></div>';
        }
        // 지도 이동 시, 이전에 선택했던 팝업 정보 숨기기
        this.hideSelectedPopup();

        try {
            const bounds = this.map.getBounds();
            const sw = bounds.getSouthWest(), ne = bounds.getNorthEast();
            const popups = await apiService.getPopupsInBounds(sw.getLat(), sw.getLng(), ne.getLat(), ne.getLng());

            if (reqId !== this._reqSeq) return; // stale response
            this.popups = popups || [];
            this.renderMapMarkers(this.popups);
            // API 응답 후, 로딩 인디케이터를 새로운 팝업 리스트로 교체
            this.renderBottomSheetList(this.popups);
        } catch (error) {
            console.error('범위 내 팝업 로드 실패:', error);
            if (popupList) {
                popupList.innerHTML = '<div class="empty-state">팝업을 불러오는 데 실패했습니다.</div>';
            }
        }
    }

    // 지도 마커와 팝업 이름 렌더링
    renderMapMarkers(popups) {
        if (!this.map) return;
        this.markers.forEach(marker => marker.setMap(null));
        this.overlays.forEach(overlay => overlay.setMap(null));
        this.markers = []; this.overlays = [];

        popups.forEach(popup => {
            if (!popup.latitude || !popup.longitude) return;
            const position = new window.kakao.maps.LatLng(popup.latitude, popup.longitude);
            const marker = new window.kakao.maps.Marker({ position, title: popup.title });
            const content = `<div class="popup-marker-label">${this.escapeHTML(popup.title || '')}</div>`;
            const customOverlay = new window.kakao.maps.CustomOverlay({ position, content, yAnchor: -0.2 });

            window.kakao.maps.event.addListener(marker, 'click', () => {
                this.showSelectedPopup(popup);
            });

            marker.setMap(this.map);
            customOverlay.setMap(this.map);
            this.markers.push(marker);
            this.overlays.push(customOverlay);
        });
    }

    // 선택된 팝업 정보 표시
    showSelectedPopup(popup) {
        const section = document.getElementById('selected-popup-section');
        if (!section || !popup) return;
        section.innerHTML = `
            <div class="popup-card-horizontal selected">
                <div class="popup-image-wrapper">
                    <img src="${popup.mainImageUrl || 'https://via.placeholder.com/80x80/667eea/ffffff?text=🎪'}" alt="${this.escapeHTML(popup.title || '')}" class="popup-image">
                </div>
                <div class="popup-info">
                    <div class="popup-title">${this.escapeHTML(popup.title || '')}</div>
                    <div class="popup-period">${this.escapeHTML(popup.period || '기간 미정')}</div>
                    <div class="popup-location">${this.escapeHTML(popup.venueName || '장소 미정')}</div>
                </div>
                <button class="close-btn">&times;</button>
            </div>
        `;
        section.style.display = 'block';
        section.querySelector('.close-btn').addEventListener('click', () => this.hideSelectedPopup());
        section.querySelector('.popup-card-horizontal.selected').addEventListener('click', (e) => {
            if (e.target.className !== 'close-btn') goToPopupDetail(popup.id);
        });
    }

    // 선택된 팝업 정보 숨기기
    hideSelectedPopup() {
        const section = document.getElementById('selected-popup-section');
        if (section) {
            section.innerHTML = '';
            section.style.display = 'none';
        }
    }

    // 하단 팝업 리스트
    renderBottomSheetList(popups) {
        const popupList = document.getElementById('popup-list');
        if (!popupList) return;
        if (!popups || popups.length === 0) {
            popupList.innerHTML = '<div class="empty-state">주변에 팝업이 없습니다</div>';
            return;
        }
        popupList.innerHTML = popups.map(popup => `
            <div class="popup-card-horizontal" onclick="goToPopupDetail('${String(popup.id).replace(/'/g, '&#39;')}')">
                <div class="popup-image-wrapper">
                    <img src="${popup.mainImageUrl || 'https://via.placeholder.com/80x80/667eea/ffffff?text=🎪'}" alt="${this.escapeHTML(popup.title || '')}" class="popup-image">               
                </div>
                <div class="popup-info">
                    <div class="popup-title">${this.escapeHTML(popup.title || '')}</div>
                    <div class="popup-period">${this.escapeHTML(popup.period || '기간 미정')}</div>
                    <div class="popup-location">${this.escapeHTML(popup.venueName || '장소 미정')}</div>
                </div>
            </div>
        `).join('');
    }

    // 카테고리 필터
    handleCategoryFilter(button) {
        document.querySelectorAll('.category-btn').forEach(btn => btn.classList.remove('active'));
        button.classList.add('active');
        const selectedCategory = button.dataset.category || '';
        const filteredPopups = selectedCategory ? this.popups.filter(p => p.categoryName === selectedCategory) : [...this.popups];
        this.renderBottomSheetList(filteredPopups);
    }

    wait(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

    debounce(fn, delay = 250) {
        let t;
        return (...args) => {
            clearTimeout(t);
            t = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    escapeHTML(str) {
        const m = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
        return String(str).replace(/[&<>"']/g, ch => m[ch]);
    }

    cleanup() {
        this.markers.forEach(marker => marker.setMap(null));
        this.overlays.forEach(overlay => overlay.setMap(null));
        this.markers = []; this.overlays = [];
        if (this.currentLocationMarker) {
            this.currentLocationMarker.setMap(null);
            this.currentLocationMarker = null;
        }
    }
}

window.mapPageManager = new MapPageManager();
if (typeof window.goToPopupDetail === 'undefined') {
    window.goToPopupDetail = (id) => console.log(`Navigating to popup detail page for ID: ${id}`);
}