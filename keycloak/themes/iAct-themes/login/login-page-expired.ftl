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
                <div class="iact-status-icon iact-status-icon-warning iact-animate-float">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                        <circle cx="12" cy="12" r="10"/>
                        <polyline points="12 6 12 12 16 14"/>
                    </svg>
                </div>
            </div>
            <h1 class="iact-page-title">Phiên làm việc đã hết hạn</h1>
            <p class="iact-page-desc">
                Phiên đăng nhập của bạn đã kết thúc hoặc thao tác trước đó không còn hiệu lực.
                Vui lòng đăng nhập lại để tiếp tục an toàn.
            </p>
        </div>

        <div class="iact-step-actions">
            <a href="${url.loginRestartFlowUrl}" class="iact-btn iact-btn-primary">
                Đăng nhập lại
            </a>

            <a href="${url.loginAction}" class="iact-btn iact-btn-ghost">
                Thử tiếp tục tác vụ
            </a>
        </div>

    </#if>
</@layout.registrationLayout>
