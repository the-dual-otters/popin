document.addEventListener('DOMContentLoaded', async function () {
    await loadComponents();   // header/footer 로드
    initializeLayout();

    try {
        // =============================
        // 내 미션셋 (API 연동)
        // =============================
        const section = document.querySelector('.content-section');

        const missionContainer = document.createElement('div');
        missionContainer.className = 'card';
        missionContainer.innerHTML = `<h2 class="mypage-title">진행 중인 미션</h2><div id="mission-list"></div>`;
        section.appendChild(missionContainer);

        const completedContainer = document.createElement('div');
        completedContainer.className = 'card';
        completedContainer.innerHTML = `<h2 class="mypage-title">완료된 미션</h2><div id="completed-mission-list"></div>`;
        section.appendChild(completedContainer);

        try {
            const missions = await apiService.get('/user-missions/my-missions');
            const activeListEl = document.getElementById('mission-list');
            const completedListEl = document.getElementById('completed-mission-list');

            const active = missions.filter(m => !m.cleared);
            const completed = missions.filter(m => m.cleared);

            if (active.length === 0) {
                activeListEl.innerHTML = `<p style="color:#777;">진행 중인 미션이 없습니다.</p>`;
            } else {
                active.forEach(m => {
                    activeListEl.appendChild(renderPopupCard(m, "이어하기", "popup-link"));
                });
            }

            if (completed.length === 0) {
                completedListEl.innerHTML = `<p style="color:#777;">완료된 미션이 없습니다.</p>`;
            } else {
                completed.forEach(m => {
                    completedListEl.appendChild(renderPopupCard(m, "다시보기", "popup-link"));
                });
            }
        } catch (e) {
            console.error('미션 목록 불러오기 실패:', e);
        }

    } catch (err) {
        console.error(err);
        const mc = document.getElementById('content-section') || document.querySelector('.content-section');
        if (mc) mc.innerHTML = `
            <p style="color:red; text-align:center;">로그인 후 이용 가능합니다.</p>
        `;
    }
});

// =============================
// 팝업 카드 렌더링 유틸 함수
// =============================
function renderPopupCard(m, actionText, linkClass) {
    const popup = m.popup;

    const item = document.createElement('div');
    item.className = 'popup-card';

    // 팝업 종료 여부 확인
    const isEnded = popup.status && popup.status.toUpperCase() === 'ENDED';

    item.innerHTML = `
        <div class="popup-image-wrapper">
            ${popup.mainImageUrl && popup.mainImageUrl.trim() !== ""
        ? `<img src="${popup.mainImageUrl}" class="popup-image" alt="${popup.popupTitle}">`
        : `<div class="popup-image placeholder">이미지를 찾을 수 없습니다.</div>`}
        </div>
        <div class="popup-info">
            <div class="popup-title">${popup.popupTitle}</div>
            <div class="popup-summary">${popup.description || '상세설명'}</div>
            <div class="popup-period">${popup.periodText || ''}</div>
            <div class="popup-action">
                ${!isEnded ? `<a class="${linkClass}" href="/missions/${m.missionSetId}">${actionText} &gt;</a>` : ""}
            </div>
        </div>
    `;
    return item;
}
