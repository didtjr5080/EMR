// EMR 처치 업무 시스템 공통 JS

function pad2(value) {
    return String(value).padStart(2, "0");
}

function updateLocalClock() {
    const clock = document.getElementById("serverClock");
    if (!clock) return;

    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = pad2(now.getMonth() + 1);
    const dd = pad2(now.getDate());
    const hh = pad2(now.getHours());
    const mi = pad2(now.getMinutes());
    const ss = pad2(now.getSeconds());

    clock.textContent = `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
}

setInterval(updateLocalClock, 1000);

// 대시보드와 완료 기록은 10초마다 자동 갱신
// 업무 등록 중인 페이지에서 너무 자주 새로고침되면 입력 중 내용이 날아가므로 dashboard/logs에만 적용
(function autoRefreshReadOnlyPages() {
    const path = window.location.pathname;
    const isReadOnlyPage = path === "/" || path === "/logs";

    if (!isReadOnlyPage) return;

    setTimeout(() => {
        window.location.reload();
    }, 10000);
})();

    // 날짜/시간 포맷 함수
    function formatKoreanDateTime(value) {
        if (!value) return "-";

        // Firestore Timestamp 객체 처리
        if (value.toDate) {
            value = value.toDate();
        }

        // 문자열 처리
        if (typeof value === "string") {
            // [Asia/Seoul] 등 제거
            value = value.replace(/\[.*?\]/g, "");
            // 마이크로초 6자리 → 밀리초 3자리로 자르기
            value = value.replace(/\.(\d{3})\d+/, ".$1");
            value = value.trim();
            value = new Date(value);
        }

        if (!(value instanceof Date) || isNaN(value.getTime())) {
            return "-";
        }

        return value.toLocaleString("ko-KR", {
            timeZone: "Asia/Seoul",
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false
        });
    }

    function formatKoreanTime(value) {
        if (!value) return "-";
        if (value.toDate) {
            value = value.toDate();
        }
        if (typeof value === "string") {
            value = value.replace(/\[.*?\]/g, "");
            value = value.replace(/\.(\d{3})\d+/, ".$1");
            value = value.trim();
            value = new Date(value);
        }
        if (!(value instanceof Date) || isNaN(value.getTime())) {
            return "-";
        }
        return value.toLocaleTimeString("ko-KR", {
            timeZone: "Asia/Seoul",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false
        });
    }