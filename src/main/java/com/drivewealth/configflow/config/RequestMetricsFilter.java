package com.drivewealth.configflow.config;

import com.drivewealth.configflow.metrics.MetricsService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestMetricsFilter implements Filter {
    private final MetricsService metricsService;

    public RequestMetricsFilter(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();
        if (path.startsWith("/api/metrics")) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.nanoTime();
        StatusCapturingResponseWrapper wrapper = new StatusCapturingResponseWrapper(httpResponse);
        try {
            chain.doFilter(request, wrapper);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            metricsService.recordRequest(path, durationMs, wrapper.getStatus());
        }
    }
}

class StatusCapturingResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
    private int status = 200;

    public StatusCapturingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);
        this.status = sc;
    }

    @Override
    public void sendError(int sc) throws IOException {
        super.sendError(sc);
        this.status = sc;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        super.sendError(sc, msg);
        this.status = sc;
    }
    @Override
    public int getStatus() {
        return status;
    }
}
