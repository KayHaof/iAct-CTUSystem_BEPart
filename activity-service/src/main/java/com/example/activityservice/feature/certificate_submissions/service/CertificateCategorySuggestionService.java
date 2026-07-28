package com.example.activityservice.feature.certificate_submissions.service;

import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.categories.repository.CategoryRepository;
import com.example.activityservice.feature.certificate_submissions.ai.CertificateAiScanResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CertificateCategorySuggestionService {

    private static final int MIN_MATCH_SCORE = 24;
    private static final Set<String> STOP_WORDS = Set.of(
            "va", "cua", "cac", "cho", "voi", "trong", "ngoai", "nam", "hoc",
            "sinh", "vien", "chung", "nhan", "dat", "diem", "ren", "luyen",
            "muc", "duoc", "theo", "hoat", "dong", "tich", "phong", "trao",
            "truong");

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public CertificateCategorySuggestion suggest(CertificateAiScanResult aiResult, List<String> warnings) {
        Categories aiCategory = resolveAiCategory(aiResult.getSuggestedCategoryId(), warnings);
        if (aiCategory != null) {
            return fromCategory(
                    aiCategory,
                    normalizePoint(aiResult.getSuggestedPoint(), aiCategory),
                    cleanReason(aiResult.getSuggestionReason(),
                            "AI đã trả về tiêu chí có trong hệ thống, Trường cần kiểm tra trước khi duyệt."));
        }

        MatchResult bestMatch = findBestMatch(aiResult);
        if (bestMatch == null) {
            if (aiResult.getSuggestedCategoryName() != null && !aiResult.getSuggestedCategoryName().isBlank()) {
                warnings.add("Chưa khớp được mục AI đề xuất với danh mục điểm rèn luyện hiện hành.");
            }
            return CertificateCategorySuggestion.empty(
                    cleanText(aiResult.getSuggestedCategoryName()),
                    cleanReason(aiResult.getSuggestionReason(), null));
        }

        Categories category = bestMatch.category();
        Integer point = normalizePoint(bestMatch.suggestedPoint() != null
                ? bestMatch.suggestedPoint()
                : aiResult.getSuggestedPoint(), category);
        String reason = bestMatch.reason() != null ? bestMatch.reason() : buildReason(bestMatch, point);
        return fromCategory(category, point, reason);
    }

    private Categories resolveAiCategory(Long categoryId, List<String> warnings) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .filter(category -> Boolean.TRUE.equals(category.getIsActive()))
                .filter(this::isLeafCategory)
                .orElseGet(() -> {
                    warnings.add(
                            "Tiêu chí AI đề xuất không tồn tại, đã ngừng sử dụng hoặc không phải tiêu chí nhỏ nhất.");
                    return null;
                });
    }

    private MatchResult findBestMatch(CertificateAiScanResult aiResult) {
        String evidence = buildEvidence(aiResult);
        Set<String> evidenceTokens = tokenize(evidence);
        if (evidenceTokens.isEmpty()) {
            return null;
        }

        List<Categories> leafCategories = findActiveLeafCategories();
        MatchResult domainMatch = findDomainRuleMatch(evidence, leafCategories);
        if (domainMatch != null) {
            return domainMatch;
        }

        return leafCategories.stream()
                .map(category -> scoreCategory(category, evidence, evidenceTokens, aiResult.getSuggestedCategoryName()))
                .filter(match -> match.score() >= MIN_MATCH_SCORE)
                .max(Comparator.comparingInt((MatchResult m) -> m.score())
                        .thenComparingInt(m -> maxPoint(m.category())))
                .orElse(null);
    }

    private List<Categories> findActiveLeafCategories() {
        List<Categories> activeCategories = categoryRepository.findByIsActive(true);
        Set<Long> activeParentIds = collectActiveParentIds(activeCategories);
        return activeCategories.stream()
                .filter(category -> category.getId() != null && !activeParentIds.contains(category.getId()))
                .toList();
    }

    private Set<Long> collectActiveParentIds(List<Categories> categories) {
        Set<Long> parentIds = new HashSet<>();
        for (Categories category : categories) {
            if (category.getParent() != null && category.getParent().getId() != null) {
                parentIds.add(category.getParent().getId());
            }
        }
        return parentIds;
    }

    private MatchResult scoreCategory(
            Categories category,
            String evidence,
            Set<String> evidenceTokens,
            String aiSuggestedCategoryName) {
        String categoryText = buildCategoryText(category);
        String normalizedCategory = normalize(categoryText);
        Set<String> categoryTokens = tokenize(categoryText);

        int score = 0;
        List<String> matchedTokens = new ArrayList<>();
        for (String token : categoryTokens) {
            if (evidenceTokens.contains(token)) {
                matchedTokens.add(token);
                score += token.length() >= 5 ? 10 : 6;
            }
        }

        String normalizedName = normalize(category.getName());
        if (!normalizedName.isBlank() && evidence.contains(normalizedName)) {
            score += 45;
        }
        if (!normalizedCategory.isBlank() && evidence.contains(normalizedCategory)) {
            score += 20;
        }
        String normalizedCode = normalize(category.getCode());
        if (!normalizedCode.isBlank() && evidence.contains(normalizedCode)) {
            score += 18;
        }

        String normalizedAiSuggestion = normalize(aiSuggestedCategoryName);
        if (!normalizedAiSuggestion.isBlank()) {
            if (normalizedName.contains(normalizedAiSuggestion) || normalizedAiSuggestion.contains(normalizedName)) {
                score += 50;
            }
            Set<String> suggestionTokens = tokenize(aiSuggestedCategoryName);
            for (String token : categoryTokens) {
                if (suggestionTokens.contains(token)) {
                    score += token.length() >= 5 ? 12 : 7;
                    if (!matchedTokens.contains(token)) {
                        matchedTokens.add(token);
                    }
                }
            }
        }

        if (matchedTokens.size() < 2 && score < 45) {
            score = 0;
        }

        return new MatchResult(category, score, matchedTokens.stream().distinct().limit(5).toList(), null, null);
    }

    private MatchResult findDomainRuleMatch(String evidence, List<Categories> leafCategories) {
        if (hasAny(evidence, "nckh", "nghien cuu khoa hoc")) {
            MatchResult match;
            if (hasAny(evidence, "bai bao", "tap chi khoa hoc", "hoi nghi khoa hoc", "cong bo khoa hoc")) {
                match = buildRuleMatch(
                        leafCategories,
                        8,
                        "Nhận diện minh chứng liên quan bài báo hoặc công bố nghiên cứu khoa học.",
                        List.of("bai bao", "nckh"),
                        "bai bao", "nckh");
                if (match != null) {
                    return match;
                }
            }
            if (isAwardEvidence(evidence) || hasAny(evidence, "chu nhiem de tai")) {
                match = buildRuleMatch(
                        leafCategories,
                        8,
                        "Nhận diện minh chứng là giấy khen NCKH hoặc chủ nhiệm đề tài NCKH.",
                        List.of("giay khen", "nckh"),
                        "giay khen", "nckh");
                if (match != null) {
                    return match;
                }
            }
            if (hasAny(evidence, "tham gia de tai", "de tai nckh", "de tai nghien cuu")) {
                match = buildRuleMatch(
                        leafCategories,
                        5,
                        "Nhận diện minh chứng tham gia đề tài nghiên cứu khoa học.",
                        List.of("tham gia", "de tai", "nckh"),
                        "tham gia", "de tai", "nckh");
                if (match != null) {
                    return match;
                }
            }
        }

        if (hasAll(evidence, "sinh vien", "5", "tot")) {
            MatchResult match = null;
            if (hasAny(evidence, "cap khoa", "doan khoa", "lien chi hoi khoa")) {
                match = buildRuleMatch(
                        leafCategories,
                        4,
                        "Nhận diện danh hiệu sinh viên 5 tốt cấp Khoa.",
                        List.of("sinh vien 5 tot", "cap khoa"),
                        "sinh vien 5 tot", "cap khoa");
            }
            if (match == null) {
                match = buildRuleMatch(
                        leafCategories,
                        6,
                        "Nhận diện danh hiệu sinh viên 5 tốt cấp Trường hoặc cao hơn.",
                        List.of("sinh vien 5 tot", "cap truong"),
                        "sinh vien 5 tot", "cap truong");
            }
            if (match != null) {
                return match;
            }
        }

        if (isAwardEvidence(evidence)) {
            MatchResult match;
            if (isHighAwardEvidence(evidence)) {
                match = buildRuleMatch(
                        leafCategories,
                        10,
                        "Nhận diện minh chứng là bằng khen hoặc khen thưởng cấp cao hơn giấy khen.",
                        List.of("bang khen"),
                        "bang khen");
                if (match != null) {
                    return match;
                }
            }
            if (isFacultyAwardIssuer(evidence)) {
                match = buildRuleMatch(
                        leafCategories,
                        6,
                        "Nhận diện minh chứng là khen thưởng cấp Khoa hoặc tương đương.",
                        List.of("khen thuong", "cap khoa"),
                        "khen thuong", "cap khoa");
                if (match != null) {
                    return match;
                }
            }
            if (isSchoolEquivalentAwardIssuer(evidence)) {
                match = buildRuleMatch(
                        leafCategories,
                        8,
                        "Nhận diện minh chứng là giấy khen cấp Trường hoặc tương đương.",
                        List.of("giay khen", "cap truong"),
                        "giay khen", "cap truong");
                if (match != null) {
                    return match;
                }
            }
            match = buildRuleMatch(
                    leafCategories,
                    8,
                    "Nhận diện minh chứng là giấy khen/khen thưởng, ưu tiên nhóm giấy khen thay vì nhóm tham gia phong trào.",
                    List.of("giay khen", "khen thuong"),
                    "giay khen", "cap truong");
            if (match != null) {
                return match;
            }
        }

        if (hasAny(evidence, "chung chi tin hoc", "tin hoc", "mos", "ic3", "ung dung cong nghe thong tin")) {
            MatchResult match = buildRuleMatch(
                    leafCategories,
                    3,
                    "Nhận diện minh chứng hoàn thành chứng chỉ tin học.",
                    List.of("chung chi", "tin hoc"),
                    "chung chi", "tin hoc");
            if (match != null) {
                return match;
            }
        }

        if (hasAny(evidence, "olympic", "ky thi", "cuoc thi chuyen nganh", "hoi thi chuyen nganh")) {
            MatchResult match = buildRuleMatch(
                    leafCategories,
                    2,
                    "Nhận diện minh chứng tham gia kỳ thi chuyên ngành hoặc Olympic.",
                    List.of("ky thi", "olympic"),
                    "tham gia", "ky thi");
            if (match != null) {
                return match;
            }
        }

        if (hasAny(evidence, "clb hoc thuat", "cau lac bo hoc thuat", "doi nhom hoc thuat")) {
            MatchResult match = buildRuleMatch(
                    leafCategories,
                    2,
                    "Nhận diện minh chứng tham gia câu lạc bộ học thuật.",
                    List.of("clb", "hoc thuat"),
                    "clb", "hoc thuat");
            if (match != null) {
                return match;
            }
        }

        if (hasAny(evidence, "ve sinh", "moi truong", "canh quan", "an ninh", "trat tu noi cong cong")) {
            MatchResult match = buildRuleMatch(
                    leafCategories,
                    10,
                    "Nhận diện minh chứng tham gia tuyên truyền hoặc giữ gìn vệ sinh, môi trường, an ninh, trật tự.",
                    List.of("ve sinh", "moi truong", "an ninh"),
                    "giu gin", "ve sinh");
            if (match != null) {
                return match;
            }
        }

        if (hasAny(evidence, "cong dong", "cong tac xa hoi", "noi cu tru", "dia phuong", "tinh nguyen vi cong dong")) {
            MatchResult match = buildRuleMatch(
                    leafCategories,
                    10,
                    "Nhận diện minh chứng tham gia đội nhóm hoặc hoạt động hướng đến lợi ích cộng đồng.",
                    List.of("cong dong", "cong tac xa hoi"),
                    "tham gia doi nhom", "loi ich cong dong");
            if (match != null) {
                return match;
            }
        }

        if (hasAny(evidence, "ho tro", "ban to chuc", "tinh nguyen vien", "su kien chung", "phong trao cap khoa")) {
            MatchResult match = buildRuleMatch(
                    leafCategories,
                    8,
                    "Nhận diện minh chứng hỗ trợ hoạt động, phong trào cấp Khoa hoặc sự kiện chung của Trường.",
                    List.of("ho tro", "phong trao", "su kien"),
                    "tich cuc tham gia ho tro");
            if (match != null) {
                return match;
            }
        }

        if (!isAwardEvidence(evidence)
                && hasAny(evidence, "tham gia", "xac nhan tham gia", "hoan thanh tham gia")
                && hasAny(evidence, "chinh tri", "xa hoi", "van hoa", "van nghe", "the thao")) {
            MatchResult match = buildRuleMatch(
                    leafCategories,
                    12,
                    "Nhận diện minh chứng tham gia hoạt động chính trị, xã hội, văn hóa, văn nghệ, thể thao.",
                    List.of("tham gia", "chinh tri", "van hoa", "the thao"),
                    "tham gia day du", "chinh tri");
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    private MatchResult buildRuleMatch(
            List<Categories> leafCategories,
            int suggestedPoint,
            String reasonPrefix,
            List<String> matchedTerms,
            String... categoryTerms) {
        Categories category = findCategoryByName(leafCategories, categoryTerms);
        if (category == null) {
            return null;
        }
        return new MatchResult(
                category,
                100 + suggestedPoint,
                matchedTerms.stream().map(this::normalize).distinct().limit(5).toList(),
                suggestedPoint,
                buildDomainReason(category, reasonPrefix));
    }

    private Categories findCategoryByName(List<Categories> leafCategories, String... requiredTerms) {
        return leafCategories.stream()
                .filter(category -> {
                    String normalizedName = normalize(category.getName());
                    for (String term : requiredTerms) {
                        if (!normalizedName.contains(normalize(term))) {
                            return false;
                        }
                    }
                    return true;
                })
                .max(Comparator.comparingInt(this::maxPoint))
                .orElse(null);
    }

    private boolean isAwardEvidence(String evidence) {
        boolean explicitAwardDocument = hasAny(evidence,
                "giay khen",
                "bang khen",
                "khen thuong",
                "duoc khen",
                "bieu duong",
                "dat giai",
                "giai nhat",
                "giai nhi",
                "giai ba",
                "giai khuyen khich");
        if (explicitAwardDocument) {
            return true;
        }

        boolean awardFormula = hasAny(evidence,
                "da co thanh tich",
                "co thanh tich tot",
                "thanh tich xuat sac",
                "thanh tich tot trong cong tac",
                "thanh tich tot trong phong trao");
        boolean awardAuthority = hasAny(evidence,
                "tang",
                "quyet dinh",
                "chu tich",
                "hieu truong",
                "uy ban nhan dan",
                "ubnd",
                "ban chap hanh");
        return awardFormula && awardAuthority;
    }

    private boolean isHighAwardEvidence(String evidence) {
        return hasAny(evidence,
                "bang khen",
                "huan chuong",
                "thu tuong",
                "bo truong",
                "trung uong",
                "quoc gia");
    }

    private boolean isSchoolEquivalentAwardIssuer(String evidence) {
        return hasAny(evidence,
                "cap truong",
                "truong dai hoc",
                "dai hoc can tho",
                "hieu truong",
                "uy ban nhan dan huyen",
                "ubnd huyen",
                "uy ban nhan dan quan",
                "ubnd quan",
                "uy ban nhan dan thanh pho",
                "ubnd thanh pho",
                "huyen",
                "quan",
                "thi xa",
                "thanh pho",
                "doan truong",
                "hoi sinh vien truong");
    }

    private boolean isFacultyAwardIssuer(String evidence) {
        return hasAny(evidence,
                "cap khoa",
                "doan khoa",
                "lien chi hoi",
                "hoi sinh vien khoa",
                "khoa cong nghe",
                "khoa cntt");
    }

    private boolean hasAll(String evidence, String... terms) {
        for (String term : terms) {
            if (!evidence.contains(normalize(term))) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAny(String evidence, String... terms) {
        for (String term : terms) {
            if (evidence.contains(normalize(term))) {
                return true;
            }
        }
        return false;
    }

    private CertificateCategorySuggestion fromCategory(Categories category, Integer point, String reason) {
        return new CertificateCategorySuggestion(
                category,
                category.getName(),
                point,
                reason);
    }

    private String buildEvidence(CertificateAiScanResult aiResult) {
        return normalize(String.join(" ",
                nullToEmpty(aiResult.getSuggestedCategoryName()),
                nullToEmpty(aiResult.getSuggestionReason()),
                nullToEmpty(aiResult.getCertificateTitle()),
                nullToEmpty(aiResult.getAchievement()),
                nullToEmpty(aiResult.getIssuer()),
                nullToEmpty(aiResult.getRawText())));
    }

    private String buildCategoryText(Categories category) {
        List<String> parts = new ArrayList<>();
        Categories current = category;
        while (current != null) {
            parts.add(nullToEmpty(current.getCode()));
            parts.add(nullToEmpty(current.getName()));
            current = current.getParent();
        }
        return String.join(" ", parts);
    }

    private String buildReason(MatchResult match, Integer point) {
        String keywordText = match.matchedTokens().isEmpty()
                ? "nội dung giấy khen"
                : "các từ khóa: " + String.join(", ", match.matchedTokens());
        String pointText = point == null
                ? "Chưa đề xuất điểm vì tiêu chí không cấu hình mức điểm tối đa."
                : "Điểm đề xuất lấy theo mức tối đa của tiêu chí, Trường có thể điều chỉnh khi duyệt.";
        return "Hệ thống khớp với tiêu chí " + categoryLabel(match.category()) + " dựa trên " + keywordText + ". "
                + pointText;
    }

    private String buildDomainReason(Categories category, String reasonPrefix) {
        return reasonPrefix + " Đề xuất vào tiêu chí " + categoryLabel(category)
                + ". Điểm đề xuất theo mức tối đa của tiêu chí, Trường có thể điều chỉnh khi duyệt.";
    }

    private String categoryLabel(Categories category) {
        String code = cleanText(category.getCode());
        return code == null
                ? category.getName()
                : "[" + code + "] " + category.getName();
    }

    private Integer normalizePoint(Integer aiPoint, Categories category) {
        int maxPoint = maxPoint(category);
        if (aiPoint != null && aiPoint >= 0) {
            return maxPoint > 0 ? Math.min(aiPoint, maxPoint) : aiPoint;
        }
        return maxPoint > 0 ? maxPoint : null;
    }

    private int maxPoint(Categories category) {
        return category.getMaxPoint() == null ? 0 : category.getMaxPoint();
    }

    private boolean isLeafCategory(Categories category) {
        return category.getId() != null && !categoryRepository.existsByParentIdAndIsActive(category.getId(), true);
    }

    private Set<String> tokenize(String value) {
        String normalized = normalize(value);
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split(" ")) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('Đ', 'D').replace('đ', 'd');
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private String cleanText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String cleanReason(String value, String fallback) {
        String cleaned = cleanText(value);
        return cleaned != null ? cleaned : fallback;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record MatchResult(
            Categories category,
            int score,
            List<String> matchedTokens,
            Integer suggestedPoint,
            String reason) {
    }
}
