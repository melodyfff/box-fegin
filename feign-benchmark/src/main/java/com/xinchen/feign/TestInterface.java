package com.xinchen.feign;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;

import java.util.List;

/**
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @date Created In 2021/12/25 2:19
 */
@Headers("Accept: application/json")
interface TestInterface {
    @RequestLine("GET /?Action=GetUser&Version=2010-05-08&limit=1")
    Response query();

    @RequestLine("GET /domains/{domainId}/records?name={name}&type={type}")
    Response mixedParams(@Param("domainId") int id,
                         @Param("name") String nameFilter,
                         @Param("type") String typeFilter);

    @RequestLine("PATCH /")
    Response customMethod();

    @RequestLine("PUT /")
    @Headers("Content-Type: application/json")
    void bodyParam(List<String> body);

    @RequestLine("POST /")
    @Body("%7B\"customer_name\": \"{customer_name}\", \"user_name\": \"{user_name}\", \"password\": \"{password}\"%7D")
    void form(@Param("customer_name") String customer,
              @Param("user_name") String user,
              @Param("password") String password);

    @RequestLine("POST /")
    @Headers({"Happy: sad", "Auth-Token: {authToken}"})
    void headers(@Param("authToken") String token);
}

