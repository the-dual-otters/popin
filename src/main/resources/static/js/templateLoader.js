// 간단한 템플릿 로더
class TemplateLoader {
    static cache = new Map();

    static async load(templateName, data = {}) {
        // 캐시에서 확인
        if (!this.cache.has(templateName)) {
            try {
                const response = await fetch(`/templates/${templateName}.html`);
                if (!response.ok) {
                    throw new Error(`Template not found: ${templateName}`);
                }
                const html = await response.text();
                this.cache.set(templateName, html);
            } catch (error) {
                console.error('Template load failed:', error);
                return `<div class="alert alert-error">템플릿을 불러올 수 없습니다: ${templateName}</div>`;
            }
        }

        let template = this.cache.get(templateName);

        // 데이터 바인딩 - {{변수명}} 치환
        Object.keys(data).forEach(key => {
            const value = data[key];
            template = template.replace(
                new RegExp(`{{${key}}}`, 'g'),
                value !== null && value !== undefined ? value : ''
            );
        });

        return template;
    }

    // 캐시 클리어 (개발용)
    static clearCache() {
        this.cache.clear();
        console.log('템플릿 캐시 클리어됨');
    }
}