<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>

    <#if section = "header">
    <#elseif section = "form">

        <div class="iact-logo-area">
            <img src="${url.resourcesPath}/img/logo.png" alt="iAct Logo" class="iact-logo">
            <span class="iact-brand-name">iAct</span>
            <span class="iact-brand-sub">Can Tho University</span>
        </div>

        <div class="iact-page-header">
            <div class="iact-status-icon-wrap">
                <div class="iact-status-icon iact-status-icon-primary">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                        <polyline points="22 4 12 14.01 9 11.01"/>
                    </svg>
                </div>
            </div>
            <h1 class="iact-page-title">Thông báo</h1>
            <p class="iact-page-desc">${message.summary}</p>
        </div>

        <#if requiredActions?? && (requiredActions?size > 0)>
            <div class="iact-alert iact-alert-warning iact-mb-8">
                <span class="iact-alert-icon">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                        <line x1="12" y1="9" x2="12" y2="13"/>
                        <line x1="12" y1="17" x2="12.01" y2="17"/>
                    </svg>
                </span>
                <div>
                    <strong>Hành động bắt buộc:</strong>
                    <#list requiredActions as reqAction>
                        <div>${msg("requiredAction.${reqAction}")}</div>
                    </#list>
                </div>
            </div>
        </#if>

        <div class="iact-step-actions">
            <#if skipLink??>
            <#else>
                <#if pageRedirectUri?has_content>
                    <p class="iact-alert iact-alert-info iact-mb-6">
                        Hệ thống đang chuẩn bị chuyển hướng cho bạn.
                    </p>
                    <a href="${pageRedirectUri}" class="iact-btn iact-btn-primary">
                        ${kcSanitize(msg("backToApplication"))?no_esc}
                    </a>
                <#elseif actionUri?has_content>
                    <a href="${actionUri}" class="iact-btn iact-btn-primary">
                        ${kcSanitize(msg("proceedWithAction"))?no_esc}
                    </a>
                <#elseif (client.baseUrl)?has_content>
                    <a href="${client.baseUrl}" class="iact-btn iact-btn-primary">
                        ${kcSanitize(msg("backToApplication"))?no_esc}
                    </a>
                </#if>
            </#if>

            <a href="${url.loginUrl}" class="iact-btn iact-btn-ghost">
                Quay lại đăng nhập
            </a>
        </div>

    </#if>
</@layout.registrationLayout>
