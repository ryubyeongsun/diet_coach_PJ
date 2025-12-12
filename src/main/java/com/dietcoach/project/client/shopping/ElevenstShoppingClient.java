package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenstShoppingClient implements ShoppingClient {

    private final RestTemplate restTemplate;

    @Value("${elevenst.api.base-url}")
    private String baseUrl;

    @Value("${elevenst.api.key}")
    private String apiKey;

    @Override
    public List<ShoppingProduct> searchProducts(String keyword, int page, int size) {
        return callRealApi(keyword, page, size);
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

        log.info("[11st] Calling API: {}", uri);

        String response = restTemplate.getForObject(uri, String.class);

        if (response == null || response.isBlank()) {
            log.warn("[11st] Empty response");
            return List.of();
        }

        try {
            return parseProductsFromXml(response);
        } catch (Exception e) {
            log.error("[11st] Failed to parse XML response", e);
            // 실API 전용: 예외를 던져 Hybrid가 fallback 하도록
            throw new RuntimeException("11st XML parse failed", e);
        }
    }

    /**
     * 11번가 ProductSearch XML 응답을 ShoppingProduct 리스트로 변환
     */
    private List<ShoppingProduct> parseProductsFromXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // XXE 방지
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

            // 가격: SalePrice 우선, 없으면 ProductPrice
            int price = 0;
            try {
                if (salePriceStr != null && !salePriceStr.isBlank()) {
                    price = Integer.parseInt(salePriceStr.trim());
                } else if (productPriceStr != null && !productPriceStr.isBlank()) {
                    price = Integer.parseInt(productPriceStr.trim());
                }
            } catch (NumberFormatException ex) {
                log.warn("[11st] Cannot parse price. salePrice={}, productPrice={}", salePriceStr, productPriceStr);
            }

            // gramPerUnit은 현재 응답에 없으니 null (추후 title 파싱으로 추정 가능)
            Double gramPerUnit = null;

            ShoppingProduct product = ShoppingProduct.builder()
                    .externalId(productCode)
                    .title(productName)
                    .price(price)
                    .gramPerUnit(gramPerUnit)
                    .imageUrl(imageUrl)
                    .productUrl(detailPageUrl)
                    .mallName((sellerNick != null && !sellerNick.isBlank()) ? sellerNick : "11st")
                    .build();

            result.add(product);
        }

        log.info("[11st] Parsed {} products from XML", result.size());
        return result;
    }

    /**
     * XML Element에서 특정 태그 텍스트 값 가져오기
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
