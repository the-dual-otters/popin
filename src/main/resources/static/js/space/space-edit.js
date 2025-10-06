class SpaceEditManager {
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

        const form = document.getElementById("space-edit-form");
        if (form) form.addEventListener("submit", (e) => this.handleSubmit(e));

        document
            .querySelectorAll('[data-act="back"], [data-act="list"]')
            .forEach((btn) => {
                btn.addEventListener("click", () => this.goDetail());
            });

        this.setupDateValidation();
        this.setupAddressSearch();

        try {
            const data = await apiService.getSpace(id);
            this.fillForm(data);
        } catch (err) {
            console.error("불러오기 실패:", err);
            alert("공간 정보를 불러오지 못했습니다.");
        }
    }

    setupDateValidation() {
        const startDateInput = document.getElementById("startDate");
        const endDateInput = document.getElementById("endDate");

        startDateInput?.addEventListener("change", (e) => {
            if (e.target.value) {
                endDateInput.min = e.target.value;
                if (endDateInput.value && endDateInput.value < e.target.value) {
                    endDateInput.value = e.target.value;
                }
            }
        });

        const today = new Date().toISOString().split("T")[0];
        if (startDateInput) startDateInput.min = today;
    }

    setupAddressSearch() {
        const btn = document.getElementById("btn-search-address");
        btn?.addEventListener("click", function () {
            new daum.Postcode({
                oncomplete: function (data) {
                    document.getElementById("roadAddress").value = data.roadAddress;
                    document.getElementById("jibunAddress").value = data.jibunAddress;
                    document.getElementById("detailAddress")?.focus();
                },
            }).open();
        });
    }

    fillForm(space) {
        const set = (id, v) => {
            const el = document.getElementById(id);
            if (el) el.value = v ?? "";
        };

        set("title", space.title);
        set("description", space.description);
        set("areaSize", space.areaSize);
        set("startDate", space.startDate?.slice(0, 10));
        set("endDate", space.endDate?.slice(0, 10));
        set("rentalFee", space.rentalFee);
        set("contactPhone", space.contactPhone);

        if (space.venue) {
            set("roadAddress", space.venue.roadAddress);
            set("jibunAddress", space.venue.jibunAddress);
            set("detailAddress", space.venue.detailAddress);
        }

        const preview = document.getElementById("imagePreview");
        if (preview) preview.src = this.getImageUrl(space.coverImageUrl);
    }

    getImageUrl(u) {
        if (!u) return "/images/noimage.png";
        if (u.startsWith("http")) return u;
        if (u.startsWith("/")) return u;
        return `/uploads/${u}`;
    }

    async handleSubmit(e) {
        e.preventDefault();

        const submitBtn =
            e.submitter || document.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        try {
            if (!this.validateForm()) return;

            const fd = new FormData();
            const v = (id) => document.getElementById(id)?.value?.trim() ?? "";

            fd.append("roadAddress", v("roadAddress"));
            fd.append("jibunAddress", v("jibunAddress"));
            fd.append("detailAddress", v("detailAddress"));

            fd.append("title", v("title"));
            fd.append("description", v("description"));
            fd.append("areaSize", v("areaSize"));
            fd.append("startDate", v("startDate"));
            fd.append("endDate", v("endDate"));
            fd.append("rentalFee", v("rentalFee"));
            fd.append("contactPhone", v("contactPhone"));

            const img = document.getElementById("image")?.files?.[0];
            if (img) fd.append("image", img);

            await apiService.updateSpace(this.spaceId, fd);
            alert("수정이 완료되었습니다.");
            this.goDetail();
        } catch (err) {
            console.error("수정 실패:", err);
            this.handleError(err);
        } finally {
            if (submitBtn) submitBtn.disabled = false;
        }
    }

    validateForm() {
        const requiredFields = [
            { id: "roadAddress", name: "주소" },
            { id: "title", name: "제목" },
            { id: "areaSize", name: "면적" },
            { id: "startDate", name: "임대 시작일" },
            { id: "endDate", name: "임대 종료일" },
            { id: "rentalFee", name: "임대료" },
            { id: "contactPhone", name: "연락처" },
        ];

        for (const field of requiredFields) {
            const el = document.getElementById(field.id);
            if (!el?.value?.trim()) {
                alert(`${field.name}을(를) 입력해주세요.`);
                el?.focus();
                return false;
            }
        }

        const phone = document.getElementById("contactPhone").value.trim();
        const phoneRegex = /^[0-9-+()\s]+$/;
        if (phone && !phoneRegex.test(phone)) {
            alert("올바른 전화번호 형식이 아닙니다.");
            document.getElementById("contactPhone").focus();
            return false;
        }

        const s = document.getElementById("startDate").value;
        const e = document.getElementById("endDate").value;
        if (s && e && e < s) {
            alert("종료일은 시작일 이후여야 합니다.");
            document.getElementById("endDate").focus();
            return false;
        }

        return true;
    }

    handleError(err) {
        const msg = String(err?.message || "");

        if (msg.includes("401")) {
            alert("로그인이 필요합니다.");
        } else if (msg.includes("400") || msg.includes("422")) {
            if (err.response?.data?.errors) {
                const errors = err.response.data.errors;
                const errorMsg = Object.values(errors).join("\n");
                alert("입력 정보를 확인해주세요:\n" + errorMsg);
            } else {
                alert("입력 정보를 확인해주세요.");
            }
        } else {
            alert("수정 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    goDetail() {
        Pages.spaceDetail(this.spaceId);
    }

    goList() {
        Pages.spaceList();
    }
}

window.SpaceEditManager = SpaceEditManager;
