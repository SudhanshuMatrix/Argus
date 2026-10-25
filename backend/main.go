// Command argus-server sends live scam alerts to every Argus phone through Firebase Cloud
// Messaging. See docs/BACKEND.md for setup and deployment.
package main

import (
	"errors"
	"log"
	"net/http"
	"os"
	"time"
)

func main() {
	cfg, err := loadConfig()
	if err != nil {
		log.Fatalf("argus-server: %v", err)
	}
	fcm, err := NewFCM(cfg.serviceAccount, nil)
	if err != nil {
		log.Fatalf("argus-server: %v", err)
	}
	srv := &http.Server{
		Addr:              ":" + cfg.port,
		Handler:           NewServer(cfg.username, cfg.password, cfg.topic, fcm).Handler(),
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      30 * time.Second,
	}
	log.Printf("argus-server: listening on :%s, topic %s", cfg.port, cfg.topic)
	log.Fatal(srv.ListenAndServe())
}

type config struct {
	username, password, topic, port string
	serviceAccount                  []byte
}

// loadConfig reads everything from the environment, so no secret is ever in the code or repo.
func loadConfig() (config, error) {
	c := config{
		username: os.Getenv("ARGUS_ADMIN_USERNAME"),
		password: os.Getenv("ARGUS_ADMIN_PASSWORD"),
		topic:    envOr("ARGUS_FCM_TOPIC", "argus-alerts"),
		port:     envOr("PORT", "8080"),
	}
	if c.username == "" || c.password == "" {
		return c, errors.New("set ARGUS_ADMIN_USERNAME and ARGUS_ADMIN_PASSWORD")
	}
	if len(c.password) < 8 {
		return c, errors.New("ARGUS_ADMIN_PASSWORD must be at least 8 characters")
	}
	if j := os.Getenv("ARGUS_FCM_SERVICE_ACCOUNT"); j != "" {
		c.serviceAccount = []byte(j)
	} else if path := envOr("ARGUS_FCM_SERVICE_ACCOUNT_FILE", os.Getenv("GOOGLE_APPLICATION_CREDENTIALS")); path != "" {
		b, err := os.ReadFile(path)
		if err != nil {
			return c, err
		}
		c.serviceAccount = b
	} else {
		return c, errors.New("set ARGUS_FCM_SERVICE_ACCOUNT (the Firebase service-account JSON) or ARGUS_FCM_SERVICE_ACCOUNT_FILE")
	}
	return c, nil
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
