package main

import (
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
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
	"time"
)

type fakeSender struct {
	topic string
	data  map[string]string
	calls int
	err   error
}

func (f *fakeSender) SendToTopic(_ context.Context, topic string, data map[string]string) (string, error) {
	f.calls++
	f.topic, f.data = topic, data
	return "projects/p/messages/1", f.err
}

func post(t *testing.T, h http.Handler, user, pass, contentType, body string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(http.MethodPost, "/api/alerts", strings.NewReader(body))
	req.RemoteAddr = "203.0.113.7:1234"
	if user != "" || pass != "" {
		req.SetBasicAuth(user, pass)
	}
	req.Header.Set("Content-Type", contentType)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	return rec
}

func TestSendAlertWithTheRightLogin(t *testing.T) {
	sender := &fakeSender{}
	h := NewServer("sudhanshu", "correct-horse", "argus-alerts", sender).Handler()
	rec := post(t, h, "sudhanshu", "correct-horse", "application/json", `{"message":"  Your SBI account is blocked, click bit.ly/x  "}`)
	if rec.Code != http.StatusOK {
		t.Fatalf("status %d: %s", rec.Code, rec.Body)
	}
	if sender.topic != "argus-alerts" || sender.data["type"] != "live" || sender.data["message"] != "Your SBI account is blocked, click bit.ly/x" || sender.data["id"] == "" {
		t.Fatalf("sent %q %v", sender.topic, sender.data)
	}
	var resp alertResponse
	_ = json.Unmarshal(rec.Body.Bytes(), &resp)
	if !resp.OK || resp.ID != sender.data["id"] {
		t.Fatalf("response %+v", resp)
	}
}

func TestFormPostsWork(t *testing.T) {
	sender := &fakeSender{}
	h := NewServer("sudhanshu", "correct-horse", "t", sender).Handler()
	rec := post(t, h, "sudhanshu", "correct-horse", "application/x-www-form-urlencoded", url.Values{"message": {"नया स्कैम: OTP मत बताइए"}}.Encode())
	if rec.Code != http.StatusOK || sender.data["message"] != "नया स्कैम: OTP मत बताइए" {
		t.Fatalf("status %d, sent %v", rec.Code, sender.data)
	}
}

func TestWrongLoginIsRefusedAndNothingIsSent(t *testing.T) {
	sender := &fakeSender{}
	h := NewServer("sudhanshu", "correct-horse", "t", sender).Handler()
	for _, c := range [][2]string{{"sudhanshu", "wrong"}, {"someone", "correct-horse"}, {"", ""}} {
		rec := post(t, h, c[0], c[1], "application/json", `{"message":"hi"}`)
		if rec.Code != http.StatusUnauthorized {
			t.Fatalf("%v: status %d", c, rec.Code)
		}
	}
	if sender.calls != 0 {
		t.Fatalf("sent %d times without a login", sender.calls)
	}
}

func TestLockoutAfterFiveWrongPasswords(t *testing.T) {
	sender := &fakeSender{}
	s := NewServer("sudhanshu", "correct-horse", "t", sender)
	now := time.Date(2026, 10, 25, 12, 0, 0, 0, time.UTC)
	s.now = func() time.Time { return now }
	h := s.Handler()
	for i := 0; i < maxFailures; i++ {
		post(t, h, "sudhanshu", "guess", "application/json", `{"message":"x"}`)
	}
	if rec := post(t, h, "sudhanshu", "correct-horse", "application/json", `{"message":"x"}`); rec.Code != http.StatusTooManyRequests {
		t.Fatalf("expected lockout, got %d", rec.Code)
	}
	now = now.Add(failureWindow + time.Second)
	if rec := post(t, h, "sudhanshu", "correct-horse", "application/json", `{"message":"x"}`); rec.Code != http.StatusOK {
		t.Fatalf("expected the lockout to end, got %d", rec.Code)
	}
}

func TestMessageValidation(t *testing.T) {
	h := NewServer("sudhanshu", "correct-horse", "t", &fakeSender{}).Handler()
	cases := map[string]string{
		"empty":    `{"message":"   "}`,
		"too long": `{"message":"` + strings.Repeat("a", maxMessageRunes+1) + `"}`,
		"not json": `message=hi`,
	}
	for name, body := range cases {
		if rec := post(t, h, "sudhanshu", "correct-horse", "application/json", body); rec.Code != http.StatusBadRequest {
			t.Errorf("%s: status %d", name, rec.Code)
		}
	}
	if rec := post(t, h, "sudhanshu", "correct-horse", "application/json", `{"message":"`+strings.Repeat("क", maxMessageRunes)+`"}`); rec.Code != http.StatusOK {
		t.Errorf("1000 Hindi characters should be fine, got %d", rec.Code)
	}
}

