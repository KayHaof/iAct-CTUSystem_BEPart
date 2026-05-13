<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>

    <#if section = "header">
    <#elseif section = "form">

        <div class="iact-logo-area">
            <img src="${url.resourcesPath}/img/logo.png" alt="iAct Logo" class="iact-logo">
            <span class="iact-brand-name">iAct</span>
            <span class="iact-brand-sub">Can Tho University</span>
        </div>

        <div class="iact-page-header">
            <div class="iact-status-icon-wrap">
                <div class="iact-status-icon iact-status-icon-info iact-animate-float">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                        <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                        <polyline points="22,6 12,13 2,6"/>
                    </svg>
                </div>
            </div>
            <h1 class="iact-page-title">Xác thực email</h1>
            <p class="iact-page-desc">Vui lòng kiểm tra hộp thư để hoàn tất xác thực tài khoản iAct.</p>
        </div>

        <div class="iact-alert iact-alert-warning iact-mb-8">
            <span class="iact-alert-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                    <line x1="12" y1="9" x2="12" y2="13"/>
                    <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
            </span>
            <span>${msg("emailVerifyInstruction")}</span>
        </div>

        <div class="iact-alert iact-alert-info iact-mb-8">
            <span class="iact-alert-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="16" x2="12" y2="12"/>
                    <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
            </span>
            <span>Nếu chưa thấy email trong hộp thư đến, hãy kiểm tra cả thư rác hoặc thư quảng cáo.</span>
        </div>

        <div class="iact-step-actions">
            <a href="${url.loginAction}" class="iact-btn iact-btn-primary">
                Gửi lại email xác nhận
            </a>

            <a href="${url.loginUrl}" class="iact-btn iact-btn-ghost">
                Quay lại đăng nhập
            </a>
        </div>

    <#elseif section = "info">
    </#if>
</@layout.registrationLayout>
