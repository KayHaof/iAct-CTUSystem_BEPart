<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
    <#-- Ẩn tiêu đề mặc định -->
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

            <#-- ICON CẢNH BÁO/THỜI GIAN -->
            <div class="flex justify-center mb-6">
                <div class="h-20 w-20 rounded-full bg-orange-50 flex items-center justify-center border-4 border-orange-100 shadow-sm">
                    <svg class="h-10 w-10 text-orange-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>
            </div>

            <#-- TIÊU ĐỀ -->
            <div class="text-center mb-8">
                <h2 class="text-2xl font-extrabold text-gray-800 mb-2">Phiên làm việc hết hạn</h2>
                <p class="text-gray-500 text-sm font-medium">
                    Yêu cầu trước đó của bạn đã được xử lý hoặc đã hết thời gian chờ. Vui lòng chọn thao tác bên dưới.
                </p>
            </div>

            <#-- 2 NÚT BẤM THAY CHO 2 LINK MẶC ĐỊNH -->
            <div class="space-y-4">
                <#-- Nút: Restart Login (Màu xanh nổi bật) -->
                <a id="loginRestartLink" href="${url.loginRestartFlowUrl}" class="block w-full text-center px-8 py-4 rounded-xl bg-blue-600 text-white hover:bg-blue-700 hover:text-white font-bold shadow-lg hover:shadow-blue-200 transition-all text-lg tracking-wide">
                    Quay lại trang Đăng nhập
                </a>

                <#-- Nút: Continue Login (Màu xám nhạt) -->
                <a id="loginContinueLink" href="${url.loginAction}" class="block w-full text-center px-8 py-4 rounded-xl bg-gray-100 text-gray-700 hover:bg-gray-200 hover:text-gray-900 font-bold shadow transition-all text-base">
                    Tiếp tục hành động hiện tại
                </a>
            </div>

        </div>
    </#if>
</@layout.registrationLayout>