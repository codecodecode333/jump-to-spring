// 모든 페이지에서 쓰는 공통 스크립트 (layout.html 에서 로드)

document.addEventListener("DOMContentLoaded", () => {
    // <a href="/questions/1" method="delete" data-confirm="정말로 삭제하시겠습니까?">삭제</a>
    // 링크(a)는 GET 만 보낼 수 있으므로, 클릭하면 숨은 폼을 만들어 POST + _method + CSRF 토큰으로 전송한다. (14168 43강)
    document.querySelectorAll("a[method]").forEach((anchor) => {
        anchor.addEventListener("click", (e) => {
            e.preventDefault(); // 기본 링크 이동 막기

            const confirmMsg = anchor.dataset.confirm;
            if (confirmMsg && !confirm(confirmMsg)) return;

            const form = document.createElement("form");
            form.method = "POST";
            form.action = anchor.getAttribute("href");

            const method = anchor.getAttribute("method").toUpperCase();
            if (method !== "POST") addHidden(form, "_method", method); // DELETE, PUT → hiddenmethod 필터가 변환

            // 스프링 시큐리티 CSRF 토큰 (layout.html 의 <meta name="_csrf">)
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfParam = document.querySelector('meta[name="_csrf_parameter"]')?.content;
            if (csrfToken && csrfParam) addHidden(form, csrfParam, csrfToken);

            document.body.appendChild(form);
            form.submit();
        });
    });
});

function addHidden(form, name, value) {
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = value;
    form.appendChild(input);
}
