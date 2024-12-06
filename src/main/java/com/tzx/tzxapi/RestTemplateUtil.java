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
 * @CreateTime: 2024-07-16
 * @Description: tzx Developer Center
 * @Version: v1.0.0
 */
public class RestTemplateUtil {

    // Please contact the administrator for this url
    private static String globalUrl = "";

    // Use the account you applied for on the platform
    private static String account = "";

    // Use your key
    private static String serKey = "";

    private static String getTokenUrl = "/user/getToken";

    private static String loginOutUrl = "/user/loginOut";

    private static String cardAvailList = "/card/availList";

    private static String walletBalance = "/wallet/balance";

    private static String userCreateCardholder = "/user/createCardholder";

    private static String userList = "/user/list";

    private static String cardOpen = "/card/open";

    private static String cardInfo = "/card/info";

    private static String cardDeposit = "/card/deposit";

    private static String cardOwnList = "/card/ownList";

    private static String cardFreezeAndUnfreeze = "/card/freezeAndUnfreeze";

    private static String cardWithdraw = "/card/withdraw";

    private static String cardCloseCard = "/card/closeCard";

    private static String cardPaging = " /card/paging";

    private static String orderGetClientIdStatus = "/order/getClientIdStatus";

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
     * getToken
     */
    public static void getToken() {
        String url = globalUrl + getTokenUrl;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        String params = "?account=" + account + "&appId=" + serKey;
        String sign = shaGet(serKey, timeStr, getTokenUrl, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);

        Map<String, Object> body = new HashMap<>();
        body.put("account", account);
        body.put("appId", serKey);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
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
        body.put("appId", account);
        body.put("clientId", generateRandomStringFromTimestamp());

        String sign = shaPost(serKey, timeStr, loginOutUrl, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * walletBalance
     *
     * @param token
     */
    public static void walletBalance(String token) {
        String url = globalUrl + walletBalance;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        String sign = shaGet(serKey, timeStr, walletBalance, "");
        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, null);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * cardAvailList
     *
     * @param token
     * @param area
     * @param current
     * @param existsCardOpen
     * @param groupName
     * @param nameCn
     * @param sectionNo
     * @param size
     */
    public static void cardAvailList(String token, String area, String current, String existsCardOpen, String groupName, String nameCn, String sectionNo, String size) {
        String url = globalUrl + cardAvailList;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(area)) {
            body.put("area", area);
        }
        if (StringUtils.isNotEmpty(current)) {
            body.put("current", current);
        }
        if (StringUtils.isNotEmpty(existsCardOpen)) {
            body.put("existsCardOpen", existsCardOpen);
        }
        if (StringUtils.isNotEmpty(groupName)) {
            body.put("groupName", groupName);
        }
        if (StringUtils.isNotEmpty(nameCn)) {
            body.put("nameCn", nameCn);
        }
        if (StringUtils.isNotEmpty(sectionNo)) {
            body.put("sectionNo", sectionNo);
        }
        if (StringUtils.isNotEmpty(size)) {
            body.put("size", size);
        }

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);
        System.out.println("拼接的参数：" + params);

        String sign = shaGet(serKey, timeStr, cardAvailList, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);


        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * userCreateCardholder
     *
     * @param token
     * @param birthday               2000-06-06
     * @param cardTypeId
     * @param email
     * @param firstName
     * @param lastName
     * @param mobile
     * @param mobilePrefix
     * @param nationalityCountryCode
     * @param residentialAddress
     * @param residentialCountryCode
     * @param residentialCity
     * @param residentialPostalCode
     * @param residentialState
     */
    public static void userCreateCardholder(String token, String birthday, String cardTypeId, String email, String firstName, String lastName, String mobile, String mobilePrefix,
                                            String nationalityCountryCode, String residentialAddress, String residentialCountryCode, String residentialCity, String residentialPostalCode, String residentialState) {

        String url = globalUrl + userCreateCardholder;
        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());
        if (StringUtils.isNotEmpty(birthday)) {
            body.put("birthday", birthday);
        }
        if (StringUtils.isNotEmpty(cardTypeId)) {
            body.put("cardTypeId", cardTypeId);
        }
        if (StringUtils.isNotEmpty(email)) {
            body.put("email", email);
        }
        if (StringUtils.isNotEmpty(firstName)) {
            body.put("firstName", firstName);
        }
        if (StringUtils.isNotEmpty(lastName)) {
            body.put("lastName", lastName);
        }
        if (StringUtils.isNotEmpty(mobile)) {
            body.put("mobile", mobile);
        }
        if (StringUtils.isNotEmpty(mobilePrefix)) {
            body.put("mobilePrefix", mobilePrefix);
        }
        if (StringUtils.isNotEmpty(nationalityCountryCode)) {
            body.put("nationalityCountryCode", nationalityCountryCode);
        }
        if (StringUtils.isNotEmpty(residentialAddress)) {
            body.put("residentialAddress", residentialAddress);
        }
        if (StringUtils.isNotEmpty(residentialCountryCode)) {
            body.put("residentialCountryCode", residentialCountryCode);
        }
        if (StringUtils.isNotEmpty(residentialCity)) {
            body.put("residentialCity", residentialCity);
        }
        if (StringUtils.isNotEmpty(residentialPostalCode)) {
            body.put("residentialPostalCode", residentialPostalCode);
        }
        if (StringUtils.isNotEmpty(residentialState)) {
            body.put("residentialState", residentialState);
        }

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        String sign = shaPost(serKey, timeStr, userCreateCardholder, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * userList
     *
     * @param token
     * @param cardTypeId
     */
    public static void userList(String token, String cardTypeId) {
        String url = globalUrl + userList;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        body.put("cardTypeId", cardTypeId);
        String params = paramsSplit(body);

        String sign = shaGet(serKey, timeStr, userList, params);
        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * cardOpen
     *
     * @param token
     * @param amt
     * @param cardCount
     * @param cardTypeId
     * @param cardType
     * @param cardUserInfoId
     * @param currencyCode
     * @param email
     * @param firstName
     * @param phone
     * @param phoneCode
     * @param remark
     * @param residentialAddress
     * @param residentialCountryCode
     * @param residentialCity
     * @param residentialPostalCode
     * @param residentialState
     * @param lastName
     */
    public static void cardOpen(String token, String amt, String cardCount, String cardTypeId, String cardType, String cardUserInfoId, String currencyCode, String email, String firstName,
                                String phone, String phoneCode, String remark, String residentialAddress, String residentialCountryCode, String residentialCity, String residentialPostalCode,
                                String residentialState, String lastName) {
        String url = globalUrl + cardOpen;

        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());

        if (StringUtils.isNotEmpty(amt)) {
            body.put("amt", amt);
        }
        if (StringUtils.isNotEmpty(cardCount)) {
            body.put("cardCount", cardCount);
        }
        if (StringUtils.isNotEmpty(cardTypeId)) {
            body.put("cardTypeId", cardTypeId);
        }
        if (StringUtils.isNotEmpty(cardType)) {
            body.put("cardType", cardType);
        }
        if (StringUtils.isNotEmpty(currencyCode)) {
            body.put("currencyCode", currencyCode);
        }
        if (StringUtils.isNotEmpty(cardUserInfoId)) {
            body.put("cardUserInfoId", cardUserInfoId);
        }
        if (StringUtils.isNotEmpty(email)) {
            body.put("email", email);
        }
        if (StringUtils.isNotEmpty(firstName)) {
            body.put("firstName", firstName);
        }
        if (StringUtils.isNotEmpty(lastName)) {
            body.put("lastName", lastName);
        }
        if (StringUtils.isNotEmpty(phone)) {
            body.put("phone", phone);
        }
        if (StringUtils.isNotEmpty(phoneCode)) {
            body.put("phoneCode", phoneCode);
        }
        if (StringUtils.isNotEmpty(remark)) {
            body.put("remark", remark);
        }
        if (StringUtils.isNotEmpty(residentialAddress)) {
            body.put("residentialAddress", residentialAddress);
        }
        if (StringUtils.isNotEmpty(residentialCountryCode)) {
            body.put("residentialCountryCode", residentialCountryCode);
        }
        if (StringUtils.isNotEmpty(residentialCity)) {
            body.put("residentialCity", residentialCity);
        }
        if (StringUtils.isNotEmpty(residentialPostalCode)) {
            body.put("residentialPostalCode", residentialPostalCode);
        }
        if (StringUtils.isNotEmpty(residentialState)) {
            body.put("residentialState", residentialState);
        }

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        String sign = shaPost(serKey, timeStr, cardOpen, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }

    }

    /**
     * cardInfo
     *
     * @param token
     * @param id
     */
    private static void cardInfo(String token, String id) {
        String url = globalUrl + cardInfo;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        body.put("id", id);
        String params = paramsSplit(body);

        String sign = shaGet(serKey, timeStr, cardInfo, params);
        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * cardOwnList
     *
     * @param token
     * @param cardTypeId
     * @param cardNumber
     * @param current
     * @param endTime
     * @param remark
     * @param sortMap
     * @param size
     * @param startTime
     * @param status
     */
    private static void cardOwnList(String token, String cardTypeId, String cardNumber, String current, String endTime, String remark,
                                    String sortMap, String size, String startTime, String status) {
        String url = globalUrl + cardOwnList;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(cardTypeId)) {
            body.put("cardTypeId", cardTypeId);
        }
        if (StringUtils.isNotEmpty(cardNumber)) {
            body.put("cardNumber", cardNumber);
        }
        if (StringUtils.isNotEmpty(current)) {
            body.put("current", current);
        }
        if (StringUtils.isNotEmpty(endTime)) {
            body.put("endTime", endTime);
        }
        if (StringUtils.isNotEmpty(remark)) {
            body.put("remark", remark);
        }
        if (StringUtils.isNotEmpty(sortMap)) {
            body.put("sortMap", sortMap);
        }
        if (StringUtils.isNotEmpty(size)) {
            body.put("size", size);
        }
        if (StringUtils.isNotEmpty(startTime)) {
            body.put("startTime", startTime);
        }
        if (StringUtils.isNotEmpty(status)) {
            body.put("status", status);
        }

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);

        String sign = shaGet(serKey, timeStr, cardOwnList, params);
        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * cardDeposit
     *
     * @param token
     * @param amount
     * @param adminCardId
     * @param remark
     */
    private static void cardDeposit(String token, String amount, String adminCardId, String remark) {
        String url = globalUrl + cardDeposit;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());

        if (StringUtils.isNotEmpty(amount)) {
            body.put("amount", amount);
        }
        if (StringUtils.isNotEmpty(adminCardId)) {
            body.put("adminCardId", adminCardId);
        }
        if (StringUtils.isNotEmpty(remark)) {
            body.put("remark", remark);
        }

        String sign = shaPost(serKey, timeStr, cardDeposit, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }

    }

    /**
     * cardWithdraw
     *
     * @param token
     * @param amount
     * @param adminCardId
     * @param remark
     */
    private static void cardWithdraw(String token, String amount, String adminCardId, String remark) {
        String url = globalUrl + cardWithdraw;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());

        if (StringUtils.isNotEmpty(amount)) {
            body.put("amount", amount);
        }
        if (StringUtils.isNotEmpty(adminCardId)) {
            body.put("adminCardId", adminCardId);
        }
        if (StringUtils.isNotEmpty(remark)) {
            body.put("remark", remark);
        }

        String sign = shaPost(serKey, timeStr, cardWithdraw, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * cardFreezeAndUnfreeze
     *
     * @param token
     * @param adminCardId
     * @param status
     */
    private static void cardFreezeAndUnfreeze(String token, String adminCardId, String status) {
        String url = globalUrl + cardFreezeAndUnfreeze;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());

        if (StringUtils.isNotEmpty(adminCardId)) {
            body.put("adminCardId", adminCardId);
        }
        if (StringUtils.isNotEmpty(status)) {
            body.put("status", status);
        }

        String sign = shaPost(serKey, timeStr, cardFreezeAndUnfreeze, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }

    }

    /**
     * cardCloseCard
     *
     * @param token
     * @param adminCardId
     */
    private static void cardCloseCard(String token, String adminCardId) {
        String url = globalUrl + cardCloseCard;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        body.put("clientId", generateRandomStringFromTimestamp());

        if (StringUtils.isNotEmpty(adminCardId)) {
            body.put("adminCardId", adminCardId);
        }
        String sign = shaPost(serKey, timeStr, cardFreezeAndUnfreeze, sortMapByKey(body));

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.POST, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }

    /**
     * cardPaging
     *
     * @param token
     * @param authType
     * @param cardNumber
     * @param current
     * @param endDate
     * @param sortMap
     * @param size
     * @param status
     * @param startDate
     * @param username
     */
    private static void cardPaging(String token, String authType, String cardNumber, String current, String endDate, String sortMap, String size,
                                   String status, String startDate, String username) {
        String url = globalUrl + cardPaging;

        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);

        Map<String, String> body = new HashMap<>();
        if (StringUtils.isNotEmpty(authType)) {
            body.put("authType", authType);
        }
        if (StringUtils.isNotEmpty(cardNumber)) {
            body.put("cardNumber", cardNumber);
        }
        if (StringUtils.isNotEmpty(current)) {
            body.put("current", current);
        }
        if (StringUtils.isNotEmpty(endDate)) {
            body.put("endDate", endDate);
        }
        if (StringUtils.isNotEmpty(sortMap)) {
            body.put("sortMap", sortMap);
        }
        if (StringUtils.isNotEmpty(status)) {
            body.put("status", status);
        }
        if (StringUtils.isNotEmpty(size)) {
            body.put("size", size);
        }
        if (StringUtils.isNotEmpty(startDate)) {
            body.put("startDate", startDate);
        }
        if (StringUtils.isNotEmpty(username)) {
            body.put("username", username);
        }

        Map newMap = sortMapByKey(body);
        String params = paramsSplit(newMap);
        String sign = shaGet(serKey, timeStr, cardPaging, params);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);

        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
        if (response.getStatusCodeValue() == 200) {
            System.out.println(response.getBody());
        }
    }


    /**
     * orderGetClientIdStatus
     *
     * @param token
     * @param clientId
     */
    private static void orderGetClientIdStatus(String token, String clientId) {
        String url = globalUrl + orderGetClientIdStatus;
        Long timestamp = System.currentTimeMillis() / 1000;
        String timeStr = String.valueOf(timestamp);
        Map<String, String> body = new HashMap<>();
        body.put("clientId", clientId);
        String params = paramsSplit(body);

        String sign = shaGet(serKey, timeStr, orderGetClientIdStatus, params);
        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timeStr);
        headers.put("sign", sign);
        headers.put("api-token", token);
        ResponseEntity response = request(null, url, HttpMethod.GET, headers, body);
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
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
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
     * test
     *
     * @param args
     * @describe
     */
    public static void main(String[] args) {
//        getToken();

//        loginOut("6c12077b-47d6-4c35-8e17-af2a3a679d81");

//        walletBalance("d982641d-ec38-4707-bbed-e4412f39f45d");

//       cardAvailList("d982641d-ec38-4707-bbed-e4412f39f45d", "us", "1", null, null, null, null, "1");

//      userCreateCardholder("d982641d-ec38-4707-bbed-e4412f39f45d", "2000-06-06", "1633357242575011842", "demo@outlook.com", "jack", "joins", "123456789", "+852", "HK",
//      "Flat130 13/F King's Commereial Building, No.2-4 Chatham Court T.S.T, Kowloon", "HK", "HK", "999077", "HK");

//      userList("d982641d-ec38-4707-bbed-e4412f39f45d", "1633357242575011842");

//        cardOpen("d982641d-ec38-4707-bbed-e4412f39f45d", "10", "1", "1798958399654772737", "2", "1796015831547691009", null,"121321321@outlook.com", null,
//                null, null, null, null, null, null, null, null, null);

//        cardInfo("d982641d-ec38-4707-bbed-e4412f39f45d", "1798181346319843329");

//        cardWithdraw("d982641d-ec38-4707-bbed-e4412f39f45d", "1", "1798181346319843329", "test");

//        cardFreezeAndUnfreeze("d982641d-ec38-4707-bbed-e4412f39f45d", "1798181346319843329", "0");

//        cardOwnList("d982641d-ec38-4707-bbed-e4412f39f45d", null, null, null, null, null, null, null, null, null);

//        orderGetClientIdStatus("d982641d-ec38-4707-bbed-e4412f39f45d", "URN19pA5Z9ao3E3jttyCnIhS4qn2J0MI");

    }

}
