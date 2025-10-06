const IMG_PLACEHOLDER =
    'data:image/svg+xml;utf8,' +
    encodeURIComponent(
        `<svg xmlns="http://www.w3.org/2000/svg" width="160" height="120">
       <rect width="100%" height="100%" fill="#f2f2f2"/>
       <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
             fill="#888" font-size="14">no image</text>
     </svg>`
    );

class SpaceDetailManager {
    constructor() {
        this.spaceId = null;
    }

    getSpaceId() {
        const queryId = new URLSearchParams(location.search).get("id");
        if (queryId) return queryId;

        const pathParts = window.location.pathname.split("/");
        return pathParts[pathParts.length - 1];
    }

    async initialize() {
        const id = this.getSpaceId();
        if (!id) {
            alert("잘못된 접근입니다.");
            this.goList();
            return;
        }
        this.spaceId = id;

        try {
            this.showLoading();
            const space = await apiService.getSpace(id);
            this.render(space);
            this.bindActions(space);
        } catch (err) {
            console.error("상세 불러오기 실패:", err);
            this.showError("상세 정보를 불러오지 못했습니다.");
        } finally {
            this.hideLoading();
        }
    }

    showLoading() {
        const el = document.getElementById("loading");
        const cont = document.getElementById("detailContent");
        if (el) el.style.display = "block";
        if (cont) cont.style.display = "none";
    }

    hideLoading() {
        const el = document.getElementById("loading");
        if (el) el.style.display = "none";
    }

    render(space) {
        const $ = (id) => document.getElementById(id);
        const img = $("heroImage");

        if (img) {
            img.src = this.getThumbUrl(space);
            img.onerror = function () {
                this.onerror = null;
                this.src = IMG_PLACEHOLDER;
            };
        }

        if ($("spaceTitle")) $("spaceTitle").textContent = space.title || "(제목 없음)";
        if ($("ownerName")) $("ownerName").textContent = space.owner?.name || "-";
        if ($("areaSize")) $("areaSize").textContent = (space.areaSize ?? "-") + " ㎡";
        if ($("rentalFee")) $("rentalFee").textContent = this.formatRentalFee(space.rentalFee);
        if ($("address")) $("address").textContent = this.formatAddress(space);
        if ($("period")) $("period").textContent = this.formatPeriod(space.startDate, space.endDate);
        if ($("contactPhone")) $("contactPhone").textContent = space.contactPhone || "-";
        if ($("description")) $("description").textContent = space.description || "";

        const ownerActions = document.getElementById("ownerActions");
        if (ownerActions) ownerActions.style.display = space.mine ? "flex" : "none";

        const cont = document.getElementById("detailContent");
        if (cont) cont.style.display = "block";
    }

    bindActions(space) {
        const id = space?.id ?? space?.spaceId ?? space?.space_id;
        document.querySelectorAll("[data-act]").forEach((el) => {
            el.addEventListener("click", () => {
                const act = el.getAttribute("data-act");
                if (act === "list") this.goList();
                else if (act === "edit") this.editSpace(id);
                else if (act === "delete") this.deleteSpace(id);
                else if (act === "reserve") this.reserveSpace(id);
            });
        });
    }

