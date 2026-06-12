package queries

import (
	"strings"
	"testing"
)

// These pure-Go tests pin the SQL constants in the postgres adapter
// query package. The hexagonal repositories (internal/adapter/postgres)
// hold a concrete *pgxpool.Pool, so the repository methods themselves
// need a live PostgreSQL instance to exercise; there is no
// pgxmock/testcontainers harness in this package. The highest-value,
// no-DB guard is to pin the SQL itself: column names, table names,
// filters, ordering, and parameter placeholders. A typo or a drift
// against the SI-canonical schema (migrations 000184/000185) would
// otherwise only surface at runtime as a scan error or — worse — a
// silently wrong column. Each test below asserts both the fragments
// that MUST be present and the legacy fragments that MUST NOT
// reappear.

// assertContains fails when any wanted fragment is missing from sql.
func assertContains(t *testing.T, name, sql string, want ...string) {
	t.Helper()
	for _, frag := range want {
		if !strings.Contains(sql, frag) {
			t.Errorf("%s missing %q\nfull SQL:\n%s", name, frag, sql)
		}
	}
}

// assertAbsent fails when any forbidden fragment is present in sql.
func assertAbsent(t *testing.T, name, sql string, forbidden ...string) {
	t.Helper()
	for _, frag := range forbidden {
		if strings.Contains(sql, frag) {
			t.Errorf("%s must not contain %q (schema drift / legacy column)\nfull SQL:\n%s", name, frag, sql)
		}
	}
}

func TestUserQueries_Shape(t *testing.T) {
	t.Parallel()
	cols := []string{
		"id", "email", "display_name", "avatar_url",
		"tesla_token_encrypted", "tesla_refresh_token_encrypted",
		"token_expires_at", "created_at", "updated_at",
	}
	assertContains(t, "GetUserByID", GetUserByID, append([]string{"FROM users", "WHERE id = $1"}, cols...)...)
	assertContains(t, "GetUserByEmail", GetUserByEmail, "FROM users", "WHERE email = $1")
	assertContains(t, "UpsertUser", UpsertUser,
		"INSERT INTO users", "ON CONFLICT (id) DO UPDATE SET", "$9")
	assertAbsent(t, "UpsertUser", UpsertUser, "$10")
	assertContains(t, "DeleteUser", DeleteUser, "DELETE FROM users WHERE id = $1")
}

func TestVehicleQueries_Shape(t *testing.T) {
	t.Parallel()
	// odometer_miles / range_miles are grandfathered legacy-named
	// columns on the vehicles table (NOT SI). Pin them so an
	// accidental rename is caught — they are the real column names.
	cols := []string{
		"id", "user_id", "vin", "display_name", "model", "year", "color",
		"fsm_state", "sub_fsm_state", "odometer_miles", "battery_level",
		"range_miles", "is_charging", "latitude", "longitude",
		"created_at", "updated_at",
	}
	assertContains(t, "GetVehicleByID", GetVehicleByID, append([]string{"FROM vehicles", "WHERE id = $1"}, cols...)...)
	assertContains(t, "GetVehicleByVIN", GetVehicleByVIN, "FROM vehicles", "WHERE vin = $1")
	assertContains(t, "GetVehiclesByUserID", GetVehiclesByUserID,
		"WHERE user_id = $1", "ORDER BY display_name")
	assertContains(t, "GetVehicleByIDForUpdate", GetVehicleByIDForUpdate, "FOR UPDATE")
	assertContains(t, "UpsertVehicle", UpsertVehicle,
		"INSERT INTO vehicles", "ON CONFLICT (id) DO UPDATE SET", "$17")
	assertContains(t, "DeleteVehicle", DeleteVehicle, "DELETE FROM vehicles WHERE id = $1")
}

func TestChargingQueries_Shape(t *testing.T) {
	t.Parallel()
	// Migration 000184 made charging_sessions SI-canonical.
	siCols := []string{
		"FROM charging_sessions",
		"total_energy_added_wh",
		"peak_power_w",
		"start_soc_pct",
		"end_soc_pct",
		"cost_decimal",
		"energy_added_wh", // SELECT alias
		"max_power_w",     // SELECT alias
	}
	assertContains(t, "GetChargingSessionByID", GetChargingSessionByID, append([]string{"WHERE id = $1::bigint"}, siCols...)...)
	assertContains(t, "GetChargingSessionsByVehicleID", GetChargingSessionsByVehicleID,
		"WHERE vehicle_id = $1::bigint", "ORDER BY started_at DESC")
	assertContains(t, "ListChargingSessionsByDateRange", ListChargingSessionsByDateRange,
		"started_at >= $2", "started_at <= $3", "ORDER BY started_at DESC")
	assertContains(t, "GetChargingSessionByIDForUpdate", GetChargingSessionByIDForUpdate, "FOR UPDATE")
	assertContains(t, "UpsertChargingSession", UpsertChargingSession,
		"INSERT INTO charging_sessions", "ON CONFLICT (id) DO UPDATE SET",
		"total_energy_added_wh = EXCLUDED.total_energy_added_wh",
		"peak_power_w = EXCLUDED.peak_power_w", "$10")
	// Legacy non-SI column names dropped by migration 000184 must not return.
	assertAbsent(t, "GetChargingSessionByID", GetChargingSessionByID,
		"energy_added_kwh", "total_energy_added_kwh", "peak_power_kw", "power_kw")
}

