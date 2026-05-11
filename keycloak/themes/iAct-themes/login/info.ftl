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
            <h1 class="iact-page-title">Thong bao</h1>
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
                    <strong>Hanh dong bat buoc:</strong>
                    <#list requiredActions as reqAction>
                        <div>${msg("requiredAction.${reqAction}")}</div>
                    </#list>
                </div>
            </div>
        </#if>

        <div style="display:flex; flex-direction:column; gap:0.75rem;">
            <#if skipLink??>
            <#else>
                <#if pageRedirectUri?has_content>
                    <p class="iact-alert iact-alert-info iact-mb-6" style="animation:pulse 2s ease-in-out infinite;">
                        Dang tu dong chuyen huong...
                    </p>
                    <a href="${pageRedirectUri}" class="iact-btn iact-btn-primary">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <line x1="5" y1="12" x2="19" y2="12"/>
                            <polyline points="12 5 19 12 12 19"/>
                        </svg>
                        ${kcSanitize(msg("backToApplication"))?no_esc}
                    </a>
                <#elseif actionUri?has_content>
                    <a href="${actionUri}" class="iact-btn iact-btn-primary">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <polyline points="20 6 9 17 4 12"/>
                        </svg>
                        ${kcSanitize(msg("proceedWithAction"))?no_esc}
                    </a>
                <#elseif (client.baseUrl)?has_content>
                    <a href="${client.baseUrl}" class="iact-btn iact-btn-primary">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                            <polyline points="9 22 9 12 15 12 15 22"/>
                        </svg>
                        ${kcSanitize(msg("backToApplication"))?no_esc}
                    </a>
                </#if>
            </#if>

            <a href="${url.loginUrl}" class="iact-btn iact-btn-ghost">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="19" y1="12" x2="5" y2="12"/>
                    <polyline points="12 19 5 12 12 5"/>
                </svg>
                Quay lai dang nhap
            </a>
        </div>

    </#if>
</@layout.registrationLayout>
