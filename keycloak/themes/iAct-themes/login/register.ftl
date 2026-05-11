<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>

    <#if section = "header">
    <#elseif section = "form">

        <div class="iact-logo-area">
            <img src="${url.resourcesPath}/img/logo.png" alt="iAct Logo" class="iact-logo">
            <span class="iact-brand-name">iAct</span>
            <span class="iact-brand-sub">Can Tho University</span>
        </div>

        <div class="iact-page-header">
            <h1 class="iact-page-title">Tao tai khoan moi</h1>
            <p class="iact-page-desc">Dang ky nhanh chong de tham gia hoat dong tai Can Tho University</p>
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

        <form id="kc-register-form" action="${url.registrationAction}" method="post" novalidate>

            <div class="iact-form-grid-2">
                <div class="iact-field">
                    <label for="lastName" class="iact-label">
                        Ho va dem <span class="iact-label-required">*</span>
                    </label>
                    <div class="iact-input-wrap">
                        <span class="iact-input-icon">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                                <circle cx="12" cy="7" r="4"/>
                            </svg>
                        </span>
                        <input type="text" id="lastName" name="lastName" value="${(register.formData.lastName!'')}"
                            class="iact-input has-icon" placeholder="VD: Nguyen Van"
                            autocomplete="family-name">
                    </div>
                    <span class="iact-error"></span>
                </div>

                <div class="iact-field">
                    <label for="firstName" class="iact-label">
                        Ten <span class="iact-label-required">*</span>
                    </label>
                    <div class="iact-input-wrap">
                        <input type="text" id="firstName" name="firstName" value="${(register.formData.firstName!'')}"
                            class="iact-input" placeholder="VD: An"
                            autocomplete="given-name">
                    </div>
                    <span class="iact-error"></span>
                </div>
            </div>

            <div class="iact-field">
                <label for="email" class="iact-label">
                    Email <span class="iact-label-required">*</span>
                </label>
                <div class="iact-input-wrap">
                    <span class="iact-input-icon">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                            <polyline points="22,6 12,13 2,6"/>
                        </svg>
                    </span>
                    <input type="email" id="email" name="email" value="${(register.formData.email!'')}"
                        class="iact-input has-icon" placeholder="email@student.ctu.edu.vn"
                        autocomplete="email">
                </div>
                <span class="iact-error"></span>
            </div>

            <#if !realm.registrationEmailAsUsername>
                <div class="iact-field">
                    <label for="username" class="iact-label">
                        Ten dang nhap <span class="iact-label-required">*</span>
                    </label>
                    <div class="iact-input-wrap">
                        <span class="iact-input-icon">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"/>
                                <path d="M12 8v4l3 3"/>
                            </svg>
                        </span>
                        <input type="text" id="username" name="username" value="${(register.formData.username!'')}"
                            class="iact-input has-icon" placeholder="VD: an.nguyen"
                            autocomplete="username">
                    </div>
                    <span class="iact-error"></span>
                </div>
            </#if>

            <#if passwordRequired??>
                <div class="iact-field">
                    <label for="password" class="iact-label">
                        Mat khau <span class="iact-label-required">*</span>
                    </label>
                    <div class="iact-input-wrap">
                        <input type="password" id="password" name="password" class="iact-input"
                            placeholder="Tao mat khau moi"
                            autocomplete="new-password">
                        <button type="button" class="iact-toggle-password" data-target="password" aria-label="Hien thi mat khau">
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
                    <div class="iact-password-strength" data-strength="password">
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
                            Ky tu dac biet (!@#$)
                        </span>
                    </div>
                    <span class="iact-error"></span>
                </div>

                <div class="iact-field">
                    <label for="password-confirm" class="iact-label">
                        Xac nhan mat khau <span class="iact-label-required">*</span>
                    </label>
                    <div class="iact-input-wrap">
                        <input type="password" id="password-confirm" name="password-confirm" class="iact-input"
                            placeholder="Nhap lai mat khau"
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
            </#if>

            <button type="submit" class="iact-btn iact-btn-primary iact-mt-6">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="8.5" cy="7" r="4"/>
                    <line x1="20" y1="8" x2="20" y2="14"/>
                    <line x1="23" y1="11" x2="17" y2="11"/>
                </svg>
                Tao tai khoan
            </button>

        </form>

        <div class="iact-footer">
            <span class="iact-footer-text">Da co tai khoan? </span>
            <a href="${url.loginUrl}" class="iact-footer-link">Dang nhap ngay</a>
        </div>

    </#if>
</@layout.registrationLayout>
