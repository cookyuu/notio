package com.notio.common.ratelimit;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import org.springframework.util.StreamUtils;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    @Getter
    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(final HttpServletRequest request, final int maxBodyBytes) throws IOException {
        super(request);
        validateContentLength(request, maxBodyBytes);
        this.cachedBody = readBody(request, maxBodyBytes);
    }

    public String cachedBodyAsString() {
        return new String(cachedBody, resolveCharset());
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(final ReadListener readListener) {
                // async IO not used
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), resolveCharset()));
    }

    private void validateContentLength(final HttpServletRequest request, final int maxBodyBytes) {
        final long contentLength = request.getContentLengthLong();
        if (contentLength > maxBodyBytes) {
            throw new PayloadTooLargeException(maxBodyBytes);
        }
    }

    private byte[] readBody(final HttpServletRequest request, final int maxBodyBytes) throws IOException {
        final byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        if (body.length > maxBodyBytes) {
            throw new PayloadTooLargeException(maxBodyBytes);
        }
        return body;
    }

    private Charset resolveCharset() {
        final String encoding = getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(encoding);
    }
}
