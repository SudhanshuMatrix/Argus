# Argus live-alert server. Build: docker build -t argus-server backend/
FROM golang:1.24-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go index.html ./
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /argus-server .

FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /argus-server /argus-server
ENV PORT=8080
EXPOSE 8080
USER nonroot:nonroot
ENTRYPOINT ["/argus-server"]
