package main

import (
	"bytes"
	"context"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"
)

// ServiceAccount is the part of a Firebase service-account JSON key the server needs.
type ServiceAccount struct {
	ProjectID   string `json:"project_id"`
	ClientEmail string `json:"client_email"`
	PrivateKey  string `json:"private_key"`
	TokenURI    string `json:"token_uri"`
}

const fcmScope = "https://www.googleapis.com/auth/firebase.messaging"

// FCM sends data messages to an FCM topic with the HTTP v1 API, authenticating with a
// service account (OAuth 2.0 JWT bearer flow). Only the standard library is used.
type FCM struct {
	account  ServiceAccount
	key      *rsa.PrivateKey
	client   *http.Client
	endpoint string // https://fcm.googleapis.com, replaceable in tests

	mu      sync.Mutex
	token   string
	expires time.Time
}

func NewFCM(serviceAccountJSON []byte, client *http.Client) (*FCM, error) {
	var sa ServiceAccount
	if err := json.Unmarshal(serviceAccountJSON, &sa); err != nil {
		return nil, fmt.Errorf("service account JSON: %w", err)
	}
	if sa.ProjectID == "" || sa.ClientEmail == "" || sa.PrivateKey == "" {
		return nil, errors.New("service account JSON needs project_id, client_email and private_key")
	}
	if sa.TokenURI == "" {
		sa.TokenURI = "https://oauth2.googleapis.com/token"
	}
	key, err := parseKey(sa.PrivateKey)
	if err != nil {
		return nil, err
	}
	if client == nil {
		client = &http.Client{Timeout: 15 * time.Second}
	}
	return &FCM{account: sa, key: key, client: client, endpoint: "https://fcm.googleapis.com"}, nil
}

func parseKey(pemKey string) (*rsa.PrivateKey, error) {
	block, _ := pem.Decode([]byte(pemKey))
	if block == nil {
		return nil, errors.New("service account private_key is not PEM")
	}
	parsed, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		if k, err2 := x509.ParsePKCS1PrivateKey(block.Bytes); err2 == nil {
			return k, nil
		}
		return nil, fmt.Errorf("service account private_key: %w", err)
	}
	k, ok := parsed.(*rsa.PrivateKey)
	if !ok {
		return nil, errors.New("service account private_key is not RSA")
	}
	return k, nil
}

// SendToTopic sends one data-only, high-priority message to every phone on topic. Returns
// FCM's message name.
func (f *FCM) SendToTopic(ctx context.Context, topic string, data map[string]string) (string, error) {
	token, err := f.accessToken(ctx)
	if err != nil {
		return "", err
	}
	body, _ := json.Marshal(map[string]any{
		"message": map[string]any{
			"topic": topic,
			"data":  data,
			// High priority wakes the phone at once; a short TTL drops stale alerts
			"android": map[string]any{"priority": "HIGH", "ttl": "600s"},
		},
	})
	u := fmt.Sprintf("%s/v1/projects/%s/messages:send", f.endpoint, url.PathEscape(f.account.ProjectID))
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, u, bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Content-Type", "application/json")
	resp, err := f.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("fcm send: %w", err)
	}
	defer resp.Body.Close()
	respBody, _ := io.ReadAll(io.LimitReader(resp.Body, 64<<10))
	if resp.StatusCode != http.StatusOK {
		if resp.StatusCode == http.StatusUnauthorized {
			f.mu.Lock()
			f.token = "" // fetch a fresh token next time
			f.mu.Unlock()
		}
		return "", fmt.Errorf("fcm send: HTTP %d: %s", resp.StatusCode, strings.TrimSpace(string(respBody)))
	}
	var out struct {
		Name string `json:"name"`
	}
	_ = json.Unmarshal(respBody, &out)
	return out.Name, nil
}

// accessToken returns a cached OAuth token, fetching a new one shortly before it expires.
func (f *FCM) accessToken(ctx context.Context) (string, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	if f.token != "" && time.Until(f.expires) > time.Minute {
		return f.token, nil
	}
	assertion, err := f.signJWT(time.Now())
	if err != nil {
		return "", err
	}
	form := url.Values{"grant_type": {"urn:ietf:params:oauth:grant-type:jwt-bearer"}, "assertion": {assertion}}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, f.account.TokenURI, strings.NewReader(form.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, err := f.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("oauth token: %w", err)
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(io.LimitReader(resp.Body, 64<<10))
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("oauth token: HTTP %d: %s", resp.StatusCode, strings.TrimSpace(string(body)))
	}
	var tok struct {
		AccessToken string `json:"access_token"`
		ExpiresIn   int    `json:"expires_in"`
	}
	if err := json.Unmarshal(body, &tok); err != nil || tok.AccessToken == "" {
		return "", errors.New("oauth token: no access_token in reply")
	}
	f.token = tok.AccessToken
	f.expires = time.Now().Add(time.Duration(tok.ExpiresIn) * time.Second)
	return f.token, nil
}

func (f *FCM) signJWT(now time.Time) (string, error) {
	enc := base64.RawURLEncoding
	header := enc.EncodeToString([]byte(`{"alg":"RS256","typ":"JWT"}`))
	claims, _ := json.Marshal(map[string]any{
		"iss":   f.account.ClientEmail,
		"scope": fcmScope,
		"aud":   f.account.TokenURI,
		"iat":   now.Unix(),
		"exp":   now.Add(time.Hour).Unix(),
	})
	signingInput := header + "." + enc.EncodeToString(claims)
	sum := sha256.Sum256([]byte(signingInput))
	sig, err := rsa.SignPKCS1v15(rand.Reader, f.key, crypto.SHA256, sum[:])
	if err != nil {
		return "", err
	}
	return signingInput + "." + enc.EncodeToString(sig), nil
}
