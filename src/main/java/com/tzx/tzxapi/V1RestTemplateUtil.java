package com.tzx.tzxapi;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.json.JSONObject;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.*;


/**
 * @Author: Pang hu
 * @CreateTime: 2024-11-27
 * @Description:
 * @Version:
 */
public class V1RestTemplateUtil {


    // Please contact the administrator for this url
    private static String globalUrl = "";

    // Use the account you applied for on the platform
    private static String account = "";

    // Use your key
    private static String serKey = "";

    //user account
    private static String getTokenUrl = "/user/token";

    private static String tokenValid = "/user/tokenValid";

    private static String loginOutUrl = "/user/loginOut";

    private static String accountPaging = "/user/accountPaging";

    private static String cardOpen = "/card/open";

    private static String cardInfo = "/card/cardInfo";

    private static String cardCvc = "/card/cvc";

    private static String ownList = "/card/ownList";

    private static String freezeOrUnfreeze = "/card/freezeOrUnfreeze";

    private static String closeCard = "/card/closeCard";

    private static String authPaging = "/card/AuthPaging";

    private static String updateLimit = "/card/updateLimit";

    private static String getClientIdStatus = "/order/getClientIdStatus";

    private RestTemplate restTemplate;

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String post(ServletRequest req, String url, Map<String, ?> params) {
        ResponseEntity<String> rss = request(req, url, HttpMethod.POST, params);
        return rss.getBody();
    }

    public String get(ServletRequest req, String url, Map<String, ?> params) {
        ResponseEntity<String> rss = request(req, url, HttpMethod.GET, params);
        return rss.getBody();
    }

    public String get(ServletRequest req, String url, Map<String, String> headers, Map<String, ?> params) {
        ResponseEntity<String> rss = request(req, url, HttpMethod.GET, headers, params);
        return rss.getBody();
    }

    public String delete(ServletRequest req, String url, Map<String, ?> params) {
        ResponseEntity<String> rss = request(req, url, HttpMethod.DELETE, params);
        return rss.getBody();
    }

    public String put(ServletRequest req, String url, Map<String, ?> params) {
        ResponseEntity<String> rss = request(req, url, HttpMethod.PUT, params);
        return rss.getBody();
    }

    /**
     * @param req
     * @param url
     * @param method
     * @param params
     * @return
     */
    private ResponseEntity<String> request(ServletRequest req, String url, HttpMethod method, Map<String, ?> params) {
        HttpServletRequest request = (HttpServletRequest) req;

        HttpHeaders requestHeaders = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            requestHeaders.add(key, value);
        }

        if (params == null) {
            params = request.getParameterMap();
        }

