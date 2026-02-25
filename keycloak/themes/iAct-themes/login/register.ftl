<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>

    <#if section = "header">
    <#elseif section = "form">
        <script src="https://cdn.tailwindcss.com"></script>

        <style>
            body, .login-pf-page {
                background-image: url('${url.resourcesPath}/img/bg.jpg') !important;
                background-size: cover !important; background-position: center !important;
                background-attachment: fixed !important; background-color: #1a1a1a !important;
                min-height: 100vh !important; width: 100% !important;
                display: flex !important; align-items: center !important; justify-content: center !important;
                margin: 0 !important; padding: 3rem 1rem !important; box-sizing: border-box !important;
            }
            .card-pf {
                background: transparent !important; border: none !important; box-shadow: none !important;
                padding: 0 !important; margin: 0 !important; width: 100% !important; max-width: 100% !important;
            }
            #kc-header, #kc-page-title { display: none !important; }
        </style>

        <div class="w-full max-w-xl mx-auto p-8 sm:p-10 bg-white rounded-3xl shadow-2xl">

            <div class="flex justify-center mb-6">
                <img src="${url.resourcesPath}/img/logo.png" alt="Logo" class="h-16 object-contain">
            </div>

            <div class="text-center mb-8">
                <h2 class="text-3xl font-extrabold text-blue-600 mb-2 tracking-tight">Đăng ký tài khoản iAct</h2>
                <p class="text-gray-500 text-base font-medium">Tạo tài khoản mới để trải nghiệm hệ thống</p>
            </div>

            <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="mb-6 p-4 rounded-xl text-sm font-bold flex items-center gap-2
                    <#if message.type = 'success'>bg-green-50 text-green-700
                    <#elseif message.type = 'warning'>bg-yellow-50 text-yellow-700
                    <#elseif message.type = 'error'>bg-red-50 text-red-700
                    <#else>bg-blue-50 text-blue-700</#if>">
                    ${kcSanitize(message.summary)?no_esc}
                </div>
            </#if>

            <form id="kc-register-form" action="${url.registrationAction}" method="post" class="space-y-6">
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
                    <div>
                        <label for="lastName" class="block text-sm font-bold text-gray-700 mb-2">Họ và đệm <span class="text-red-500">*</span></label>
                        <input type="text" id="lastName" name="lastName" value="${(register.formData.lastName!'')}" class="w-full px-5 py-4 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg placeholder-gray-400" placeholder="VD: Nguyễn Văn" autocomplete="family-name" />
                    </div>
                    <div>
                        <label for="firstName" class="block text-sm font-bold text-gray-700 mb-2">Tên <span class="text-red-500">*</span></label>
                        <input type="text" id="firstName" name="firstName" value="${(register.formData.firstName!'')}" class="w-full px-5 py-4 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg placeholder-gray-400" placeholder="VD: An" autocomplete="given-name" />
                    </div>
                </div>

                <div>
                    <label for="email" class="block text-sm font-bold text-gray-700 mb-2">Email <span class="text-red-500">*</span></label>
                    <input type="email" id="email" name="email" value="${(register.formData.email!'')}" class="w-full px-5 py-4 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg placeholder-gray-400" placeholder="email@ctu.edu.vn" autocomplete="email" />
                </div>

                <#if !realm.registrationEmailAsUsername>
                    <div>
                        <label for="username" class="block text-sm font-bold text-gray-700 mb-2">Tên đăng nhập <span class="text-red-500">*</span></label>
                        <input type="text" id="username" name="username" value="${(register.formData.username!'')}" class="w-full px-5 py-4 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg placeholder-gray-400" placeholder="Nhập tên đăng nhập" autocomplete="username" />
                    </div>
                </#if>

                <#if passwordRequired??>
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
                        <div>
                            <label for="password" class="block text-sm font-bold text-gray-700 mb-2">Mật khẩu <span class="text-red-500">*</span></label>
                            <div class="relative">
                                <input type="password" id="password" name="password" class="w-full px-5 py-4 pr-12 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg tracking-widest placeholder-gray-400 placeholder:tracking-normal" placeholder="••••••••" autocomplete="new-password"/>
                                <button type="button" class="toggle-password absolute inset-y-0 right-0 px-4 flex items-center text-gray-400 hover:text-blue-600 focus:outline-none" data-target="password">
                                    <svg class="h-6 w-6 eye-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                                </button>
                            </div>
                        </div>
                        <div>
                            <label for="password-confirm" class="block text-sm font-bold text-gray-700 mb-2">Xác nhận mật khẩu <span class="text-red-500">*</span></label>
                            <div class="relative">
                                <input type="password" id="password-confirm" name="password-confirm" class="w-full px-5 py-4 pr-12 bg-gray-50 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all font-semibold text-gray-800 text-lg tracking-widest placeholder-gray-400 placeholder:tracking-normal" placeholder="••••••••" autocomplete="new-password"/>
                                <button type="button" class="toggle-password absolute inset-y-0 right-0 px-4 flex items-center text-gray-400 hover:text-blue-600 focus:outline-none" data-target="password-confirm">
                                    <svg class="h-6 w-6 eye-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                                </button>
                            </div>
                        </div>
                    </div>
                </#if>

                <div class="pt-3 mt-3">
                    <button type="submit" class="w-full px-8 py-4 rounded-xl bg-blue-600 text-white hover:bg-blue-700 font-bold shadow-lg hover:shadow-blue-300 transition-all text-lg tracking-wide">
                        Đăng ký ngay
                    </button>
                </div>

                <div class="text-center mt-6">
                    <span class="text-gray-500 font-medium text-base">Bạn đã có tài khoản? </span>
                    <a href="${url.loginUrl}" class="font-bold text-blue-600 hover:text-blue-800 transition-colors underline underline-offset-4 text-base">Đăng nhập tại đây</a>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>