document.addEventListener("DOMContentLoaded", async () => {
    await loadComponents();
    initializeLayout();

    const section = document.querySelector(".content-section");

    // 리뷰 목록 불러오기
    async function loadReviews(page = 0, size = 10) {
        try {
            const data = await apiService.getMyReviews(page, size);
            renderReviews(data.content || []);
        } catch (e) {
            console.error(e);
            section.innerHTML = `<p class="error">리뷰 목록을 불러오는 중 오류가 발생했습니다.</p>`;
        }
    }

    function getStars(rating) {
        return Array.from({ length: 5 }, (_, i) => (i < rating ? "★" : "☆")).join("");
    }

    function renderReviews(reviews) {
        section.innerHTML = "";
        if (reviews.length === 0) {
            section.innerHTML = `<p class="empty">작성한 리뷰가 없습니다.</p>`;
            return;
        }

        reviews.forEach((r) => {
            const card = document.createElement("div");
            card.className = "card";

            const starsHtml = [1,2,3,4,5]
                .map(i => `<span class="rating-star ${i <= r.rating ? 'active' : ''}">★</span>`)
                .join("");

            card.innerHTML = `
      <div class="popup-info">
        <div class="popup-title">${escapeHtml(r.popupTitle || "팝업")}</div>

        <div class="meta-row">
          <span class="meta-val">${starsHtml}</span>
        </div>
        
        <div class="meta-row">
          <span class="meta-key">작성일자</span>
          <span class="meta-val">${fmt(r.createdAt)}</span>
        </div>


        <div class="meta-row">
          <span class="meta-key">내용</span>
          <span class="meta-val">${escapeHtml(r.content || "")}</span>
        </div>

        <div class="popup-action" style="display:flex; gap:8px; flex-wrap:wrap;">
          <button type="button" class="review-button" 
                  onclick="openEditModal(${r.id}, ${r.rating}, \`${r.content}\`)">수정</button>
          <button type="button" class="review-button" 
                  onclick="deleteReview(${r.id})">삭제</button>
        </div>
      </div>
    `;
            section.appendChild(card);
        });
    }

    // 첫 로딩
    await loadReviews();

    // 모달 컨테이너
    const modalContainer = document.createElement("div");
    document.body.appendChild(modalContainer);

    // 수정 모달 열기 (작성 폼과 동일 스타일)
    window.openEditModal = function (id, rating, content) {
        modalContainer.innerHTML = `
        <div class="modal-backdrop">
          <div class="modal">
            <h2 class="modal-title">리뷰 수정</h2>

            <div class="form-group">
              <label class="form-label">평점을 선택해주세요</label>
              <div class="rating-input" id="editRatingInput" data-rating="${rating}">
                ${[1,2,3,4,5].map(i =>
            `<span class="rating-star ${i <= rating ? 'active' : ''}" data-rating="${i}">★</span>`
        ).join("")}
              </div>
              <div class="rating-labels">
                <span class="rating-label ${rating===1?'active':''}" data-rating="1">매우 나쁨</span>
                <span class="rating-label ${rating===2?'active':''}" data-rating="2">나쁨</span>
                <span class="rating-label ${rating===3?'active':''}" data-rating="3">보통</span>
                <span class="rating-label ${rating===4?'active':''}" data-rating="4">좋음</span>
                <span class="rating-label ${rating===5?'active':''}" data-rating="5">매우 좋음</span>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">리뷰 내용</label>
              <textarea
                  class="review-textarea"
                  id="edit-content"
                  placeholder="팝업 스토어에 대한 솔직한 후기를 남겨주세요.&#10;방문 경험, 전시 내용, 굿즈, 서비스 등에 대해 자유롭게 작성해주세요. (10자 이상 1000자 이하)"
                  maxlength="1000"
              ></textarea>
              <div class="char-count"><span id="charCount">0</span>/1000</div>
              <div class="input-hint">최소 10자 이상 작성해주세요.</div>
            </div>

            <div class="modal-actions">
              <button class="button button-secondary" onclick="closeModal()">취소</button>
              <button class="button button-primary" onclick="saveReview(${id})">저장</button>
            </div>
          </div>
        </div>`;

        // 별점 이벤트
        const ratingInput = document.getElementById("editRatingInput");
        const stars = ratingInput.querySelectorAll(".rating-star");
        const labels = document.querySelectorAll(".rating-labels .rating-label");

        stars.forEach((star) => {
            star.addEventListener("click", () => {
                const selected = parseInt(star.dataset.rating, 10);
                ratingInput.dataset.rating = String(selected);

                stars.forEach((s) =>
                    s.classList.toggle("active", parseInt(s.dataset.rating, 10) <= selected)
                );
                labels.forEach((l) =>
                    l.classList.toggle("active", parseInt(l.dataset.rating, 10) === selected)
                );
            });
        });

        // 내용/글자수 초기화 + 카운트
        const textarea = document.getElementById("edit-content");
        const countEl = document.getElementById("charCount");
        textarea.value = content || "";
        countEl.textContent = String(textarea.value.length);
        textarea.addEventListener("input", () => {
            countEl.textContent = String(textarea.value.length);
        });
    };

    // 모달 닫기
    window.closeModal = function () {
        modalContainer.innerHTML = "";
    };

    // 리뷰 저장 (PUT) - 10자 미만/평점 미선택 시 alert
    window.saveReview = async function (id) {
        const content = document.getElementById("edit-content").value.trim();
        const ratingStr = (document.getElementById("editRatingInput")?.dataset.rating) || "";
        const rating = parseInt(ratingStr, 10);

        if (!rating || rating < 1 || rating > 5) {
            alert("평점을 선택해주세요.");
            return;
        }
        if (content.length < 10) {
            alert("리뷰는 10자 이상 입력해주세요.");
            return;
        }

        try {
            await apiService.updateReview(id, { content, rating });
            alert("리뷰가 수정되었습니다.");
            closeModal();
            await loadReviews();
        } catch (e) {
            console.error(e);
            alert("리뷰 수정 실패");
        }
    };

    // 리뷰 삭제 (DELETE)
    window.deleteReview = async function (id) {
        if (!confirm(`리뷰 ${id}을(를) 삭제하시겠습니까?`)) return;

        try {
            await apiService.deleteReview(id);
            alert("삭제 완료");
            await loadReviews();
        } catch (e) {
            console.error(e);
            alert("리뷰 삭제 실패");
        }
    };
});

function fmt(iso) {
    if (!iso) return '-';
    try { return new Date(iso).toLocaleString(); } catch { return iso; }
}

function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}