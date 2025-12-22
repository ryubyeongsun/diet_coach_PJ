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

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenstShoppingClient implements ShoppingClient {

    private final RestTemplate restTemplate;

    @Value("${elevenst.api.base-url}")
    private String baseUrl;

    @Value("${elevenst.api.key}")
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
        String traceId = currentTraceId();
        try {
            long startMs = System.currentTimeMillis();
            List<ShoppingProduct> real = callRealApi(keyword, page, size);
            if (real != null && !real.isEmpty()) {
                log.info("[SHOPPING_CLIENT][{}] REAL keyword=\"{}\" responseCount={} tookMs={}",
                        traceId, keyword, real.size(), System.currentTimeMillis() - startMs);
                return ShoppingClientResult.builder()
                        .products(real)
                        .source("REAL")
                        .build();
            }
            log.warn("[SHOPPING_CLIENT][{}] REAL keyword=\"{}\" responseCount=0 tookMs={}",
                    traceId, keyword, System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            log.warn("[SHOPPING_CLIENT][{}] REAL_FAIL keyword=\"{}\" ex={} fallback={}",
                    traceId, keyword, e.getClass().getSimpleName(), useMockWhenError);
            if (!useMockWhenError) throw e;
        }

        List<ShoppingProduct> mock = searchFromMock(keyword, page, size);
        log.info("[SHOPPING_CLIENT][{}] MOCK keyword=\"{}\" responseCount={}",
                traceId, keyword, mock.size());
        return ShoppingClientResult.builder()
                .products(mock)
                .source("MOCK")
                .build();
    }

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

        String response = restTemplate.getForObject(uri, String.class);

        if (response == null || response.isBlank()) {
            return List.of();
        }

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

        if (fromIndex >= filtered.size()) return List.of();
        return filtered.subList(fromIndex, toIndex);
    }

    private List<ShoppingProduct> parseProductsFromXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element e = (Element) node;

            String productCode = getTagValue(e, "ProductCode");
            String productName = getTagValue(e, "ProductName");
            String productPriceStr = getTagValue(e, "ProductPrice");
            String salePriceStr = getTagValue(e, "SalePrice");
            String imageUrl = getTagValue(e, "ProductImage");
            String detailPageUrl = getTagValue(e, "DetailPageUrl");
            String sellerNick = getTagValue(e, "SellerNick");

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

            ShoppingProduct product = ShoppingProduct.builder()
                    .externalId(productCode)
                    .title(productName)
                    .price(price)
                    .gramPerUnit(null)
                    .imageUrl(imageUrl)
                    .productUrl(detailPageUrl)
                    .mallName((sellerNick != null && !sellerNick.isBlank()) ? sellerNick : "11st")
                    .build();

            result.add(product);
        }

        log.info("[SHOPPING_CLIENT][{}] REAL_PARSED responseCount={}", currentTraceId(), result.size());
        return result;
    }

    private String getTagValue(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;
        Node node = list.item(0);
        if (node == null) return null;
        Node child = node.getFirstChild();
        return (child != null) ? child.getNodeValue() : null;
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId == null || traceId.isBlank()) ? "no-trace" : traceId;
    }
}
