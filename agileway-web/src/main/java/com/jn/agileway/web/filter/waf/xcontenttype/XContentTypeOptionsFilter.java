package com.jn.agileway.web.filter.waf.xcontenttype;

import com.jn.agileway.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class XContentTypeOptionsFilter extends OncePerRequestFilter {
    private XContentTypeOptionsProperties properties = new XContentTypeOptionsProperties();

    public void setProperties(XContentTypeOptionsProperties properties) {
        this.properties = properties;
    }


    @Override
    protected void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (properties.isEnabled()) {
            if (request instanceof HttpServletRequest) {
                ((HttpServletResponse) response).setHeader("X-Content-Type-Options", "nosniff");
            }
        }
        doFilter(request, response, chain);
    }
}
