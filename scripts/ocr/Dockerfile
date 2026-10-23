# syntax=docker/dockerfile:1
FROM ubuntu:24.04
RUN --mount=type=secret,id=apt-ca,mode=0444 \
    if [ -f /run/secrets/apt-ca ]; then \
      sed -i 's|http://|https://|g' /etc/apt/sources.list.d/ubuntu.sources; \
      printf 'Acquire::https::CaInfo "/run/secrets/apt-ca";' > /etc/apt/apt.conf.d/99-build-ca; \
    fi; \
    apt-get update && apt-get install -y --no-install-recommends ca-certificates tesseract-ocr tesseract-ocr-eng tesseract-ocr-chi-sim && \
    rm -rf /var/lib/apt/lists/* /etc/apt/apt.conf.d/99-build-ca
ENV OMP_THREAD_LIMIT=2
CMD ["tesseract", "--version"]

