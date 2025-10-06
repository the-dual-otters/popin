// 페이지별 로직
const Pages = {
    // 홈 (팝업 리스트 - index.html에서 표시)
    async popupList() {
        const manager = new PopupListManager();
        await manager.initialize();
    },

    // 팝업 검색 페이지로 이동
    popupSearch(searchParams = {}) {
        const params = new URLSearchParams();

        if (searchParams.query) params.set('query', searchParams.query);
        if (searchParams.region) params.set('region', searchParams.region);
        if (searchParams.category) params.set('category', searchParams.category);

        const queryString = params.toString();
        const url = queryString ? `/popup/search?${queryString}` : '/popup/search';

        window.location.href = url;
    },

    // 지도 페이지로 이동
    map(filterParams = {}) {
        const params = new URLSearchParams();

        if (filterParams.region) params.set('region', filterParams.region);
        if (filterParams.category) params.set('category', filterParams.category);

        const queryString = params.toString();
        const url = queryString ? `/map?${queryString}` : '/map';

        window.location.href = url;
    },

    // 북마크 페이지
    bookmark() {
        window.location.href = '/bookmarks';
    },
    // == 마이페이지 - 공간제공자 (현재 비어있음) ===


    // === 공간 목록 페이지 ===
    spaceList() {
        window.location.href = '/space/list';
    },
    spaceRegister() {
        window.location.href = '/space/register';
    },
    spaceDetail(spaceId) {
        window.location.href = `/space/detail/${spaceId}`;
    },
    spaceEdit(spaceId) {
        window.location.href = `/space/edit/${spaceId}`;
    },
    // == 마이페이지 - 호스트 ==
    mypageHost() {
        window.location.href = '/mypage/host';
    },

    // 팝업 등록
    popupRegister() {
        window.location.href = '/popup/register';
    },

    // 팝업 수정
    popupEdit(popupId) {
        window.location.href = `/mypage/host/popup/${popupId}/edit`;
    },

    // 예약 관리
    reservationManage(popupId) {
        window.location.href = `/mypage/host/popup/${popupId}/reservation`;
    },
    // 채팅 페이지
    chat(reservationId) {
        const token = localStorage.getItem('accessToken') ||
            localStorage.getItem('authToken') ||
            sessionStorage.getItem('accessToken') ||
            sessionStorage.getItem('authToken');
        if (!token) {
            window.location.href = '/auth/login';
            return;
        }
        window.location.href = `/chat/${reservationId}?token=${encodeURIComponent(token)}`;
    },
    //팝업 통계 페이지
    popupStats(popupId) {
        window.location.href = `/mypage/host/popup/${popupId}/stats`;
    },

};

// 팝업 상세 페이지로 이동
function goToPopupDetail(popupId) {
    console.log('팝업 상세 이동:', popupId);

    if (!popupId || !/^\d+$/.test(String(popupId))) {
        console.warn('잘못된 팝업 ID:', popupId);
        alert('잘못된 팝업 정보입니다.');
        return;
    }

    window.location.href = `/popup/${encodeURIComponent(popupId)}`;
}

window.Pages = Pages;
window.goToPopupDetail = goToPopupDetail;