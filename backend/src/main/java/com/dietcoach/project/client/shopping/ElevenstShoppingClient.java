package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.dietcoach.project.util.shopping.ProductWeightParser;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenstShoppingClient implements ShoppingClient {

    private final RestTemplate restTemplate;
    private final ProductWeightParser weightParser;

    @Value("${elevenst.api.base-url:}")
    private String baseUrl;

    @Value("${elevenst.api.key:}")
    private String apiKey;

    @Value("${shopping.elevenst.useMockWhenError:true}")
    private boolean useMockWhenError;

    private final List<ShoppingProduct> mockProducts = new ArrayList<>();

    @PostConstruct
    void initMockData() {
        mockProducts.add(ShoppingProduct.builder()
                .externalId("11st-1001")
                .title("닭가슴살 1kg 대용량")
                .price(12900)
                .gramPerUnit(1000.0)
                .imageUrl("https://example.com/chicken-1kg.jpg")
                .productUrl("https://www.11st.co.kr/product/1001")
                .mallName("11st")
                .build());

        mockProducts.add(ShoppingProduct.builder()
                .externalId("11st-1002")
                .title("닭가슴살 500g x 2팩")
                .price(13900)
                .gramPerUnit(1000.0)
                .imageUrl("https://example.com/chicken-2pack.jpg")
                .productUrl("https://www.11st.co.kr/product/1002")
                .mallName("11st")
                .build());

        mockProducts.add(ShoppingProduct.builder()
                .externalId("11st-2001")
                .title("현미 4kg 국산")
                .price(18900)
                .gramPerUnit(4000.0)
                .imageUrl("https://example.com/brownrice-4kg.jpg")
                .productUrl("https://www.11st.co.kr/product/2001")
                .mallName("11st")
                .build());

        log.info("Initialized {} mock shopping products", mockProducts.size());
    }

    @Override
    public ShoppingClientResult searchProducts(String keyword, int page, int size) {
        return searchProducts(keyword, page, size, null);
    }

    /**
     * ✅ 카테고리 타겟팅 검색 지원
     */
    public ShoppingClientResult searchProducts(String keyword, int page, int size, String dispCtgrNo) {
        String traceId = currentTraceId();
        try {
            long startMs = System.currentTimeMillis();
            List<ShoppingProduct> real = callRealApi(keyword, page, size, dispCtgrNo);
            if (real != null && !real.isEmpty()) {
                log.info("[SHOPPING_CLIENT][{}] REAL keyword=\"{}\" cat={} responseCount={} tookMs={}",
                        traceId, keyword, dispCtgrNo, real.size(), System.currentTimeMillis() - startMs);
                return ShoppingClientResult.builder()
                        .products(real)
                        .source("REAL")
                        .build();
            }
        } catch (Exception e) {
            log.warn("[SHOPPING_CLIENT][{}] REAL_FAIL keyword=\"{}\" ex={} fallback={}",
                    traceId, keyword, e.getClass().getSimpleName(), useMockWhenError);
            if (!useMockWhenError)
                throw e;
        }

        List<ShoppingProduct> mock = searchFromMock(keyword, page, size);
        return ShoppingClientResult.builder()
                .products(mock)
                .source("MOCK")
                .build();
    }

    private List<ShoppingProduct> callRealApi(String keyword, int page, int size, String dispCtgrNo) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[11st] API Key is missing. Skipping real API call.");
            return List.of();
        }
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .queryParam("key", apiKey)
                .queryParam("apiCode", "ProductSearch")
                .queryParam("keyword", keyword)
                .queryParam("pageSize", size)
                .queryParam("pageNum", page);

        // ✅ 카테고리 번호가 있으면 파라미터 추가
        if (dispCtgrNo != null && !dispCtgrNo.isBlank()) {
            builder.queryParam("dispCtgrNo", dispCtgrNo);
        }

        URI uri = builder.build().encode(StandardCharsets.UTF_8).toUri();
        log.debug("[11st] Request URI: {}", uri);

        String response = restTemplate.getForObject(uri, String.class);
        if (response == null || response.isBlank())
            return List.of();

        try {
            return parseProductsFromXml(response);
        } catch (Exception e) {
            log.error("[11st] Failed to parse XML response", e);
            return List.of();
        }
    }

    private List<ShoppingProduct> searchFromMock(String keyword, int page, int size) {
        List<ShoppingProduct> filtered = mockProducts.stream()
                .filter(p -> p.getTitle() != null && p.getTitle().contains(keyword))
                .collect(Collectors.toList());

        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(filtered.size(), fromIndex + size);

        if (fromIndex >= filtered.size())
            return List.of();
        return filtered.subList(fromIndex, toIndex);
    }

    private List<ShoppingProduct> parseProductsFromXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        Document doc = builder.parse(is);
        doc.getDocumentElement().normalize();

        NodeList productNodes = doc.getElementsByTagName("Product");
        List<ShoppingProduct> result = new ArrayList<>();

        for (int i = 0; i < productNodes.getLength(); i++) {
            Node node = productNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element e = (Element) node;

            String productCode = getTagValue(e, "ProductCode");
            String productName = getTagValue(e, "ProductName");
            String salePriceStr = getTagValue(e, "SalePrice");
            if (salePriceStr == null)
                salePriceStr = getTagValue(e, "ProductPrice");
            String imageUrl = getTagValue(e, "ProductImage");
            if (imageUrl == null)
                imageUrl = getTagValue(e, "ProductImage300");
            String detailPageUrl = getTagValue(e, "DetailPageUrl");
            String sellerNick = getTagValue(e, "SellerNick");

            // ✅ 카테고리 정보 파싱 (멀티 태그 지원)
            String dispNo = getTagValue(e, "CategoryCode");
            if (dispNo == null)
                dispNo = getTagValue(e, "DispatchDispNo");
            if (dispNo == null)
                dispNo = getTagValue(e, "DispNo");

            String dispNm = getTagValue(e, "CategoryName");
            if (dispNm == null)
                dispNm = getTagValue(e, "DispatchDispNm");
            if (dispNm == null)
                dispNm = getTagValue(e, "DispNm");

            int price = 0;
            try {
                if (salePriceStr != null)
                    price = Integer.parseInt(salePriceStr.trim());
            } catch (Exception ex) {
            }

            if ((detailPageUrl == null || detailPageUrl.isBlank()) && productCode != null && !productCode.isBlank()) {
                detailPageUrl = "https://www.11st.co.kr/products/" + productCode;
            }

            // Parse weight
            int weight = weightParser.parseWeightInGrams(productName);

            result.add(ShoppingProduct.builder()
                    .externalId(productCode)
                    .title(productName)
                    .price(price)
                    .imageUrl(imageUrl)
                    .productUrl(detailPageUrl)
                    .mallName((sellerNick != null) ? sellerNick : "11st")
                    .categoryCode(dispNo)
                    .categoryName(dispNm)
                    .gramPerUnit((double) weight)
                    .build());
        }
        return result;
    }

    private String getTagValue(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0)
            return null;
        Node node = list.item(0);
        if (node == null)
            return null;
        Node child = node.getFirstChild();
        return (child != null) ? child.getNodeValue() : null;
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId == null || traceId.isBlank()) ? "no-trace" : traceId;
    }
}
