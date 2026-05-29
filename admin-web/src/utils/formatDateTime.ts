/**
 * Format a datetime string for display.
 * Handles both ISO format (2026-05-09T09:00:00) and standard format (2026-05-09 09:00:00).
 * Returns 'YYYY-MM-DD HH:mm' format.
 * Empty/null/undefined values return '-'.
 * Invalid dates return the original string to avoid showing 'Invalid Date'.
 */
export function formatDateTime(dt?: string | null): string {
  if (!dt) return '-';
  // Replace T with space and take first 16 chars (YYYY-MM-DD HH:mm)
  const cleaned = dt.replace('T', ' ');
  if (cleaned.length >= 16) {
    return cleaned.substring(0, 16);
  }
  // Short string (date only or partial) — return as-is
  return cleaned;
}

/**
 * Format a date-only string for display.
 * Returns 'YYYY-MM-DD' format.
 * Empty/null/undefined values return '-'.
 */
export function formatDate(dt?: string | null): string {
  if (!dt) return '-';
  const cleaned = dt.replace('T', ' ');
  if (cleaned.length >= 10) {
    return cleaned.substring(0, 10);
  }
  return cleaned;
}
