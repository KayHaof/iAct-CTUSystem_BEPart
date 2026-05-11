<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>

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
                        <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                    </svg>
                </div>
            </div>
            <h1 class="iact-page-title">Doi mat khau</h1>
            <p class="iact-page-desc">Tao mat khau moi cho tai khoan cua ban</p>
        </div>

        <#if message?has_content && message.type == 'error'>
            <div class="iact-alert iact-alert-error iact-mb-6">
                <span class="iact-alert-icon">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="15" y1="9" x2="9" y2="15"/>
                        <line x1="9" y1="9" x2="15" y2="15"/>
                    </svg>
                </span>
                <span>${kcSanitize(message.summary)?no_esc}</span>
            </div>
        </#if>

        <form action="${url.loginAction}" method="post" novalidate>

            <div class="iact-field">
                <label for="password-new" class="iact-label">
                    Mat khau moi
                </label>
                <div class="iact-input-wrap">
                    <input type="password" id="password-new" name="password-new" class="iact-input"
                        placeholder="Nhap mat khau moi"
                        autocomplete="new-password">
                    <button type="button" class="iact-toggle-password" data-target="password-new" aria-label="Hien thi mat khau">
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
                <div class="iact-password-strength" data-strength="password-new">
                    <div class="iact-strength-bars">
                        <span class="iact-strength-bar"></span>
                        <span class="iact-strength-bar"></span>
                        <span class="iact-strength-bar"></span>
                        <span class="iact-strength-bar"></span>
                    </div>
                    <span class="iact-strength-label"></span>
                </div>
                <div class="iact-pw-requirements">
                    <span class="iact-pw-req" data-req="length">
                        <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        It nhat 8 ky tu
                    </span>
                    <span class="iact-pw-req" data-req="uppercase">
                        <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        Chu hoa (A-Z)
                    </span>
                    <span class="iact-pw-req" data-req="number">
                        <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        Chu so (0-9)
                    </span>
                    <span class="iact-pw-req" data-req="special">
                        <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        Ky tu dac biet
                    </span>
                </div>
                <span class="iact-error"></span>
            </div>

            <div class="iact-field">
                <label for="password-confirm" class="iact-label">
                    Xac nhan mat khau moi
                </label>
                <div class="iact-input-wrap">
                    <input type="password" id="password-confirm" name="password-confirm" class="iact-input"
                        placeholder="Nhap lai mat khau moi"
                        autocomplete="new-password">
                    <button type="button" class="iact-toggle-password" data-target="password-confirm" aria-label="Hien thi mat khau">
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
                <div class="iact-pw-requirements">
                    <span class="iact-pw-req" data-req="match">
                        <svg class="iact-pw-req-check" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        Mat khau khop nhau
                    </span>
                </div>
                <span class="iact-error"></span>
            </div>

            <button type="submit" class="iact-btn iact-btn-primary iact-mt-6">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"/>
                </svg>
                Cap nhat mat khau
            </button>

        </form>
    </#if>
</@layout.registrationLayout>
