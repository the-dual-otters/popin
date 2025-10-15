document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");

    if (token) {
        // JWT 저장
        localStorage.setItem("accessToken", token);
        console.log("✅ 소셜 로그인 성공, 토큰 저장 완료:", token);

        // JWT payload 로그 (optional)
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            console.log("JWT Payload:", payload);
        } catch (e) {
            console.warn("⚠️ 토큰 디코딩 실패:", e);
        }

        // 로그인 완료 안내
        setTimeout(() => {
            alert("로그인 성공했습니다.");
            window.location.href = "/";
        }, 500);
    } else {
        alert("로그인 실패 또는 토큰이 전달되지 않았습니다.");
        window.location.href = "/auth/login";
    }
});
