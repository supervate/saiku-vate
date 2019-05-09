package org.saiku.web.filterts;


import org.apache.http.HttpStatus;

import javax.servlet.*;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CorsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse)servletResponse;
        HttpServletRequest httpRequest = (HttpServletRequest)servletRequest;

        httpResponse.addHeader("Access-Control-Allow-Origin", "*");
        httpResponse.addHeader("Access-Control-Allow-Methods", "POST,GET,PUT,DELETE,OPTIONS");
        httpResponse.addHeader("Access-Control-Allow-Headers", "Origin,token, Content-Type, Accept, Authorization, x-requested-with, cache-control, Access-Control-Allow-Origin, Access-Control-Allow-Credentials, uuid");
        httpResponse.addHeader("Access-Control-Max-Age", "360000");
        httpResponse.addHeader("Access-Control-Allow-Credentials", "false");
        httpResponse.addHeader("Access-Control-Request-Headers", "Origin,token, Content-Type, Accept, Authorization, x-requested-with, cache-control, Access-Control-Allow-Origin, Access-Control-Allow-Credentials, uuid");
        httpResponse.addHeader("Access-Control-Expose-Headers", "Origin,token, Content-Type, Accept, Authorization, x-requested-with, cache-control, Access-Control-Allow-Origin, Access-Control-Allow-Credentials, uuid");

        if (httpRequest.getMethod().equals("OPTIONS")) {
            httpResponse.setStatus(HttpStatus.SC_OK);
           // httpResponse.getWriter().write("OPTIONS returns OK");
            return;

        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
