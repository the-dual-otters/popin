// reservation-manage.js
const ReservationManagePage = {
    popupId: null,

    async init() {
        try {
            // 1) 쿼리스트링에서 가져오기
            const params = new URLSearchParams(window.location.search);
            this.popupId = params.get("popupId");

            // 2) 없으면 pathname에서 숫자 추출 (/reservation/68 등)
            if (!this.popupId) {
                const pathParts = window.location.pathname.split("/").filter(Boolean);
                const idPart = pathParts.find(p => /^\d+$/.test(p));
                if (idPart) {
                    this.popupId = idPart;
                }
            }

            // 3) 그래도 없으면 에러
            if (!this.popupId) {
                alert("팝업 ID가 필요합니다.");
                return;
            }

            // 원래 기능 유지
            await this.loadPopup();
            await this.loadReservations();

            // 예약 설정 초기화
            if (window.ReservationSettings) {
                ReservationSettings.init(this.popupId);
            }
        } catch (error) {
            console.error("예약 관리 페이지 초기화 실패:", error);
            alert("페이지를 불러오는 중 오류가 발생했습니다.");
        }
    },

    // 팝업 정보 로드
    async loadPopup() {
        try {
            const popup = await apiService.getPopup(this.popupId);
            document.getElementById("popup-title").textContent = popup.title || "제목 없음";
        } catch (err) {
            console.error("팝업 정보 로딩 실패:", err);
            alert("팝업 정보를 불러오지 못했습니다.");
        }
    },

    // 예약자 목록 로드
    async loadReservations() {
        try {
            const reservations = await apiService.getPopupReservations(this.popupId);

            this.renderStats(reservations);
            this.renderReservations(reservations);
        } catch (err) {
            console.error("예약 목록 로딩 실패:", err);
            alert("예약 목록을 불러오지 못했습니다.");
        }
    },

    // 예약 통계 렌더링
    renderStats(reservations) {
        const stats = {
            reserved: 0,
            visited: 0,
            cancelled: 0,
            total: reservations.length
        };

        reservations.forEach(r => {
            if (r.status === "RESERVED") stats.reserved++;
            if (r.status === "VISITED") stats.visited++;
            if (r.status === "CANCELLED") stats.cancelled++;
        });

        document.getElementById("reserved-count").textContent = stats.reserved;
        document.getElementById("visited-count").textContent = stats.visited;
        document.getElementById("total-count").textContent = stats.total;
    },

    // 예약자 목록 렌더링
    renderReservations(reservations) {
        const container = document.getElementById("reservation-list");
        container.innerHTML = "";

        if (!reservations || reservations.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="icon">📅</div>
                    <div class="text">예약자가 없습니다.</div>
                </div>`;
            return;
        }

        reservations.forEach(r => {
            const card = this.createReservationCard(r);
            container.appendChild(card);
        });
    },

    // 예약 카드 생성
    createReservationCard(r) {
        const card = document.createElement("div");
        card.className = "reservation-card";

        const top = document.createElement("div");
        top.className = "reservation-top";
        top.innerHTML = `
            <div class="reservation-status status-${r.status.toLowerCase()}">${this.getStatusText(r.status)}</div>
            <div class="reservation-name">${r.name || "이름 없음"}</div>
            <div class="reservation-phone">${r.phone || "연락처 없음"}</div>
        `;
        card.appendChild(top);

        const body = document.createElement("div");
        body.className = "reservation-body";
        body.innerHTML = `
            <div class="reservation-date">예약일: ${formatReservationDate(r.reservationDate)}</div>
            <div class="reservation-party">인원: ${r.partySize || 1}명</div>
        `;
        card.appendChild(body);

        const actions = document.createElement("div");
        actions.className = "reservation-actions";

        if (r.status === "RESERVED") {
            const btnVisit = document.createElement("button");
            btnVisit.className = "btn-visit";
            btnVisit.textContent = "방문완료";
            btnVisit.addEventListener("click", () => this.markAsVisited(r.id));

            const btnCancel = document.createElement("button");
            btnCancel.className = "btn-cancel";
            btnCancel.textContent = "취소";
            btnCancel.addEventListener("click", () => this.cancelReservation(r.id));

            actions.appendChild(btnVisit);
            actions.appendChild(btnCancel);
        }

        card.appendChild(actions);
        return card;
    },

    getStatusText(status) {
        const map = {
            RESERVED: "예약됨",
            VISITED: "방문완료",
            CANCELLED: "취소됨"
        };
        return map[status] || status;
    },

    async markAsVisited(id) {
        if (!confirm("이 예약을 방문완료 처리하시겠습니까?")) return;
        try {
            await apiService.visitReservation(id);
            alert("방문완료 처리되었습니다.");
            await this.loadReservations();
        } catch (err) {
            console.error("방문완료 실패:", err);
            alert("방문완료 처리에 실패했습니다.");
        }
    },

    async cancelReservation(id) {
        if (!confirm("이 예약을 취소하시겠습니까?")) return;
        try {
            await apiService.cancelReservation(id);ㄴ
            await this.loadReservations();
        } catch (err) {
            console.error("취소 실패:", err);
            alert("예약 취소에 실패했습니다.");
        }
    },



    switchTab(tabName) {
        // 모든 탭 버튼/콘텐츠 초기화
        document.querySelectorAll(".tab-button").forEach(btn => btn.classList.remove("active"));
        document.querySelectorAll(".tab-content").forEach(tab => tab.classList.remove("active"));

        // 선택된 탭 활성화
        const selectedBtn = document.querySelector(`.tab-button[onclick*="${tabName}"]`);
        const selectedTab = document.getElementById(`tab-${tabName}`);
        if (selectedBtn) selectedBtn.classList.add("active");
        if (selectedTab) selectedTab.classList.add("active");
    }

};

// 날짜 포매팅
function formatReservationDate(dateString) {
    if (!dateString) return "-";

    try {
        const date = new Date(dateString);
        return date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (error) {
        return dateString; // 파싱 실패시 원본 반환
    }
}

window.ReservationManagePage = ReservationManagePage;
