<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password'); section>

    <#if section = "header">
    <#elseif section = "form">

        <div class="iact-logo-area">
            <img src="${url.resourcesPath}/img/logo.png" alt="iAct Logo" class="iact-logo">
            <span class="iact-brand-name">iAct</span>
            <span class="iact-brand-sub">Can Tho University</span>
        </div>

        <div class="iact-page-header">
            <h1 class="iact-page-title">
                <#if usernameEditDisabled??>Xác thực tài khoản<#else>Đăng nhập hệ thống</#if>
            </h1>
            <p class="iact-page-desc">
                <#if usernameEditDisabled??>Vui lòng nhập mật khẩu để tiếp tục xác thực.<#else>Chào mừng bạn quay lại với không gian hoạt động của iAct.</#if>
            </p>
        </div>

        <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="iact-alert iact-alert-<#if message.type = 'success'>success<#elseif message.type = 'warning'>warning<#elseif message.type = 'error'>error<#else>info</#if> iact-mb-6">
                <span class="iact-alert-icon">
                    <#if message.type = 'success'>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                    <#elseif message.type = 'warning'>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                    <#elseif message.type = 'error'>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                    <#else>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                    </#if>
                </span>
                <span>${kcSanitize(message.summary)?no_esc}</span>
            </div>
        </#if>

        <form id="kc-form-login" action="${url.loginAction}" method="post" novalidate>

            <div class="iact-field">
                <label for="username" class="iact-label">
                    <#if !realm.loginWithEmailAllowed>Tên đăng nhập<#elseif !realm.registrationEmailAsUsername>Tên đăng nhập hoặc email<#else>Email</#if>
                </label>
                <div class="iact-input-wrap">
                    <span class="iact-input-icon">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                        </svg>
                    </span>
                    <#if usernameEditDisabled??>
                        <input tabindex="1" id="username" name="username" value="${(login.username!'')}"
                            type="text" class="iact-input has-icon" disabled autocomplete="off">
                        <input type="hidden" name="username" value="${(login.username!'')}">
                    <#else>
                        <input tabindex="1" id="username" name="username" value="${(login.username!'')}"
                            type="text" class="iact-input has-icon" autocomplete="username"
                            placeholder="Nhập tài khoản hoặc email" autofocus>
                    </#if>
                </div>
                <span class="iact-error"></span>
            </div>

            <div class="iact-field">
                <label for="password" class="iact-label">
                    Mật khẩu
                    <#if realm.resetPasswordAllowed>
                        <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="iact-label-link">Quên mật khẩu?</a>
                    </#if>
                </label>
                <div class="iact-input-wrap">
                    <input tabindex="2" id="password" name="password" type="password"
                        class="iact-input" autocomplete="current-password"
                        placeholder="Nhập mật khẩu">
                    <button type="button" class="iact-toggle-password" data-target="password" aria-label="Hiển thị mật khẩu">
                        <svg class="iact-eye-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                        <svg class="iact-eye-off-icon hidden" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                        </svg>
                    </button>
                </div>
                <span class="iact-error"></span>
            </div>

            <input type="hidden" id="id-hidden-input" name="credentialId"
                <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

            <button tabindex="4" name="login" id="kc-login" type="submit" class="iact-btn iact-btn-primary iact-mt-4">
                <#if usernameEditDisabled??>Xác thực mật khẩu<#else>Đăng nhập</#if>
            </button>

            <#if usernameEditDisabled??>
                <div class="iact-footer-simple iact-mt-4">
                    <a href="${url.loginAction?replace('authenticate', 'cancel')}" class="iact-footer-link">
                        Quay lại
                    </a>
                </div>
            </#if>

        </form>

        <#if realm.password && realm.registrationAllowed && !registrationDisabled?? && !usernameEditDisabled??>
            <div class="iact-footer">
                <span class="iact-footer-text">Bạn chưa có tài khoản?</span>
                <a tabindex="6" href="${url.registrationUrl}" class="iact-footer-link">Đăng ký ngay</a>
            </div>
        </#if>

    </#if>
</@layout.registrationLayout>