    async reserveSpace(spaceId) {
        const modal = document.getElementById("reserveModal");
        modal.classList.remove("hidden");

        try {
            const response = await apiService.get("/hosts/popups");
            const popups = response.content || response.data || []

            const select = document.getElementById("popupSelect");
            select.innerHTML = '<option value="">팝업을 선택하세요</option>';

            if (popups.length === 0) {
                select.innerHTML = '<option value="">등록된 팝업이 없습니다</option>';
                document.getElementById("reserveSubmit").disabled = true;
            } else {
                popups.forEach((p) => {
                    const option = document.createElement("option");
                    option.value = p.id;
                    option.textContent = p.title;
                    select.appendChild(option);
                });
                document.getElementById("reserveSubmit").disabled = false;
            }
        } catch (e) {
            console.error("팝업 목록 불러오기 실패:", e);
            alert("내 팝업 목록을 불러오지 못했습니다.");
            modal.classList.add("hidden");
            return;
        }

        const today = new Date().toISOString().split("T")[0];
        document.getElementById("startDate").min = today;
        document.getElementById("endDate").min = today;

        document.getElementById("startDate").addEventListener("change", function () {
            document.getElementById("endDate").min = this.value;
        });

        document.getElementById("reserveSubmit").onclick = async () => {
            const popupId = Number(document.getElementById("popupSelect").value);
            const startDate = document.getElementById("startDate").value;
            const endDate = document.getElementById("endDate").value;
            const message = document.getElementById("message").value.trim();

            if (!popupId) {
                alert("팝업을 선택하세요.");
                return;
            }
            if (!startDate) {
                alert("시작일을 입력하세요.");
                return;
            }
            if (!endDate) {
                alert("종료일을 입력하세요.");
                return;
            }
            if (new Date(startDate) > new Date(endDate)) {
                alert("종료일은 시작일보다 늦어야 합니다.");
                return;
            }

            const body = {
                spaceId: Number(spaceId),
                popupId,
                startDate,
                endDate,
                message: message || null,
                contactPhone: null,
            };

            try {
                const result = await apiService.post("/space-reservations", body);
                alert(`예약 신청이 완료되었습니다! (ID: ${result.id})`);
                modal.classList.add("hidden");

                document.getElementById("popupSelect").value = "";
                document.getElementById("startDate").value = "";
                document.getElementById("endDate").value = "";
                document.getElementById("message").value = "";
            } catch (error) {
                let userMessage = "예약 신청에 실패했습니다.";
                if (error.message && error.message.includes("400")) {
                    userMessage = "입력 정보를 확인해주세요.";
                } else if (error.message && error.message.includes("401")) {
                    userMessage = "로그인이 필요합니다.";
                } else if (error.message && error.message.includes("409")) {
                    userMessage = "해당 기간에 이미 예약이 있습니다.";
                } else if (error.message && error.message.includes("500")) {
                    userMessage = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                }
                alert(userMessage);
            }
        };

        document.getElementById("reserveCancel").onclick = () => {
            modal.classList.add("hidden");
            document.getElementById("popupSelect").value = "";
            document.getElementById("startDate").value = "";
            document.getElementById("endDate").value = "";
            document.getElementById("message").value = "";
        };

        modal.addEventListener("click", (e) => {
            if (e.target === modal) {
                modal.classList.add("hidden");
            }
        });
    }

    getThumbUrl(space) {
        if (space?.coverImageUrl) return `${window.location.origin}${space.coverImageUrl}`;
        if (space?.coverImage) return `${window.location.origin}${space.coverImage}`;
        const u = space?.thumbnailUrl || space?.imageUrl || space?.imagePath || space?.thumbnailPath || "";
        if (!u) return IMG_PLACEHOLDER;
        if (u.startsWith("http")) return u;
        if (u.startsWith("/")) return u;
        return `/uploads/${u}`;
    }

    formatAddress(space) {
        if (space?.address && space.address !== "주소 정보 없음") {
            return space.address;
        }
        if (space?.venue) {
            const venue = space.venue;
            let address = "";
            if (venue.roadAddress) {
                address = venue.roadAddress;
            } else if (venue.jibunAddress) {
                address = venue.jibunAddress;
            }
            if (venue.detailAddress && address) {
                address += ` ${venue.detailAddress}`;
            } else if (venue.detailAddress && !address) {
                address = venue.detailAddress;
            }
            return address || "주소 정보 없음";
        }
        return "주소 정보 없음";
    }

    formatRentalFee(amount) {
        if (!amount && amount !== 0) return "-";
        return `${amount} 만원`;
    }

    formatPeriod(s, e) {
        const f = (d) => {
            if (!d) return "-";
            try {
                return new Date(d).toLocaleDateString("ko-KR");
            } catch {
                return d;
            }
        };
        return `${f(s)} ~ ${f(e)}`;
    }

    goList() {
        Pages.spaceList();
    }

    editSpace(id) {
        Pages.spaceEdit(id);
    }

    async deleteSpace(id) {
        if (!confirm("정말 삭제하시겠습니까?")) return;
        try {
            await apiService.deleteSpace(id);
            alert("삭제되었습니다.");
            this.goList();
        } catch (e) {
            console.error("삭제 실패:", e);
            alert("삭제 실패");
        }
    }

    showError(message) {
        const cont = document.getElementById("detailContent");
        if (cont) {
            cont.style.display = "block";
            cont.innerHTML = `<div class="error-state">${message}</div>`;
        }
    }
}

window.SpaceDetailManager = SpaceDetailManager;
