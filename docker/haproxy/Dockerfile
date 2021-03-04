FROM haproxy:2.2.10

COPY entrypoint.sh /usr/local/bin/
RUN set -xe; \
    chmod +x /usr/local/bin/entrypoint.sh; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        socat \
        ;

ENTRYPOINT ["entrypoint.sh"]
CMD ["haproxy", "-f", "/usr/local/etc/haproxy/haproxy.cfg"]