func TestTripQueries_Shape(t *testing.T) {
	t.Parallel()
	// Trips aggregate SI-canonical drive columns (migration 000185).
	siCols := []string{
		"FROM trips t",
		"distance_m",
		"energy_used_wh",
		"max_speed_mps",
		"efficiency_wh_per_m",
		"JOIN drives d ON d.id = td.drive_id",
	}
	assertContains(t, "GetTripByID", GetTripByID, append([]string{"WHERE t.id = $1::bigint"}, siCols...)...)
	assertContains(t, "GetTripsByVehicleID", GetTripsByVehicleID,
		"WHERE t.vehicle_id = $1::bigint", "ORDER BY t.started_at DESC")
	assertContains(t, "ListTripsByDateRange", ListTripsByDateRange,
		"t.started_at >= $2", "t.started_at <= $3")
	assertContains(t, "GetTripByIDForUpdate", GetTripByIDForUpdate, "FOR UPDATE OF t")
	assertContains(t, "UpsertTrip", UpsertTrip,
		"INSERT INTO trips", "ON CONFLICT (id) DO UPDATE SET")
	// Legacy imperial / kWh column names dropped by migration 000185.
	assertAbsent(t, "GetTripByID", GetTripByID,
		"distance_km", "distance_mi", "max_speed_mph", "energy_used_kwh", "efficiency_wh_per_km")
}

func TestExportQueries_Shape(t *testing.T) {
	t.Parallel()
	cols := []string{
		"id", "user_id", "format", "vehicle_id", "date_from", "date_to",
		"fsm_state", "file_path", "file_size", "failed_reason", "created_at", "completed_at",
	}
	assertContains(t, "GetExportJobByID", GetExportJobByID, append([]string{"FROM export_jobs", "WHERE id = $1"}, cols...)...)
	assertContains(t, "GetExportJobsByUserID", GetExportJobsByUserID,
		"WHERE user_id = $1", "ORDER BY created_at DESC")
	assertContains(t, "GetExportJobByIDForUpdate", GetExportJobByIDForUpdate, "FOR UPDATE")
	assertContains(t, "UpsertExportJob", UpsertExportJob,
		"INSERT INTO export_jobs", "ON CONFLICT (id) DO UPDATE SET", "$12")
}

func TestNotificationQueries_Shape(t *testing.T) {
	t.Parallel()
	cols := []string{
		"id", "user_id", "type", "title", "body", "fsm_state", "channel",
		"failed_reason", "retry_count", "created_at", "sent_at",
	}
	assertContains(t, "GetNotificationByID", GetNotificationByID, append([]string{"FROM notifications", "WHERE id = $1"}, cols...)...)
	assertContains(t, "GetNotificationsByUserID", GetNotificationsByUserID,
		"WHERE user_id = $1", "ORDER BY created_at DESC")
	assertContains(t, "GetPendingNotifications", GetPendingNotifications,
		"WHERE fsm_state = 'pending'", "ORDER BY created_at ASC", "LIMIT $1")
	assertContains(t, "GetNotificationByIDForUpdate", GetNotificationByIDForUpdate, "FOR UPDATE")
	assertContains(t, "UpsertNotification", UpsertNotification,
		"INSERT INTO notifications", "ON CONFLICT (id) DO UPDATE SET", "$11")
}

func TestFSMHistoryQueries_Shape(t *testing.T) {
	t.Parallel()
	cols := []string{"id", "entity_id", "fsm_name", "from_state", "event", "to_state", "created_at"}
	assertContains(t, "InsertFSMTransition", InsertFSMTransition,
		append([]string{"INSERT INTO fsm_transitions", "$7"}, cols...)...)
	assertContains(t, "GetFSMHistory", GetFSMHistory,
		"FROM fsm_transitions", "WHERE entity_id = $1", "ORDER BY created_at DESC", "LIMIT $2")
	assertContains(t, "GetFSMHistoryByEntityID", GetFSMHistoryByEntityID,
		"WHERE entity_id = $1", "ORDER BY created_at ASC")
}