func TestFirebaseFailureIsReported(t *testing.T) {
	h := NewServer("sudhanshu", "correct-horse", "t", &fakeSender{err: errors.New("down")}).Handler()
	if rec := post(t, h, "sudhanshu", "correct-horse", "application/json", `{"message":"x"}`); rec.Code != http.StatusBadGateway {
		t.Fatalf("status %d", rec.Code)
	}
}

func TestIndexPageAndHealth(t *testing.T) {
	h := NewServer("u", "password1", "t", &fakeSender{}).Handler()
	for path, want := range map[string]string{"/": "Argus live scam alert", "/healthz": "ok"} {
		rec := httptest.NewRecorder()
		h.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, path, nil))
		if rec.Code != http.StatusOK || !strings.Contains(rec.Body.String(), want) {
			t.Errorf("%s: %d %q", path, rec.Code, rec.Body.String()[:min(80, rec.Body.Len())])
		}
	}
}

// FCM against fake Google endpoints: the JWT is signed by the service account's key,
// the token is cached, and the message is data-only and high priority.
func TestFCMSendsSignedRequests(t *testing.T) {
	key, _ := rsa.GenerateKey(rand.Reader, 2048)
	der, _ := x509.MarshalPKCS8PrivateKey(key)
	pemKey := string(pem.EncodeToMemory(&pem.Block{Type: "PRIVATE KEY", Bytes: der}))

	tokenCalls := 0
	var sent map[string]any
	mux := http.NewServeMux()
	mux.HandleFunc("POST /token", func(w http.ResponseWriter, r *http.Request) {
		tokenCalls++
		_ = r.ParseForm()
		parts := strings.Split(r.Form.Get("assertion"), ".")
		if len(parts) != 3 {
			t.Errorf("assertion %q", r.Form.Get("assertion"))
			return
		}
		sig, _ := base64.RawURLEncoding.DecodeString(parts[2])
		sum := sha256.Sum256([]byte(parts[0] + "." + parts[1]))
		if err := rsa.VerifyPKCS1v15(&key.PublicKey, crypto.SHA256, sum[:], sig); err != nil {
			t.Errorf("bad JWT signature: %v", err)
		}
		claims, _ := base64.RawURLEncoding.DecodeString(parts[1])
		if !strings.Contains(string(claims), fcmScope) || !strings.Contains(string(claims), "argus@p.iam.gserviceaccount.com") {
			t.Errorf("claims %s", claims)
		}
		_, _ = w.Write([]byte(`{"access_token":"tok-1","expires_in":3600}`))
	})
	mux.HandleFunc("POST /v1/projects/p/messages:send", func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("Authorization") != "Bearer tok-1" {
			t.Errorf("auth header %q", r.Header.Get("Authorization"))
		}
		body, _ := io.ReadAll(r.Body)
		_ = json.Unmarshal(body, &sent)
		_, _ = w.Write([]byte(`{"name":"projects/p/messages/42"}`))
	})
	google := httptest.NewServer(mux)
	defer google.Close()

	sa, _ := json.Marshal(ServiceAccount{ProjectID: "p", ClientEmail: "argus@p.iam.gserviceaccount.com", PrivateKey: pemKey, TokenURI: google.URL + "/token"})
	fcm, err := NewFCM(sa, google.Client())
	if err != nil {
		t.Fatal(err)
	}
	fcm.endpoint = google.URL
	for i := 0; i < 2; i++ {
		name, err := fcm.SendToTopic(context.Background(), "argus-alerts", map[string]string{"type": "live", "id": "a1", "message": "scam"})
		if err != nil || name != "projects/p/messages/42" {
			t.Fatalf("send: %q %v", name, err)
		}
	}
	if tokenCalls != 1 {
		t.Errorf("token fetched %d times, want 1 (cached)", tokenCalls)
	}
	msg := sent["message"].(map[string]any)
	if msg["topic"] != "argus-alerts" || msg["notification"] != nil {
		t.Errorf("message %v: must be a data-only topic message", msg)
	}
	if msg["android"].(map[string]any)["priority"] != "HIGH" {
		t.Errorf("android %v: must be high priority", msg["android"])
	}
	if msg["data"].(map[string]any)["message"] != "scam" {
		t.Errorf("data %v", msg["data"])
	}
}

func TestBadServiceAccountsAreRejected(t *testing.T) {
	for _, j := range []string{`not json`, `{}`, `{"project_id":"p","client_email":"e","private_key":"not pem"}`} {
		if _, err := NewFCM([]byte(j), nil); err == nil {
			t.Errorf("%s: expected an error", j)
		}
	}
}
