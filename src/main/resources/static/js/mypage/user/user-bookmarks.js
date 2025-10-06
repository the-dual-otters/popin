document.addEventListener('DOMContentLoaded', async function () {

    // 로그인 체크
    const token = apiService.getStoredToken();
    if (!token) {
        const section = document.querySelector('.content-section');
        section.innerHTML = `
            <div class="card" style="text-align:center; padding:40px;">
                <p style="color:#666; margin-bottom:20px;">로그인이 필요한 서비스입니다.</p>
                <button onclick="window.location.href='/auth/login'" 
                        style="padding:10px 20px; background:#4B5AE4; color:white; border:none; border-radius:8px;">
                    로그인하기
                </button>
            </div>
        `;
        return;
    }

    // =============================
    // 내 북마크 (API 연동)
    // =============================
    const section = document.querySelector('.content-section');

    const bookmarkContainer = document.createElement('div');
    bookmarkContainer.className = 'card';
    bookmarkContainer.innerHTML = `<h2 class="mypage-title">내 북마크</h2><div id="bookmark-list"></div>`;
    section.appendChild(bookmarkContainer);

    try {
        // 북마크 목록 호출
        const data = await apiService.get('/bookmarks');
        const bookmarks = data.bookmarks || [];

        const listEl = document.getElementById('bookmark-list');

        if (bookmarks.length === 0) {
            listEl.innerHTML = `<p style="color:#777;">북마크한 팝업이 없습니다.</p>`;
        } else {
            bookmarks.forEach(b => {
                listEl.appendChild(renderBookmarkCard(b));
            });
        }
    } catch (e) {
        console.error('북마크 목록 불러오기 실패:', e);
        const listEl = document.getElementById('bookmark-list');
        if (listEl) {
            listEl.innerHTML = `<p style="color:red; text-align:center;">북마크를 불러오는 중 오류가 발생했습니다.</p>`;
        }
    }
});

// =============================
// 북마크 카드 렌더링 함수
// =============================
function renderBookmarkCard(b) {
    const item = document.createElement('div');
    item.className = 'popup-card';

    const popup = b.popup;  // 북마크 안의 팝업 정보

    const period = popup && popup.periodText
        ? `<div class="popup-period">${popup.periodText}</div>`
        : '';

    item.innerHTML = `
        <div class="popup-image-wrapper">
            ${popup && popup.mainImageUrl && popup.mainImageUrl.trim() !== ""
        ? `<img src="${popup.mainImageUrl}" class="popup-image" alt="${popup.popupTitle}">`
        : `<div class="popup-image placeholder">이미지를 찾을 수 없습니다.</div>`}
        </div>
        <div class="popup-info">
            <div class="popup-title-wrapper">
                <div class="popup-title">${popup ? popup.popupTitle : '제목없음'}</div>
                <button class="bookmark-heart" title="북마크 삭제">
                  <svg xmlns="http://www.w3.org/2000/svg" 
                       width="30" height="30" viewBox="0 0 24 24" 
                       fill="red" stroke="red" stroke-width="2" 
                       stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20.8 4.6c-1.8-1.9-4.8-1.9-6.6 0L12 6.9l-2.2-2.3c-1.8-1.9-4.8-1.9-6.6 0-1.9 1.8-1.9 4.8 0 6.6l2.2 2.2L12 21l6.6-7.6 2.2-2.2c1.8-1.8 1.8-4.8 0-6.6z"></path>
                  </svg>
                </button>
            </div>
            <div class="popup-summary">${popup && popup.description ? popup.description : '상세설명'}</div>
            ${period}
            <div class="popup-action">
                <a class="popup-link" href="/popup/${popup ? popup.popupId : ''}">자세히 보기 &gt;</a>
            </div>
        </div>
    `;

    // 하트 버튼 이벤트 등록
    const heartBtn = item.querySelector('.bookmark-heart');
    heartBtn.addEventListener('click', async () => {
        if (confirm('이 북마크를 삭제하시겠습니까?')) {
            try {
                await apiService.delete(`/bookmarks/${popup.popupId}`);
                item.remove(); // 화면에서 카드 제거
            } catch (err) {
                console.error('북마크 삭제 실패:', err);
                alert('북마크 삭제에 실패했습니다.');
            }
        }
    });

    return item;
}
