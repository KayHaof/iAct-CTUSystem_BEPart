<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>

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
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                </div>
            </div>
            <h1 class="iact-page-title">Quen mat khau?</h1>
            <p class="iact-page-desc">Nhap email hoac ten dang nhap de nhan lien ket khoi phuc mat khau</p>
        </div>

        <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="iact-alert iact-alert-<#if message.type = 'success'>success<#elseif message.type = 'warning'>warning<#elseif message.type = 'error'>error<#else>info</#if> iact-mb-6">
                <span class="iact-alert-icon">
                    <#if message.type = 'success'>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                    <#elseif message.type = 'error'>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                    <#else>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                    </#if>
                </span>
                <span>${kcSanitize(message.summary)?no_esc}</span>
            </div>
        </#if>

        <form id="kc-reset-password-form" action="${url.loginAction}" method="post" novalidate>

            <div class="iact-field">
                <label for="username" class="iact-label">
                    <#if !realm.loginWithEmailAllowed>Ten dang nhap<#elseif !realm.registrationEmailAsUsername>Ten dang nhap hoac Email<#else>Email</#if>
                </label>
                <div class="iact-input-wrap">
                    <span class="iact-input-icon">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                            <polyline points="22,6 12,13 2,6"/>
                        </svg>
                    </span>
                    <input type="text" id="username" name="username"
                        class="iact-input has-icon"
                        value="${(auth.attemptedUsername!'')}"
                        placeholder="email@student.ctu.edu.vn"
                        autofocus>
                </div>
                <span class="iact-error"></span>
            </div>

            <button type="submit" class="iact-btn iact-btn-primary iact-mt-4">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <line x1="22" y1="2" x2="11" y2="13"/>
                    <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
                Gui yeu cau khoi phuc
            </button>

            <div class="iact-footer-simple">
                <a href="${url.loginUrl}" class="iact-footer-link">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="19" y1="12" x2="5" y2="12"/>
                        <polyline points="12 19 5 12 12 5"/>
                    </svg>
                    Quay lai dang nhap
                </a>
            </div>

        </form>

    <#elseif section = "info">
    </#if>
</@layout.registrationLayout>
