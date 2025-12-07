package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * 11번가 쇼핑 클라이언트 구현.
 * - 1순위: 실제 11번가 OPEN API 호출
 * - 실패 시: 메모리 mock 데이터로 fallback
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenstShoppingClient implements ShoppingClient {

    private final RestTemplate restTemplate;

    @Value("${elevenst.api.base-url}")
    private String baseUrl;

    @Value("${elevenst.api.key}")
    private String apiKey;

    /**
     * 개발용 mock 데이터 (실 API 실패 시 fallback 용도)
     */
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
    public List<ShoppingProduct> searchProducts(String keyword, int page, int size) {
        // 1순위: 실제 11번가 API 호출
        try {
            List<ShoppingProduct> real = callRealApi(keyword, page, size);
            if (!real.isEmpty()) {
                return real;
            }
        } catch (Exception e) {
            log.warn("11st real API call failed, falling back to mock. reason={}", e.getMessage());
        }

        // 2순위: 실패 시 mock 데이터로 fallback
        return searchFromMock(keyword, page, size);
    }

    /**
     * 실제 11번가 ProductSearch API 호출 
     */
    private List<ShoppingProduct> callRealApi(String keyword, int page, int size) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .queryParam("key", apiKey)
                .queryParam("apiCode", "ProductSearch")
                .queryParam("keyword", keyword)
                .queryParam("pageSize", size)
                .queryParam("pageNum", page)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        
        
        log.info("[11st] Calling API: {}", uri);

        String response = restTemplate.getForObject(uri, String.class);
        log.debug("[11st] Raw response: {}", response);

        if (response == null || response.isBlank()) {
            log.warn("[11st] Empty response");
            return List.of();
        }

        try {
            return parseProductsFromXml(response);
        } catch (Exception e) {
            log.error("[11st] Failed to parse XML response", e);
            return List.of(); // 파싱 실패 시 상위에서 mock fallback
        }
    }

    /**
     * 기존 mock 데이터 기반 검색 (실 API 실패 시 사용)
     */
    private List<ShoppingProduct> searchFromMock(String keyword, int page, int size) {
        List<ShoppingProduct> filtered = mockProducts.stream()
                .filter(p -> p.getTitle() != null && p.getTitle().contains(keyword))
                .collect(Collectors.toList());

        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(filtered.size(), fromIndex + size);

        if (fromIndex >= filtered.size()) {
            return List.of();
        }

        return filtered.subList(fromIndex, toIndex);
    }
    
    /**
     * 11번가 ProductSearch XML 응답을 ShoppingProduct 리스트로 변환.
     */
    private List<ShoppingProduct> parseProductsFromXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // XXE 공격 방지용 기본 옵션 (습관처럼 넣어두면 좋음)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        Document doc = builder.parse(is);
        doc.getDocumentElement().normalize();

        NodeList productNodes = doc.getElementsByTagName("Product");

        List<ShoppingProduct> result = new ArrayList<>();

        for (int i = 0; i < productNodes.getLength(); i++) {
            Node node = productNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element e = (Element) node;

            String productCode = getTagValue(e, "ProductCode");
            String productName = getTagValue(e, "ProductName");
            String productPriceStr = getTagValue(e, "ProductPrice");
            String salePriceStr = getTagValue(e, "SalePrice");
            String imageUrl = getTagValue(e, "ProductImage");
            String detailPageUrl = getTagValue(e, "DetailPageUrl");
            String sellerNick = getTagValue(e, "SellerNick");

            // 가격은 SalePrice 우선, 없으면 ProductPrice 사용
            int price = 0;
            try {
                if (salePriceStr != null && !salePriceStr.isBlank()) {
                    price = Integer.parseInt(salePriceStr.trim());
                } else if (productPriceStr != null && !productPriceStr.isBlank()) {
                    price = Integer.parseInt(productPriceStr.trim());
                }
            } catch (NumberFormatException ex) {
                log.warn("[11st] Cannot parse price: salePrice={}, productPrice={}", salePriceStr, productPriceStr);
            }

            // 현재 11번가 응답에는 중량 정보(gram)는 없으므로 null로 둠
            // (나중에 상품명 파싱해서 '1kg', '500g' 추출하는 로직을 추가해도 됨)
            Double gramPerUnit = null;

            ShoppingProduct product = ShoppingProduct.builder()
                    .externalId(productCode)
                    .title(productName)
                    .price(price)
                    .gramPerUnit(gramPerUnit)
                    .imageUrl(imageUrl)
                    .productUrl(detailPageUrl)
                    .mallName(sellerNick != null && !sellerNick.isBlank() ? sellerNick : "11st")
                    .build();

            result.add(product);
        }

        log.info("[11st] Parsed {} products from XML", result.size());
        return result;
    }

    /**
     * XML Element에서 특정 태그의 텍스트 값 가져오기.
     */
    private String getTagValue(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;

        Node node = list.item(0);
        if (node == null) return null;

        Node child = node.getFirstChild();
        return (child != null) ? child.getNodeValue() : null;
    }
}
