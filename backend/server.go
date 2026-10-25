package main

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	_ "embed"
	"encoding/hex"
	"encoding/json"
	"log"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"
	"unicode/utf8"
)

// Sender delivers one data message to every phone subscribed to topic.
type Sender interface {
	SendToTopic(ctx context.Context, topic string, data map[string]string) (string, error)
}

// Server is the Argus live-alert server: the admin posts a scam message, and every Argus
// Online and Argus Demo phone shows it as a red full-screen warning within seconds.
type Server struct {
	username string
	password string
	topic    string
	sender   Sender
	now      func() time.Time

	mu       sync.Mutex
	failures map[string][]time.Time // failed logins per client IP
}

const (
	maxMessageRunes = 1000
	maxFailures     = 5
	failureWindow   = 10 * time.Minute
)

//go:embed index.html
var indexHTML []byte

func NewServer(username, password, topic string, sender Sender) *Server {
	return &Server{username: username, password: password, topic: topic, sender: sender, now: time.Now, failures: map[string][]time.Time{}}
}

func (s *Server) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /{$}", s.index)
	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, _ *http.Request) { _, _ = w.Write([]byte("ok")) })
	mux.HandleFunc("POST /api/alerts", s.sendAlert)
	return securityHeaders(mux)
}

func securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		h := w.Header()
		h.Set("X-Content-Type-Options", "nosniff")
		h.Set("X-Frame-Options", "DENY")
		h.Set("Referrer-Policy", "no-referrer")
		h.Set("Content-Security-Policy", "default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; form-action 'self'")
		next.ServeHTTP(w, r)
	})
}

func (s *Server) index(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	_, _ = w.Write(indexHTML)
}

type alertRequest struct {
	Message string `json:"message"`
}

type alertResponse struct {
	OK    bool   `json:"ok"`
	ID    string `json:"id,omitempty"`
	Error string `json:"error,omitempty"`
}

func (s *Server) sendAlert(w http.ResponseWriter, r *http.Request) {
	ip := clientIP(r)
	if s.lockedOut(ip) {
		writeJSON(w, http.StatusTooManyRequests, alertResponse{Error: "too many wrong passwords; try again in 10 minutes"})
		return
	}
	user, pass, ok := r.BasicAuth()
	if !ok || !s.checkLogin(user, pass) {
		s.recordFailure(ip)
		w.Header().Set("WWW-Authenticate", `Basic realm="Argus", charset="UTF-8"`)
		writeJSON(w, http.StatusUnauthorized, alertResponse{Error: "wrong username or password"})
		return
	}
	s.clearFailures(ip)

	r.Body = http.MaxBytesReader(w, r.Body, 16<<10)
	var req alertRequest
	if strings.HasPrefix(r.Header.Get("Content-Type"), "application/json") {
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			writeJSON(w, http.StatusBadRequest, alertResponse{Error: "body must be JSON like {\"message\": \"...\"}"})
			return
		}
	} else {
		req.Message = r.FormValue("message")
	}
	message := strings.TrimSpace(req.Message)
	if message == "" {
		writeJSON(w, http.StatusBadRequest, alertResponse{Error: "message is empty"})
		return
	}
	if !utf8.ValidString(message) || utf8.RuneCountInString(message) > maxMessageRunes {
		writeJSON(w, http.StatusBadRequest, alertResponse{Error: "message must be valid text of at most 1000 characters"})
		return
	}

	id := newID()
	ctx, cancel := context.WithTimeout(r.Context(), 20*time.Second)
	defer cancel()
	if _, err := s.sender.SendToTopic(ctx, s.topic, map[string]string{"type": "live", "id": id, "message": message}); err != nil {
		log.Printf("alert %s: send failed: %v", id, err)
		writeJSON(w, http.StatusBadGateway, alertResponse{Error: "could not reach Firebase; see the server log"})
		return
	}
	log.Printf("alert %s: sent to topic %s (%d characters)", id, s.topic, utf8.RuneCountInString(message))
	writeJSON(w, http.StatusOK, alertResponse{OK: true, ID: id})
}

// checkLogin compares in constant time, so the reply time doesn't leak the password.
func (s *Server) checkLogin(user, pass string) bool {
	u1, u2 := sha256.Sum256([]byte(user)), sha256.Sum256([]byte(s.username))
	p1, p2 := sha256.Sum256([]byte(pass)), sha256.Sum256([]byte(s.password))
	userOK := subtle.ConstantTimeCompare(u1[:], u2[:]) == 1
	passOK := subtle.ConstantTimeCompare(p1[:], p2[:]) == 1
	return userOK && passOK
}

func (s *Server) lockedOut(ip string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	cutoff := s.now().Add(-failureWindow)
	kept := s.failures[ip][:0]
	for _, t := range s.failures[ip] {
		if t.After(cutoff) {
			kept = append(kept, t)
		}
	}
	if len(kept) == 0 {
		delete(s.failures, ip)
		return false
	}
	s.failures[ip] = kept
	return len(kept) >= maxFailures
}

func (s *Server) recordFailure(ip string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.failures[ip] = append(s.failures[ip], s.now())
}

func (s *Server) clearFailures(ip string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.failures, ip)
}

func clientIP(r *http.Request) string {
	// Behind Cloud Run, Render or Fly the first X-Forwarded-For entry is the client
	if fwd := r.Header.Get("X-Forwarded-For"); fwd != "" {
		return strings.TrimSpace(strings.Split(fwd, ",")[0])
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}

func newID() string {
	b := make([]byte, 8)
	_, _ = rand.Read(b)
	return hex.EncodeToString(b)
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
