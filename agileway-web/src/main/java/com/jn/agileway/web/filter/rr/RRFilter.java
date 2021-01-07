package com.jn.agileway.web.filter.rr;

import com.jn.agileway.web.filter.OncePerRequestFilter;
import com.jn.agileway.web.servlet.HttpServletRequestStreamWrapper;
import com.jn.langx.util.BooleanEvaluator;
import com.jn.langx.util.Emptys;
import com.jn.langx.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RRFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RRFilter.class);
    private boolean streamWrapperEnabled = false;
    private String encoding = "UTF-8";

    public void init(final FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        String streamWrapperEnabled = filterConfig.getInitParameter("streamWrapperEnabled");
        boolean enabled = BooleanEvaluator.createTrueEvaluator(false, true, new Object[]{"true"}).evalTrue(streamWrapperEnabled);
        this.streamWrapperEnabled = enabled;

        String encoding = filterConfig.getInitParameter("encoding");
        if (Emptys.isNotEmpty(encoding)) {
            setEncoding(encoding);
        }

        logger.info("Initial Base Web Filter (RRFilter) with config : {}", filterConfig);

    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void doFilterInternal(final ServletRequest request, final ServletResponse response, final FilterChain chain) {
        try {
            request.setCharacterEncoding(encoding);
            if (request instanceof HttpServletRequest) {
                HttpServletRequest req = streamWrapperEnabled
                        ? new HttpServletRequestStreamWrapper((HttpServletRequest) request)
                        : (HttpServletRequest) request;
                RRHolder.set(req, (HttpServletResponse) response);
            }
            chain.doFilter(request, response);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            Throwables.throwAsRuntimeException(t);
        } finally {
            RRHolder.remove();
        }
    }
}
