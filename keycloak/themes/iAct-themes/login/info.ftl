<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
    <#-- Để trống -->
    <#elseif section = "form">
        <script src="https://cdn.tailwindcss.com"></script>

        <style>
            body, .login-pf-page {
                background-image: url('${url.resourcesPath}/img/bg.jpg') !important;
                background-size: cover !important; background-position: center !important;
                background-attachment: fixed !important; background-color: #f3f4f6 !important;
                min-height: 100vh !important; width: 100vw !important;
                display: flex !important; align-items: center !important; justify-content: center !important;
                margin: 0 !important; padding: 2rem 1rem !important; box-sizing: border-box !important;
            }
            .card-pf {
                background: transparent !important; border: none !important; box-shadow: none !important;
                padding: 0 !important; margin: 0 !important; width: 100% !important; max-width: 100% !important;
            }
            #kc-header, #kc-page-title { display: none !important; }
        </style>

        <div class="w-full max-w-lg mx-auto p-8 sm:p-10 bg-white rounded-3xl shadow-2xl animate-fade-in">

            <#-- LOGO -->
            <div class="flex justify-center mb-6">
                <img src="${url.resourcesPath}/img/logo.png" alt="Logo" class="h-20 object-contain">
            </div>

            <#-- ICON -->
            <div class="flex justify-center mb-6">
                <div class="h-20 w-20 rounded-full bg-blue-50 flex items-center justify-center border-4 border-blue-100 shadow-sm">
                    <svg class="h-10 w-10 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>
            </div>

            <#-- THÔNG BÁO -->
            <div class="text-center mb-8">
                <h2 class="text-xl font-bold text-gray-800 leading-relaxed mb-4">
                    ${message.summary}
                </h2>
                <#if requiredActions??>
                    <#list requiredActions as reqAction>
                        <p class="text-sm font-semibold text-red-500">${msg("requiredAction.${reqAction}")}</p>
                    </#list>
                </#if>
            </div>

            <#-- NÚT BẤM (ĐÃ SỬA HOVER CHO NÚT MÀU XANH) -->
            <div class="space-y-4">
                <#if skipLink??>
                <#else>
                    <#if pageRedirectUri?has_content>
                        <p class="text-center text-sm text-gray-500 font-medium mb-3 animate-pulse">Đang tự động chuyển hướng...</p>
                        <a href="${pageRedirectUri}" class="block w-full text-center px-8 py-4 rounded-xl bg-blue-600 text-white hover:bg-blue-700 hover:text-white font-bold shadow-lg transition-all text-lg">
                            ${kcSanitize(msg("backToApplication"))?no_esc}
                        </a>
                    <#elseif actionUri?has_content>
                    <#-- ĐÂY LÀ NÚT BỊ LỖI TRONG HÌNH CỦA NÍ -->
                        <a href="${actionUri}" class="block w-full text-center px-8 py-4 rounded-xl bg-blue-600 text-white hover:bg-blue-700 hover:text-white font-bold shadow-lg hover:shadow-blue-200 transition-all text-lg tracking-wide">
                            ${kcSanitize(msg("proceedWithAction"))?no_esc}
                        </a>
                    <#elseif (client.baseUrl)?has_content>
                        <a href="${client.baseUrl}" class="block w-full text-center px-8 py-4 rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 font-bold shadow transition-all text-lg">
                            ${kcSanitize(msg("backToApplication"))?no_esc}
                        </a>
                    </#if>
                </#if>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>