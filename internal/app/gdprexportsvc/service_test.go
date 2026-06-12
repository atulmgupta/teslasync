package gdprexportsvc

import (
	"context"
	"errors"
	"io"
	"testing"
	"time"

	dbgdpr "github.com/ev-dev-labs/teslasync/internal/database/gdpr"
)

// fakeStorage implements external.StorageProvider. Only GetSignedURL is
// exercised; the other methods satisfy the interface.
type fakeStorage struct {
	gotKey    string
	gotExpiry time.Duration
	url       string
	err       error
}

func (f *fakeStorage) Upload(_ context.Context, _ string, _ io.Reader) (string, error) {
	return "", nil
}

func (f *fakeStorage) GetSignedURL(_ context.Context, key string, expiry time.Duration) (string, error) {
	f.gotKey = key
	f.gotExpiry = expiry
	return f.url, f.err
}

func (f *fakeStorage) Delete(_ context.Context, _ string) error { return nil }

func TestObjectKeyFromStoragePath(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name string
		path string
		want string
	}{
		{"s3 nested key", "s3://my-bucket/gdpr/2024/abc.tar.gz", "gdpr/2024/abc.tar.gz"},
		{"s3 single segment key", "s3://my-bucket/abc.tar.gz", "abc.tar.gz"},
		{"s3 bucket only is unfetchable", "s3://my-bucket", ""},
		{"bare key returned unchanged", "gdpr/abc.tar.gz", "gdpr/abc.tar.gz"},
		{"leading slash stripped", "/gdpr/abc.tar.gz", "gdpr/abc.tar.gz"},
		{"empty path", "", ""},
	}
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			if got := objectKeyFromStoragePath(tc.path); got != tc.want {
				t.Errorf("objectKeyFromStoragePath(%q) = %q, want %q", tc.path, got, tc.want)
			}
		})
	}
}

func TestSignedDownloadURL_NotConfiguredWhenRepoNil(t *testing.T) {
	t.Parallel()
	svc := New(nil, &fakeStorage{})
	_, err := svc.SignedDownloadURL(context.Background(), &Artifact{ID: "x"})
	if !errors.Is(err, ErrNotConfigured) {
		t.Fatalf("want ErrNotConfigured, got %v", err)
	}
}

func TestSignedDownloadURL_StorageUnavailableWhenProviderNil(t *testing.T) {
	t.Parallel()
	svc := New(&dbgdpr.ArtifactRepo{}, nil)
	_, err := svc.SignedDownloadURL(context.Background(), &Artifact{ID: "x"})
	if !errors.Is(err, ErrStorageUnavailable) {
		t.Fatalf("want ErrStorageUnavailable, got %v", err)
	}
}

func TestSignedDownloadURL_NotFoundWhenArtifactNil(t *testing.T) {
	t.Parallel()
	svc := New(&dbgdpr.ArtifactRepo{}, &fakeStorage{})
	_, err := svc.SignedDownloadURL(context.Background(), nil)
	if !errors.Is(err, ErrNotFound) {
		t.Fatalf("want ErrNotFound, got %v", err)
	}
}

func TestSignedDownloadURL_UnparseablePath(t *testing.T) {
	t.Parallel()
	svc := New(&dbgdpr.ArtifactRepo{}, &fakeStorage{})
	a := &Artifact{ID: "abc", StorageKind: StorageKindS3, StoragePath: "s3://bucket-only"}
	if _, err := svc.SignedDownloadURL(context.Background(), a); err == nil {
		t.Fatal("want error for unparseable storage path, got nil")
	}
}

func TestSignedDownloadURL_HappyPath(t *testing.T) {
	t.Parallel()
	fs := &fakeStorage{url: "https://signed.example/abc"}
	svc := New(&dbgdpr.ArtifactRepo{}, fs)
	a := &Artifact{ID: "abc", StorageKind: StorageKindS3, StoragePath: "s3://bucket/gdpr/abc.tar.gz"}

	url, err := svc.SignedDownloadURL(context.Background(), a)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if url != "https://signed.example/abc" {
		t.Errorf("url = %q, want signed url", url)
	}
	if fs.gotKey != "gdpr/abc.tar.gz" {
		t.Errorf("signed key = %q, want bucket stripped to object key", fs.gotKey)
	}
	if fs.gotExpiry != signedURLTTL {
		t.Errorf("expiry = %v, want %v", fs.gotExpiry, signedURLTTL)
	}
}

func TestSignedDownloadURL_PropagatesProviderError(t *testing.T) {
	t.Parallel()
	fs := &fakeStorage{err: errors.New("s3 down")}
	svc := New(&dbgdpr.ArtifactRepo{}, fs)
	a := &Artifact{ID: "abc", StorageKind: StorageKindS3, StoragePath: "s3://bucket/abc.tar.gz"}
	if _, err := svc.SignedDownloadURL(context.Background(), a); err == nil {
		t.Fatal("want wrapped provider error, got nil")
	}
}
