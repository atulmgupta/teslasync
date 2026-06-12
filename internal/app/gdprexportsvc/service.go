// Package gdprexportsvc orchestrates the GDPR data-subject export
// surface used by internal/handler/v1/gdpr_export_handler.go.
//
// The actual export bundle creation happens in the export-worker
// reading export_jobs rows. This service only exposes the manifest
// + download path so the HTTP handler stays thin (per ADR-009
// TestHandlerV1Thinness) and the worker stays decoupled from any
// HTTP-layer types.
package gdprexportsvc

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	dbgdpr "github.com/ev-dev-labs/teslasync/internal/database/gdpr"
	"github.com/ev-dev-labs/teslasync/internal/port/external"
)

// ErrNotConfigured is returned when the backing repo is nil
// (subsystem disabled on this deployment).
var ErrNotConfigured = errors.New("gdpr export subsystem not configured on this deployment")

// ErrNotFound is returned when an artifact id is unknown.
var ErrNotFound = errors.New("export not found")

// ErrStorageUnavailable is returned when an artifact lives in an
// object store but no StorageProvider was wired on this deployment.
// Distinct from ErrNotConfigured (which means the whole subsystem is
// off): the artifact exists, but this node cannot serve it.
var ErrStorageUnavailable = errors.New("object storage backend not configured for this artifact")

// signedURLTTL bounds the lifetime of a presigned object-store
// download link. Kept short: the link is handed straight to the
// browser via a 302 and consumed immediately.
const signedURLTTL = 5 * time.Minute

// Artifact is the wire shape returned by Get + recorded by Insert.
type Artifact = dbgdpr.Artifact

// StorageKind enumerates the supported backends.
type StorageKind = dbgdpr.StorageKind

// StorageKind constants re-exported for the handler.
const (
	StorageKindLocalFS = dbgdpr.StorageKindLocalFS
	StorageKindS3      = dbgdpr.StorageKindS3
)

// Service is the orchestrator. Holds a pointer so the App can wire
// once and pass nil for any subsystem not configured.
type Service struct {
	repo    *dbgdpr.ArtifactRepo
	storage external.StorageProvider
}

// New constructs the service. repo MAY be nil; methods then return
// ErrNotConfigured. storage MAY be nil; S3-backed artifacts then
// return ErrStorageUnavailable from SignedDownloadURL while local-fs
// artifacts keep working.
func New(repo *dbgdpr.ArtifactRepo, storage external.StorageProvider) *Service {
	return &Service{repo: repo, storage: storage}
}

// Get fetches the manifest by ID. Returns ErrNotConfigured when the
// repo is nil and ErrNotFound when the id is unknown.
func (s *Service) Get(ctx context.Context, id string) (*Artifact, error) {
	if s == nil || s.repo == nil {
		return nil, ErrNotConfigured
	}
	a, err := s.repo.GetByID(ctx, id)
	if err != nil {
		return nil, err
	}
	if a == nil {
		return nil, ErrNotFound
	}
	return a, nil
}

// RecordDownload bumps the download counter + audit row. Best-effort:
// the caller already streamed bytes successfully when this is invoked.
func (s *Service) RecordDownload(ctx context.Context, id string) error {
	if s == nil || s.repo == nil {
		return ErrNotConfigured
	}
	return s.repo.RecordDownload(ctx, id)
}

// SignedDownloadURL returns a short-lived presigned URL for an
// object-store-backed artifact so the HTTP handler can 302 the
// browser straight at the bucket instead of proxying potentially
// large GDPR bundles through the API process.
//
// Returns ErrStorageUnavailable when no StorageProvider is wired.
// Callers MUST only invoke this for StorageKindS3 artifacts;
// local-fs artifacts are streamed from disk by the handler.
func (s *Service) SignedDownloadURL(ctx context.Context, a *Artifact) (string, error) {
	if s == nil || s.repo == nil {
		return "", ErrNotConfigured
	}
	if s.storage == nil {
		return "", ErrStorageUnavailable
	}
	if a == nil {
		return "", ErrNotFound
	}
	key := objectKeyFromStoragePath(a.StoragePath)
	if key == "" {
		return "", fmt.Errorf("artifact %s: unparseable storage path %q", a.ID, a.StoragePath)
	}
	url, err := s.storage.GetSignedURL(ctx, key, signedURLTTL)
	if err != nil {
		return "", fmt.Errorf("signing download url for artifact %s: %w", a.ID, err)
	}
	return url, nil
}

// objectKeyFromStoragePath extracts the object key from a storage
// path. The export-worker writes paths in the canonical
// "s3://bucket/key/with/slashes" form; the bucket is owned by the
// configured StorageProvider so only the key portion is returned.
// A bare key (no scheme) is returned unchanged so future callers that
// already persist keys keep working.
func objectKeyFromStoragePath(path string) string {
	const scheme = "s3://"
	if !strings.HasPrefix(path, scheme) {
		return strings.TrimPrefix(path, "/")
	}
	rest := path[len(scheme):]
	slash := strings.IndexByte(rest, '/')
	if slash < 0 {
		// "s3://bucket" with no key — nothing to fetch.
		return ""
	}
	return rest[slash+1:]
}