        HttpEntity<String> requestEntity = new HttpEntity<String>(null, requestHeaders);
        ResponseEntity<String> rss = restTemplate.exchange(url, method, requestEntity, String.class, params);
        return rss;
    }

    private static ResponseEntity<String> request(ServletRequest req, String url, HttpMethod method, Map<String, String> headers, Map<String, ?> params) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpHeaders.add(entry.getKey(), entry.getValue());
            }
        }

        // constructor
        HttpEntity<Map<String, ?>> httpEntity = null;
        if (params != null && HttpMethod.POST.equals(method)) {
            httpEntity = new HttpEntity<>(params, httpHeaders);
        } else {
            httpEntity = new HttpEntity<>(httpHeaders);
        }

        RestTemplate restTemplate = new RestTemplate();

        //Prevent the return value from being garbled in Chinese
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

        String queryString = buildQueryString(params);
        String fullUrl = url;
        if (HttpMethod.GET.equals(method)) {
            fullUrl = url + (StringUtils.isEmpty(queryString) ? "" : "?" + queryString);
        }
        System.out.println(fullUrl);
        try {
            ResponseEntity<String> response = restTemplate.exchange(fullUrl, method, httpEntity, String.class);
            return response;
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Evaluating get request strings
     *
     * @param params
     * @return
     */
    private static String buildQueryString(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(entry.getKey()).append("=").append(entry.getValue().toString());
        }

        return queryString.toString();
    }

    /**
     * Get method cryptographic signature
     *
     * @param serKey
     * @param timeStr
     * @param requestPath
     * @param param
     * @return
     */
    public static String shaGet(String serKey, String timeStr, String requestPath, String param) {
        HMac hMac = SecureUtil.hmacSha256(serKey);
        String dataToHash = timeStr + "GET" + requestPath + param;
        System.out.println("GET request concatenated request path：" + dataToHash);
        return hMac.digestBase64(dataToHash, false);
    }

    /**
     * Post method cryptographic signature
     *
     * @param serKey
     * @param timeStr
     * @param requestPath
     * @param obj
     * @return
     */
    public static String shaPost(String serKey, String timeStr, String requestPath, Object obj) {
        JSONObject from = new JSONObject(obj);
        HMac hMac = SecureUtil.hmacSha256(serKey);
        String dataToHash = timeStr + "POST" + requestPath + from.toString();
        System.out.println("POST request concatenated request path：" + dataToHash);
        return hMac.digestBase64(dataToHash, false);
    }


    /**
     * getToken
     */
    public static void getToken() {
        String url = globalUrl + getTokenUrl;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map<String, String> body = new HashMap<>();
        body.put("account", account);
        body.put("appId", serKey);
        body.put("clientId", generateRandomStringFromTimestamp());

        String sign = shaPost(serKey, timeStr, getTokenUrl, sortMapByKey(body));
        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * getToken
     */
    public static void tokenValid(String token) {
        String url = globalUrl + tokenValid;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("share-api-token", token);
        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * loginOut
     *
     * @param token
     */
    public static void loginOut(String token) {
        String url = globalUrl + loginOutUrl;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());

        String sign = shaPost(serKey, timeStr, loginOutUrl, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * accountPaging
     *
     * @param token
     * @param accountId
     * @param current
     * @param size
     */
    public static void accountPaging(String token,
                                     String idNumber,
                                     String accountId,
                                     String current,
                                     String size) {

        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(idNumber)) {
            body.put("idNumber", idNumber);
        }
        if (StringUtils.isNotEmpty(accountId)) {
            body.put("accountId", accountId);
        }
        if (StringUtils.isNotEmpty(current)) {
            body.put("current", current);
        }
        if (StringUtils.isNotEmpty(size)) {
            body.put("size", size);
        }

        String url = globalUrl + accountPaging;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);
        String sign = shaGet(serKey, timeStr, accountPaging, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * cardOpen
     *
     * @param token
     * @param accountId
     */
    public static void cardOpen(String token, String accountId) {
        String url = globalUrl + cardOpen;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());
        body.put("accountId", accountId);

        String sign = shaPost(serKey, timeStr, cardOpen, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * freezeOrUnfreeze
     *
     * @param token
     * @param cardId
     * @param useFlag 0: Unfreeze  1:freeze
     */
    public static void freezeOrUnfreeze(String token, String cardId, String useFlag) {
        String url = globalUrl + freezeOrUnfreeze;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());
        body.put("token", token);
        body.put("cardId", cardId);
        body.put("useFlag", useFlag);
        String sign = shaPost(serKey, timeStr, freezeOrUnfreeze, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * cardInfo
     *
     * @param token
     */
    public static void cardInfo(String token,
                                String cardId
    ) {
        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(cardId)) {
            body.put("cardId", cardId);
        }


        String url = globalUrl + cardInfo;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);
        String sign = shaGet(serKey, timeStr, cardInfo, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * cardInfo
     *
     * @param token
     */
    public static void cardCvc(String token,
                               String cardId
    ) {
        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(cardId)) {
            body.put("cardId", cardId);
        }

        String url = globalUrl + cardCvc;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);
        String sign = shaGet(serKey, timeStr, cardCvc, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * cardInfo
     *
     * @param token
     */
    public static void getClientIdStatus(String token,
                                         String clientId
    ) {
        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(clientId)) {
            body.put("clientId", clientId);
        }

        String url = globalUrl + getClientIdStatus;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);
        String sign = shaGet(serKey, timeStr, getClientIdStatus, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * closeCard
     *
     * @param token
     * @param cardId
     */
    public static void closeCard(String token, String cardId) {
        String url = globalUrl + closeCard;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());
        body.put("cardId", cardId);
        String sign = shaPost(serKey, timeStr, closeCard, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * ownList
     *
     * @param token
     * @param accountId
     * @param current
     * @param size
     */

    public static void ownList(String token,
                               String accountId,
                               String cardNumber,
                               String endDate,
                               String startDate,
                               String current,
                               String status,
                               String size) {

        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(accountId)) {
            body.put("accountId", accountId);
        }
        if (StringUtils.isNotEmpty(cardNumber)) {
            body.put("cardNumber", cardNumber);
        }
        if (StringUtils.isNotEmpty(endDate)) {
            body.put("endDate", endDate);
        }
        if (StringUtils.isNotEmpty(startDate)) {
            body.put("startDate", startDate);
        }
        if (StringUtils.isNotEmpty(status)) {
            body.put("status", status);
        }
        if (StringUtils.isNotEmpty(current)) {
            body.put("current", current);
        }
        if (StringUtils.isNotEmpty(size)) {
            body.put("size", size);
        }

        String url = globalUrl + ownList;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);
        String sign = shaGet(serKey, timeStr, ownList, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * authPaging
     *
     * @param token
     * @param cardId
     * @param endDate
     * @param startDate
     * @param current
     * @param size
     */
    public static void authPaging(String token,
                                  String cardId,
                                  String endDate,
                                  String startDate,
                                  String current,
                                  String size) {

        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(cardId)) {
            body.put("cardId", cardId);
        }
        if (StringUtils.isNotEmpty(endDate)) {
            body.put("endDate", endDate);
        }
        if (StringUtils.isNotEmpty(startDate)) {
            body.put("startDate", startDate);
        }
        if (StringUtils.isNotEmpty(current)) {
            body.put("current", current);
        }
        if (StringUtils.isNotEmpty(size)) {
            body.put("size", size);
        }

        String url = globalUrl + authPaging;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);
        String sign = shaGet(serKey, timeStr, authPaging, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * updateLimit
     *
     * @param token
     * @param cardId
     * @param limit
     */
    public static void updateLimit(String token, String cardId, String limit) {
        String url = globalUrl + updateLimit;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());
        body.put("token", token);
        body.put("cardId", cardId);
        body.put("limit", limit);
        String sign = shaPost(serKey, timeStr, updateLimit, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("share-api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * Generate a 32-bit ID containing letters and numbers
     *
     * @return
     */
    public static String generateRandomStringFromTimestamp() {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        long timestamp = System.currentTimeMillis();
        random.setSeed(timestamp);

        for (int i = 0; i < 32; i++) {
            int randomIndex = random.nextInt(chars.length());
            sb.append(chars.charAt(randomIndex));
        }
        return sb.toString();
    }

    /**
     * Sort the keys of the Map alphabetically
     *
     * @param map
     * @return
     */
    public static Map<String, Object> sortMapByKey(Map<String, String> map) {
        // Converts a Map to a TreeMap, which sorts the keys.
        Map<String, Object> sortedMap = new TreeMap<>(map);
        return sortedMap;
    }

    /**
     * Sort the values in the map from a-z according to the key value
     *
     * @param params
     * @return
     */
    public static String paramsSplit(Map<String, String> params) {
        // Create a list to hold the eligible key-value pairs
        List<String> keyValues = new ArrayList<>();

        // Iterate over the Map and find all keys with the value targetValue
        for (Map.Entry<String, String> entry : params.entrySet()) {
            keyValues.add(entry.getKey() + "=" + entry.getValue());
        }
        // Concatenate found key-value pairs with "&" to form a string
        String result = String.join("&", keyValues);
        if (params != null && result != null && !result.isBlank()) {
            result = "?" + result;
        }
        return result;
    }


    public static void main(String[] args) {
//        getToken();
        //  loginOut("");
    }

}
